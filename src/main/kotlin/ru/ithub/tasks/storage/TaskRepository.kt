package ru.ithub.tasks.storage

import ru.ithub.tasks.model.BadRequestException
import ru.ithub.tasks.model.ConflictException
import ru.ithub.tasks.model.CreateTaskRequest
import ru.ithub.tasks.model.Priority
import ru.ithub.tasks.model.Task
import ru.ithub.tasks.model.UpdateTaskRequest

/** Потокобезопасное хранилище задач в памяти процесса. */
class TaskRepository(seed: List<Task> = emptyList()) {
    private val tasks = seed.toMutableList()
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0) + 1

    fun list(): List<Task> = synchronized(tasks) {
        tasks.sortedBy { it.id }
    }

    fun find(id: Int): Task? = synchronized(tasks) {
        tasks.find { it.id == id }
    }

    fun create(request: CreateTaskRequest): Task = synchronized(tasks) {
        val title = normalizeTitle(request.title)
        val task = Task(
            id = nextId++,
            title = title,
            done = request.done,
            priority = request.priority,
        )
        tasks += task
        task
    }

    fun update(id: Int, request: UpdateTaskRequest): Task? = synchronized(tasks) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) {
            return@synchronized null
        }
        val updated = tasks[index].copy(
            title = normalizeTitle(request.title, ignoreId = id),
            done = request.done,
            priority = request.priority,
        )
        tasks[index] = updated
        updated
    }

    fun delete(id: Int): Boolean = synchronized(tasks) {
        tasks.removeAll { it.id == id }
    }

    private fun normalizeTitle(raw: String, ignoreId: Int? = null): String {
        val title = raw.trim()
        if (title.isEmpty()) {
            throw BadRequestException("Название задачи не может быть пустым")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            throw BadRequestException("Название задачи не длиннее $MAX_TITLE_LENGTH символов")
        }
        val duplicate = tasks.any { task ->
            task.id != ignoreId && task.title.equals(title, ignoreCase = true)
        }
        if (duplicate) {
            throw ConflictException("Задача с названием «$title» уже существует")
        }
        return title
    }

    companion object {
        const val MAX_TITLE_LENGTH = 200

        fun withSamples(): TaskRepository = TaskRepository(
            listOf(
                Task(id = 1, title = "Изучить маршруты Ktor", done = false, priority = Priority.HIGH),
                Task(id = 2, title = "Подключить JSON-сериализацию", done = true, priority = Priority.MEDIUM),
                Task(id = 3, title = "Написать README", done = false, priority = Priority.LOW),
            ),
        )
    }
}
