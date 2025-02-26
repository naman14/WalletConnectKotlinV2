@file:JvmSynthetic

package com.walletconnect.push.common

internal object JsonRpcMethod {

    @get:JvmSynthetic
    const val WC_PUSH_REQUEST: String = "wc_pushRequest"

    @get:JvmSynthetic
    const val WC_PUSH_MESSAGE: String = "wc_pushMessage"

    @get:JvmSynthetic
    const val WC_PUSH_DELETE: String = "wc_pushDelete"
}