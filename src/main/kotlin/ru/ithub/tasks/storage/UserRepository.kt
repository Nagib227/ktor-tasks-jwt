package ru.ithub.tasks.storage

import ru.ithub.tasks.model.ConflictException
import ru.ithub.tasks.model.User

/** Пользователи в памяти. Сохраняется только хэш пароля. */
class UserRepository {
    private val users = mutableListOf<User>()
    private var nextId = 1

    fun findById(id: Int): User? = synchronized(users) {
        users.find { it.id == id }
    }

    fun findByLogin(login: String): User? = synchronized(users) {
        users.find { it.login == login }
    }

    fun create(login: String, passwordHash: String): User = synchronized(users) {
        if (users.any { it.login == login }) {
            throw ConflictException("Пользователь с логином «$login» уже существует")
        }
        val user = User(id = nextId++, login = login, passwordHash = passwordHash)
        users += user
        user
    }
}
