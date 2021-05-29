package com.siddhantkushwaha.scavenger.index

import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths


object IndexManager {

    private const val indexPath = "index"

    private const val keyKey = "key"
    private const val keyPath = "path"
    private const val keyName = "name"
    private const val keyDescription = "description"
    private const val keyData = "data"
    private const val keyExtension = "fileExtension"
    private const val keyDataSource = "dataSource"

    private val indexDirectory: Directory

    private val analyzer: Analyzer = StandardAnalyzer()

    private val indexReader: IndexReader
    private val indexSearcher: IndexSearcher
    private val indexWriter: IndexWriter

    init {
        indexDirectory = FSDirectory.open(Paths.get(indexPath))

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

    public fun getDoc(docId: Int): Document? {
        return indexSearcher.doc(docId)
    }

    public fun searchDocs(
        queryText: String,
        fields: Array<String>?,
        limit: Int
    ): TopFieldDocs {
        val booleanQueryBuilder = BooleanQuery.Builder()
        val sort = Sort()

        // there might be other indexed attributes too, but only lookup these by default
        val fieldsToSearchIn = fields ?: arrayOf(keyPath, keyName, keyDescription, keyData)
        fieldsToSearchIn.forEach { field ->
            val qp = QueryParser(field, analyzer)
            val query = qp.parse(queryText)
            booleanQueryBuilder.add(query, BooleanClause.Occur.SHOULD)
        }

        val finalQuery = booleanQueryBuilder.build()
        return indexSearcher.search(finalQuery, limit, sort)
    }

    public fun getHighlights(
        queryText: String,
        results: TopFieldDocs,
        numFragments: Int,
        highlightLength: Int
    ): HashMap<Int, Array<String>> {
        val resultMap = HashMap<Int, Array<String>>()

        val qp = QueryParser(keyData, analyzer)
        val query = qp.parse(queryText)

        val formatter = SimpleHTMLFormatter()

        val scorer = QueryScorer(query)
        val highlighter = Highlighter(formatter, scorer)
        val fragmenter = SimpleSpanFragmenter(scorer, highlightLength)
        highlighter.textFragmenter = fragmenter

        results.scoreDocs.forEach { scoreDoc ->
            val document = getDoc(scoreDoc.doc) ?: return@forEach

            val content = document.get(keyData)
            val stream = analyzer.tokenStream(keyData, content)
            val fragments = highlighter.getBestFragments(stream, content, numFragments)
            resultMap[scoreDoc.doc] = fragments
        }
        return resultMap
    }

    public fun deleteDocument(docKey: String): Int {
        return try {
            val documentTerm = Term(keyKey, docKey)
            indexWriter.deleteDocuments(documentTerm)
            0
        } catch (e: Exception) {
            1
        }
    }

    public fun processIndexRequest(request: IndexRequest, commit: Boolean): Int {
        val docKey = request.key ?: return 2
        val docName = request.name ?: return 2
        val docDescription = request.description ?: return 2
        val docData = request.data ?: return 2
        val docExtension = request.fileExtension ?: return 2
        val docSource = request.dataSource ?: return 2

        return indexDocument(docKey, docName, docDescription, docData, docExtension, docSource, commit)
    }

    private fun indexDocument(
        docKey: String,
        docName: String,
        docDescription: String,
        docData: String,
        docExtension: String,
        docSource: String,
        commit: Boolean
    ): Int {
        try {
            val document = Document()

            document.add(StringField(keyKey, docKey, Field.Store.YES))

            document.add(TextField(keyPath, docKey, Field.Store.YES))
            document.add(TextField(keyName, docName, Field.Store.YES))
            document.add(TextField(keyDescription, docDescription, Field.Store.YES))
            document.add(TextField(keyData, docData, Field.Store.YES))

            document.add(StringField(keyExtension, docExtension, Field.Store.YES))
            document.add(StringField(keyDataSource, docSource, Field.Store.YES))

            val documentTerm = Term(keyKey, docKey)
            indexWriter.updateDocument(documentTerm, document)
            if (commit) commit()

            return 0
        } catch (e: Exception) {
            return 1
        }
    }
}