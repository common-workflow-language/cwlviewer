FROM maven:3.3-jdk-8-alpine
MAINTAINER Stian Soiland-Reyes <stain@apache.org>

RUN mkdir /usr/share/maven/ref/repository

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# Top-level files (ignoring .git etc)
ADD pom.xml LICENSE.md NOTICE.md README.md /usr/src/app/

# add src/ (which often change)
ADD src /usr/src/app/src
# Skip tests while building as that requires a local mongodb
RUN mvn clean package -DskipTests && cp target/cwlvisualiser-*.jar /usr/lib/cwlvisualizer.jar && rm -rf target

# NOTE: ~/.m2/repository is a VOLUME and so will be deleted anyway
# This also means that every docker build downloads all of it..

WORKDIR /tmp

EXPOSE 8080

# Expects mongodb on port 27017
CMD ["/usr/bin/java", "-Dspring.data.mongodb.host=mongo", "-jar", "/usr/lib/cwlvisualizer.jar"]
