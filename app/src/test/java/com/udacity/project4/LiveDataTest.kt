package com.udacity.project4

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {

    var data: T? = null
    val countDownLatch = CountDownLatch(1)

    val observer = object : Observer<T> {
        override fun onChanged(o: T?) { data = o
            countDownLatch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    try {
        afterObserve.invoke()
        if (!countDownLatch.await(time, timeUnit)) {
            throw TimeoutException("No LiveData found.")
        }

    } finally {
        this.removeObserver(observer)
    }

    return  data as T
}
