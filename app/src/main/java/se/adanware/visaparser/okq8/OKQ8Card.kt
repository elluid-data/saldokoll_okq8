package se.adanware.visaparser.okq8

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*



data class Invoice(private val _totalAmount: Double, private val _dueDate: Long, val invoiceUrl: String) {
    lateinit var pdfUrl: String
    val totalAmount: String
        get() {
            return if (_totalAmount == -1.0) {
                "Ingen data"
            } else {
                NumberFormat.getCurrencyInstance().format(_totalAmount)
            }
        }

    val dueDate: String get() {
        if(_dueDate == 0L) {
            return "Ingen data"
        } else {
            return DateFormat.getDateInstance(DateFormat.LONG).format(Date(_dueDate))
        }
    }
}

data class OKQ8Card(val accountingBalance: Double,
                    val availableBalance: Double,
                    val limitAmount: Double,
                    val name: String,
                    val transactionsUrl: String = "",
                    val invoiceUrl: String = "",
                    val detailsUrl: String = "")
{

    var productStatus = ProductStatus.NORMAL
    lateinit var ocr: String
    lateinit var transactions: MutableList<Transaction>
    lateinit var invoice: Invoice
    lateinit var session: Session
    val formater = NumberFormat.getCurrencyInstance().also {
        it.currency = Currency.getInstance(Locale("sv", "SE"))
    }


    fun getBalance() : String {
        return formater.format(accountingBalance)
    }

    fun getAvailableBalance() : String {
        return formater.format(availableBalance)
    }

    fun getLimit() : String {
        return formater.format(limitAmount)
    }

    companion object {
        fun createCard(jsonAsText: String) : OKQ8Card {
            val rootUrl = "https://secure.okq8bank.se/secesb/rest/era"
            val jsonData = Json.parse(jsonAsText).asObject().get("list").asArray()
            for (jsonValue in jsonData) {

                if (jsonValue.asObject().get("product") == null || jsonValue.asObject().get("product").asObject().get("name") == null) {
                    continue // Continue if product or name doesn't exists.
                }
                if (jsonValue.asObject().get("product").asObject().get("name").asString() == "OKQ8 VISA") {
                    val accountJsonObject = jsonValue.asObject().get("accountBalance").asObject()
                    val availableBalance = (accountJsonObject.getDouble("availableBalance", 0.0))
                    val accountingBalance = accountJsonObject.getDouble("accountingBalance", 0.0)
                    val limitAmount = accountJsonObject.getDouble("limitAmount", 0.0)
                    val name = jsonValue.asObject().get("owner").asObject().getString("name", "Ingen data")
                    val linksJsonObject = jsonValue.asObject().get("links").asObject()
                    val transactionsUrl = linksJsonObject.get("transactions").asObject().getString("href", "")
                    val invoiceUrl = linksJsonObject.get("invoice").asObject().getString("href", "")
                    val detailsUrl = linksJsonObject.get("details").asObject().getString("href", "")
                    return OKQ8Card(accountingBalance,
                                    availableBalance,
                                    limitAmount,
                                    name,
                                    rootUrl + transactionsUrl,
                                    rootUrl + invoiceUrl,
                                    rootUrl + detailsUrl)
                }
            }
            return OKQ8Card(0.0, 0.0, 0.0,"error").also { it.productStatus = ProductStatus.OKQ8_PRODUCT_NOT_FOUND }
        }

        fun createTransactions(data: String): MutableList<Transaction> {
            val transactions = mutableListOf<Transaction>()
            val jsonValue = Json.parse(data) ?: return transactions
            if (jsonValue.asObject()["list"] == null) {
                return transactions
            }
            val array = jsonValue.asObject()["list"].asArray()
            for (value in array) {
                if (value == null) continue
                val amount = setTransactionAmount(value)
                val currency = setCurrency(value)
                val isPayment = setIsPayment(value)
                val transactionDate = setTransactionDate(value)
                val transactionText = setTransactionMerchant(value)
                val transactionCity = setTransactionMerchantCity(value)
                val isReserved = setIsReserved(value)

                var transaction: Transaction
                if(isForeignCurrency(value)) {
                    transaction = Transaction(
                        amount,
                        transactionText,
                        transactionCity,
                        transactionDate,
                        isPayment,
                        currency,
                        isReserved,
                        true,
                        getForeignCurrencyAmount(value),
                        getForeignCurrencySymbol(value)
                    )
                }
                else {
                    transaction = Transaction(
                        amount,
                        transactionText,
                        transactionCity,
                        transactionDate,
                        isPayment,
                        currency,
                        isReserved,
                    )

                }
                transactions.add(transaction)
            }
            return transactions
        }

        fun createInvoiceData(jsonAsText: String) : Invoice {
            val jsonData = Json.parse(jsonAsText).asObject().get("list").asArray()
            if(!jsonData.isEmpty) {
                val dueDate = jsonData[0].asObject().getLong("dueDate", 0)
                val totalAmount = jsonData[0].asObject().get("amounts").asObject()
                    .getDouble("totalAmountInvoice", 0.0)
                val invoiceurl =
                    jsonData[0].asObject().get("links").asObject().get("invoice").asObject()
                        .getString("href", "")
                return Invoice(totalAmount, dueDate, invoiceurl)
            }
            return Invoice(-1.0, 0L, "")
        }


        private fun getForeignCurrencyAmount(jsonData: JsonValue) : Double {
            return jsonData.asObject()["amounts"].asObject()["instructed"].asObject().getDouble("value", 0.0)
        }

        private fun getForeignCurrencySymbol(jsonData: JsonValue) : String {
            return jsonData.asObject()["amounts"].asObject()["instructed"].asObject().getString("currency", "?")
        }

        private fun isForeignCurrency(value: JsonValue): Boolean {
            return if (value.asObject()["amounts"] == null && value.asObject()["amounts"].asObject()["instructed"] == null) {
                false // Fields non-existant.
            } else !value.asObject()["amounts"].asObject()["instructed"].asObject()
                .getString("currency", "SEK").equals("SEK", ignoreCase = true)
        }

        private fun setIsReserved(value: JsonValue): Boolean {
            return if (value.asObject()["reserved"] != null) {
                value.asObject()["reserved"].asBoolean()
            } else false
        }

        private fun setTransactionMerchantCity(value: JsonValue): String {
            return if (value.asObject()["merchant"] != null) {
                value.asObject()["merchant"].asObject().getString("city", "")
            } else "Ingen data."
        }

        private fun setTransactionMerchant(value: JsonValue): String {
            return if (value.asObject()["merchant"] != null) {
                value.asObject()["merchant"].asObject().getString("name", "Ingen data").trim()
            } else "Ingen data."
        }

        private fun setTransactionDate(value: JsonValue): Long {
            return if (value.asObject()["transactionDate"] != null) {
                value.asObject()["transactionDate"].asLong()
            } else 0
        }

        private fun setIsPayment(value: JsonValue): Boolean {
            return if (value.asObject()["type"] != null) {
                value.asObject()["type"].asString().equals("payment", ignoreCase = true)
            } else false
        }

        private fun setTransactionAmount(value: JsonValue): Double {
            return if (value.asObject()["amounts"] != null && value.asObject()["amounts"].asObject()["executed"] != null) {
                value.asObject()["amounts"].asObject()["executed"].asObject().getDouble("value", 0.0)
            } else 0.0
        }

        private fun setCurrency(value: JsonValue): String {
            return if (value.asObject()["amounts"] != null && value.asObject()["amounts"].asObject()["executed"] != null) {
                value.asObject()["amounts"].asObject()["executed"].asObject().getString("currency", "")
            } else ""
        }

    }


}

enum class ProductStatus {
    NORMAL,
    OKQ8_PRODUCT_NOT_FOUND
}