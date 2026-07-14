package com.cristobalcariqueo.defensadedeudores

import android.app.Application
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncEngine
import com.cristobalcariqueo.defensadedeudores.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DefensaApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncEngine: SyncEngine by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DefensaApp)
            modules(appModule)
        }
        // Auto-sync while signed in + online; no-ops otherwise.
        syncEngine.start(appScope)
    }
}
