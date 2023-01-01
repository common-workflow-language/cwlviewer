package org.commonwl.view.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.commonwl.view.CwlViewerApplication;
import org.commonwl.view.WebConfig;
import org.commonwl.view.git.GitDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("it")
@Testcontainers
// @SpringBootTest(
//        classes={WebConfig.class, CwlViewerApplication.class, QueuedWorkflowRepository.class}
// )
@DataJpaTest(showSql = true)
@EnableJpaRepositories
@EntityScan
@Transactional(propagation = Propagation.REQUIRED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(
    initializers = PostgreSQLContextInitializer.class,
    classes = {WebConfig.class, CwlViewerApplication.class, QueuedWorkflowRepository.class})
public class QueuedWorkflowRepositoryTest {

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:9.6.12")
          .withDatabaseName("cwlviewer")
          .withUsername("sa")
          .withPassword("sa");

  @Autowired QueuedWorkflowRepository repository;

  @Test
  public void deleteQueuedWorkflowByRetrievedFromTest() {

    assertNotNull(repository);

    // create stub queued workflow
    GitDetails gitDetails =
        new GitDetails("https://github.com/common-workflow-language/cwlviewer/", "main", "/");
    gitDetails.setPackedId("test_packedId");

    Workflow workflow = new Workflow();
    workflow.setRetrievedFrom(gitDetails);

    QueuedWorkflow queuedWorkflow = new QueuedWorkflow();
    queuedWorkflow.setTempRepresentation(workflow);

    // save queued workflow
    repository.saveAndFlush(queuedWorkflow);

    List<QueuedWorkflow> all = repository.findAll();
    assertNotNull(all);
    assertEquals(1, all.size());

    // retrieve saved queued workflow by workflow git details
    QueuedWorkflow retrievedQueuedWorkflowAfterSave =
        repository.findByRetrievedFrom(queuedWorkflow.getTempRepresentation().getRetrievedFrom());
    assertNotNull(retrievedQueuedWorkflowAfterSave);

    // delete saved queued workflow by workflow git details
    repository.deleteByTempRepresentation_RetrievedFrom(
        queuedWorkflow.getTempRepresentation().getRetrievedFrom());

    // retrieve deleted queued workflow by workflow git details
    QueuedWorkflow retrievedQueuedWorkflowAfterDelete =
        repository.findByRetrievedFrom(queuedWorkflow.getTempRepresentation().getRetrievedFrom());
    assertNull(retrievedQueuedWorkflowAfterDelete);
  }

  @Test
  public void deleteQueuedWorkflowByRetrievedFromTest2() {
    // create stub queued workflow
    GitDetails gitDetails = new GitDetails("test_repo_url", "test_branch", "test_path");
    gitDetails.setPackedId("test_packedId");

    Workflow workflow = new Workflow();
    workflow.setRetrievedFrom(gitDetails);

    QueuedWorkflow queuedWorkflow = new QueuedWorkflow();
    queuedWorkflow.setTempRepresentation(workflow);

    // save queued workflow
    repository.save(queuedWorkflow);

    assertNotNull(workflow);
  }
}
