package io.github.mayhewsw;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import io.github.mayhewsw.controllers.Common;
import io.github.mayhewsw.utils.SFQueryBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        Query q = SFQueryBuilder.buildDisjunctiveBooleanQuery(terms, isPrefix);
        logger.debug("Query: {}", q.toString());
        return searchWithQuery(q);
    }

    public List<Pair<String, String>> searchWithQuery(Query q) throws IOException {

        List<Pair<String, String>> texts = new ArrayList<>();
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


    public static void queryFromFileList(String qFileDir, String qOutDir, SFQueryIndex sfqi, int TOP_K) throws IOException {
        String[] queryFiles = IOUtils.ls(qFileDir);


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


    /**
     * reads a set of query files, each with a set of keywords, and retrieve the top K relevant documents.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 5) {
            System.out.println("Usage: " + NAME + " indexDir queryFile componentFileDir queryOutDir numResults");
            System.exit(-1);
        }


        final int TOP_K = Integer.parseInt(args[4]);
        final String outDir = args[3];

        SFQueryIndex sfqi = new SFQueryIndex(args[0], TOP_K);

        String DEFAULT_TOPICS = "src/main/resources/disasters.txt";
        String DEFAULT_ACTORS = "src/main/resources/actors.txt";
        String DEFAULT_VICTIMS = "src/main/resources/victims.txt";

//        SFQueryIndex.queryFromFileList(args[1], args[2], sfqi, TOP_K);

        SFQueryBuilder queryBuilder = new SFQueryBuilder(DEFAULT_TOPICS, DEFAULT_ACTORS, DEFAULT_VICTIMS, TOP_K);
        Map<String, Query> queries = queryBuilder.buildCompoundQueries(args[1], args[2]);

        for (String topic : queries.keySet()) {
            System.err.println("running query for topic '" + topic + "'....");
            String outFile = outDir + "/" + topic + "-files.txt";
            List<Pair<String, String>> results = sfqi.searchWithQuery(queries.get(topic));
            List<String> resFiles = new ArrayList<>();
            for (int i = 0; i < Math.min(TOP_K, results.size()); ++i) {
                Pair<String, String> res = results.get(i);
                resFiles.add(res.getFirst());
            }
            System.err.println("found " + resFiles.size() + " results for topic '" + topic + "'.");
            LineIO.write(outFile, resFiles);
        }
    }

}
