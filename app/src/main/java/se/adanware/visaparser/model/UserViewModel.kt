package se.adanware.visaparser.model

import androidx.databinding.ObservableArrayList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {

    var username = MutableLiveData<String>()
    private val _saveCredentials = MutableLiveData<Boolean>()
    val saveCredentials : LiveData<Boolean> = _saveCredentials
    val loginErrors = ObservableArrayList<Errors>()

    fun setSaveCredentials(isSaving: Boolean) {
        _saveCredentials.value = isSaving
    }

    fun validateUsername() : Boolean {
        loginErrors.clear()
        if(username.value?.length != 12) {
            loginErrors.add(Errors.INVALID_USERNAME)
        }
        return loginErrors.isEmpty()
    }


}