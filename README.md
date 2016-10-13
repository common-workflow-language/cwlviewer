## About

This is a work-in-progress [Spring Boot](http://projects.spring.io/spring-boot/) MVC application which fetches [Common Workflow Language](http://www.commonwl.org/) files from a Github repository and creates a page for it detailing the main workflow and its inputs, outputs and steps


## Building and Running

Spring Boot uses an embedded HTTP server. The Spring Boot Maven plugin includes a run goal which can be used to quickly compile and run it:

```
$ mvn spring-boot:run
```

Alternatively, you can run the application from your IDE as a simple Java application by importing the Maven project

It is also possible to build a war file which is executable and deployable into an external container by [following the instructions here](that is both executable and deployable into an external container)