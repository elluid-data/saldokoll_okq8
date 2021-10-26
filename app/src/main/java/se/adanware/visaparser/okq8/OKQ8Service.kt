package se.adanware.visaparser.okq8

import android.util.Log
import com.eclipsesource.json.Json
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import kotlinx.coroutines.CoroutineExceptionHandler
import java.net.MalformedURLException

class OKQ8Service {

    private val OKQ8_BANK_DOMAIN = "https://secure.okq8bank.se"
    private val OKQ8_BANK_LOGIN_URL = "https://secure.okq8bank.se/authenticate/login/selectauth?configKey=sb0066"
    private val OKQ8_API_LOGIN = "https://secure.okq8bank.se/secesb/rest/esb/v1/login"
    private val OKQ8_API_ERA_LOGIN = "https://secure.okq8bank.se/secesb/rest/era/login"
    private val OKQ8_BANK_CARDS = "https://secure.okq8bank.se/secesb/rest/era/creditcardaccounts"

    private var sessionUrl = ""
    private var sessionID = ""
    private var currentViewStateParameter : String? = ""
    private lateinit var okQ8Card: OKQ8Card
    private lateinit var cookieStorage: OKQ8Cookies
    private lateinit var accessToken: String
    private lateinit var session: Session


    suspend fun connect() : Response {

        val (_, responseConnect, resultConnect) = Fuel.get(OKQ8_BANK_LOGIN_URL).awaitStringResponse()

        if(responseConnect.statusCode != 200) {
            return Response(false, "Fel : Statuskod: ${responseConnect.statusCode}")
        }
        sessionID = OKQ8Parser.getSessionId(resultConnect)
        // Save cookies.
        cookieStorage = OKQ8Cookies(listOf(responseConnect.headers["Set-Cookie"].first(), "JSESSIONID=$sessionID"))
        val redirectUrlResult = OKQ8Parser.getActionRedirectUrl(resultConnect)
        if(!redirectUrlResult.isSuccess) {
            return Response(false, redirectUrlResult.errorMessage)
        }
        sessionUrl = redirectUrlResult.data.orEmpty()
        var viewstateDataResult = OKQ8Parser.getViewState(resultConnect)
        if(!viewstateDataResult.isSuccess) {
            return Response(false, viewstateDataResult.errorMessage)
        }
        currentViewStateParameter = viewstateDataResult.data

        val postRequest = Fuel.post(OKQ8_BANK_DOMAIN + sessionUrl)
        postRequest.header(*cookieStorage.getCookies().toTypedArray())
        postRequest.parameters = listOf(
            Pair("form1", "form1"),
            Pair("javax.faces.ViewState", currentViewStateParameter),
            Pair("form1:swedishBankidMobileLink", "form1:swedishBankidMobileLink")
        )
        val (_, responsePost, resultPost) = postRequest.awaitStringResponse()
        // Clicked and opened the BankID sign in box. Need to parse viewstate.
        viewstateDataResult = OKQ8Parser.getViewState(resultPost)
        if(!viewstateDataResult.isSuccess) {
            return Response(false, viewstateDataResult.errorMessage)
        }
        currentViewStateParameter = viewstateDataResult.data
        return Response(true)
    }

    suspend fun signIn(username: String) : Response {
        var signInRequest = Fuel.post(OKQ8_BANK_DOMAIN + getUpdatedSessionUrl())
        signInRequest.header(*cookieStorage.getCookies().toTypedArray())
        signInRequest.parameters = listOf(
            Pair("loginForm", "loginForm"),
            Pair("swebid-userid", username),
            Pair("j_idt19", "Fortsätt"),
            Pair("javax.faces.ViewState", currentViewStateParameter)
        )

        val (_, _, result) = signInRequest.awaitStringResponse()
        val viewStateResult = OKQ8Parser.getViewState(result)
        if(!viewStateResult.isSuccess) {
            return Response(false, viewStateResult.errorMessage)
        }
        currentViewStateParameter = viewStateResult.data
        // New session every time page reloads.
        sessionID = OKQ8Parser.getSessionId(result)
        sessionUrl = OKQ8Parser.getActionRedirectUrl(result).data.orEmpty()
        return Response(true)
    }

    suspend fun checkIfSignedIn() : Response {
        val statusRequest = Fuel.post(OKQ8_BANK_DOMAIN + getUpdatedSessionUrl())
        statusRequest.header(Pair(Headers.COOKIE, "JSESSIONID=$sessionID"), Pair(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded"))
        statusRequest.body("collect-form=collect-form&collect-btn=&javax.faces.ViewState=" + currentViewStateParameter)
        val (_, _, result) = statusRequest.awaitStringResponse()
        if(OKQ8Parser.isBankIDSigningCancelled(result)) {
            return Response(false, BANKID.IS_CANCELLED.name)
        }
        if(!OKQ8Parser.isBankIDSigningComplete(result)) {
            sessionID = OKQ8Parser.getSessionId(result)
            sessionUrl = OKQ8Parser.getActionRedirectUrl(result).data.orEmpty()
            return Response(false, BANKID.NOT_OPENED.name)
        }

        accessToken = OKQ8Parser.getAccessToken(OKQ8Parser.getAccessUrl(result).data.orEmpty()).data.orEmpty()
        return Response(true, BANKID.COMPLETE.name)
    }

    suspend fun startSession() : Response {
        val sessionRequest = Fuel.post(OKQ8_API_LOGIN).also {
            it.header(Headers.CONTENT_TYPE, "application/json;charset=UTF-8")
            it.header("X-EVRY-CLIENT-ACCESSTOKEN", "null")
            it.header("X-EVRY-CLIENT-CLIENTNAME", "RetailOnline")
            it.header("X-EVRY-CLIENT-REQUESTID", Session.getNumericString())
            it.body("{\"credentials\":{\"tokenType\":\"EvrySO\",\"token\":\"" + accessToken + "\"},\"tokenProtocolVersions\":[\"ATP-1.0\",\"1.1\"],\"loginContext\":\"PRIVATE\"}")
        }
        val (_, response, result) = sessionRequest.awaitStringResponse()
        // Save token and cookies in session
        session = Session(result, response.headers[Headers.SET_COOKIE])
        val apiRequest = Fuel.post(OKQ8_API_ERA_LOGIN).also {
            session.setHeader(it)
        }
        val (_, apiResponse, _) = apiRequest.awaitStringResponse()
        if(apiResponse.statusCode >= 400) {
            return Response(false, "Autentisering misslyckades. Felkod: ${apiResponse.responseMessage}")
        }

        okQ8Card = getCardData()
        if(okQ8Card.productStatus  == ProductStatus.OKQ8_PRODUCT_NOT_FOUND) {
            return Response(false, "Kunde ej skapa kort detaljer.")
        }
        return Response(true)
    }

    private suspend fun getCardData() : OKQ8Card {
        val cardRequest = Fuel.get(OKQ8_BANK_CARDS).also {
            session.setHeader(it)
            it.appendHeader("X-EVRY-REST-SERVICE-VERSION", "2")
        }
        val (_, _, cardResult) = cardRequest.awaitStringResponse()
        return OKQ8Card.createCard(cardResult)
    }

    suspend fun getDetails() {
        kotlin.runCatching {
            val detailsRequest = Fuel.get(okQ8Card.detailsUrl).also {
                session.setHeader(it)
                it.appendHeader("X-EVRY-REST-SERVICE-VERSION", "2")
            }
            val (_, _, detailsResult) = detailsRequest.awaitStringResponse()
            okQ8Card.ocr = OKQ8Parser.getOCR(detailsResult)
        }.onSuccess {  }
            .onFailure {
                okQ8Card.ocr = "Ingen data"
                Log.d("OKQ8", "MalformedUrlException: ${it.message}")
            }
    }

    suspend fun getTransactions() {
        val transactionsRequest = Fuel.get(okQ8Card.transactionsUrl).also {
            session.setHeader(it)
            it.appendHeader("X-EVRY-REST-SERVICE-VERSION", "2")
        }
        val (_, _, transactionsResult) = transactionsRequest.awaitStringResponse()
        okQ8Card.transactions = OKQ8Card.createTransactions(transactionsResult)
    }

    suspend fun getInvoices() {
        val invoiceRequest = Fuel.get(okQ8Card.invoiceUrl).also {
            session.setHeader(it)
        }
        val (_, _, invoiceResult) = invoiceRequest.awaitStringResponse()
        okQ8Card.invoice = OKQ8Card.createInvoiceData(invoiceResult)
        if(!okQ8Card.invoice.invoiceUrl.isEmpty()) {
            val invoicePdfRequest =
                Fuel.post("https://secure.okq8bank.se/secesb/rest/esb/v1/uritoken").also {
                    session.setHeader(it)
                    it.body("""{"operation":"GET","uris":["era${okQ8Card.invoice.invoiceUrl}"]}""")
                }
            val (_, _, invoicePdfUrl) = invoicePdfRequest.awaitStringResponse()
            val pdfUrl =
                "https://secure.okq8bank.se/secesb/rest/" + Json.parse(invoicePdfUrl).asObject().get("uris").asArray().get(0).asString()
            okQ8Card.invoice.pdfUrl = pdfUrl
        } else {
            okQ8Card.invoice.pdfUrl = ""
        }

        okQ8Card.session = session
    }

    fun getCard() : OKQ8Card {
        return okQ8Card
    }
    private fun getUpdatedSessionUrl() : String {
        return sessionUrl.replaceAfter("?", "execution=$currentViewStateParameter")
    }

}

data class Response(val isSuccess: Boolean, val message: String = "")