package com.rhinepereira.versetrack.data

import com.rhinepereira.versetrack.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(GoTrue)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = BuildConfig.GOOGLE_CLIENT_ID)
            }
        }
    }
}
