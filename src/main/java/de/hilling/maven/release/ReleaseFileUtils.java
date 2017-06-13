package de.hilling.maven.release;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

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

    public static String canonicalName(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new ReleaseException("unable to get canonical path " + file, e);
        }
    }
}
