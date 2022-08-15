package org.commonwl.view;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.commonwl.view.workflow.QueuedWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scheduler class for recurrent processes.
 */
@Component
public class Scheduler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final QueuedWorkflowRepository queuedWorkflowRepository;

    @Value("${queuedWorkflowAgeLimitHours}")
    private Integer QUEUED_WORKFLOW_AGE_LIMIT_HOURS;

    @Value("${tmpDirAgeLimitDays}")
    private Integer TMP_DIR_AGE_LIMIT_DAYS;

    // We do not want to remove the bundles, as we use the disk as a sort of
    // cache. Whenever a workflow page is displayed in the browser the UI
    // fires a request to re-generate it. We skip that by keeping files on disk.
    @Value("${graphvizStorage}")
    private String graphvizStorage;
    @Value("${gitStorage}")
    private String gitStorage;

    @Autowired
    public Scheduler(QueuedWorkflowRepository queuedWorkflowRepository) {
        this.queuedWorkflowRepository = queuedWorkflowRepository;
    }


    /**
     * A Scheduled function to delete old queued workflow entries
     * from the queue. Age is determined by QUEUED_WORKFLOW_AGE_LIMIT_HOURS
     */
    @Scheduled(cron = "${cron.deleteOldQueuedWorkflows}")
    public void removeOldQueuedWorkflowEntries() {
        Calendar calendar = Calendar.getInstance();
        Date now = new Date();
        calendar.setTime(now);

        if (QUEUED_WORKFLOW_AGE_LIMIT_HOURS == null) {
            QUEUED_WORKFLOW_AGE_LIMIT_HOURS = 24;
        }

        // calculate time QUEUED_WORKFLOW_AGE_LIMIT_HOURS before now
        calendar.add(Calendar.HOUR, -QUEUED_WORKFLOW_AGE_LIMIT_HOURS);
        Date removeTime = calendar.getTime();

        logger.info("The time is " + now);
        logger.info("Delete time interval is : OLDER THAN " + QUEUED_WORKFLOW_AGE_LIMIT_HOURS + " HOURS");
        logger.info("Deleting queued workflows older than or equal to " + removeTime);

        logger.info(queuedWorkflowRepository.deleteByTempRepresentation_RetrievedOnLessThanEqual(removeTime)
                + " Old queued workflows removed");
    }

    /**
     * Scheduled function to delete old temporary directories.
     *
     * <p>Will scan each temporary directory (graphviz, RO, git), searching
     * for files exceeding a specified threshold.</p>
     *
     * <p>It scans the first level directories, i.e. it does not recursively
     * scans directories. So it will delete any RO or Git repository directories
     * that exceed the threshold. Similarly, it will delete any graph (svg, png,
     * etc) that also exceed it.</p>
     *
     * <p>Errors logged through Logger. Settings in Spring application properties
     * file.</p>
     *
     * @since 1.4.5
     */
    @Scheduled(cron = "${cron.clearTmpDir}")
    public void clearTmpDir() {
        // Temporary files used for graphviz, RO, and git may be stored in different
        // locations, so we will collect all of them here.
        List<String> temporaryDirectories = Stream.of(graphvizStorage, gitStorage)
                .distinct()
                .toList();
        temporaryDirectories.forEach(this::clearDirectory);
    }

    /**
     * For a given temporary directory, scans it (not recursively) for files and
     * directories exceeding the age limit threshold.
     *
     * @since 1.4.5
     * @see <a href="https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/filefilter/AgeFileFilter.html">https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/filefilter/AgeFileFilter.html</a>
     * @param temporaryDirectory temporary directory
     */
    private void clearDirectory(String temporaryDirectory) {
        final Instant cutoff = Instant.now().minus(Duration.ofDays(TMP_DIR_AGE_LIMIT_DAYS));

        File temporaryDirectoryFile = new File(temporaryDirectory);
        String[] files = temporaryDirectoryFile.list(new AgeFileFilter(Date.from(cutoff)));

        if (files != null && files.length > 0) {
            for (String fileName : files) {
                File fileToDelete = new File(temporaryDirectoryFile, fileName);
                try {
                    FileUtils.forceDelete(fileToDelete);
                } catch (IOException e) {
                    // Here we probably have a more serious case. Since the Git repository, RO directory, or graphs are
                    // not expected to be in use, and the application must have access, I/O errors are not expected and
                    // must be treated as errors.
                    logger.error(String.format("Failed to delete old temporary file or directory [%s]: %s", fileToDelete.getAbsolutePath(), e.getMessage()), e);
                }
            }
        }
    }
}
