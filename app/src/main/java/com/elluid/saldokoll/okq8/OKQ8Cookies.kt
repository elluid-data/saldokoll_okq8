package com.elluid.saldokoll.okq8

import com.github.kittinunf.fuel.core.Headers

data class OKQ8Cookies(var cookieText: List<String>) {

    var pairs : MutableList<Pair<String, String>> = mutableListOf()

    init {
        cookieText.forEach {
            pairs.add(Pair(Headers.COOKIE, it))
        }
        pairs.add(Pair(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded"))
    }
    fun getCookies() : MutableList<Pair<String, String>> {
        return pairs
    }
}