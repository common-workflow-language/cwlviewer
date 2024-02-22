# CWL Viewer

This is a [Spring Boot](http://projects.spring.io/spring-boot/) MVC application which fetches [Common Workflow Language](http://www.commonwl.org/) files from a Github repository and creates a page for it detailing the main workflow and its inputs, outputs and steps.

[![Build Status](https://github.com/common-workflow-language/cwlviewer/workflows/CWL%20Viewer%20Build/badge.svg?branch=main)](https://github.com/common-workflow-language/cwlviewer/actions?query=workflow%3A%22CWL%20Viewer%20Build%22) [![Coverage Status](https://coveralls.io/repos/github/common-workflow-language/cwlviewer/badge.svg)](https://coveralls.io/github/common-workflow-language/cwlviewer) [![Gitter](https://img.shields.io/gitter/room/gitterHQ/gitter.svg)](https://gitter.im/common-workflow-language/cwlviewer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![](https://images.microbadger.com/badges/image/commonworkflowlanguage/cwlviewer.svg)](https://microbadger.com/images/commonworkflowlanguage/cwlviewer "MicroBadger commonworkflowlanguage/cwlviewer") [![Docker image commonworkflowlanguage/cwlviewer](https://images.microbadger.com/badges/version/commonworkflowlanguage/cwlviewer.svg)](https://hub.docker.com/r/commonworkflowlanguage/cwlviewer/ "Docker Hub commonworkflowlanguage/cwlviewer")
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.823534.svg)](https://doi.org/10.5281/zenodo.823534)

# Using CWL Viewer

You are recommended to use the **production instance** of CWL Viewer at https://view.commonwl.org/ which runs the latest [release](https://github.com/common-workflow-language/cwlviewer/releases). Any downtime should be reported on the [gitter chat for cwlviewer](https://gitter.im/common-workflow-language/cwlviewer).

<!--- I don't think this is a thing any more.
The **dev instance** at http://view.commonwl.org:8082/ corresponds to the current `master` branch, and is updated every 6 minutes to run the latest [commonworkflowlanguage/cwlviewer docker image](https://hub.docker.com/r/commonworkflowlanguage/cwlviewer/builds/). Note that this instance is NOT secured and might break at any time.
-->

# Running

If you are a developer, or you want to use the CWL Viewer in a closed environment, then you can run your own instance.

## Recommended - Running with Docker

This application can be started with [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/install/) and Docker Compose is the recommended method of running or developing this codebase.

Then run the following commands to clone the project in your local system.

   ```
    git clone https://github.com/common-workflow-language/cwlviewer.git
    cd cwlviewer
  ```

In the project directory, to start CWLViewer exposed on port `8080`, run:

    docker compose up

The web server will connect to a local host, you'll see the message saying "Tomcat started on port(s):8080".

To see the locally running CWL Viewer app, visit http://localhost:8080/ in your web browser.

To stop and remove:

    docker compose down


If you change the source code, then use this `docker-compose.override.yml` and
re-build with `docker compose build`:

```yaml
version: '3.9'
services:
  spring:
    build: .
```

See the [docker-compose.yml](docker-compose.yml) file for details.

If you have modified the source code, then you may want to build the docker image locally first:

    docker build -t commonworkflowlanguage/cwlviewer .

## Running Spring Boot locally for development, with PostgreSQL and Jena Fuseki in Docker

Create `docker-compose.override.yml`:

```
version: '3.9'
services:
  postgres:
    ports:
     - "5432:5432"
  sparql:
    ports:
     - "3030:3030"
```

Then start the containers:

```
docker compose up
```

Then start Spring Boot locally:

```
mvn spring-boot:run -Dserver.port=7999
```

Now you can connect to http://localhost:7999 in your browser.

## Deleting the data volumes to reset state

To completely reset the state, you must delete the data volumes:

```
docker compose down
docker volume rm  cwlviewer_bundle cwlviewer_git cwlviewer_graphviz cwlviewer_postgres cwlviewer_sparql
```

## Running without Docker

### Requirements

#### PostgreSQL

You will need to have [PostgreSQL](https://www.postgresql.org/) running,
by default on `localhost:5432`

If you are running from the command line, you can override this by supplying
system properties like `-Dspring.datasource.url=jdbc:postgresql://localhost:5432/cwlviewer` and
`-Dspring.datasource.password=sa`

#### Apache Jena Fuseki (or alternative SPARQL server)

You will also need to have a SPARQL server such as [Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/) running,
by default on `localhost:3030`

#### Ruby and Licensee

To retrieve license information, CWL Viewer uses the [Licensee](https://github.com/licensee/licensee) Ruby Gem. To install it,
[configure Ruby](https://www.ruby-lang.org/en/documentation/installation/) on your environment and then run

```bash
gem install licensee
```

You may use a dependency from your operating system package manager if you
prefer too, e.g. `ruby-licensee` for Ubuntu LTS 22.04.1.

Before running Maven, try running `licensee` in the command-line to verify
it was installed successfully.

## Compiling and Running

To compile you will need [Java 17](https://www.oracle.com/java/technologies/downloads/) or a compatible distribution
(e.g. [Eclipse Adoptium](https://projects.eclipse.org/projects/adoptium)) and version, as well as
[Apache Maven 3](https://maven.apache.org/download.cgi) (`apt install maven`).

Spring Boot uses an embedded HTTP server. The Spring Boot Maven plugin includes a run goal which can be used to quickly compile and run it:

```
$ mvn spring-boot:run
```

Alternatively, you can run the application from your IDE as a simple Java application by importing the Maven project.

You need to install [Graphviz](http://www.graphviz.org/) for all unit tests to pass.

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

## Configuration

There are a variety of configuration options detailed in the [application configuration file](https://github.com/common-workflow-language/cwlviewer/blob/master/src/main/resources/application.properties) which can be adjusted.

When deploying with docker, these can be overridden externally by creating/modifying `docker-compose.override.yml` as follows:

```yaml
version: '3.9'
services:
  spring:
    environment:
            applicationName: Common Workflow Language Viewer
            applicationURL: https://view.commonwl.org
            cacheDays: 1
```

The properties can alternatively be provided as system properties on the
command line, e.g. `-DcacheDays=1` or via a [variety of other methods supported by Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)

# Dump/restore

While you can perform backup of the Docker volumes,
for larger upgrades of CWL Viewer it is recommended instead to do a JSON dump
and re-load, which will force CWL Viewer to fetch and parse again.

The script `dump.py` can be used for regular backups, it will store the full
output of /workflows as one or multiple timestamped JSON files (you can use
`gzip` to compress them):

    $ python dump.py --viewer https://view.commonwl.org/ --output /var/backups --page 0 --size 100
      INFO:Viewer URL: https://view.commonwl.org/
      INFO:Output: /var/backups
      INFO:Dumping workflows from https://view.commonwl.org/, page 0, size 100 to /var/backups

    $ python dump.py -o /var/backups -a
      INFO:Viewer URL: https://view.commonwl.org/
      INFO:Output: /var/backups
      INFO:Dumping all the workflows from https://view.commonwl.org/ to /var/backups
      100%|█████████████████████████████████████████████████████████████████████████████████████████████████████████████████| 16/16 [04:39<00:00, 17.49s/it]

The script `load.py` (requires Python 3) can be used to restore from such JSON dumps:

    ./load.py /var/backups/cwl/2018-06-06T135133+0000.json.gz https://view.commonwl.org/

The optional parameter `--no-commits` can be added to skip those entries that
look like a commit ID. Note that this might break previous permalinks.

# Documentation

2017 Poster <https://doi.org/10.7490/f1000research.1114375.1?>

2017 Video overview <https://youtu.be/_yjhVTmvxLU>

2017 Technical Report <https://doi.org/10.5281/zenodo.823295>

## License

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
See the file [LICENSE.md](LICENSE.md) for details, and
[NOTICE.md](NOTICE.md) for required attribution notices.

## Contribute

Feel free to contribute! You may [raise an issue](https://github.com/common-workflow-language/cwlviewer/issues),
provide a [pull request](https://github.com/common-workflow-language/cwlviewer/pulls)
or join the [gitter chat for cwlviewer](https://gitter.im/common-workflow-language/cwlviewer)!

## Changelog
See [CHANGELOG](https://github.com/common-workflow-language/cwlviewer/blob/main/CHANGELOG.md)

## Making a development snapshot container image
(and optionally publishing that image to DockerHub)

```shell
# confirm the build arguments
# if these don't look correct, troubleshoot before continuing.
BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') VCS_REF=$(git rev-parse HEAD) VERSION=$(git describe)
echo BUILD_DATE=${BUILD_DATE} VCS_REF=${VCS_REF} VERSION=${VERSION}
# build the container image
docker build --build-arg BUILD_DATE=${BUILD_DATE} --build-arg VCS_REF=${VCS_REF} \
  --build-arg VERSION=${VERSION} \
  -t cwlviewer:${VERSION} .
# the rest is optional
docker tag cwlviewer:${VERSION} docker.io/commonworkflowlanguage/cwlviewer:${VERSION}
docker tag cwlviewer:${VERSION} quay.io/commonwl/cwlviewer:${VERSION}
docker push docker.io/commonworkflowlanguage/cwlviewer:${VERSION}
docker push quay.io/commonwl/cwlviewer:${VERSION}
```

## Making a release and publishing to GitHub, DockerHub, and Quay.io

After CHANGELOG.md has been updated and the `-SNAPSHOT` suffix removed from `pom.xml`, run the following:

```shell
git checkout main
git pull
new_version=1.4.3  # CHANGEME
# create an annotated git tag
git tag -a -m "release version ${new_version}" v${new_version}
# confirm the build arguments
# if these don't look correct, troubleshoot before continuing.
# for example, was your tag an annotated (-a) tag?
BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') VCS_REF=$(git rev-parse HEAD) VERSION=$(git describe)
echo BUILD_DATE=${BUILD_DATE} VCS_REF=${VCS_REF} VERSION=${VERSION}
# build the container image
docker build --build-arg BUILD_DATE=${BUILD_DATE} --build-arg VCS_REF=${VCS_REF} \
  --build-arg VERSION=${VERSION} \
  -t cwlviewer:${VERSION} .
# tag this container image in preparation for pushing to Docker Hub and Quay.io
docker tag cwlviewer:${VERSION} docker.io/commonworkflowlanguage/cwlviewer:${VERSION}
docker tag cwlviewer:${VERSION} docker.io/commonworkflowlanguage/cwlviewer:latest
docker tag cwlviewer:${VERSION} quay.io/commonwl/cwlviewer:${VERSION}
docker tag cwlviewer:${VERSION} quay.io/commonwl/cwlviewer:latest
# push the container image to Docker Hub and Quay.io
docker push docker.io/commonworkflowlanguage/cwlviewer:${VERSION}
docker push docker.io/commonworkflowlanguage/cwlviewer:latest
docker push quay.io/commonwl/cwlviewer:${VERSION}
docker push quay.io/commonwl/cwlviewer:latest
# upload the annotated tag to GitHub
git push --tags
git push
```

Then copy the changelog into https://github.com/common-workflow-language/cwlviewer/releases/new
using the tag you just pushed.

Finally, make a new PR to bump the version and restore the `-SNAPSHOT` suffix in `pom.xml`.

# Thanks

Developers and [contributors](https://github.com/common-workflow-language/cwlviewer/graphs/contributors) include:

* **Mark Robinson** http://orcid.org/0000-0002-8184-7507
* Stian Soiland-Reyes http://orcid.org/0000-0001-9842-9718
* Michael Crusoe http://orcid.org/0000-0002-2961-9670
* Carole Goble http://orcid.org/0000-0003-1219-2137
* Charles Overbeck https://github.com/coverbeck
* Finn Bacall http://orcid.org/0000-0002-0048-3300
* Osakpolor Obaseki https://github.com/obasekiosa
* Bruno P. Kinoshita https://orcid.org/0000-0001-8250-4074

Thanks to:

* [eScience Lab](http://www.esciencelab.org.uk/) at [The University of Manchester](http://www.cs.manchester.ac.uk/)
* [BioExcel Center of Excellence for Computational Biomolecular Research](http://bioexcel.eu/)
* * European Commission's [H2020 grant 675728](http://cordis.europa.eu/projects/675728)
* [Common Workflow Language](http://www.commonwl.org/) community and the [CWL Gitter room](https://gitter.im/common-workflow-language/common-workflow-language)
