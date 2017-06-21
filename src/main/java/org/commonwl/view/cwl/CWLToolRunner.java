package org.commonwl.view.cwl;

import org.commonwl.view.github.GithubDetails;
import org.commonwl.view.graphviz.GraphVizService;
import org.commonwl.view.workflow.Workflow;
import org.commonwl.view.workflow.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Replace existing workflow with the one given by cwltool
 */
@Component
@EnableAsync
public class CWLToolRunner {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorkflowRepository workflowRepository;
    private final CWLService cwlService;
    private final GraphVizService graphVizService;

    @Autowired
    public CWLToolRunner(WorkflowRepository workflowRepository,
                         CWLService cwlService,
                         GraphVizService graphVizService) {
        this.workflowRepository = workflowRepository;
        this.cwlService = cwlService;
        this.graphVizService = graphVizService;
    }

    @Async
    public void updateModelWithCwltool(GithubDetails githubInfo,
                                       String latestCommit)
            throws IOException, InterruptedException {

        Workflow workflow = workflowRepository.findByRetrievedFrom(githubInfo);

        // Chance that this thread could be done before workflow model is saved
        int attempts = 5;
        while (attempts > 0 && workflow == null) {
            // Delay this thread by 0.5s and try again until success or too many attempts
            Thread.sleep(1000L);
            workflow = workflowRepository.findByRetrievedFrom(githubInfo);
            attempts--;
        }

        // Parse using cwltool and replace in database
        try {
            Workflow newWorkflow = cwlService.parseWorkflowWithCwltool(githubInfo, latestCommit);

            workflowRepository.delete(workflow);
            graphVizService.deleteCache(workflow.getID());

            newWorkflow.setId(workflow.getID());
            newWorkflow.setRetrievedOn(workflow.getRetrievedOn());
            newWorkflow.setRetrievedFrom(githubInfo);
            newWorkflow.setLastCommit(latestCommit);
            newWorkflow.setCwltoolStatus(Workflow.Status.SUCCESS);
            workflowRepository.save(newWorkflow);
        } catch (CWLValidationException ex) {
            logger.error(ex.getMessage(), ex);
            workflow.setCwltoolStatus(Workflow.Status.ERROR);
            workflow.setCwltoolLog(ex.getMessage());
            workflowRepository.save(workflow);
        }

    }

}
