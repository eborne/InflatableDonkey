package com.github.horrorho.inflatabledonkey.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Created by jason on 4/28/2016.
 */
public final class FileUtils {
    public static boolean createDirectories(Path file) {
        return createDirectories(file, null);
    }

    public static boolean createDirectories(Path file, Logger logger) {
        Path parent = file.getParent();
        if (parent != null) {
            if (Files.exists(parent)) {
                if (Files.isDirectory(parent)) {
                    return true;

                } else {
                    if (logger != null) {
                        logger.warn("-- createDirectories() - path exists but is not a directory: {}", file);
                    }
                    return false;
                }
            }

            try {
                Files.createDirectories(parent);
                return true;

            } catch (IOException ex) {
                if (logger != null) {
                    logger.debug("-- createDirectories() - IOException: {}", ex);
                }
                return false;
            }
        }
        return true;
    }
}
