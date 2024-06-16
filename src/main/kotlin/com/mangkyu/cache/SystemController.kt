package com.mangkyu.cache

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemController {

    @Autowired
    private lateinit var service: CacheService

    @GetMapping("/local")
    fun health1(): String {
        return service.localCache()

    }

    @GetMapping("/redis")
    fun health2(): String {
        return service.redisCache()
    }

    @GetMapping("/composite")
    fun health3(): String {
        return service.compositeCache()
    }
}

@Service
class CacheService {

    @Cacheable(cacheNames = [CacheKey.LOCAL_ONLY])
    fun localCache(): String {
        log.info { "localCache called" }
        return "local"
    }

    @Cacheable(cacheNames = [CacheKey.GLOBAL_ONLY])
    fun redisCache(): String {
        log.info { "redisCache called" }
        return "redis"
    }

    @Cacheable(cacheNames = [CacheKey.COMPOSITE])
    fun compositeCache(): String {
        log.info { "compositeCache called" }
        return "composite"
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}
