package org.commonwl.view.workflow;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores workflows in the queue waiting for cwltool.
 *
 * <p>Use only queries without objects and JSON here. For other methods use the Impl class to avoid
 * issues with serialization.
 */
@Repository
public interface QueuedWorkflowRepository
    extends JpaRepository<QueuedWorkflow, String>, QueuedWorkflowRepositoryCustom {

  /**
   * Deletes all queued workflows with date retrieved on older or equal to the Date argument passed.
   *
   * @param retrievedOn Date of when the queued workflow was retrieved
   * @return The number of queued workflows deleted
   */
  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM queued_workflow q WHERE q.temp_representation ->> 'retrievedOn' <= ?1",
      nativeQuery = true)
  Integer deleteByTempRepresentation_RetrievedOnLessThanEqual(Date retrievedOn);

  /**
   * Finds and returns all queued workflows with date retrieved on older or equal to the Date
   * argument passed.
   *
   * @param retrievedOn Details of where the queued workflow is from
   * @return A list of queued workflows
   */
  @Query(
      value =
          "SELECT q.* FROM queued_workflow q WHERE q.temp_representation ->> 'retrievedOn' <= ?1",
      nativeQuery = true)
  List<QueuedWorkflow> findByTempRepresentation_RetrievedOnLessThanEqual(Date retrievedOn);
}
