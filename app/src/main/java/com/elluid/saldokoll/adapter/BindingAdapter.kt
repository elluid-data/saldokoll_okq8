package com.elluid.saldokoll.adapter

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout


@BindingAdapter("app:errorText")
fun setErrorMessage(view: TextInputLayout, message: String?) {
    view.error = message
}