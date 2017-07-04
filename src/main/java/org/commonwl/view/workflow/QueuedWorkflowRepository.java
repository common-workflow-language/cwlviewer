package org.commonwl.view.workflow;

import org.commonwl.view.github.GithubDetails;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Stores workflows in the queue waiting for cwltool
 */
public interface QueuedWorkflowRepository extends PagingAndSortingRepository<QueuedWorkflow, String> {

    /**
     * Finds a queued workflow in the database based on where it was retrieved from
     * @param retrievedFrom Details of where the queued workflow is from
     * @return The queued workflow
     */
    @Query("{tempRepresentation.retrievedFrom: ?0}")
    QueuedWorkflow findByRetrievedFrom(GithubDetails retrievedFrom);

}
