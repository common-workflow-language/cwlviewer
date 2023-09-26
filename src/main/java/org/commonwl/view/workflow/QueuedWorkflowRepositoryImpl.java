package org.commonwl.view.workflow;

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
    // N.B. The migration from Spring Boot 2 to 3 got blocked for a while as
    //      we couldn't figure out a way to get the mapping of types to work.
    //      We always ended up the error about the invalid operator for the
    //      jsonb = bytea types. Found this comment from the author of the
    //      mapping library we use, with what was the fix for our issue (ran
    //      out of ideas, so started testing everything found online):
    //      Use `new JsonType(GitDetails.class)`. That finally solved it.
    //      Ref: https://github.com/common-workflow-language/cwlviewer/pull/568
    return (QueuedWorkflow)
        entityManager
            .createNativeQuery(QUERY_FIND_BY_RETRIEVED_FROM, QueuedWorkflow.class)
            .unwrap(Query.class)
            .setParameter("retrievedFrom", retrievedFrom, new JsonType(GitDetails.class))
            .stream()
            .findFirst()
            .orElse(null);
  }

  @Override
  public void deleteByTempRepresentation_RetrievedFrom(GitDetails retrievedFrom) {
    entityManager
        .createNativeQuery(QUERY_DELETE_BY_RETRIEVED_FROM, QueuedWorkflow.class)
        .unwrap(Query.class)
        .setParameter("retrievedFrom", retrievedFrom, new JsonType(GitDetails.class))
        .executeUpdate();
  }
}
