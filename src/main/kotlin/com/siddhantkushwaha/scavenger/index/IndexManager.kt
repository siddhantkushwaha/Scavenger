package com.siddhantkushwaha.scavenger.index

import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.search.IndexSearcher
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

    private val indexReader: IndexReader
    private val indexSearcher: IndexSearcher
    private val indexWriter: IndexWriter

    init {
        indexDirectory = FSDirectory.open(Paths.get(indexPath))

        val analyzer: Analyzer = StandardAnalyzer()
        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND

        indexWriter = IndexWriter(indexDirectory, indexWriterConfig)

        indexReader = DirectoryReader.open(indexWriter)
        indexSearcher = IndexSearcher(indexReader)
    }

    public fun totalDocuments(): Int {
        return indexReader.numDocs()
    }

    public fun commit(): Long {
        return indexWriter.commit()
    }

    public fun processIndexRequest(request: IndexRequest, commit: Boolean): Int {
        val docKey = request.key ?: return 2
        val docName = request.name ?: return 2
        val docDescription = request.description ?: return 2
        val docData = request.data ?: return 2

        return indexDocument(docKey, docName, docDescription, docData, commit)
    }

    private fun indexDocument(
        docKey: String,
        docName: String,
        docDescription: String,
        docData: String,
        commit: Boolean
    ): Int {
        try {
            val document = Document()

            document.add(StringField(keyKey, docKey, Field.Store.YES))
            document.add(TextField(keyName, docName, Field.Store.YES))
            document.add(TextField(keyDescription, docDescription, Field.Store.YES))
            document.add(TextField(keyData, docData, Field.Store.YES))

            val documentTerm = Term(keyKey, docKey)
            indexWriter.updateDocument(documentTerm, document)
            if (commit) indexWriter.commit()

            return 0
        } catch (e: Exception) {
            return 1
        }
    }
}