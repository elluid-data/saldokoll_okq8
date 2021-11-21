package com.elluid.saldokoll.okq8


import java.text.DateFormat
import java.text.NumberFormat
import java.util.*


data class Transaction(val amount: Double,
                        val merchantName: String,
                        val merchantCity: String,
                        val date: Long,
                        val isPayment: Boolean,
                        val currency: String,
                        val isReserved: Boolean = false,
                        val isForeignCurrency: Boolean = false,
                        val foreignCurrencyAmount: Double = 0.0,
                        val foreignCurrencySymbol: String = "")
{
    val formater = NumberFormat.getCurrencyInstance().also {
        it.currency = Currency.getInstance(Locale("sv", "SE"))
    }


    fun getDate(): String? {
        return DateFormat.getDateInstance(DateFormat.LONG).format(Date(date))
    }

    fun getForeignAmount(): String {
        val formater = NumberFormat.getCurrencyInstance()
        formater.currency = Currency.getInstance(foreignCurrencySymbol)
        return formater.format(foreignCurrencyAmount)
    }
    fun getAmount(): String {
            return formater.format(amount)
    }
}
