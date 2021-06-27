package com.webasyst.waid

import java.security.MessageDigest
import kotlin.random.Random

class CodeChallenge(
    length: Int = 64
) {
    val password: String
    val encoded: String
    val challengeMethod: String

    init {
        password = randomString(length = length)
        encoded = password
        challengeMethod = CHALLENGE_METHOD_PLAIN
        /*
        // TODO: SHA256 disabled
        var challengeMethod = CHALLENGE_METHOD_PLAIN
        encoded = try {
            val hash = password.computeHash()
            val encodedPassword = Base64.encodeToString(hash, Base64.URL_SAFE)
            challengeMethod = CHALLENGE_METHOD_SHA265
            encodedPassword
        } catch (e: Throwable) {
            password
        }
        this.challengeMethod = challengeMethod
         */
    }

    private fun randomString(
        charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9'),
        length: Int = 64,
    ): String = (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString(separator = "")

    /**
     * Computes SHA256 hash of given String
     */
    private fun String.computeHash(): ByteArray =
        MessageDigest.getInstance("SHA-256").let { digest ->
            digest.reset()
            digest.update(encodeToByteArray())
            digest.digest(encodeToByteArray())
        }

    companion object {
        const val CHALLENGE_METHOD_PLAIN = "plain"
        const val CHALLENGE_METHOD_SHA265 = "SHA256"
    }
}
