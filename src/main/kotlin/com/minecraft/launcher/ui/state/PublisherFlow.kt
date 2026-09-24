package com.minecraft.launcher.ui.state

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * java.util.concurrent.Flow.Publisher → kotlinx.coroutines Flow 的 UI 侧适配。
 * 请求无界并 trySend（后端均为低频快照流，无背压压力）。
 */
fun <T> java.util.concurrent.Flow.Publisher<T>.toFlow(): Flow<T> = callbackFlow {
    var subscription: java.util.concurrent.Flow.Subscription? = null
    val subscriber = object : java.util.concurrent.Flow.Subscriber<T> {
        override fun onSubscribe(s: java.util.concurrent.Flow.Subscription) {
            subscription = s
            s.request(Long.MAX_VALUE)
        }

        override fun onNext(item: T) {
            trySend(item)
        }

        override fun onError(throwable: Throwable) {
            close(throwable)
        }

        override fun onComplete() {
            close()
        }
    }
    subscribe(subscriber)
    awaitClose { subscription?.cancel() }
}
