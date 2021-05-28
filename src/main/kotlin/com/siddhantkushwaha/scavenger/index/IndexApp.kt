package com.siddhantkushwaha.scavenger.index

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.readText


class IndexApp {
    private val supportedFileExtensions = setOf("c", "cpp", "py", "java", "kt", "rs")

    private val gson = Gson()

    private val indexManager = IndexManager()

    public fun getIndexManager(): IndexManager {
        return indexManager
    }

    public fun getDocument(docId: Int): JsonObject? {
        val document = indexManager.getDoc(docId) ?: return null

        val docResponse = JsonObject()
        val stringKeys = arrayOf("key", "name", "description", "data")

        docResponse.addProperty("id", docId)
        stringKeys.forEach {
            val value = StringEscapeUtils.unescapeJava(document.get(it))
            docResponse.addProperty(it, value)
        }

        return docResponse
    }

    public fun search(text: String, limit: Int): JsonObject {
        val resultResponse = JsonObject()

        val results = indexManager.searchDocs(text, limit)
        val highlights = indexManager.getHighlights(text, results, 3, 50)

        resultResponse.addProperty("totalDocuments", indexManager.totalDocuments())
        resultResponse.addProperty("totalHits", results.totalHits)
        val docList = ArrayList<JsonObject>()

        results.scoreDocs.forEach { scoreDoc ->
            val document = getDocument(scoreDoc.doc) ?: return@forEach

            val highlightsForDoc = highlights.get(scoreDoc.doc)

            // add highlights to same doc
            document.add("highlights", gson.toJsonTree(highlightsForDoc))

            docList.add(document)
        }

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
}