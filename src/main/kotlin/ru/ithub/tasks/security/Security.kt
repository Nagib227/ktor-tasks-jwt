package ru.ithub.tasks.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import ru.ithub.tasks.model.ErrorResponse
import ru.ithub.tasks.storage.UserRepository

fun Application.configureAuthentication(jwt: JwtService, users: UserRepository) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwt.settings.realm
            verifier(jwt.verifier())
            validate { credential ->
                val userId = runCatching { credential.payload.getClaim("userId").asInt() }.getOrNull()
                if (userId != null && users.findById(userId) != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        error = "unauthorized",
                        message = "Нужен действительный токен в заголовке Authorization: Bearer",
                    ),
                )
            }
        }
    }
}
