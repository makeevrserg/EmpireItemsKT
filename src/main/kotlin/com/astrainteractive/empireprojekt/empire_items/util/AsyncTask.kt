package com.astrainteractive.empireprojekt.empire_items.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface AsyncTask : CoroutineScope {
    private val job: Job
        get() = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
}