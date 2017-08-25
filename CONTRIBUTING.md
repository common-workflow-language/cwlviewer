# Contributing
Thank you for taking an interest in contributing to CWL Viewer.

The following is a set of guidelines for contribution and helpful 
tips and resources for working with the code base.

## Code of Conduct
This project and everyone participating in it is governed by 
the [CWL Project Code of Conduct](https://github.com/common-workflow-language/common-workflow-language/blob/master/CODE_OF_CONDUCT.md). 
By participating, you are expected to uphold this code. 
Please report unacceptable behavior to 
[leadership@commonwl.org](mailto:leadership@commonwl.org).

## Issue Contribution
Issues for both bug reports and feature requests are welcome. In 
the case of bug reports, please try to provide a verifiable example 
on the production instance if this is possible.

Before you submit an issue, please search the issue tracker, maybe 
an issue for your problem already exists and the discussion might 
inform you of workarounds readily available.

## Code Contribution

### Workflow
The preferred workflow for contributing to CWL Viewer is to fork the
[main repository](https://github.com/common-workflow-language/cwlviewer) on
GitHub, clone, and develop on a branch. Steps:

1. Fork the [project repository](https://github.com/common-workflow-language/cwlviewer)
   by clicking on the 'Fork' button near the top right of the page. This creates
   a copy of the code under your GitHub user account. For more details on
   how to fork a repository see [this guide](https://help.github.com/articles/fork-a-repo/).

2. Clone your fork of the cwlviewer repo from your GitHub account to your local disk:

   ```bash
   $ git clone git@github.com:YourLogin/cwlviewer.git
   $ cd cwlviewer
   ```

3. Create a ``feature`` branch to hold your development changes:

   ```bash
   $ git checkout -b my-feature
   ```

   Always use a ``feature`` branch. It's good practice to never work on the ``master`` branch!

4. Develop the feature on your feature branch. Add changed files using ``git add`` and then ``git commit`` files:

   ```bash
   $ git add modified_files
   $ git commit
   ```

   to record your changes in Git, then push the changes to your GitHub account with:

   ```bash
   $ git push -u origin my-feature
   ```

5. Follow [these instructions](https://help.github.com/articles/creating-a-pull-request-from-a-fork)
to create a pull request from your fork.

### Code Structure and Dependencies
This project uses the [Maven standard directory layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html) 
and is a [Model-view-controller](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) 
application built with [Spring Boot](https://projects.spring.io/spring-boot/).

Packaging is done by feature and all Spring configuration is 
Java annotation based.

Templates for the view use [Thymeleaf](http://www.thymeleaf.org/), 
which allows them to be displayed in browsers as static prototypes.

MongoDB is used to store information about `Workflow` and `QueuedWorkflow` 
objects using [Spring Data JPA](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/).

The application also uses a triple store to keep the RDF representing 
workflows (gathered from [cwltool](https://github.com/common-workflow-language/cwltool)'s 
`--print-rdf` functionality).

See [README.md](README.md) for details on running the application with dependencies.
The tests can be run using the standard `mvn test` command.

