package com.mangkyu.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.CompositeCacheManager

class CompositeCacheCacheManager(
    private val cacheManagers: List<CacheManager>,
    private val updatableCacheManager: UpdatableCacheManager,
) : CompositeCacheManager() {

    override fun getCache(name: String): Cache? {
        if (CacheName.isCompositeType(name)) {
            return CompositeCache(
                caches = cacheManagers.mapNotNull { it.getCache(name) },
                updatableCacheManager = updatableCacheManager,
            )
        }

        return cacheManagers
            .asSequence()
            .map { it.getCache(name) }
            .firstOrNull { it != null }
    }
}