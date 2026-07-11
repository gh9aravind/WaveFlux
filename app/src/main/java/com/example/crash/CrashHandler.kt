package com.example.crash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object CrashHandler {
    private const val PREFS_NAME = "crash_prefs"
    private const val KEY_LAST_CRASH = "last_crash"

    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()

                // Use commit() (synchronous), not apply() (async) - the
                // process gets killed right after this, so an async write
                // could be lost in the race, leaving "no crash details".
                app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, trace)
                    .commit()

                val intent = Intent(app, CrashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                app.startActivity(intent)
            } catch (e: Exception) {
                // If even the crash handler fails, just fall through to killing the process
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    fun getLastCrash(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, null)
    }

    fun clearLastCrash(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_CRASH)
            .apply()
    }
}
