package org.commonwl.view.workflow;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A test application context initializer that creates the database Docker container used for
 * functional tests.
 */
public class PostgreSQLContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    PostgreSQLContainer<?> postgreSQLContainer =
        new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("cwlviewer")
            .withUsername("sa")
            .withPassword("sa");
    postgreSQLContainer.start();
    TestPropertyValues.of(
            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
            "spring.datasource.password=" + postgreSQLContainer.getPassword(),
            "spring.jpa.hibernate.ddl-auto=create")
        .applyTo(applicationContext.getEnvironment());
    applicationContext.addApplicationListener(
        new ApplicationListener<ContextClosedEvent>() {
          @Override
          public void onApplicationEvent(ContextClosedEvent event) {
            postgreSQLContainer.stop();
          }
        });
  }
}
