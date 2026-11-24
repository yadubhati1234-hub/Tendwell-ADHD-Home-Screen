package app.tendwell.onboarding

/**
 * Some phone makers' Android versions (Xiaomi's MIUI and HyperOS, Oppo's ColorOS,
 * realme UI) switch from gesture navigation to the 3 buttons while a third-party
 * Home app is the default. Tendwell can't change that, so the set-home step says
 * so up front on those phones instead of surprising people.
 */
object GestureNote {
    private val brands = mapOf(
        "xiaomi" to "Xiaomi",
        "redmi" to "Xiaomi",
        "poco" to "Xiaomi",
        "oppo" to "Oppo",
        "realme" to "Realme",
    )

    /** The maker's name to show, or null when the note isn't needed. Pass Build.MANUFACTURER and Build.BRAND. */
    fun brandFor(manufacturer: String?, brand: String?): String? =
        listOf(manufacturer, brand).firstNotNullOfOrNull { brands[it?.trim()?.lowercase()] }

    fun text(brandName: String): String =
        "On $brandName phones, Android may swap gestures for the 3 buttons while Tendwell is Home. " +
            "Switching back to your old Home brings gestures back."
}
