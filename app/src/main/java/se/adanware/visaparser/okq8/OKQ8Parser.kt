import java.net.URLDecoder
import com.eclipsesource.json.Json

import com.eclipsesource.json.JsonValue




class OKQ8Parser {
    companion object {
        fun getSessionId(htmlData: String) : String {
            val url = Regex("jsessionid=([^\"]*)\\?").find(htmlData)?.groups?.get(1)?.value
            return url.orEmpty()
        }

        fun getActionRedirectUrl(htmlData: String) : ParseResult {
            val url = Regex("action=\"([^\"]*)\"").find(htmlData)?.groups?.get(1)?.value
            if(url == "null") {
                return ParseResult(url, false, "Fel : kan ej hitta redirect url")
            }
            return ParseResult(url, true)
        }

        fun getViewState(htmlData: String) : ParseResult {
            val viewState = Regex("value=\"(e[^\"]*)\"").find(htmlData)?.groups?.get(1)?.value
            if(viewState == "null") {
                return ParseResult(viewState, false, "Fel : Hittar ej viewstate parameter")
            }
            return ParseResult(viewState, true)
        }

        fun isBankIDSigningCancelled(htmlData: String) : Boolean {
            return htmlData.contains("Avbruten autentisering.")
        }

        fun isBankIDSigningComplete(htmlData: String): Boolean {
            return !htmlData.contains("href=\"bankid:")
        }

        fun getAccessUrl(htmlData: String): ParseResult {
            val url = Regex("href=\"(https:[^\"]*)\"").find(htmlData)?.groups?.get(1)?.value
            if(url == "null" || url == null) {
                return ParseResult(url, false, "Fel : Hittar ej api redirect url")
            }
            val decodedUrl = URLDecoder.decode(url, "utf-8")
            return ParseResult(decodedUrl, true)
        }

        fun getAccessToken(string: String) : ParseResult {
            val token = Regex("so=([^&]*)").find(string)?.groups?.get(1)?.value
            if(token == "null") {
                return ParseResult(token, false, "Fel : Hittar ej access token")
            }
            return ParseResult(token, true)
        }

        fun getOCR(jsonData: String): String {
            val jsonValue = Json.parse(jsonData)
            return jsonValue.asObject().getString("invoiceReference", "")
        }


    }

}

data class ParseResult(val data: String?, val isSuccess: Boolean, val errorMessage: String = "")

enum class  BANKID {
    NOT_OPENED,
    IS_CANCELLED,
    COMPLETE
}

