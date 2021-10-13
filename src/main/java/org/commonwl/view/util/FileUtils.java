package org.commonwl.view.util;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.io.IOException;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.commonwl.view.util.StreamGobbler;

public class FileUtils {

    private final Logger logger;

    public FileUtils(Logger logger) {
        this.logger = logger;
    }

    private long fileAge(File file) throws IOException {
        
        FileTime t =  Files.getLastModifiedTime(file.toPath());
        Instant fileInstant = t.toInstant();
        Instant now = (Clock.systemUTC()).instant();
        Duration difference = Duration.between(fileInstant, now);
        long days = difference.toDays();
        return days;
    
    }

    public void deleteWithinDirectory(File file, long days) throws IOException{
        File[] files = file.listFiles();

        if (files != null) {
            for (File subfile : files) {
                if (subfile.isDirectory()) {
                deleteWithinDirectory(subfile, days);
                }    

                long daysOld = fileAge(subfile);

                if (daysOld >= days ) {
                    if (!subfile.isDirectory()) {
                        logger.info("deleting file " + subfile.getPath());
                        subfile.delete();
                        logger.info("deleted file " + subfile.getPath());
                    } else {
                        File[] contents = subfile.listFiles();
                        if (contents != null && contents.length == 0) {
                            logger.info("deteting Directory " + subfile.getPath());
                            subfile.delete();
                            logger.info("deleted Directory " + subfile.getPath());
                        }
                    }
                }
            }
        }
    }

    public void deleteWithinDirectoryCMD(String directoryPath, int days) throws IOException, InterruptedException {

        String[] command = {"find", directoryPath, "-ctime", "+" + days, "-exec", "rm", "-rf", "{}", "+"};
        ProcessBuilder clearProcess = new ProcessBuilder(command);
        logger.info("Clearing /tmp directory for content older than " + days + " day" + (days > 1 ? "s" : "") + "...");
        Process process = clearProcess.start();

        // Read output from the process using threads
        StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream());
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
        errorGobbler.start();
        inputGobbler.start();

        // Wait for process to complete
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            inputGobbler.join();
            logger.info(inputGobbler.getContent());
            logger.info("Successfully Cleared " + directoryPath + " directory");
        } else {
            errorGobbler.join();
            logger.warn("Could not clear " + directoryPath + " directory");
            logger.warn(errorGobbler.getContent());
        
        }
    }

}
