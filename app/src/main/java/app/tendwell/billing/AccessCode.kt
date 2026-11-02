package app.tendwell.billing

import java.security.MessageDigest

/**
 * A code that turns Pro on without a purchase, for Google Play's app reviewers:
 * they can't buy or start free trials, so Play Console's "Sign in details" form
 * gives them this code instead. Only the code's SHA-256 ships here. The code
 * itself is kept with the signing files, never in this public repository.
 */
object AccessCode {
    /** SHA-256 of the reviewers' code, normalized (see [normalize]). */
    private const val REVIEW_HASH = "7dfe454c1b68e82759d5fa13475ecb1f22456ab89e071e4a6d8729721555cb7c"

    /** Letters and digits only, uppercase, so "zkxk d4yr" and "ZKXK-D4YR" read the same. */
    fun normalize(input: String): String = input.filter { it.isLetterOrDigit() }.uppercase()

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun matches(input: String, hash: String = REVIEW_HASH): Boolean {
        val code = normalize(input)
        if (code.isEmpty()) return false
        return MessageDigest.isEqual(sha256(code).toByteArray(), hash.toByteArray())
    }
}
