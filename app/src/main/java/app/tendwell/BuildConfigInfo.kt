package app.tendwell

object BuildConfigInfo {
    val versionLabel: String get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
}
