@file:JvmSynthetic

package com.walletconnect.sign.di

import com.squareup.moshi.Moshi
import com.tinder.scarlet.utils.getRawType
import com.walletconnect.android.common.JsonRpcResponse
import com.walletconnect.android.common.di.AndroidCommonDITags
import com.walletconnect.sign.common.adapters.JsonRpcResultAdapter
import com.walletconnect.sign.common.adapters.SessionRequestVOJsonAdapter
import com.walletconnect.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.reflect.jvm.jvmName
import com.walletconnect.android.common.di.commonModule as androidCommonModule

@JvmSynthetic
internal fun commonModule() = module {

    includes(androidCommonModule())

    //todo: Maybe this needs to be registered as named(AndroidCommonDITags.MOSHI) to properly work with new JsonRpcSerializer approach
    single(createdAtStart = true) {
        get<Moshi.Builder>(named(AndroidCommonDITags.MOSHI))
            .add { type, _, moshi ->
                when (type.getRawType().name) {
                    SessionRequestVO::class.jvmName -> SessionRequestVOJsonAdapter(moshi)
                    JsonRpcResponse.JsonRpcResult::class.jvmName -> JsonRpcResultAdapter(moshi)
                    else -> null
                }
            }
    }
}