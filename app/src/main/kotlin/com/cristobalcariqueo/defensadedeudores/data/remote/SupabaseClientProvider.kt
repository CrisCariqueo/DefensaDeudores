package com.cristobalcariqueo.defensadedeudores.data.remote

import com.cristobalcariqueo.defensadedeudores.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Single Supabase client for the app. Requires SUPABASE_URL / SUPABASE_ANON_KEY
 * in local.properties (see local.properties.example) -- BuildConfig fields are
 * empty strings if unset, which fails fast and loudly rather than silently.
 */
object SupabaseClientProvider {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }
}
