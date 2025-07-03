package com.embabel.tripper.util

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks the validity of image links in HTML content.
 */
object ImageChecker {

    private val logger = LoggerFactory.getLogger(ImageChecker::class.java)

    private val IMG_REX = "<img[^>]*>".toRegex()
    private val SRC_REX = "src=[\"']([^\"']*)[\"']".toRegex()

    fun removeInvalidImageLinks(html: String): String {
        return runBlocking {
            val imgTags = IMG_REX.findAll(html).toList()
            val validationResults = imgTags.map { matchResult ->
                async(Dispatchers.IO) {
                    val imgTag = matchResult.value
                    val srcMatch = SRC_REX.find(imgTag)
                    val src = srcMatch?.groupValues?.get(1)

                    val isValid = src?.let { isImageUrlValid(it) } ?: false
                    matchResult to isValid
                }
            }.awaitAll()

            var result = html
            validationResults.forEach { (matchResult, isValid) ->
                if (!isValid) {
                    logger.info("Removing invalid image link: {}", matchResult.value)
                    result = result.replace(matchResult.value, "")
                }
            }
            result
        }
    }

    private suspend fun isImageUrlValid(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val responseCode = connection.responseCode
            val contentType = connection.contentType

            connection.disconnect()

            responseCode == HttpURLConnection.HTTP_OK &&
                    contentType?.startsWith("image/") == true
        } catch (e: Exception) {
            false
        }
    }
}