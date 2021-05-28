package com.siddhantkushwaha.scavenger.index

import com.siddhantkushwaha.scavenger.message.IndexRequest
import java.nio.file.*
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

class IndexApp {

    private val supportedFileExtensions = setOf("c", "cpp", "py", "java", "kt", "rs")

    private val indexManager = IndexManager()

    public fun getIndexManager(): IndexManager {
        return indexManager
    }

    public fun indexDocumentsInDirectory(path: String) {
        val pathObj = Paths.get(path).toRealPath()
        if (pathObj.isDirectory())
            Files.walk(pathObj)
                .filter { pt -> supportedFileExtensions.contains(pt.extension) }
                .forEach { pt -> indexDocument(pt) }
        else
            indexDocument(pathObj)
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

        val errorCode = indexManager.processIndexRequest(indexRequest)
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
            indexApp.indexDocumentsInDirectory("/Users/siddhantkushwaha/Documents")
        }
    }
}