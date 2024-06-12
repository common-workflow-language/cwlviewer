package org.commonwl.view.workflow;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.hypersistence.utils.hibernate.type.json.JsonBlobType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.commonwl.view.git.GitDetails;
import org.hibernate.query.Query;

public class QueuedWorkflowRepositoryImpl implements QueuedWorkflowRepositoryCustom {

  // Tested this query directly, and it works! Problem is elsewhere!
  private static final String QUERY_FIND_BY_RETRIEVED_FROM =
      "SELECT q.* FROM queued_workflow q WHERE q.temp_representation -> 'retrievedFrom' = :retrievedFrom";

  private static final String QUERY_DELETE_BY_RETRIEVED_FROM =
      "DELETE FROM queued_workflow q WHERE q.temp_representation -> 'retrievedFrom' = :retrievedFrom";

  @PersistenceContext EntityManager entityManager;

  @Override
  public QueuedWorkflow findByRetrievedFrom(GitDetails retrievedFrom) {
    return (QueuedWorkflow)
        entityManager
            .createNativeQuery(QUERY_FIND_BY_RETRIEVED_FROM, QueuedWorkflow.class)
            .unwrap(Query.class)
            .setParameter("retrievedFrom", retrievedFrom, JsonBlobType.INSTANCE)
            .getSingleResult();
  }

  @Override
  public void deleteByTempRepresentation_RetrievedFrom(GitDetails retrievedFrom) {
    entityManager
        .createNativeQuery(QUERY_DELETE_BY_RETRIEVED_FROM, QueuedWorkflow.class)
        .unwrap(Query.class)
        .setParameter("retrievedFrom", retrievedFrom, JsonType.INSTANCE)
        .executeUpdate();
  }
}
