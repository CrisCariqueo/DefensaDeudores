package com.cristobalcariqueo.defensadedeudores.di

import com.cristobalcariqueo.defensadedeudores.data.remote.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

// Repository bindings (Person/Source/Track/Registry) get added here as their
// implementations land -- tasks #5, #7, #9.
val appModule = module {
    single<SupabaseClient> { SupabaseClientProvider.client }
}
