package app.tendwell.apps

import android.content.ComponentName
import android.os.UserHandle

/** One launchable activity, in the main profile or a work profile. */
data class AppEntry(
    /** Stable id used in saved lists: "package/class#userSerial". */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
)
