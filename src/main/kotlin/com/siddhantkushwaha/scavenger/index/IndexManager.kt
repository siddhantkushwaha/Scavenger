package com.siddhantkushwaha.scavenger.index

import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths


class IndexManager {

    private val indexPath = "index"

    private val keyKey = "key"
    private val keyName = "name"
    private val keyDescription = "description"
    private val keyData = "data"

    private val indexDirectory: Directory
    private val indexWriter: IndexWriter

    init {
        indexDirectory = FSDirectory.open(Paths.get(indexPath))

        val analyzer: Analyzer = StandardAnalyzer()
        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriter = IndexWriter(indexDirectory, indexWriterConfig)
    }

    public fun processIndexRequest(request: IndexRequest): Int {
        val docKey = request.key ?: return 2
        val docName = request.name ?: return 2
        val docDescription = request.description ?: return 2
        val docData = request.data ?: return 2

        return indexDocument(docKey, docName, docDescription, docData)
    }

    private fun indexDocument(docKey: String, docName: String, docDescription: String, docData: String): Int {
        try {
            val document = Document()

            document.add(TextField(keyKey, docKey, Field.Store.YES))
            document.add(TextField(keyName, docName, Field.Store.YES))
            document.add(TextField(keyDescription, docDescription, Field.Store.YES))
            document.add(TextField(keyData, docData, Field.Store.YES))

            val documentTerm = Term(docKey)
            indexWriter.updateDocument(documentTerm, document)

            return 0
        } catch (e: Exception) {
            return 1
        }
    }
}