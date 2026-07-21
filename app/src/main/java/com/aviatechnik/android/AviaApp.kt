package com.aviatechnik.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class AviaApp : Application(), ImageLoaderFactory {

    // Shared OkHttp client: Coil image requests get the same Bearer/Host
    // headers as API calls — media endpoints are token-protected.
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this).okHttpClient(okHttpClient).build()
}
