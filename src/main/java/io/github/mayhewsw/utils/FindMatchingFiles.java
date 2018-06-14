package io.github.mayhewsw.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * An updated method to walk a directory structure. Ignores
 */
public class FindMatchingFiles extends SimpleFileVisitor<Path> {

    private final String[] suffixes;
    private List<Path> matchingFiles;

//    List<PathMatcher> pathMatchers;
    

    public FindMatchingFiles(String[] suffixes) {
        this.suffixes = suffixes;
        this.matchingFiles = new ArrayList<>();
//        this.pathMatchers = new ArrayList<>();
//        for (String suff : suffixes)
//            this.pathMatchers.add(FileSystems.getDefault().getPathMatcher("glob:*" + suff));
//    
     }


    public List<Path> getMatchingFiles() {
        return matchingFiles;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
//            for (PathMatcher matcher : pathMatchers)
//                if (matcher.matches(file))
            for (String suff : suffixes)
                if (file.getFileName().toString().endsWith(suff))
                    matchingFiles.add(file);
        }
        return CONTINUE;
    }

    public List<Path> findMatchingFiles(String dir) throws IOException {
        return findMatchingFiles(Paths.get(dir));
    }

    public List<Path> findMatchingFiles(Path path) throws IOException {
        Files.walkFileTree(path, this);
        return getMatchingFiles();
    }

//    // Print each directory visited.
//    @Override
//    public FileVisitResult postVisitDirectory(Path dir,
//                                              IOException exc) {
//        System.out.format("Directory: %s%n", dir);
//        return CONTINUE;
//    }
}
