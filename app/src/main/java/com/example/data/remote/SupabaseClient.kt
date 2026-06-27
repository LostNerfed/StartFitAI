package com.example.data.remote

import com.example.data.api.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient

object SupabaseConfig {
    val client: SupabaseClient
        get() = SupabaseClientProvider.client
}
