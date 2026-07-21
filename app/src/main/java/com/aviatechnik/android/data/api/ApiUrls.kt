package com.aviatechnik.android.data.api

import com.aviatechnik.android.BuildConfig

object ApiUrls {
    /**
     * The server builds absolute media URLs against ITS OWN host (APP_URL,
     * e.g. http://avia/...), which the device can't necessarily resolve.
     * Rebase them onto our API origin — path (incl. /api/mobile/...) stays,
     * the shared token works on both contours.
     */
    fun rebase(url: String?): String? {
        if (url.isNullOrBlank()) return url
        val path = url.substringAfter("://", "").substringAfter('/', "")
        if (path.isEmpty()) return url
        val origin = BuildConfig.API_BASE_URL.substringBefore("/api/")
        return "$origin/$path"
    }
}
