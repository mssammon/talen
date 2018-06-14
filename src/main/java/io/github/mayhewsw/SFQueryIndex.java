package io.github.mayhewsw;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import io.github.mayhewsw.controllers.Common;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Build queries to find relevant examples in text collection.
 *
 * @author mssammon
 */
public class SFQueryIndex {

    private static final Logger logger = LoggerFactory.getLogger(SFQueryIndex.class);
    private static final int NUM_TOP_RESULTS = 100;

    private final IndexReader reader;
    private final IndexSearcher searcher;

    private static Analyzer analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer source = new WhitespaceTokenizer();
            TokenStream filter = new ShingleFilter(source, 6);
            return new TokenStreamComponents(source, filter);
        }

    };


    public SFQueryIndex(String indexDir) throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        searcher = new IndexSearcher(reader);
    }

    public List<Pair<String, String>> conjunctiveSearch(String[] terms, boolean isPrefix) throws IOException {
        List<Pair<String, String>> texts = new ArrayList<>();

        Query q = buildBooleanQuery(terms, isPrefix);
        logger.debug("Query: {}", q.toString());

        TopScoreDocCollector collector = TopScoreDocCollector.create(NUM_TOP_RESULTS);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        //System.out.println("There are total of: " + searcher.count(q) + " hits.");

        logger.debug("Found {} hits.", hits.length);
        for(int i=0; i<hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            texts.add(new Pair(d.get(Common.SF_PATH), d.get(Common.SF_TEXT)));
        }
        return texts;
    }



    private Query buildBooleanQuery(String[] terms, boolean isPrefix) {
        List<Query> componentQueries = new ArrayList<>();

        for (String term : terms) {
            Query query = null;
            if (isPrefix)
                query = new PrefixQuery(new Term(Common.SF_TEXT, term));
            else
                query = new TermQuery(new Term(Common.SF_TEXT, term));
            componentQueries.add(query);
        }

        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        for (Query q : componentQueries)
            bqb.add(q, BooleanClause.Occur.SHOULD);

        return bqb.build();
    }


    public static void main(String[] args) {

    }

}
