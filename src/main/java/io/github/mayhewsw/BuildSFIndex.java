package io.github.mayhewsw;

/**
 * Simple main method to build SF index.
 *
 * @author mssammon
 */
public class BuildSFIndex {
    private static final String NAME = BuildSFIndex.class.getCanonicalName();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: " + NAME + " inDir indexDir");
            System.exit(-1);
        }
        try {
            TextFileIndexer.buildindex(args[0], args[1], false);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
