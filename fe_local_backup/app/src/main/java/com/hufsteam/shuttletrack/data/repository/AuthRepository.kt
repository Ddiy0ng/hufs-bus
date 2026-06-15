package com.hufsteam.shuttletrack.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.data.remote.ApiService
import com.hufsteam.shuttletrack.data.remote.RetrofitClient
import com.hufsteam.shuttletrack.data.remote.TokenStore
import com.hufsteam.shuttletrack.data.remote.dto.LoginRequest
import com.hufsteam.shuttletrack.data.remote.dto.SignupRequest
import retrofit2.HttpException

data class AuthSession(
    val email: String,
    val role: UserRole,
    val accessToken: String?
)

class AuthRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    suspend fun login(email: String, password: String): Result<AuthSession> = runCatching {
        val response = callOrThrowServerMessage {
            apiService.login(LoginRequest(email = email, password = password))
        }
        val payload = response.payloadObject()
        val token = payload.findString("accessToken", "token", "jwt", "access_token")
            ?: throw IllegalStateException("로그인 응답에 accessToken이 없습니다. 백엔드 응답 필드명을 확인해 주세요.")
        val role = payload.findString("role", "userRole", "authority", "type")
            ?: payload.findObject("user")?.findString("role", "userRole", "authority", "type")
        val responseEmail = payload.findString("email")
            ?: payload.findObject("user")?.findString("email")
            ?: email

        val resolvedRole = role.toUserRole()
        TokenStore.saveSession(token, responseEmail, resolvedRole.name)
        AuthSession(
            email = responseEmail,
            role = resolvedRole,
            accessToken = token
        )
    }

    suspend fun signup(
        email: String,
        password: String,
        privacyTermAgree: Boolean,
        serviceTermAgree: Boolean
    ): Result<AuthSession> = runCatching {
        val response = callOrThrowServerMessage {
            apiService.signup(
                SignupRequest(
                    email = email,
                    password = password,
                    privacyTermAgree = privacyTermAgree,
                    serviceTermAgree = serviceTermAgree
                )
            )
        }
        val payload = response.payloadObject()
        val token = payload.findString("accessToken", "token", "jwt", "access_token")
        val role = payload.findString("role", "userRole", "authority", "type")
            ?: payload.findObject("user")?.findString("role", "userRole", "authority", "type")
        val responseEmail = payload.findString("email")
            ?: payload.findObject("user")?.findString("email")
            ?: email

        val resolvedRole = role.toUserRole()
        TokenStore.saveSession(token, responseEmail, resolvedRole.name)
        AuthSession(
            email = responseEmail,
            role = resolvedRole,
            accessToken = token
        )
    }

    suspend fun getCurrentUser(): Result<AuthSession> = runCatching {
        val token = TokenStore.accessToken
        val savedRole = TokenStore.role
        if (!token.isNullOrBlank() && !savedRole.isNullOrBlank()) {
            return@runCatching AuthSession(
                email = TokenStore.email.orEmpty(),
                role = savedRole.toUserRole(),
                accessToken = token
            )
        }

        val serverSession = runCatching {
            val response = callOrThrowServerMessage { apiService.getUser() }
            val payload = response.payloadObject()
            AuthSession(
                email = payload.findString("email") ?: TokenStore.email.orEmpty(),
                role = payload.findString("role", "userRole", "authority", "type").toUserRole(),
                accessToken = TokenStore.accessToken
            )
        }
        serverSession.getOrElse { throwable ->
            if (!token.isNullOrBlank() && !savedRole.isNullOrBlank()) {
                AuthSession(
                    email = TokenStore.email.orEmpty(),
                    role = savedRole.toUserRole(),
                    accessToken = token
                )
            } else {
                throw throwable
            }
        }
    }

    fun logout() {
        TokenStore.clear()
    }
}

private suspend fun callOrThrowServerMessage(block: suspend () -> JsonElement): JsonElement {
    return try {
        block()
    } catch (e: HttpException) {
        throw IllegalStateException(e.serverMessage())
    }
}

private fun JsonElement.payloadObject(): JsonObject {
    val root = asJsonObjectOrNull() ?: JsonObject()
    return root.findObject("data", "result", "item") ?: root
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return if (isJsonObject) asJsonObject else null
}

private fun JsonObject.findObject(vararg keys: String): JsonObject? {
    keys.forEach { key ->
        val element = get(key)
        if (element != null && element.isJsonObject) return element.asJsonObject
    }
    return null
}

private fun JsonObject.findString(vararg keys: String): String? {
    keys.forEach { key ->
        val element = get(key)
        if (element != null && !element.isJsonNull && element.isJsonPrimitive) {
            return element.asString.takeIf { it.isNotBlank() }
        }
    }
    return null
}

private fun HttpException.serverMessage(): String {
    val fallback = "HTTP ${code()}"
    val raw = response()?.errorBody()?.string() ?: return fallback
    return runCatching {
        val parsed = JsonParser.parseString(raw)
        if (parsed.isJsonObject) {
            parsed.asJsonObject.findString("message", "error", "detail") ?: fallback
        } else {
            fallback
        }
    }.getOrDefault(fallback)
}

private fun String?.toUserRole(): UserRole {
    return when (this?.uppercase()) {
        "ADMIN", "ROLE_ADMIN", "MANAGER" -> UserRole.ADMIN
        "DRIVER", "ROLE_DRIVER", "BUS_DRIVER" -> UserRole.DRIVER
        else -> UserRole.STUDENT
    }
}
