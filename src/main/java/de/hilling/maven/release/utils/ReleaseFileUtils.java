package de.hilling.maven.release.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.hilling.maven.release.exceptions.ReleaseException;

public class ReleaseFileUtils {
    public static String pathOf(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e1) {
            path = file.getAbsolutePath();
        }
        return path;
    }

    /**
     * Write the given content to the named file, using UTF-8 encoding.
     * <ul>
     *     <li>Throws a ReleaseException if the file already exists.</li>
     *     <li>Appends a newline to the file if necessary.</li>
     * </ul>
     *
     * @param fileName name of the file.
     * @param string content of file.
     */
    public static void write(String fileName, String string) {
        try {
            final File file = new File(fileName);
            if (file.exists()) {
                throw new ReleaseException("file already exists: " + fileName);
            }
            if(string.endsWith("\n")) {
                FileUtils.write(file, string, StandardCharsets.UTF_8);
            } else {
                FileUtils.write(file, string + "\n", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ReleaseException("unable to write " + fileName, e);
        }
    }

    /**
     * Reads the contents of the given file, using UTF-8 encoding.
     * @param fileName name of file.
     * @return content
     */
    public static List<String> read(String fileName) {
        final File file = new File(fileName);
        if (!file.exists()) {
            throw new ReleaseException("file does not exists: " + fileName);
        }

        try {
            return FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReleaseException("unable to read " + fileName, e);
        }
    }

    /**
     * Return the canonical name of the given file.
     * @param file file to get name from.
     * @return canonical name.
     */
    public static String canonicalName(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new ReleaseException("unable to get canonical path " + file, e);
        }
    }

}
