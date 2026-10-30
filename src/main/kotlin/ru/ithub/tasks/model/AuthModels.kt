package ru.ithub.tasks.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
)

@Serializable
data class UserResponse(
    val id: Int,
    val login: String,
)

@Serializable
data class TokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val userId: Int,
)

data class User(
    val id: Int,
    val login: String,
    val passwordHash: String,
) {
    override fun toString(): String = "User(id=$id, login=$login)"
}
