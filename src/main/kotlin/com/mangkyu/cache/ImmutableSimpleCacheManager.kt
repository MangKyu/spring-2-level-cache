package com.mangkyu.cache

import jakarta.annotation.PostConstruct
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.lang.Nullable
import java.util.concurrent.ConcurrentHashMap

class ImmutableSimpleCacheManager(
    private val caches: List<Cache> = emptyList(),
) : CacheManager, UpdatableCacheManager {

    private var cacheMap: Map<String, Cache> = emptyMap()

    @Volatile
    private var cacheNames: Set<String> = emptySet()

    @PostConstruct
    fun initializeCaches() {
        val cacheNames = LinkedHashSet<String>(caches.size)
        val cacheMap = ConcurrentHashMap<String, Cache>(16)
        for (cache in caches) {
            val name = cache.name
            cacheMap[name] = cache
            cacheNames.add(name)
        }

        this.cacheNames = cacheNames
        this.cacheMap = cacheMap
    }

    @Nullable
    override fun getCache(name: String): Cache? {
        return cacheMap[name]
    }

    override fun getCacheNames(): Collection<String> {
        return cacheNames
    }

    override fun update(cache: Cache, key: Any, value: Any) {
        val localCache = getCache(cache.name)
        localCache?.putIfAbsent(key, value)
    }
}

interface UpdatableCacheManager {

    fun update(cache: Cache, key: Any, value: Any)

}
