package com.siddhantkushwaha.scavenger.controller

import com.siddhantkushwaha.scavenger.index.IndexApp
import com.siddhantkushwaha.scavenger.message.IndexRequest
import com.siddhantkushwaha.scavenger.message.IndexResponse
import org.springframework.web.bind.annotation.*

@RestController
class IndexController {

    private val indexApp = IndexApp()

    @GetMapping("/")
    fun base(): String {
        return "Server is running."
    }

    @PostMapping("/index/write")
    fun func(@RequestBody request: IndexRequest): IndexResponse {

        val docName = request.name ?: throw Exception("Document name cannot be null.")
        val errorCode = indexApp.getIndexManager().processIndexRequest(request, commit = true)

        return IndexResponse(
            docName = docName,
            statusCode = errorCode
        )
    }
}