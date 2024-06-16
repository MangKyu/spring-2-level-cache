package com.mangkyu.cache

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

inline fun <reified T> createGzipRedisTemplate(
    clazz: Class<T>,
    objectMapper: ObjectMapper,
    redisConnectionFactory: RedisConnectionFactory,
): RedisTemplate<String, T> {
    return RedisTemplate<String, T>()
        .apply {
            setConnectionFactory(redisConnectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = GzipRedisSerializer(clazz, objectMapper)
        }
}

class GzipRedisSerializer<T>(
    private val clazz: Class<T>,
    private val objectMapper: ObjectMapper,
) : RedisSerializer<T> {

    override fun serialize(t: T?): ByteArray? {
        if (t == null) {
            return ByteArray(0)
        }

        return kotlin.runCatching {
            ByteArrayOutputStream().use { outputStream ->
                GZIPOutputStream(outputStream).use { gzipOutputStream ->
                    objectMapper.writeValue(gzipOutputStream, t)
                    gzipOutputStream.finish()
                }
                outputStream.toByteArray()
            }
        }.onFailure {
            log.error(it) { "Redis serialize fail" }
        }.getOrThrow()
    }

    override fun deserialize(bytes: ByteArray?): T? {
        if (bytes == null) {
            return null
        }

        return runCatching {
            ByteArrayInputStream(bytes).use { inputStream ->
                if (bytes.isGzipCompressed()) {
                    GZIPInputStream(inputStream).use { gzipInputStream ->
                        objectMapper.readValue(
                            gzipInputStream,
                            clazz
                        )
                    }
                } else {
                    objectMapper.readValue(inputStream, clazz)
                }
            }
        }.onFailure {
            log.error(it) { "Redis deserialize fail" }
        }.getOrThrow()
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}

private fun ByteArray.isGzipCompressed() =
    this.size >= 2 && this[0] == gzipMagicNumber[0] && this[1] == gzipMagicNumber[1]

private val gzipMagicNumber: ByteArray = byteArrayOf(0x1f.toByte(), 0x8b.toByte())
