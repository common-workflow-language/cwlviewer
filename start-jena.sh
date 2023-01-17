#!/bin/sh

mkdir -p /fuseki/databases/cwlviewer
/jena-fuseki/fuseki-server --loc=/fuseki/databases/cwlviewer /cwlviewer
