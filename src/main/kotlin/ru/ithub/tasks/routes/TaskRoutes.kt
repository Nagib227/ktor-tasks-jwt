package ru.ithub.tasks.routes

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ru.ithub.tasks.model.BadRequestException
import ru.ithub.tasks.model.CreateTaskRequest
import ru.ithub.tasks.model.NotFoundException
import ru.ithub.tasks.model.ServiceInfo
import ru.ithub.tasks.model.UpdateTaskRequest
import ru.ithub.tasks.security.AuthService
import ru.ithub.tasks.storage.TaskRepository

fun Application.configureRouting(tasks: TaskRepository, auth: AuthService) {
    routing {
        get("/") {
            call.respond(
                ServiceInfo(
                    name = "ktor-tasks-jwt",
                    description = "CRUD задач и JWT-аутентификация",
                ),
            )
        }
        post("/register") {
            val created = auth.register(call.receive())
            call.respond(HttpStatusCode.Created, created)
        }
        post("/login") {
            call.respond(auth.login(call.receive()))
        }
        route("/tasks") {
            get {
                call.respond(tasks.list())
            }
            get("/{id}") {
                val id = call.requireTaskId()
                val task = tasks.find(id) ?: throw NotFoundException("Задача с id=$id не найдена")
                call.respond(task)
            }
            authenticate("auth-jwt") {
                post {
                    val created = tasks.create(call.receive<CreateTaskRequest>())
                    call.response.header(HttpHeaders.Location, "/tasks/${created.id}")
                    call.respond(HttpStatusCode.Created, created)
                }
                put("/{id}") {
                    val id = call.requireTaskId()
                    val updated = tasks.update(id, call.receive<UpdateTaskRequest>())
                        ?: throw NotFoundException("Задача с id=$id не найдена")
                    call.respond(updated)
                }
                delete("/{id}") {
                    val id = call.requireTaskId()
                    if (!tasks.delete(id)) {
                        throw NotFoundException("Задача с id=$id не найдена")
                    }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun ApplicationCall.requireTaskId(): Int {
    val id = parameters["id"]?.toIntOrNull()
    if (id == null || id <= 0) {
        throw BadRequestException("Параметр path id должен быть положительным целым числом")
    }
    return id
}
