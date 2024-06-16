package com.mangkyu.cache;

import java.time.Duration

enum class CacheName(
    val cacheName: String,
    val expiredAfterWrite: Duration,
    val cacheType: CacheType,
) {

    LOCAL_ONLY(
        CacheKey.LOCAL_ONLY,
        Duration.ofMinutes(10),
        CacheType.LOCAL,
    ),

    GLOBAL_ONLY(
        CacheKey.GLOBAL_ONLY,
        Duration.ofMinutes(10),
        CacheType.GLOBAL,
    ),

    COMPOSITE_ALL(
        CacheKey.COMPOSITE,
        Duration.ofMinutes(10),
        CacheType.COMPOSITE,
    ),
    ;

    companion object {
        private val CARD_CACHE_MAP = entries.associateBy { it.cacheName }

        fun isCompositeType(cacheName: String): Boolean {
            return get(cacheName).cacheType == CacheType.COMPOSITE
        }

        private fun get(cacheName: String): CacheName {
            return CARD_CACHE_MAP[cacheName]
                ?: throw NoSuchElementException("$cacheName Cache Name Not Found")
        }
    }
}

object CacheKey {
    const val LOCAL_ONLY = "local"
    const val GLOBAL_ONLY = "global"
    const val COMPOSITE = "composite"
}
