package com.mangkyu.cache

enum class CacheType(
    private val desc: String,
) {
    LOCAL("로컬 캐시만 적용함"),
    GLOBAL("분산 캐시만 적용함"),
    COMPOSITE("로컬 캐시와 분산 캐시를 모두 적용함"),
}