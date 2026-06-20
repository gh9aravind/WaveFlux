package com.example.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An OkHttp interceptor that attaches Auth0 or Firebase Auth bearer tokens
 * to API requests targeting our Node.js/Express production stack.
 */
class Auth0FirebaseTokenInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    private val TAG = "Auth0FirebaseInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Get authentication token from Auth0 / Firebase source securely
        val jwtToken = tokenProvider()

        if (!jwtToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $jwtToken")
            Log.d(TAG, "Successfully injected Auth0/Firebase JWT bearer token into network request.")
        } else {
            Log.w(TAG, "Request sent without a bearer token. Endpoint might require authorization headers.")
        }

        // Tag client agent for tracking (React Native/Android client indicator)
        requestBuilder.header("X-Client-Platform", "Android-Native-SoundSpot")
        requestBuilder.header("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
