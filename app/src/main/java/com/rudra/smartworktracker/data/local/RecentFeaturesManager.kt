package com.rudra.smartworktracker.data.local

import android.content.Context

class RecentFeaturesManager(context: Context) {
    private val prefs = context.getSharedPreferences("recent_features", Context.MODE_PRIVATE)
    private val legacyKey = "recent_routes"
    private val key = "recent_routes_ordered"
    private val maxRecents = 10

    fun addRecentFeature(route: String) {
        val recents = getRecentFeatureRoutes().toMutableList()
        recents.remove(route) // Remove if already exists to move it to the front
        recents.add(0, route) // Add to the beginning
        val updatedRecents = recents.take(maxRecents)
        // Stored as one delimited string: a StringSet does not keep most-recent-first order
        prefs.edit()
            .remove(legacyKey)
            .putString(key, updatedRecents.joinToString(SEPARATOR))
            .apply()
    }

    fun getRecentFeatureRoutes(): List<String> {
        prefs.getString(key, null)?.let { stored ->
            return stored.split(SEPARATOR).filter { it.isNotEmpty() }
        }
        // Unordered value written by older versions
        return prefs.getStringSet(legacyKey, emptySet())?.toList() ?: emptyList()
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}
