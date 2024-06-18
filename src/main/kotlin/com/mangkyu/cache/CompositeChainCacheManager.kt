package com.mangkyu.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.CompositeCacheManager

class CompositeCacheCacheManager(
    private val cacheManagers: List<CacheManager>,
    private val updatableCacheManager: UpdatableCacheManager,
) : CacheManager {

    private val cacheNames: List<String>

    init {
        cacheNames = mutableListOf()
        for (cacheManager in cacheManagers) {
            cacheNames.addAll(cacheManager.cacheNames)
        }
    }

    override fun getCache(name: String): Cache? {
        if (CacheName.isCompositeType(name)) {
            return CompositeCache(
                cacheManagers.mapNotNull { it.getCache(name) },
                updatableCacheManager,
            )
        }

        return cacheManagers
            .map { it.getCache(name) }
            .firstOrNull { it != null }
    }

    override fun getCacheNames(): MutableCollection<String> {
        return cacheNames.toMutableList()
    }
}