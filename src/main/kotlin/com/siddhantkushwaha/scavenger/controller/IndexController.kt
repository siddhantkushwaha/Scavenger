package com.siddhantkushwaha.scavenger.controller

import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.index.IndexApp
import com.siddhantkushwaha.scavenger.message.IndexRequest
import com.siddhantkushwaha.scavenger.message.IndexResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
class IndexController {

    private val indexApp = IndexApp()

    @GetMapping("/")
    fun base(): String {
        return "Server is running."
    }

    @GetMapping("/search")
    fun requestSearch(
        @RequestParam(required = true) query: String,
        @RequestParam(required = false, defaultValue = "20") limit: Int
    ): JsonObject {
        return indexApp.search(query, limit)
    }

    @ResponseBody
    @GetMapping("/get")
    fun requestGet(
        @RequestParam(required = true) docId: Int,
    ): JsonObject {
        return indexApp.getDocument(docId) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND, "Document not found."
        )
    }

    @PostMapping("/index/write")
    fun requestWrite(@RequestBody request: IndexRequest): IndexResponse {
        val key = request.key ?: throw Exception("Document name cannot be null.")
        val errorCode = indexApp.getIndexManager().processIndexRequest(request, commit = false)

        return IndexResponse(
            docKey = key,
            statusCode = errorCode
        )
    }

    @PostMapping("/index/commit")
    fun requestCommit(): JsonObject {
        val errorCode = indexApp.getIndexManager().commit()

        val response = JsonObject()
        response.addProperty("statusCode", errorCode)
        return response
    }
}