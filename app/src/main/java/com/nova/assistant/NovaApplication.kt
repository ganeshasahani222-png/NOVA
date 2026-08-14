package com.nova.assistant

import android.app.Application
import com.nova.assistant.core.NovaContainer

class NovaApplication : Application() {
    lateinit var container: NovaContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = NovaContainer(this)
    }
}
