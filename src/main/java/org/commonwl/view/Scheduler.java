package org.commonwl.view.workflow;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Scheduler {

    private final QueuedWorkflowRepository queuedWorkflowRepository;

    @Autowired
    public Scheduler(QueuedWorkflowRepository queuedWorkflowRepository) {
        this.queuedWorkflowRepository = queuedWorkflowRepository;
    }

    @Scheduled(cron = "* * * * * ?")
    public void removeOldQueuedWorkflowEntries() {
        Date now = new Date();
        System.out.println("The time is " + now);
        System.out.println("The repository is " + queuedWorkflowRepository.toString());
    }
}
