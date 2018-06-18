package io.github.mayhewsw.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import io.github.mayhewsw.controllers.Common;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Given a file specifying query subfiles, build a compound query as a conjunction over disjunctions.
 *
 * @author mssammon
 */
public class SFQueryBuilder {

    private static final String NEED = "need";
    private static final String DEFAULT = "default";
    private static final int BOOLEAN_CONJ_MIN_MATCH_TERMS = 2;

    //    private final String componentDir;
    private final Query defaultTopicQuery;
    private final Query defaultActorQuery;
    private final Query defaultVictimQuery;
    private final int topK;

    /**
     * class to build compound queries from components
     * @param defaultTopics query term files for common query needs: topic terms
     * @param defaultActors query term files for common query needs: actors
     * @param defaultVictims query term files for common query needs: victims
     * @param topK number of terms to read from each file
     */
    public SFQueryBuilder(String defaultTopics, String defaultActors, String defaultVictims, int topK) throws FileNotFoundException {
//        this.componentDir = componentDir;
        this.defaultTopicQuery = SFQueryBuilder.buildDisjunctiveBooleanQuery(readTerms(defaultTopics, topK), false); // disasters for needs
        this.defaultActorQuery = SFQueryBuilder.buildDisjunctiveBooleanQuery(readTerms(defaultActors, topK), false); // actors for most needs
        this.defaultVictimQuery = SFQueryBuilder.buildDisjunctiveBooleanQuery(readTerms(defaultVictims, topK), false); // victims of needs and some issues
        this.topK = topK;
    }

    /**
     * expects one term (word or phrase) per line
     * @param termFile
     * @param topK number of entries to read
     * @return an array of terms of size topK OR the number of terms in the file, whichever is less
     */
    private String[] readTerms(String termFile, int topK) throws FileNotFoundException {

        List<String> allTerms = LineIO.read(termFile);
        int numTerms = Math.min(allTerms.size(), topK);
        String[] retTerms = new String[numTerms];

        for (int i = 0; i < numTerms; ++i)
            retTerms[i] = allTerms.get(i);

        return retTerms;
    }

    /**
     * reads a specification file that lists component files to be combined in conjunctive query
     * @param masterFile file containing lists of query parts as filenames, tab-separated, with leftmost entry
     *                   naming the topic
     * @param componentFileDir directory containing files listed in masterFile
     * @return a map of topic to query
     */
    public Map<String, Query> buildCompoundQueries(String masterFile, String componentFileDir) throws FileNotFoundException {

        List<String> masterLines = LineIO.read(masterFile);
        Map<String, Query> topicToQuery = new HashMap<>();

        for (String line : masterLines) {
            if (line.startsWith("#") || line.matches("^\\s*$"))
                continue;
            else {
                String [] parts = line.split("\t");
                String title = parts[0];
                String type = title.substring(0, title.indexOf(':'));
                String issueNeed = title.substring(title.indexOf(':') + 1);
                List<Query> queryParts = new ArrayList<>();

                if (NEED.equalsIgnoreCase(type))
                    queryParts.add(this.defaultTopicQuery);
                String actorFileName = parts[2];
                String victimFileName = parts[3];
                String topicFile = componentFileDir + "/" + parts[1];
                String actorFile = componentFileDir + "/" + actorFileName;
                String victimFile = componentFileDir + "/" + victimFileName;

                queryParts.add(buildDisjunctiveBooleanQuery(readTerms(topicFile, topK), false));

                if (DEFAULT.equalsIgnoreCase(actorFileName))
                    queryParts.add(defaultActorQuery);
                else
                    queryParts.add(buildDisjunctiveBooleanQuery(readTerms(actorFile, topK), false));

                if (DEFAULT.equalsIgnoreCase(victimFileName))
                    queryParts.add(defaultVictimQuery);
                else
                    queryParts.add(buildDisjunctiveBooleanQuery(readTerms(victimFile, topK), false));

                topicToQuery.put(issueNeed, buildConjunctiveBooleanQuery(queryParts));
            }
        }
        return topicToQuery;
    }


    public static Query buildDisjunctiveBooleanQuery(String[] terms, boolean isPrefix) {

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


    public static Query buildConjunctiveBooleanQuery(List<Query> componentQueries) {

        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        for (Query q : componentQueries) {
            bqb.add(q, BooleanClause.Occur.MUST);
        }
//        bqb.setMinimumNumberShouldMatch(BOOLEAN_CONJ_MIN_MATCH_TERMS);
        return bqb.build();
    }
}
