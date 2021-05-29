package com.siddhantkushwaha.scavenger.index

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.commons.text.StringEscapeUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.readText


object IndexApp {

    private val supportedFileExtensions = setOf(
        "c", "cpp", "py", "java", "kt", "rs",   // code
        "kts", "gradle", "json", "xml",         // project configurations
        "csv", "txt"                            // data
    )

    private val gson = Gson()

    public fun getDocument(docId: Int): JsonObject? {
        val document = IndexManager.getDoc(docId) ?: return null

        val docResponse = JsonObject()
        val stringKeys = arrayOf("key", "name", "description", "data", "fileExtension", "dataSource")

        docResponse.addProperty("id", docId)
        stringKeys.forEach {
            val value = StringEscapeUtils.unescapeJava(document.get(it))
            docResponse.addProperty(it, value)
        }

        return docResponse
    }

    public fun search(text: String, limit: Int): JsonObject {
        val resultResponse = JsonObject()

        val results = IndexManager.searchDocs(text, fields = null, limit = limit)
        val highlights = IndexManager.getHighlights(text, results, 3, 50)

        resultResponse.addProperty("totalDocuments", IndexManager.totalDocuments())
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

    public fun indexDocumentsInDirectory(
        path: Path,
        adjustRequestCallback: (IndexRequest) -> Unit
    ) {
        val indexedDocs = ArrayList<String>()
        if (path.isDirectory()) {
            Files.walk(path)
                .filter { pt ->
                    !pt.isHidden() && supportedFileExtensions.contains(pt.extension)
                }
                .forEach { pt ->
                    val errorCode = indexDocument(pt, adjustRequestCallback)
                    if (errorCode == 0)
                        indexedDocs.add(pt.toString())
                }

            IndexManager.commit()
        }
    }

    private fun indexDocument(
        path: Path,
        adjustRequestCallback: (IndexRequest) -> Unit
    ): Int {
        val key = path.toString()
        val content = path.readText()
        val title = getAttribute(content, "Title")
        val description = getAttribute(content, "Description")
        val extension = path.extension
        val source = "disk"

        // default behavior
        val indexRequest = IndexRequest()
        indexRequest.key = key
        indexRequest.name = title ?: ""
        indexRequest.description = description ?: ""
        indexRequest.data = content
        indexRequest.fileExtension = extension
        indexRequest.dataSource = source

        // for agent specific behavior
        adjustRequestCallback(indexRequest)

        val errorCode = IndexManager.processIndexRequest(indexRequest, commit = false)
        if (errorCode > 0)
            println("Failed to index file [$key], error code [$errorCode].")
        else
            println("Indexed file [$key]")

        return errorCode
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