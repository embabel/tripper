package com.embabel.example.travel;

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Necessary to avoid putting the Google Maps API key in image URLs.
 */
@RestController
@RequestMapping("api/v1/maps")
class SafeMappingController(
    @Value("\${GOOGLE_MAPS_API_KEY}")
    private val googleMapsApiKey: String,
    private val restTemplate: RestTemplate,
) {

    private val logger = LoggerFactory.getLogger(SafeMappingController::class.java)

    @GetMapping("/image")
    fun getMapImage(
        @RequestParam("locations") locations: String,
        @RequestParam(value = "size", defaultValue = "600x400") size: String,
        @RequestParam(value = "maptype", defaultValue = "roadmap") mapType: String
    ): ResponseEntity<ByteArray> {

        return try {
            // Parse the pipe-separated encoded locations
            val locationList = locations.split("|").filter { it.isNotBlank() }

            if (locationList.isEmpty()) {
                return ResponseEntity.badRequest().build()
            }

            // Parse size parameter
            val (width, height) = parseSize(size)

            // Build the Google Static Maps URL
            val mapUrl = buildStaticMapUrl(locationList, width, height, mapType)

            // Fetch the image from Google Maps API
            val imageBytes = fetchImageFromUrl(mapUrl)
                ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()

            // Return the image with proper headers
            val headers = HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
//                cacheControl = CacheControl.maxAge(Duration.ofHours(1))
            }

            ResponseEntity.ok()
                .headers(headers)
                .body(imageBytes)

        } catch (e: Exception) {
            logger.error("Error generating map image: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun parseSize(size: String): Pair<Int, Int> {
        return try {
            val parts = size.split("x")
            if (parts.size == 2) {
                val width = parts[0].toIntOrNull() ?: DEFAULT_WIDTH
                val height = parts[1].toIntOrNull() ?: DEFAULT_HEIGHT
                Pair(
                    if (width > 0 && width <= 2048) width else DEFAULT_WIDTH,
                    if (height > 0 && height <= 2048) height else DEFAULT_HEIGHT
                )
            } else {
                Pair(DEFAULT_WIDTH, DEFAULT_HEIGHT)
            }
        } catch (e: Exception) {
            Pair(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        }
    }

    private fun buildStaticMapUrl(
        encodedLocations: List<String>,
        width: Int,
        height: Int,
        mapType: String
    ): String {
        val baseUrl = STATIC_MAPS_BASE_URL
        val params = mutableListOf(
            "size=${width}x$height",// Width x Height in pixels
            "maptype=$mapType", // roadmap, satellite, terrain, hybrid
            "key=$googleMapsApiKey",
        )

        // Add markers for each location
        encodedLocations.forEachIndexed { index, location ->
            params.add("markers=color:red%7Clabel:${index + 1}%7C$location")
        }

        // If multiple locations, add path between them
        if (encodedLocations.size > 1) {
            params.add("path=color:0x0000ff%7Cweight:3%7C${encodedLocations.joinToString("%7C")}")
        }

        return "$baseUrl?${params.joinToString("&")}"
    }

    private fun fetchImageFromUrl(imageUrl: String): ByteArray? {
        return try {
            logger.debug("Fetching image from URL: {}", imageUrl)

            // Create URI directly to avoid double encoding
            val uri = URI.create(imageUrl)
            logger.debug("Created URI: {}", uri)

            // Set proper headers for the request
            val headers = HttpHeaders().apply {
                set("User-Agent", "Mozilla/5.0 (compatible; StaticMapProxy/1.0)")
                accept = listOf(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG, MediaType.ALL)
            }

            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(uri, HttpMethod.GET, entity, ByteArray::class.java)

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                response.body
            } else {
                logger.warn("Failed to fetch image: Status ${response.statusCode}")
                null
            }

        } catch (e: Exception) {
            logger.warn("Exception fetching image from Google Maps API: ${e.message}")
            null
        }
    }


    private fun fetchImageFromUrl2(imageUrl: String): ByteArray? {
        return try {
            val restTemplate = RestTemplate()
            val response = restTemplate.getForEntity(imageUrl, ByteArray::class.java)

            if (response.statusCode == HttpStatus.OK) {
                response.body
            } else null

        } catch (e: Exception) {
            logger.warn("Failed to fetch image from Google Maps API: ${e.message}")
            null
        }
    }

    @GetMapping("/map-url")
    fun getMapUrl(@RequestParam("locations") locations: String): ResponseEntity<Map<String, String>> {
        return try {
            val locationList = locations.split(",").filter { it.isNotBlank() }

            if (locationList.isEmpty()) {
                return ResponseEntity.badRequest().build()
            }

            // Generate the interactive Google Maps URL (safe to share)
            val interactiveUrl = buildInteractiveMapUrl(locationList)

            val response = mapOf("mapUrl" to interactiveUrl)

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun buildInteractiveMapUrl(locations: List<String>): String {
        return if (locations.size == 1) {
            "https://www.google.com/maps/search/?api=1&query=${locations.first()}"
        } else {
            "https://www.google.com/maps/dir/${locations.joinToString("/")}"
        }
    }

    companion object {
        private const val STATIC_MAPS_BASE_URL = "https://maps.googleapis.com/maps/api/staticmap"
        private const val DEFAULT_WIDTH = 600
        private const val DEFAULT_HEIGHT = 400
    }
}