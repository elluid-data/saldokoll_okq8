package se.adanware.visaparser.okq8

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue;
import com.github.kittinunf.fuel.core.HeaderValues
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import java.lang.StringBuilder

data class Session(val jsonData: String, val cookieData: HeaderValues) {
    var accessToken: String
    var sessionId: String

    init {
        val jsonValue: JsonValue = Json.parse(jsonData)
        accessToken = jsonValue.asObject().get("accessToken").asString()
        sessionId = jsonValue.asObject().get("sessionId").asString()
    }

    fun setHeader(request: Request) {
        request.header(
            Pair(Headers.CONTENT_TYPE,"application/json;charset=UTF-8"),
            Pair("Accept-Language", "sv-SE"),
            Pair("X-EVRY-CLIENT-ACCESSTOKEN", accessToken),
            Pair("X-EVRY-CLIENT-CLIENTNAME", "RetailOnline"),
            Pair("X-EVRY-CLIENT-REQUESTID", getNumericString())
        )
        request.header(Headers.COOKIE, cookieData)
    }



    companion object {
        fun getNumericString(): String {
            // chose a Character random from this String
            // chose a Character random from this String
            val numericString = "0123456789"

            // create StringBuffer size of AlphaNumericString

            // create StringBuffer size of AlphaNumericString
            val sb = StringBuilder(8)

            for (i in 0..7) {
                // generate a random number between
                // 0 to AlphaNumericString variable length
                val index = (numericString.length * Math.random()).toInt()

                // add Character one by one in end of sb
                sb.append(numericString[index])
            }

            return sb.toString()
        }
    }
}
