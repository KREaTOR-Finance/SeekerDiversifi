package com.diversify.core.extension

import android.util.Base64

fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.DEFAULT)
fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.DEFAULT)
