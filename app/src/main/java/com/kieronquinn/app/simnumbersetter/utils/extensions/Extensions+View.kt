package com.kieronquinn.app.simnumbersetter.utils.extensions

import android.view.View
import android.content.Context
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

private const val CLICK_DEBOUNCE = 250L

@OptIn(FlowPreview::class)
fun View.onClicked() = callbackFlow<View> {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(CLICK_DEBOUNCE)

fun View.hideIme() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}