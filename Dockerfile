FROM maven:3-eclipse-temurin-17-alpine
MAINTAINER Stian Soiland-Reyes <stain@apache.org>

# Build-time metadata as defined at https://github.com/opencontainers/image-spec/blob/main/annotations.md
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.opencontainers.image.created=$BUILD_DATE \
  org.opencontainers.image.title="CWL Viewer" \
  org.opencontainers.image.description="Viewer of Common Workflow Language" \
  org.opencontainers.image.url="https://view.commonwl.org/" \
  org.opencontainers.image.revision=$VCS_REF \
  org.opencontainers.image.source="https://github.com/common-workflow-language/cwlviewer" \
  org.opencontainers.image.vendor="Common Workflow Language project" \
  org.opencontainers.image.version=$VERSION

RUN apk add --update \
  graphviz \
  ttf-freefont \
  py3-pip \
  gcc \
  python3-dev \
  libc-dev \
  nodejs \
  libc-dev \
  linux-headers \
  libxml2-dev \
  libxml2-utils \
  libxslt-dev \
  && rm -rf /var/cache/apk/*

#wheel needed by ruamel.yaml for some reason
RUN pip3 install -U wheel setuptools pip
RUN pip3 install cwltool html5lib

RUN cwltool --version

RUN mkdir /usr/share/maven/ref/repository

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# Top-level files (ignoring .git etc)
ADD pom.xml LICENSE.md NOTICE.md README.md /usr/src/app/

# add src/ (which often change)
ADD src /usr/src/app/src
# Skip tests while building as that requires a local postgres
RUN mvn clean package -DskipTests && cp target/cwlviewer-*.jar /usr/lib/cwlviewer.jar && rm -rf target

# NOTE: ~/.m2/repository is a VOLUME and so will be deleted anyway
# This also means that every docker build downloads all of it..

WORKDIR /tmp

EXPOSE 8080
ENV LC_ALL C.UTF-8
CMD ["/opt/java/openjdk/bin/java", "-jar", "/usr/lib/cwlviewer.jar"]
