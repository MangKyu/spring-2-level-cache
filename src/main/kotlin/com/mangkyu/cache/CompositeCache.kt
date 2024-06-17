package com.mangkyu.cache

import org.springframework.cache.Cache
import java.util.concurrent.Callable

data class CompositeCache(
    private val caches: List<Cache>,
    private val updatableCacheManager: UpdatableCacheManager,
) : Cache {

    override fun getName(): String {
        return caches.first().name
    }

    override fun getNativeCache(): Any {
        return caches.map { it.nativeCache }
    }

    override fun get(key: Any): Cache.ValueWrapper? {
        for (cache in caches) {
            val valueWrapper = cache.get(key)
            val value = valueWrapper?.get()
            if (valueWrapper != null && value != null) {
                updatableCacheManager.update(cache, key, value)
                return valueWrapper
            }
        }

        return null
    }

    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        for (cache in caches) {
            val value = cache.get(key, type)
            if (value != null) {
                updatableCacheManager.update(cache, key, value)
                return value
            }
        }

        return null
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        for (cache in caches) {
            val value = cache.get(key, valueLoader)
            if (value != null) {
                updatableCacheManager.update(cache, key, value)
                return value
            }
        }

        return null
    }

    override fun put(key: Any, value: Any?) {
        for (cache in caches) {
            cache.put(key, value)
        }
    }

    override fun evict(key: Any) {
        for (cache in caches) {
            cache.evict(key)
        }
    }

    override fun clear() {
        for (cache in caches) {
            cache.clear()
        }
    }
}