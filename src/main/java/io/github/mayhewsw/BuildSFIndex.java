package io.github.mayhewsw;

import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.annotation.XmlTextAnnotationMaker;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.XmlTextAnnotation;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.XmlDocumentProcessor;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.XmlDocumentReader;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.aceReader.SimpleXMLParser;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import io.github.mayhewsw.utils.FindMatchingFiles;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static edu.illinois.cs.cogcomp.core.io.IOUtils.isDirectory;
import static edu.illinois.cs.cogcomp.core.io.IOUtils.isFile;

/**
 * build SF index from NYT annotated corpus, in stages.
 * PROCESS: read NYT corpus and convert to TextAnnotations, json format, in matching directory structure.
 * BUILD: read TextAnnotation json directory structure and index.
 * USE: search the index using command line queries.
 *
 * @author mssammon
 */
public class BuildSFIndex {
    private static final String NAME = BuildSFIndex.class.getCanonicalName();

    private static final String CORPUSID = "NYT_ANNOTATED";

    private enum Mode {PROCESS, BUILD, USE};

    private static final Logger logger = LoggerFactory.getLogger(BuildSFIndex.class);
    private final NYTCorpusDocumentParser parser;
    private final TextAnnotationBuilder bldr;
    private boolean doOverwrite;
    private boolean useJson;
//    private final XmlTextAnnotationMaker xmlDocReader;
//    private StupidNytReader reader;

    public BuildSFIndex() {
        this.parser = new NYTCorpusDocumentParser();
        this.bldr = new TokenizerTextAnnotationBuilder(new StatefulTokenizer());
//        Set<String> tags = new HashSet<>(Arrays.asList(NYT_TAGS));
//        this.xmlDocReader = new XmlTextAnnotationMaker(bldr, new XmlDocumentProcessor(tags, new HashMap<>(), new HashSet<>(), false));
        doOverwrite = true;
        useJson = true;
    }

    public static void main(String[] args) {
        boolean isUsageError = false;

        //serialized json: /shared/preprocessed/resources/NYT-TA-JSON/
        if (args[0].equals(Mode.PROCESS.name())) {
            if (args.length != 3)
                isUsageError = true;
            else {
                IOUtils.mkdir(args[2]);
                BuildSFIndex sfIndexBuilder = new BuildSFIndex();
                try {
                    sfIndexBuilder.extractNytDocText(args[1], args[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (args[0].equals(Mode.BUILD.name())) {
            if (args.length != 3)
                isUsageError = true;
            else {
                try {
                    IOUtils.mkdir(args[2]);
                    TextFileIndexer.buildindex(args[1], args[2], false);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        } else if (args[0].equals(Mode.USE.name())) {
            if (args.length != 2)
                isUsageError = true;
            else {
                try {
                    TextFileIndexer.testindex(args[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
        else
            isUsageError = true;
        if (isUsageError) {
            System.err.println("Usage: " + NAME + " " + Mode.PROCESS.name() + " nytDir txtDir");
            System.err.println("OR: " + NAME + " " + Mode.BUILD.name() + " txtDir indexDir");
            System.err.println("OR: " + NAME + " " + Mode.USE.name() + " indexDir");
            System.exit(-1);
        }
    }

    public void extractNytDocText(String sourceDir, String targetDir) throws IOException {
        FindMatchingFiles fmf = new FindMatchingFiles(new String[]{".tgz"});
        List<Path> files = fmf.findMatchingFiles(sourceDir);
        logger.info("Read {} source files.", files.size());
        int num = 0;
        for (Path f : files) {

            TarArchiveInputStream tarInput =
                    new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(f.toFile())));
            TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
            BufferedReader br = null;
            while (currentEntry != null) {
                if (currentEntry.isFile()) {
                    br = new BufferedReader(new InputStreamReader(tarInput)); // Read directly from tarInput
                    StringBuilder sb = new StringBuilder();
                    logger.debug("reading file {}", currentEntry.getName());
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }

                    processDocument(currentEntry.getName(), sb.toString(), targetDir);

                    if (++num % 100 == 0)
                        logger.info("processed {} files.", num);
                }
                currentEntry = tarInput.getNextTarEntry();
            }

        }
    }

    private void processDocument(String fileName, String s, String targetDir) throws IOException { //throws IOException {

//        XmlTextAnnotation xmlTa = this.xmlDocReader.createTextAnnotation(s, CORPUSID, fileName);
//        TextAnnotation ta = xmlTa.getTextAnnotation();
        File file = new File(fileName);

        Document dom = null;
        try {
            dom = parser.parseStringToDOM(s, "UTF-8", file, false);
        } catch (Exception e) {
            logger.error("Failed to parse file '{}': exception msg '{}'.", fileName, e.getMessage());
        }
        if (null != dom) {
            NYTCorpusDocument doc = null;
            doc = parser.parseNYTCorpusDocumentFromDOMDocument(file, dom);
            String headline = doc.getHeadline();
            String bodyText = doc.getBody();
            TextAnnotation ta = bldr.createTextAnnotation("NYT_TA", fileName, headline + ".\n" + bodyText);

            String outFile = targetDir + "/" + ta.getId();
            String outPath = new File(fileName).getParent();
            IOUtils.mkdir(targetDir + "/" + outPath);

            SerializationHelper.serializeTextAnnotationToFile(ta, outFile, doOverwrite, useJson);
        }
    }

}
