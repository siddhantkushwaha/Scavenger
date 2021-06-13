package com.siddhantkushwaha.scavenger.index

import org.apache.lucene.search.highlight.Formatter
import org.apache.lucene.search.highlight.TokenGroup

class CustomFormatter : Formatter {
    private val preTag = ""
    private val postTag = ""
    override fun highlightTerm(originalText: String, tokenGroup: TokenGroup): String {
        return if (tokenGroup.totalScore <= 0) {
            originalText
        } else preTag + originalText + postTag
    }
}