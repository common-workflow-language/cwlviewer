package org.commonwl.view.workflow;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import org.commonwl.view.git.GitDetails;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

public class QueuedWorkflowRepositoryImpl implements QueuedWorkflowRepositoryCustom {

  private static final String QUERY_FIND_BY_RETRIEVED_FROM =
      "SELECT q.* FROM queued_workflow q WHERE q.temp_representation -> 'retrievedFrom' = :retrievedFrom";

  private static final String QUERY_DELETE_BY_RETRIEVED_FROM =
      "DELETE FROM queued_workflow q WHERE q.temp_representation -> 'retrievedFrom' = :retrievedFrom";

  @PersistenceContext(type = PersistenceContextType.EXTENDED)
  EntityManager entityManager;

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
    final Query<?> query =
        entityManager
            .createNativeQuery(QUERY_FIND_BY_RETRIEVED_FROM, QueuedWorkflow.class)
            .unwrap(Query.class);

    if (query == null) {
      return null;
    }

    query.setParameter("retrievedFrom", retrievedFrom, new JsonType(GitDetails.class));
    return (QueuedWorkflow) query.uniqueResult();
  }

  @Transactional
  @Override
  public void deleteByTempRepresentation_RetrievedFrom(GitDetails retrievedFrom) {
    final Query<?> query =
        entityManager
            .createNativeQuery(QUERY_DELETE_BY_RETRIEVED_FROM, QueuedWorkflow.class)
            .unwrap(Query.class);

    if (query != null) {
      query.setParameter("retrievedFrom", retrievedFrom, new JsonType(GitDetails.class));
      query.executeUpdate();
    }
  }
}
