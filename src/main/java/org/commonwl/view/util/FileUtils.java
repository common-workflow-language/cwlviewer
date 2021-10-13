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


    public void clearDirectory(String directoryPath, long days) {
        try {
            logger.info("Clearing "+ directoryPath + " directory for content older than " + days + " day" + (days > 1 ? "s" : "") + "...");
            File file = new File(directoryPath);
            deleteWithinDirectory(file, days);
            logger.info("Successfully cleared " + directoryPath + " directory");
        } catch(IOException e) {
            logger.warn("Could not clear " + directoryPath + " directory");
            logger.error("Error running clear " + directoryPath + " dir process", e);
        }
        
    }


    private void deleteWithinDirectory(File file, long days) throws IOException{
        File[] files = file.listFiles();

        if (files != null) {
            for (File subfile : files) {
                if (subfile.isDirectory()) {
                deleteWithinDirectory(subfile, days);
                }    

                long daysOld = fileAge(subfile);

                if (daysOld >= days ) {
                    if (!subfile.isDirectory()) {
                        logger.info("Deleting file " + subfile.getPath());
                        subfile.delete();
                        logger.info("Deleted file " + subfile.getPath());
                    } else {
                        File[] contents = subfile.listFiles();
                        if (contents != null && contents.length == 0) {
                            logger.info("Deleting directory " + subfile.getPath());
                            subfile.delete();
                            logger.info("Deleted directory " + subfile.getPath());
                        }
                    }
                }
            }
        }
    }

    private long fileAge(File file) throws IOException {
        
        FileTime t =  Files.getLastModifiedTime(file.toPath());
        Instant fileInstant = t.toInstant();
        Instant now = (Clock.systemUTC()).instant();
        Duration difference = Duration.between(fileInstant, now);
        long days = difference.toDays();
        return days;
    
    }

    private void deleteWithinDirectoryCMD(String directoryPath, int days) throws IOException, InterruptedException {

        String[] command = {"find", directoryPath, "-ctime", "+" + days, "-exec", "rm", "-rf", "{}", "+"};
        ProcessBuilder clearProcess = new ProcessBuilder(command);
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
            logger.info("Successfully cleared " + directoryPath + " directory");
        } else {
            errorGobbler.join();
            logger.warn("Could not clear " + directoryPath + " directory");
            logger.warn(errorGobbler.getContent());
        
        }
    }

}
