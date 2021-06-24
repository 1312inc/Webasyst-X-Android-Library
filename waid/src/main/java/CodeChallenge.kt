package com.webasyst.waid

import android.util.Base64
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
        var challengeMethod = CHALLENGE_METHOD_PLAIN
        encoded = try {
            val hash = password.computeHash()
            challengeMethod = CHALLENGE_METHOD_SHA265
            hash
        } catch (e: Throwable) {
            password
        }.toBase64()
        this.challengeMethod = challengeMethod
    }

    private fun randomString(
        charPool: List<Char> = ('a'..'z') + ('0'..'9'),// + '-' + '.' + '_' + '~',
        length: Int = 64,
    ): String = (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString(separator = "")

    /**
     * Computes SHA256 hash of given String
     */
    private fun String.computeHash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(encodeToByteArray())
        val bytes = digest.digest(encodeToByteArray())
        return bytes.joinToString(separator = "") {
            ((it.toInt() and 0xff) + 0x100).toString(16).substring(1)
        }
    }

    private fun String.toBase64() =
        Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)

    companion object {
        const val CHALLENGE_METHOD_PLAIN = "plain"
        const val CHALLENGE_METHOD_SHA265 = "SHA256"
    }
}
