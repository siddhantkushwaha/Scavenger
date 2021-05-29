package com.siddhantkushwaha.scavenger.index

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.commons.text.StringEscapeUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*


object IndexApp {

    private val supportedFileExtensions = setOf(
        "c", "cpp", "py", "java", "kt", "rs",   // code
        "kts", "gradle", "json", "xml",         // project configurations
        "csv", "txt", "md",                     // data

        // TODO notebook files need special handling
        // "ipynb"
    )

    private val gson = Gson()

    public fun getDocument(docId: Int, addContent: Boolean = true): JsonObject? {
        val document = IndexManager.getDoc(docId) ?: return null

        val docResponse = JsonObject()
        val stringKeys = arrayListOf(
            IndexManager.keyKey,
            IndexManager.keyName,
            IndexManager.keyDescription,
            IndexManager.keyExtension,
            IndexManager.keyDataSource
        )

        if (addContent)
            stringKeys.add(IndexManager.keyData)

        docResponse.addProperty("id", docId)
        stringKeys.forEach {
            val value = StringEscapeUtils.unescapeJava(document.get(it))
            docResponse.addProperty(it, value)
        }

        return docResponse
    }

    public fun search(
        text: String,
        limit: Int,
        fields: Array<String>? = null
    ): JsonObject {
        val resultResponse = JsonObject()

        // there might be other indexed attributes too, but only lookup these by default
        val fieldsToSearch = fields ?: arrayOf(
            IndexManager.keyPath,
            IndexManager.keyName,
            IndexManager.keyDescription,
            IndexManager.keyData
        )

        val results = IndexManager.searchDocs(text, fields = fieldsToSearch, limit = limit)
        val highlights = IndexManager.getHighlights(text, results, 3, 50)

        resultResponse.addProperty("totalDocuments", IndexManager.totalDocuments())
        resultResponse.addProperty("totalHits", results.totalHits)
        val docList = ArrayList<JsonObject>()

        results.scoreDocs.forEach { scoreDoc ->
            val document = getDocument(scoreDoc.doc, addContent = false) ?: return@forEach

            val highlightsForDoc = highlights[scoreDoc.doc]?.map {
                StringEscapeUtils.unescapeJava(it)
            }?.toTypedArray()

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
                    var qualify = !pt.isHidden()
                    qualify = qualify && supportedFileExtensions.contains(pt.extension)
                    qualify = qualify && pt.fileSize() <= (2 * 1024 * 1024) // don't index any file bigger than 2 MB
                    qualify
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