package io.github.mayhewsw.utils;

import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * When your gigantic, 36-hour indexing process gets screwed by e.g. exhausted drive space, maybe lucene
 *     doesn't create the segments file and you can't do anything with the incomplete, 25G set of files it generated
 *     so far. I found this code here:
 *
 *     https://lists.gt.net/lucene/java-user/39744
 *
 *     ...but it looks like it uses out of date Lucene version, some classes don't exist.
 *
 *    Trying Lucene's CheckIndex first.
 *
 * @author mssammon
 */
public class RepairLuceneIndex {

    private static final String NAME = RepairLuceneIndex.class.getCanonicalName();

    public static void main(String[] args) {
        if (!(args.length == 1)) {
            System.err.println("Usage: " + NAME + " indexDir");
            System.exit(0);
        }
        String indexDir = args[0];// path to index

        FSDirectory dir = null;
//        try {
//            dir = FSDirectory.open(Paths.get(indexDir));
//            CheckIndex checkIndex = new CheckIndex(dir);
//            System.out.println("checking index....");
//            CheckIndex.Status status = checkIndex.checkIndex();
//            System.out.println("index status: " + status.toString());
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }

        Directory directory = null;
        try {
            directory = FSDirectory.open(Paths.get(indexDir));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        File file = new File(indexDir);
        String[] files = file.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".cfs");
            }

        });

        SegmentInfos infos = new SegmentInfos();
//        int counter = 0;
//        for (int i = 0; i < files.length; i++) {
//            String fileName = files[i];
//            String segmentName = fileName.substring(1, fileName.lastIndexOf('.'));
//
//            int segmentInt = Integer.parseInt(segmentName, Character.MAX_RADIX);
//            counter = Math.max(counter, segmentInt);
//
//            segmentName = fileName.substring(0, fileName.lastIndexOf('.'));
//
//
//            IndexInput indexStream = null;
//            try {
//                indexStream = directory.openInput(segmentName + ".fdx", IOContext.READONCE);
//                int size = (int) (indexStream.length() / 8);
//                indexStream.close();
//
//                SegmentInfo segmentInfo = new SegmentInfo(segmentName, size, directory);
//                SegmentCommitInfo sci = new SegmentCommitInfo(segmentInfo);
//                infos.add(sci);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        infos.counter = counter++;
//
//        infos.write(directory);
    }
}
