package com.cristobalcariqueo.defensadedeudores

import android.app.Application
import com.cristobalcariqueo.defensadedeudores.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DefensaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DefensaApp)
            modules(appModule)
        }
    }
}
