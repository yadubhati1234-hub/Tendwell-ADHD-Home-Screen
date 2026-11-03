package app.tendwell

import android.app.Application
import app.tendwell.analytics.Analytics
import app.tendwell.apps.AppRepository
import app.tendwell.billing.BillingRepository
import app.tendwell.data.OpenLog
import app.tendwell.data.Prefs

class TendwellApp : Application() {
    // Created on first use, so nothing heavy runs before the first frame.
    val apps: AppRepository by lazy { AppRepository(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    val openLog: OpenLog by lazy { OpenLog(this) }
    val billing: BillingRepository by lazy { BillingRepository(this, prefs) }
    val analytics: Analytics by lazy { Analytics(this) }
}
