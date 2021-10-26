package se.adanware.visaparser.repository

import android.content.Context

const val PREFERENCES_FILE = "settings"
const val SAVE_CREDENTIALS = "isChecked"
const val NIGHT_MODE = "nightMode"
const val PREFS_SSN = "ssn"

class PreferencesRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

    fun setNightMode(nightMode: Int) {
        val editor = preferences.edit()
        editor.putInt(NIGHT_MODE, nightMode).apply()
    }

    fun getNightMode() : Int {
        return preferences.getInt(NIGHT_MODE, 1)
    }

    fun saveSsn(ssn: String) {
        val editor = preferences.edit()
        editor.putString(PREFS_SSN, ssn).apply()
    }

    fun getSsn() : String {
        return preferences.getString(PREFS_SSN, "").orEmpty()
    }

    // Only save username when hitting 'Logga In'.
    fun setSaveCredentials(isSaving: Boolean) {
        val editor = preferences.edit()
        if(isSaving) {
            editor.putBoolean(SAVE_CREDENTIALS, isSaving).apply()
        } else {
            editor.putBoolean(SAVE_CREDENTIALS, false)
            editor.putString(PREFS_SSN, "")
            editor.apply()
        }
    }

    fun isSavingCredentials() : Boolean {
        return preferences.getBoolean(SAVE_CREDENTIALS, false)
    }
}