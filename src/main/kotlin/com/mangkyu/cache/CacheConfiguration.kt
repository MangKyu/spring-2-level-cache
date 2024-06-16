package com.mangkyu.cache

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfiguration(
    private val redisConnectionFactory: RedisConnectionFactory,
) {

    @Bean
    fun localCacheManager(): ImmutableSimpleCacheManager {
        return ImmutableSimpleCacheManager(
            caches = CacheName.entries
                .filter { it.cacheType == CacheType.LOCAL || it.cacheType == CacheType.COMPOSITE }
                .map { toCaffeineCache(it) },
        )
    }

    private fun toCaffeineCache(it: CacheName): CaffeineCache {
        return CaffeineCache(
            it.cacheName,
            Caffeine.newBuilder()
                .expireAfterWrite(it.expiredAfterWrite.seconds, TimeUnit.SECONDS)
                .build()
        )
    }

    @Bean
    fun redisSerializer(): RedisSerializer<Any> {
        val objectMapper = JsonMapper.builder()
            .addModules(
                listOf(
                    JavaTimeModule(),
                    KotlinModule.Builder()
                        .withReflectionCacheSize(512)
                        .configure(KotlinFeature.NullToEmptyCollection, true)
                        .configure(KotlinFeature.NullToEmptyMap, true)
                        .configure(KotlinFeature.NullIsSameAsDefault, true)
                        .configure(KotlinFeature.SingletonSupport, true)
                        .configure(KotlinFeature.StrictNullChecks, true)
                        .build()
                )
            )
            .activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
            )
            .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

        return GzipRedisSerializer(
            clazz = Any::class.java,
            objectMapper = objectMapper
        )
    }

    @Bean
    fun redisCacheManager(redisSerializer: RedisSerializer<Any>): CacheManager {
        val redisCacheConfigurationMap = CacheName.entries
            .filter { it.cacheType == CacheType.GLOBAL || it.cacheType == CacheType.COMPOSITE }
            .associate {
                it.cacheName to RedisCacheConfiguration.defaultCacheConfig()
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                    .entryTtl(it.expiredAfterWrite)
            }

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)
            .cacheWriter(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
            .withInitialCacheConfigurations(redisCacheConfigurationMap)
            .enableStatistics()
            .build()
    }

    @Bean
    @Primary
    fun cacheManager(redisSerializer: RedisSerializer<Any>): CacheManager {
        val localCacheManager = localCacheManager()
        return CompositeCacheCacheManager(
            cacheManagers = listOf(localCacheManager, redisCacheManager(redisSerializer)),
            updatableCacheManager = localCacheManager,
        )
    }
}