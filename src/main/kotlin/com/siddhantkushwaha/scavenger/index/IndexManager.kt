package com.siddhantkushwaha.scavenger.index

import com.siddhantkushwaha.scavenger.message.IndexRequest
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Paths
import java.time.Instant


object IndexManager {

    private const val indexPath = "index"

    public const val keyKey = "key"
    public const val keyPath = "path"
    public const val keyName = "name"
    public const val keyDescription = "description"
    public const val keyData = "data"
    public const val keyExtension = "fileExtension"
    public const val keyDataSource = "dataSource"
    public const val keyModifiedTime = "modifiedEpochTime"

    private val indexDirectory: Directory

    private val analyzer: Analyzer = StandardAnalyzer()

    private val indexWriter: IndexWriter

    init {
        indexDirectory = FSDirectory.open(Paths.get(indexPath))

        val indexWriterConfig = IndexWriterConfig(analyzer)
        indexWriterConfig.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND

        indexWriter = IndexWriter(indexDirectory, indexWriterConfig)
    }

    public fun totalDocuments(): Int {
        return getIndexReader().numDocs()
    }

    private fun getIndexReader(): DirectoryReader {
        return DirectoryReader.open(indexWriter)
    }

    private fun getIndexSearcher(): IndexSearcher {
        return IndexSearcher(getIndexReader())
    }

    public fun commit(): Long {
        return indexWriter.commit()
    }

    public fun getDoc(docId: Int): Document? {
        return getIndexSearcher().doc(docId)
    }

    public fun searchDocs(
        queryText: String,
        fields: Array<String>,
        limit: Int,
        escapeQuery: Boolean
    ): TopFieldDocs {
        val booleanQueryBuilder = BooleanQuery.Builder()
        val sort = Sort()

        fields.forEach { field ->
            val qp = QueryParser(field, analyzer)
            val query = qp.parse(if (escapeQuery) QueryParser.escape(queryText) else queryText)
            val clause = if (fields.size > 1) BooleanClause.Occur.SHOULD else BooleanClause.Occur.MUST
            booleanQueryBuilder.add(query, clause)
        }

        val finalQuery = booleanQueryBuilder.build()
        return getIndexSearcher().search(finalQuery, limit, sort)
    }

    public fun getHighlights(
        queryText: String,
        results: TopFieldDocs,
        numFragments: Int,
        highlightLength: Int,
        escapeQuery: Boolean
    ): HashMap<Int, Array<String>> {
        val resultMap = HashMap<Int, Array<String>>()

        val qp = QueryParser(keyData, analyzer)
        val query = qp.parse(if (escapeQuery) QueryParser.escape(queryText) else queryText)

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
            val lastModified = Instant.now().epochSecond

            val document = Document()

            document.add(StringField(keyKey, docKey, Field.Store.YES))

            document.add(TextField(keyPath, docKey, Field.Store.YES))
            document.add(TextField(keyName, docName, Field.Store.YES))
            document.add(TextField(keyDescription, docDescription, Field.Store.YES))
            document.add(TextField(keyData, docData, Field.Store.YES))

            document.add(StringField(keyExtension, docExtension, Field.Store.YES))
            document.add(StringField(keyDataSource, docSource, Field.Store.YES))

            document.add(LongPoint(keyModifiedTime, lastModified))
            document.add(StoredField(keyModifiedTime, lastModified))

            val documentTerm = Term(keyKey, docKey)
            indexWriter.updateDocument(documentTerm, document)
            if (commit) commit()

            return 0
        } catch (e: Exception) {
            return 1
        }
    }
}