package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton coordinator which manages client connection states to the production Node.js backend.
 * Provides dynamically customizable base URLs and integrates the token interceptor.
 */
object ProductionStackManager {

    private const val DEFAULT_PRODUCTION_BASE_URL = "https://api.soundspot-production.internal/"
    private const val DEFAULT_CLOUDFRONT_BASE_URL = "https://cdn.soundspot-media.net/"

    private var activeApiToken: String? = null

    // Moshi serializer configured with Kotlin reflex parsing
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Retrieve active identity token (Auth0/Firebase Auth provider mock-hook)
    fun setApiToken(token: String?) {
        activeApiToken = token
    }

    private val authInterceptor = Auth0FirebaseTokenInterceptor {
        activeApiToken ?: "mock-transient-auth0-token-for-preview-purposes"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .build()

    val apiService: SoundSpotApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_PRODUCTION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SoundSpotApiService::class.java)
    }

    /**
     * Resolves raw storage paths in S3 bucket keys to their edge-optimized, cached CloudFront CDN distribution endpoints.
     */
    fun resolveCloudFrontStreamUrl(s3BucketKey: String): String {
        val sanitizedKey = s3BucketKey.trim().removePrefix("/").removePrefix("s3://")
        return "$DEFAULT_CLOUDFRONT_BASE_URL$sanitizedKey"
    }
}
