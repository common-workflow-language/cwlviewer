FROM maven:3.3-jdk-8-alpine
MAINTAINER Stian Soiland-Reyes <stain@apache.org>

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
  org.label-schema.name="CWL Viewer" \
  org.label-schema.description="Viewer of Common Workflow Language" \
  org.label-schema.url="https://view.commonwl.org/" \
  org.label-schema.vcs-ref=$VCS_REF \
  org.label-schema.vcs-url="https://github.com/common-workflow-language/cwlviewer" \
  org.label-schema.vendor="Common Workflow Language project" \
  org.label-schema.version=$VERSION \
  org.label-schema.schema-version="1.0"


RUN apk add --update graphviz ttf-freefont py2-pip gcc python2-dev libc-dev && rm -rf /var/cache/apk/*


RUN pip install cwltool html5lib
# cwl-refrunner?

# Workaround to enable <1024-bit certificates
# as Alpine Linux Python 2 links to 
# libressl2.4-libssl-2.4.4-r0
# https://github.com/certifi/python-certifi#1024-bit-root-certificates
RUN sed -i 's/import where/import old_where as where/' /usr/lib/python2.7/site-packages/requests/certs.py
RUN cwltool --version

RUN mkdir /usr/share/maven/ref/repository

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# Top-level files (ignoring .git etc)
ADD pom.xml LICENSE.md NOTICE.md README.md /usr/src/app/

# add src/ (which often change)
ADD src /usr/src/app/src
# Skip tests while building as that requires a local mongodb
RUN mvn clean package -DskipTests && cp target/cwlviewer-*.jar /usr/lib/cwlviewer.jar && rm -rf target

# NOTE: ~/.m2/repository is a VOLUME and so will be deleted anyway
# This also means that every docker build downloads all of it..

WORKDIR /tmp

EXPOSE 8080

# Expects mongodb on port 27017
ENV SPRING_DATA_MONGODB_HOST=mongo
ENV SPRING_DATA_MONGODB_PORT=27017
CMD ["/usr/bin/java", "-jar", "/usr/lib/cwlviewer.jar"]
