package com.siddhantkushwaha.scavenger.controller

import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.index.IndexApp
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
class IndexController {

    private val indexApp = IndexApp()

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
}