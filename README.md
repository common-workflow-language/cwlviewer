# CWL Viewer

This is a work-in-progress [Spring Boot](http://projects.spring.io/spring-boot/) MVC application which fetches [Common Workflow Language](http://www.commonwl.org/) files from a Github repository and creates a page for it detailing the main workflow and its inputs, outputs and steps.

[![Build Status](https://travis-ci.org/common-workflow-language/cwlviewer.svg?branch=master)](https://travis-ci.org/common-workflow-language/cwlviewer) [![Gitter](https://img.shields.io/gitter/room/gitterHQ/gitter.svg)](https://gitter.im/common-workflow-language/common-workflow-language?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![](https://images.microbadger.com/badges/image/commonworkflowlanguage/cwlviewer.svg)](https://microbadger.com/images/commonworkflowlanguage/cwlviewer "MicroBadger commonworkflowlanguage/cwlviewer") [![Docker image commonworkflowlanguage/cwlviewer](https://images.microbadger.com/badges/version/commonworkflowlanguage/cwlviewer.svg)](https://hub.docker.com/r/commonworkflowlanguage/cwlviewer/ "Docker Hub commonworkflowlanguage/cwlviewer")



## License

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
See the file [LICENSE.md](LICENSE.md) for details, and
[NOTICE.md](NOTICE.md) for required attribution notices.

## Contribute

Feel free to contribute! You may [raise an issue](https://github.com/common-workflow-language/cwlviewer/issues),
provide a [pull request](https://github.com/common-workflow-language/cwlviewer/pulls)
or join the [gitter chat for common-workflow-language](https://gitter.im/common-workflow-language/common-workflow-language)!


## Running with Docker

This application can be started with [Docker](https://www.docker.com/).

If you have [Docker Compose](https://docs.docker.com/compose/install/), then to start
MongoDB and CWLViewer exposed on port `8080`, run:

    docker-compose up

To stop and remove:

    docker-compose down

If you change the source code, then use this `docker-compose.override.yml` and 
re-build with `docker-compose build`:

```yaml
version: '2'
services:
  spring:
    build: .
```


See the [docker-compose.yml](docker-compose.yml) file for details.

If you don't want to use Docker Compose, you can do the equivalent manually with `docker`
and the [commonworkflowlanguage/cwlviewer](https://hub.docker.com/r/commonworkflowlanguage/cwlviewer/builds/) docker image.

    docker run --name cwlviewer-mongo -p 27017:27017 -d mongo
    docker run --name cwlviewer -p 8080:8080 --link cwlviewer-mongo:mongo -d commonworkflowlanguage/cwlviewer
    docker logs -f cwlviewer

If you have modified the source code, then you may want to build the docker image locally first:

    docker build -t commonworkflowlanguage/cwlviewer .


## Requirement: MongoDB

You will need to have [MongoDB](https://www.mongodb.com/) running,
by default on `localhost:27017`.

If you are running from the command line, you can override this by supplying
system properties like `-Dspring.data.mongodb.host=mongo.example.org` and
`-Dspring.data.mongodb.port=1337`

If you have Docker, but are not using the Docker Compose method above,
you may start MongoDB with [Docker](https://www.docker.com/) using:

    docker run --name cwlviewer-mongo -p 27017:27017 -d mongo

**WARNING**: The above expose mongodb to the world on port `27017`.

## Configuration

There are a variety of configuration options detailed in the [application configuration file](https://github.com/common-workflow-language/cwlviewer/blob/master/src/main/resources/application.properties) which can be adjusted.

When deploying with docker, these can be overriden externally by creating/modifying `docker-compose.override.yml` as follows:

```yaml
version: '2'
services:
  spring:
    environment:
            githubAPI.authentication: oauth
            githubAPI.oauthToken: abcdefghhijklmnopqrstuvwxyz
```

The properties can alternatively be provided as system properties on the
command line, e.g. `-DgithubAPI.authentication=oauth`
`-DgithubAPI.oauthToken=abcdefghhijklmnopqrstuvwxyz` or via a [variety of other methods supported by Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)

### Github API

If you run cwlviewer in production, you are likely to hit the GitHub API's rate limit of 60 requests/hr. This can be increased to 5000 requests/hr by using authentication (either basic or OAuth) by setting the `githubAPI.authentication` and either `githubAPI.oauthToken` or both `githubAPI.username` and `githubAPI.password` in the [application configuration file](https://github.com/common-workflow-language/cwlviewer/blob/master/src/main/resources/application.properties) depending on the method.

OAuth tokens can be obtained using the [Github authorizations API](https://developer.github.com/v3/oauth_authorizations/).

## Private Repositories

If you wish to use cwlviewer to view private Github repositories, set

```
githubAPI.useForDownloads = true
singleFileSizeLimit = 1048575
```

Along with an authentication method which has the privileges necessary to access the repository (see above).

**WARNING**: This uses the [Github Contents API](https://developer.github.com/v3/repos/contents/) to download files instead of [Rawgit](https://rawgit.com/) which will increase API calls significantly.

## Building and Running

To compile you will need [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or OpenJDK 8 (`apt install openjdk-8-jdk`),
as well as [Apache Maven 3](https://maven.apache.org/download.cgi) (`apt install maven`).

Spring Boot uses an embedded HTTP server. The Spring Boot Maven plugin includes a run goal which can be used to quickly compile and run it:

```
$ mvn spring-boot:run
```


Alternatively, you can run the application from your IDE as a simple Java application by importing the Maven project.

You can create an executable JAR file by using:

    mvn clean install

Afterwards, run:

    java -jar target/cwlviewer*.jar

(The exact filename will vary per version)

Once CWL Viewer is running, you should see log output somewhat like:

```
()..)
s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
org.researchobject.CwlViewerApplication  : Started CwlViewerApplication in 28.604 seconds
```

Now check out http://localhost:8080/ to access CWL Viewer.
