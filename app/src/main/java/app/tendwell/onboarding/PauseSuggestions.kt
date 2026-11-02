package app.tendwell.onboarding

/**
 * Apps people commonly lose time in, suggested first on the "Which apps pull
 * you in?" step. Only installed apps are shown; nothing is pre-selected.
 */
object PauseSuggestions {
    private val PACKAGES = listOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill", // TikTok (some regions)
        "com.google.android.youtube",
        "com.reddit.frontpage",
        "com.twitter.android", // X
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.barcelona", // Threads
        "com.snapchat.android",
        "com.pinterest",
        "com.tumblr",
        "tv.twitch.android.app",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient", // Prime Video
        "com.disney.disneyplus",
        "com.google.android.apps.youtube.music",
        "com.linkedin.android",
        "com.bereal.ft",
        "com.discord",
        "org.telegram.messenger",
        "com.king.candycrushsaga",
        "com.supercell.clashroyale",
        "com.roblox.client",
        "com.einnovation.temu",
        "com.zzkko", // Shein
        "com.amazon.mShop.android.shopping",
    )

    private val rank = PACKAGES.withIndex().associate { (i, p) -> p to i }

    fun isSuggested(packageName: String): Boolean = packageName in rank

    /** Splits package names into suggested ones (in the order above) and the rest (order kept). */
    fun <T> split(items: List<T>, packageOf: (T) -> String): Pair<List<T>, List<T>> {
        val (suggested, other) = items.partition { isSuggested(packageOf(it)) }
        return suggested.sortedBy { rank.getValue(packageOf(it)) } to other
    }
}
