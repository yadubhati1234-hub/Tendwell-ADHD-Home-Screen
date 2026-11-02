package app.tendwell

import android.app.Application
import app.tendwell.apps.AppRepository
import app.tendwell.data.OpenLog
import app.tendwell.data.Prefs

class TendwellApp : Application() {
    // Created on first use, so nothing heavy runs before the first frame.
    val apps: AppRepository by lazy { AppRepository(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    val openLog: OpenLog by lazy { OpenLog(this) }
}
