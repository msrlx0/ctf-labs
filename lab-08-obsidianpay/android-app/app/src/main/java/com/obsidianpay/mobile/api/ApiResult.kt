package com.obsidianpay.mobile.api

/**
 * Minimal result type for API calls: either a parsed [Success] value plus the
 * HTTP status, or an [Error] with a message and optional HTTP status.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T, val httpCode: Int) : ApiResult<T>()
    data class Error(val message: String, val httpCode: Int? = null) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
}
