package io.github.mayhewsw.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * An updated method to walk a directory structure. Ignores
 */
public class WalkDirectory extends SimpleFileVisitor<Path> {

    private final List<String> suffixes;

    private List<Path> matchingFiles;

    public WalkDirectory(List<String> suffixes) {
        this.suffixes = suffixes;
        this.matchingFiles = new ArrayList<>();
    }


    public List<Path> getMatchingFiles() {
        return matchingFiles;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            for (String suff : suffixes)
                if (file.endsWith(suff))
                    matchingFiles.add(file);
        }
        return CONTINUE;
    }

//    // Print each directory visited.
//    @Override
//    public FileVisitResult postVisitDirectory(Path dir,
//                                              IOException exc) {
//        System.out.format("Directory: %s%n", dir);
//        return CONTINUE;
//    }
}
