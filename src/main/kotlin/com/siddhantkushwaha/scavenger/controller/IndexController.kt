package com.siddhantkushwaha.scavenger.controller

import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.index.IndexApp
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class IndexController {

    @GetMapping("/")
    fun home(): String {
        return "Server is running."
    }

    @GetMapping("/search")
    fun requestSearch(
        @RequestParam(required = true) query: String,
        @RequestParam(required = false, name = "field") fields: Array<String>?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "true") regex: Boolean
    ): JsonObject {
        var fieldsToSearch: Array<String>? = null
        if (fields?.isNotEmpty() == true)
            fieldsToSearch = fields

        return IndexApp.search(query, fields = fieldsToSearch, limit = limit, escapeQuery = !regex)
    }

    @ResponseBody
    @GetMapping("/get")
    fun requestGet(
        @RequestParam(required = true) docId: Int
    ): JsonObject {
        return IndexApp.getDocument(docId) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND, "Document not found."
        )
    }
}
