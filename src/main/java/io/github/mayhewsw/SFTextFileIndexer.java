package io.github.mayhewsw;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import io.github.mayhewsw.controllers.Common;
import io.github.mayhewsw.controllers.SentenceController;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 * MS: modified to allow hierarchical directory structure. Adapted code from
 *  {@url https://lucene.apache.org/core/6_4_2/demo/src-html/org/apache/lucene/demo/IndexFiles.html}
 */
public class SFTextFileIndexer {

    private static final String NAME = SFTextFileIndexer.class.getCanonicalName();
    private static final Logger logger = LoggerFactory.getLogger(SFTextFileIndexer.class);

    private static int numUpdated = 0;
    private static int numAdded = 0;


    private static Analyzer analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer source = new WhitespaceTokenizer();
            TokenStream filter = new ShingleFilter(source, 6);
            return new TokenStreamComponents(source, filter);
        }

    };

    /**
     * Filedir holds the  files that are to be indexed.
     * @param filedir
     * @param indexDir
     * @throws IOException
     */
    public static void buildindex(String filedir, String indexDir) throws Exception {

        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(dir, config);

//        TextAnnotation ta;

        SFTextFileIndexer.indexDocs(writer, Paths.get(filedir));
//        File file = new File(filedir);
//
//        for (File fname : file.listFiles()) {
//            // read each file separately...
//            if (isConll) {
//                CoNLLNerReader cnr = new CoNLLNerReader(fname.getAbsolutePath());
//                ta = cnr.next();
//            }
//            else
//                ta = SerializationHelper.deserializeFromJson(LineIO.slurp(fname.getAbsolutePath()));
//
//            StringReader sr = new StringReader(ta.getTokenizedText());
//
//            Document d = new Document();
//            d.add(new TextField("body", sr));
//            d.add(new StringField("filename", ta.getId(), Field.Store.YES));
//            writer.addDocument(d);
//        }
//
        writer.close();
    }


    /**
     * This reads documents one at a time, and builds an index of sentences.
     * @param conlldir
     * @param origfiledir
     * @param indexDir
     * @throws IOException
     */
    public static void buildsentenceindex(String conlldir, String origfiledir, String indexDir) throws IOException {
        // we write to this open file object.

        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(dir, config);

        TextAnnotation ta;
        File file = new File(conlldir);

        //int k =0;

        for(File fname : file.listFiles()){
            CoNLLNerReader cnr = new CoNLLNerReader(fname.getAbsolutePath());
            CoNLLNerReader origcnr = new CoNLLNerReader(origfiledir + "/" + fname.getName());

            ta = cnr.next();
            View sentview = ta.getView(ViewNames.SENTENCE);
            List<Constituent> sentences = sentview.getConstituents();

            ta = origcnr.next();
            View origsentview = ta.getView(ViewNames.SENTENCE);
            List<Constituent> origsentences = origsentview.getConstituents();

            if(sentences.size() != origsentences.size()) {
                System.err.println("Sentences aren't the same size!");
                continue;
            }


            for(int i = 0; i < sentences.size(); i++){
                Constituent sent = sentences.get(i);
                Constituent origsent = origsentences.get(i);

                //StringReader sr = new StringReader(sent.getTokenizedSurfaceForm());

                Document d = new Document();
                TextField tf = new TextField(Common.SF_TEXT, sent.getTokenizedSurfaceForm(), Field.Store.YES);
                //TextField tf = new TextField("body", sr, Field.Store.YES);
                d.add(tf);
                d.add(new StringField("filename", SentenceController.getSentId(sent), Field.Store.YES));

                TextField origtf = new TextField("origbody", origsent.getTokenizedSurfaceForm(), Field.Store.YES);
                d.add(origtf);

                writer.addDocument(d);
            }
        }

        writer.close();
    }

    /**
     * A simple function to test the index that you built using buildsentenceindex.
     * @param indexdir
     */
    public static void testindex(String indexdir) throws IOException {
        //=========================================================
        // Now search
        //=========================================================
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
        IndexSearcher searcher = new IndexSearcher(reader);


        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));

        String s = "";
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out.println("Enter the search query (q=quit): ");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }
                QueryParser parser = new QueryParser("body", analyzer);
                parser.setAllowLeadingWildcard(true);

                //Query q = parser.parse("*" + s + "*");

//                Query q = new PrefixQuery(new Term("body", s));

                Query q = new TermQuery(new Term(Common.SF_TEXT, s));

                System.out.println(q);
                TopScoreDocCollector collector = TopScoreDocCollector.create(40);
                searcher.search(q, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;

                //System.out.println("There are total of: " + searcher.count(q) + " hits.");

                // 4. display results
                System.out.println("Found " + hits.length + " hits.");
                for(int i=0; i<hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);

                    String[] b = d.get("body").split(" ");
                    String[] ob = d.get("origbody").split(" ");

                    for(int j = 0; j < b.length; j++){
                        if(b[j].contains(s)){
                            System.out.println(b[j] + " " + ob[j]);
                        }
                    }

                    //System.out.println((i + 1) + ". " + d.get("body") + " score=" + hits[i].score);
                    //System.out.println((i + 1) + ". " + d.get("origbody") + " score=" + hits[i].score);
                    //System.out.println((i + 1) + ". " + d.get("filename") + " score=" + hits[i].score);

                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error searching " + s + " : " + e.getMessage());
            }
        }

        reader.close();
    }


    static void indexDocs(final IndexWriter writer, Path path) throws Exception {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    /** Indexes a single document stored as json-serialized TextAnnotation */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws Exception {

        String filePath = file.toFile().getAbsolutePath();
        TextAnnotation ta = SerializationHelper.deserializeFromJson(LineIO.slurp(filePath));
        // make a new, empty document
        Document doc = new Document();

        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField("path", file.toString(), Field.Store.YES);
        doc.add(pathField);

        // Add the last modified date of the file a field named "modified".
        // Use a LongPoint that is indexed (i.e. efficiently filterable with
        // PointRangeQuery).  This indexes to milli-second resolution, which
        // is often too fine.  You could instead create a number based on
        // year/month/day/hour/minutes/seconds, down the resolution you require.
        // For example the long value 2011021714 would mean
        // February 17, 2011, 2-3 PM.
        doc.add(new LongPoint("modified", lastModified));

//      using a reader results in field NOT being stored. Go figure.
//        doc.add(new TextField("body", new BufferedReader(new StringReader(ta.getTokenizedText()))));
//        doc.add(new TextField("origbody", new BufferedReader(new StringReader(ta.getText()))));

        doc.add(new TextField(Common.SF_TEXT, ta.getTokenizedText(), Field.Store.YES));
        doc.add(new TextField("origbody", ta.getTokenizedText(), Field.Store.YES));

        if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            logger.debug("adding {}", file);
            writer.addDocument(doc);
            numAdded++;
            if (numAdded % 1000 == 0)
                logger.info("indexing: added {} docs so far.", numAdded);
        } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            logger.debug("updating {}", file);
            writer.updateDocument(new Term("path", file.toString()), doc);
            numUpdated++;
            if (numUpdated % 1000 == 0)
                logger.info("indexing: updated {} docs so far.", numUpdated);
        }
    }

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
