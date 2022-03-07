package org.commonwl.view.workflow;

import org.commonwl.view.git.GitDetails;
import org.springframework.data.repository.query.Param;

public interface QueuedWorkflowRepositoryCustom {
    /**
     * Finds a queued workflow based on where it was retrieved from.
     *
     * @param retrievedFrom Details of where the queued workflow is from
     * @return The queued workflow
     */
    QueuedWorkflow findByRetrievedFrom(@Param("retrievedFrom") GitDetails retrievedFrom);

    /**
     * Deletes a queued workflow based on where it was retrieved from.
     *
     * @param retrievedFrom Details of where the queued workflow is from
     */
    void deleteByTempRepresentation_RetrievedFrom(GitDetails retrievedFrom);
}
