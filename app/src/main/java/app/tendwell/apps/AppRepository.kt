package app.tendwell.apps

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator

/**
 * Lists every launchable app (main and work profiles) and keeps the list
 * fresh when apps are installed, removed or updated.
 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)

    /** Alphabetical list of apps, or null until the first load finishes. */
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        scope.launch { _apps.value = load() }
    }

    private fun load(): List<AppEntry> {
        val me = Process.myUserHandle()
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map { info ->
                    AppEntry(
                        key = "${info.componentName.flattenToShortString()}#$serial",
                        label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                        component = info.componentName,
                        user = user,
                        isWorkProfile = user != me,
                    )
                }
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** Opens the app. Returns false if it could not be started (for example it was just uninstalled). */
    fun launch(app: AppEntry, sourceBounds: Rect? = null): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, null)
        true
    } catch (e: ActivityNotFoundException) {
        refresh(); false
    } catch (e: SecurityException) {
        refresh(); false
    }

    fun openAppInfo(app: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: ActivityNotFoundException) {
            refresh()
        } catch (e: SecurityException) {
            refresh()
        }
    }

    /**
     * Picks the starting essential apps for a fresh install: the default
     * phone, messages, maps, camera and music apps, if the phone has them.
     */
    fun defaultEssentialKeys(apps: List<AppEntry>): List<String> {
        val pm = context.packageManager
        fun resolvePackage(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                ?.takeIf { it != "android" }
        fun selector(category: String) = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)

        val packages = listOfNotNull(
            resolvePackage(Intent(Intent.ACTION_DIAL)),
            Telephony.Sms.getDefaultSmsPackage(context) ?: resolvePackage(selector(Intent.CATEGORY_APP_MESSAGING)),
            resolvePackage(selector(Intent.CATEGORY_APP_MAPS)),
            resolvePackage(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            resolvePackage(selector(Intent.CATEGORY_APP_MUSIC)),
        ).distinct()

        val mainProfile = apps.filter { !it.isWorkProfile }
        return packages.mapNotNull { pkg -> mainProfile.firstOrNull { it.component.packageName == pkg }?.key }
    }

    /** True when Tendwell is the phone's current home app. */
    fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == context.packageName
    }

    /** Intent for Android's "Home app" setting, where the user can switch launchers. */
    fun homeSettingsIntent(): Intent {
        val home = Intent(Settings.ACTION_HOME_SETTINGS)
        return if (home.resolveActivity(context.packageManager) != null) home else Intent(Settings.ACTION_SETTINGS)
    }

    companion object {
        fun componentOf(key: String): ComponentName? =
            ComponentName.unflattenFromString(key.substringBefore('#'))
    }
}
