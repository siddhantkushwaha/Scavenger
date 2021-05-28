package com.siddhantkushwaha.scavenger.index

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.message.IndexRequest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

class IndexApp {

    private val supportedFileExtensions = setOf("c", "cpp", "py", "java", "kt", "rs")

    private val indexManager = IndexManager()

    public fun getIndexManager(): IndexManager {
        return indexManager
    }

    public fun search(text: String): JsonObject {
        val resultResponse = JsonObject()

        val limit = 20
        val results = indexManager.searchDocs(text, limit)

        resultResponse.addProperty("totalHits", results.totalHits)
        val docList = ArrayList<JsonObject>()

        results.scoreDocs.forEach { scoreDoc ->
            val document = indexManager.getDoc(scoreDoc.doc) ?: return@forEach

            val docResponse = JsonObject()
            docResponse.addProperty("key", document.get("key"))
            docResponse.addProperty("name", document.get("name"))
            docResponse.addProperty("description", document.get("description"))
            docResponse.addProperty("data", document.get("data"))
            docList.add(docResponse)
        }

        val gson = Gson()
        resultResponse.add("documents", gson.toJsonTree(docList))
        return resultResponse
    }

    public fun indexDocumentsInDirectory(path: String) {
        val pathObj = Paths.get(path).toRealPath()

        if (pathObj.isDirectory())
            Files.walk(pathObj)
                .filter { pt -> supportedFileExtensions.contains(pt.extension) }
                .forEach { pt -> indexDocument(pt) }
        else
            indexDocument(pathObj)

        indexManager.commit()
    }

    private fun indexDocument(path: Path) {
        val key = path.toString()
        val content = path.readText()
        val title = getAttribute(content, "Title")
        val description = getAttribute(content, "Description")

        val indexRequest = IndexRequest()
        indexRequest.key = key
        indexRequest.name = title ?: key
        indexRequest.description = description ?: key
        indexRequest.data = content

        val errorCode = indexManager.processIndexRequest(indexRequest, commit = false)
        if (errorCode > 0)
            println("Failed to index file [$key], error code [$errorCode].")
        else
            println("Indexed file [$key]")
    }

    private fun getAttribute(content: String, attributeName: String): String? {
        val attributeKey = "$attributeName - "

        val startIndex = content.indexOf(attributeKey)
        if (startIndex < 0)
            return null

        val endIndex = content.indexOf(';', startIndex = startIndex + attributeKey.length)
        if (endIndex < 0)
            return null

        if (endIndex - startIndex > 500)
            return null

        val attributeValue = content.substring(startIndex + attributeKey.length, endIndex)
        return attributeValue.trim()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val indexApp = IndexApp()

            // index everything we have
            // indexApp.indexDocumentsInDirectory("/Users/siddhantkushwaha/Documents/Workspace")

            // test search results
            val textQuery = "dijkstra~"
            indexApp.search(textQuery).getAsJsonArray("documents").forEach { docElement ->
                println(docElement.asJsonObject.get("name"))
            }
        }
    }
}