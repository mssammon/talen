package io.github.mayhewsw;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
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
    private static final String NAME = SFQueryIndex.class.getCanonicalName();

    private static final Logger logger = LoggerFactory.getLogger(SFQueryIndex.class);
    private int numResultLimit;

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


    public SFQueryIndex(String indexDir, int numResultLimit) throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        searcher = new IndexSearcher(reader);
        this.numResultLimit = numResultLimit;
    }

    public List<Pair<String, String>> conjunctiveSearch(String[] terms, boolean isPrefix) throws IOException {
        List<Pair<String, String>> texts = new ArrayList<>();

        Query q = buildBooleanQuery(terms, isPrefix);
        logger.debug("Query: {}", q.toString());

        TopScoreDocCollector collector = TopScoreDocCollector.create(numResultLimit);
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


    /**
     * reads a set of query files, each with a set of keywords, and retrieve the top K relevant documents.
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 4) {
            System.out.println("Usage: " + NAME + " indexDir queryFileDir queryOutDir numResults");
            System.exit(-1);
        }

        final int TOP_K = Integer.parseInt(args[3]);

        SFQueryIndex sfqi = new SFQueryIndex(args[0], TOP_K);
        String qFileDir = args[1];
        String[] queryFiles = IOUtils.ls(qFileDir);

        String qOutDir = args[2];

        for (String qFile : queryFiles) {
            String qFilePath = qFileDir + "/" + qFile;
//            String outDir = qOutDir + "/" + qFile;
            ArrayList<String> qTerms = LineIO.read(qFilePath);
            String[] qTermArray = new String[qTerms.size()];
            List<Pair<String, String>> results = sfqi.conjunctiveSearch(qTerms.toArray(qTermArray), false);
            String outFile = qOutDir + "/" + qFile;
            List<String> resFiles = new ArrayList<>(results.size());
            System.out.println("found " + results.size() + " results:");

            for (int i = 0; i < Math.min(TOP_K, results.size()); ++i) {
                Pair<String, String> res = results.get(i);
                resFiles.add(res.getFirst());
//                System.out.println("Text: " + res.getSecond());
//                System.out.println("---------------------");
//                String outFile = outDir + "/" + IOUtils.getFileName(res.getFirst()) + ".txt";
            }
            logger.info("writing results to file {}", outFile);
            LineIO.write(outFile, resFiles);

        }
    }

}
