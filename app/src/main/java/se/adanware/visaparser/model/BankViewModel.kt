package se.adanware.visaparser.model

import android.util.Log
import androidx.lifecycle.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitUnit
import kotlinx.coroutines.*
import se.adanware.visaparser.data.Event
import se.adanware.visaparser.okq8.OKQ8Service
import java.io.File

const val PDF_DIRECTORY = "invoices"
const val PDF_FILENAME = "invoice.pdf"

class BankViewModel : ViewModel() {

    lateinit var okQ8Service: OKQ8Service
    fun isOkq8ServiceAlive() : Boolean {
        return if(this::okQ8Service.isInitialized) {
            true
        } else {
            signInStatus.value = Event(LoginStatus.DESTROYED)
            false
        }
    }
    val downloadEvent = MutableLiveData<Event<DownloadStatus>>()
    val signInErrors = MutableLiveData<MutableList<Pair<Errors, String>>>()
    val signInStatus = MutableLiveData<Event<LoginStatus>>()

    lateinit var errorMessage: String
    private var job: Job? = null

    fun startSignIn(userName: String) {

        viewModelScope.launch(Dispatchers.IO) {
            okQ8Service = OKQ8Service()
            signInErrors.value?.clear()
            val response = okQ8Service.connect()
            if (!response.isSuccess) {
                withContext(Dispatchers.Main) {
                    signInErrors.value?.add(Pair(Errors.SIGN_IN_ERROR, response.message))
                }
                cancel()
            }
            withContext(Dispatchers.Main) {
                signInStatus.value = Event(LoginStatus.AWAITING_BANKID)
            }

            val bankidResponse = okQ8Service.signIn(userName)
            if (!bankidResponse.isSuccess) {
                withContext(Dispatchers.Main) {
                    signInErrors.value?.add(Pair(Errors.SIGN_IN_ERROR, response.message))
                }
                cancel()
            }
            withContext(Dispatchers.Main) {
                signInStatus.value = Event(LoginStatus.BANKID_RECEIVED)
            }
        }
    }

    fun checkSignInStatus() {
        viewModelScope.launch {
            delay(500) // Delay 0.5 sec, until checking.
            val bankIdStatus = okQ8Service.checkIfSignedIn().message
            when (BANKID.valueOf(bankIdStatus)) {
                BANKID.NOT_OPENED -> {
                    signInStatus.value = Event(LoginStatus.NOT_SIGNED)
                    cancel()
                }
                BANKID.IS_CANCELLED -> {
                    signInStatus.value = Event(LoginStatus.CANCELLED)
                    cancel()
                }
                BANKID.COMPLETE -> {
                    signInStatus.value = Event(LoginStatus.GET_SESSION)
                    // Update UI - Start session
                    val response = okQ8Service.startSession()
                    if(!response.isSuccess) {
                        errorMessage = response.message
                        signInStatus.value = Event(LoginStatus.GET_SESSION_FAILED)
                        return@launch
                    }
                    // Update UI - Fetching details
                    okQ8Service.getDetails()
                    val transactionsJob = async { okQ8Service.getTransactions() }
                    val invoiceJob = async { okQ8Service.getInvoices() }

                    invoiceJob.await()
                    transactionsJob.await()
                    // Create account fragment
                    signInStatus.value = Event(LoginStatus.COMPLETE)
                }
            }
        }
    }



    fun retrieveInvoice(pathToAppData: File) {
        job?.isActive?.let {
            isActive ->
            if(isActive) {
                downloadEvent.value = Event(DownloadStatus.INCOMPLETE)
                return
            }
        }

        val handler = CoroutineExceptionHandler { _, exception ->
            downloadEvent.value = Event(DownloadStatus.ERROR) // Flag the error
            Log.d("OKQ8", "Exception: ${exception.message}") }

        job = viewModelScope.launch(handler) {
            withContext(Dispatchers.IO) {
            Fuel.download(okQ8Service.getCard().invoice.pdfUrl).also {
                okQ8Service.getCard().session.setHeader(it)
            }.fileDestination { _, _ ->
                val path = File(pathToAppData, PDF_DIRECTORY)
                if (!path.exists()) {
                    path.mkdir()
                }
                File(path, PDF_FILENAME)
                }.awaitUnit()
            }
            downloadEvent.value = Event(DownloadStatus.COMPLETE)
        }

    }
}

enum class LoginStatus {
        AWAITING_BANKID,
        BANKID_RECEIVED,
        NOT_SIGNED,
        CANCELLED,
        GET_SESSION,
        GET_SESSION_FAILED,
        COMPLETE,
        DESTROYED,
}

enum class DownloadStatus {
    COMPLETE,
    ERROR,
    INCOMPLETE
}