
# Changelog 

## v1.4.2 [202?-??-??]

## v1.4.1 [2021-12-21]
This version started using SpringBoot 2.6.1, and had other small code changes, and many dependencies upgraded.

Smaller changes:
- Convert label-schema to OCI annotations #361 @mr-c

Misc fixes:
- Added plug-in configuration for avoiding any usages of outdated log4j2 versions, some of which are subject to the RCE CVE-2021-44228 ("Log4Shell") and CVE-2021-45046 20c58b9d782802de04cdce3e975198940900a504 @mr-c

Dependencies upgrade:
- Upgrade to SpringBoot 2.6.1 #284 @mr-c @etzanis @kinow
- update pip & setuptools #364 @mr-c
- Dependency updates, courtesy of @dependabot-bot
  - Bump jackson-core from 2.12.5 to 2.13.0 #358
  - Bump jsonld-java from 0.13.3 to 0.13.4 #365
  - Bump snakeyaml from 1.29 to 1.30 #366
  - Bump hibernate-validator from 6.0.13.Final to 6.0.20.Final #370
  - Bump jena-core from 4.3.1 to 4.3.2 #372
  - Bump hibernate-validator from 6.0.20.Final to 7.0.1.Final #373
  - Bump jackson-core from 2.13.0 to 2.13.1 #374

## v1.4.0 [2021-10-04]
Many updates since 2018, but the most important is the fix (#355) for [CVE-2021-41110](https://github.com/common-workflow-language/cwlviewer/security/advisories/GHSA-7g7j-f5g3-fqp7) courtesy of @kinow 

New features:
- Streamable CWL graph images #240 @stain 
- Schedule recurrent CWL Viewer maintained cron-job for purging of old queued workflows from database #326 @obasekiosa 

Smaller changes:
-  Separate workflow URL from repository URL in "retrieved from" column of workflows page #316 @obasekiosa 
-  Fix replace non working deleteByRetrievedFrom function with working delete function #321 @obasekiosa 

Documentation updates:
- Update README.md to explain better how to get started #308 @Anushka-shukla
- README.d: correct example URL to use port 8080 #311 @yichiehc
- Add links to the 2017 Video overview & Mark's report 612f5b4  b83b4fd 3380d44 @mr-c  @stain 
- Notes on running Mongo/Jena in Docker, and spring boot on host #334 @tetron 
- Typos in README.md #349 @kinow 

Misc fixes:
- Update jena & switch to Turtle syntax for SPARQL connection #213 @stain 
- fix reversed class & id attributes #235 @mr-c & fixed by @kinow in #352 #353 
- Use HTTPS instead of HTTP to resolve dependencies #250 [security update!] @JLLeitschuh 
- Indexing retrievedOn in mongo to fix  "Clicking 'Last' on the Explore page gives ISE" (#270) 922b434 @stain 
- Update copyright year (#286) @stain 
- add skip-schemas to the `cwltool` invocation so we are more lenient 

Changes related to the migration of view.commonwl.org from University of Manchester to Curii, Inc (Many thanks to @stain and UNIMAN for their years of service to the public!)
- Add restart:always in docker-compose.yml #294 @cure 
- Update the data controller for the https://view.commonwl.org instance. #297 @cure 
- Tweaks for the docker-compose.yml file & set a larger internalQueryExecMaxBlockingSortBytes value for mongod #298  @cure 
- Remove mention of dev instance 51a7d38 @tetron 

Dependencies upgrade:
- Dependency upgrades  to patch security vulnerabilities in transitive dependencies (Fixes CVE-2017-5929 CVE-2018-7489 CVE-2017-7525 CVE-2017-15095 CVE-2017-17485 CVE-2018-5968 CVE-2017-5651 CVE-2016-3093 CVE-2017-5648 CVE-2017-5650 CVE-2017-5647) #209 @MarkRobbo 
- More dependency upgrades via @snyk-bot #223 #225 #289 #342 
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETHRIFT-173706
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-451342
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-451343
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-451458
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHETOMCATEMBED-451459
  - https://snyk.io/vuln/SNYK-JAVA-ORGAPACHECOMMONS-460507
  - https://snyk.io/vuln/SNYK-RUBY-FFI-22037
  - https://snyk.io/vuln/SNYK-RUBY-JEKYLL-451462
  - https://snyk.io/vuln/SNYK-RUBY-KRAMDOWN-585939
  - https://snyk.io/vuln/SNYK-RUBY-ADDRESSABLE-1316242
  - https://snyk.io/vuln/SNYK-RUBY-KRAMDOWN-1087436
  - https://snyk.io/vuln/SNYK-RUBY-REXML-1244518
- Even more dependency updates, courtesy of @dependabot-preview
  - Bump snakeyaml from 1.23 to 1.29 #233 #253 #276 #300 #340 
  - Bump jsonld-java from 0.12.1 to 0.13.3 #232 #242 #277 #315 
  - Bump jackson-core from 2.9.6 to 2.12.5 #229 #238 #246 #256 #267 #272 #278 #317 #241 #346 
  - Bump org.eclipse.jgit from 4.9.7.201810191756-r to 5.11.0.202103091610-r  #230 #245 #251 #258 #259 #266 #271 #275#302 
  - Bump jena-osgi from 3.11.0 to 4.1.0 #237 #248 #264 #269 #309 #338 
  - Bump commons-compress from 1.19 to 1.21 #330 #343
- spring-boot 1.5.22 @mr-c ea1e273 

CI updates:
- stop double testing PRs with Travis (#234) @mr-c 
- speed up Travis by caching maven ea47b4a76fb9072f8aa4d2847edfc48a4f11d825 @mr-c 
- Install codeql-analysis.yml #268 @mr-c (this helped @kinow and I find CVE-2021-41110; thanks https://github.com/github/codeql-action !)
- Update primary branch name to `main` 3e3865c3f5b779458f8395f1cfc6c0589e89cb59 @mr-c 
- Mergify: configuration update a867bd3 1178ed2 @mr-c 
- Upgrade to GitHub-native Dependabot 0a5b427 @mr-c 
- Update codeql-analysis.yml to run less often 50401a5 @mr-c 
- codeql: git checkout HEAD^2 is no longer necessary 2502986 @mr-c 


## v1.3.0 [2018-07-06]
### Added
- [Permalinks](https://github.com/common-workflow-language/cwlviewer/pull/175) with content negotiation
- Show workflow [license](https://github.com/common-workflow-language/cwlviewer/pull/198)
- Added [Privacy Policy](https://github.com/common-workflow-language/cwlviewer/pull/194)
### Fixed
- Support packed workflows in [permalinks](https://github.com/common-workflow-language/cwlviewer/pull/177)
- Use [Python 3 and nodejs](https://github.com/common-workflow-language/cwlviewer/pull/197)
- Support [git tags](https://github.com/common-workflow-language/cwlviewer/pull/192) 
- UI: [Shrink description text](https://github.com/common-workflow-language/cwlviewer/pull/178)

## v1.2.2 [2017-08-24]
### Fixed
- Fix for invalid branch names being accepted in some circumstances
- Fix regression in supporting slashes in branch names
- [References](https://doi.org/10.5281/zenodo.848163)

## v1.2.1 [2017-08-22]
### Added
- Adds directory listing functionality
### Fixed
- Better support for packed workflows
    *  Use %26 (#) in the URL to view individual workflows as per cwltool
    * Links to subworkflows within a packed file now function correctly
- Improved error reporting
- Fix for array and optional types not being parsed correctly
- Fix for twitter meta tags not containing the correct image URL
- Fix for a concurrency issue
- Brings API documentation up to date with the current state of the JSON API

## v1.2.0 [2017-08-22]
- This release contains no changes - please use [v1.2.1](https://github.com/common-workflow-language/cwlviewer/releases/tag/v1.2.1) instead
- [References](https://doi.org/10.5281/zenodo.846747)

## v1.1.1 [2017-08-11]
### Fixed
- This patch release of CWL Viewer fixes documentation and a potential git checkout issue.
- [References](https://doi.org/10.5281/zenodo.841782)

## v.1.1.0 [2017-08-11]
- Support for any workflow stored in a Git repository
   * Specific additional features eg linking directly to files for Github, Gitlab, Bitbucket
- Adds cwltool use for parsing - uses RDF representation with triple store
- Intermediate loading screen
- Research Object enhancements
   * Add visualisation images as bundled aggregates
   * RDF representation as annotation
   * Packed version of workflow files as annotation
   * Git2prov link for the repository as history
   * Schema.org support for author attribution in CWL descriptions
- API and documentation backing current features
- Visualisation improvements
   * Intermediate value names
- Ontology information linked and name given for format fields on inputs and outputs
- "About" page with best practices for writing CWL for parsing by the viewer
- Label and description searching on the explore page
- Various bug fixes
- [References](https://doi.org/10.7490/f1000research.1114375.1)

## v1.0.1 [2017-08-11]
### Fixed
- Bug fixes and attribution.
- [References](https://doi.org/10.5281/zenodo.841680)

## v1.0 [2017-07-06]
- [References](https://doi.org/10.5281/zenodo.823295)

