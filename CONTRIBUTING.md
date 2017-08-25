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

### Running the Application and Tests
See [README.md](README.md) for details on running the application with dependencies.
The tests can be run using the standard `mvn test` command.

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

In general, the controller classes call services which have the main logic for 
the application. These controllers are:
* `PageController` - handles basic static pages such as the index and documentation
* `workflow.WorkflowController` - the main controller class for the application
* `workflow.WorkflowJSONController` - handles API functionality
* `workflow.PermalinkController` - handles permalinks with content negotiation for 
retrieving different formats

Notable services include
* `workflow.WorkflowService` - Handles workflow related functionality
* `researchobject.ROBundleService` - Creates research object bundles
* `graphviz.GraphVizService` - A wrapper for `com.github.jabbalaci.graphviz.GraphViz` 
to generate images from DOT source code
* `git.GitService` - Builds on JGit to provide Git functionality
* `cwl.CWLService` - Implements parsing of cwl files

Note: For the async operations, Spring does not support the calling of a method within 
the same class (as a proxy needs to kick in to spawn a new thread). For this reason 
some extra classes such as `researchobject.ROBundleFactory` and `cwl.CWLToolRunner` 
are used when they would otherwise not be required.

### Basic Application Flow

1. User fills in the form on the front page. This is represented by 
`workflow.WorkflowForm` and consists of just a URL to Github/Gitlab, 
or a URL to a git repository as well as the branch and path of a 
workflow within the repository.

2. This is submitted and picked up by a method in `workflow.WorkflowController`. 
The form is validated and parsed by `workflow.WorkflowFormValidator` to 
produce a `git.GitDetails` object with a repository URL, branch and path. 
The MongoDB database is checked for already pending `workflow.QueuedWorkflow` or 
created `workflow.Workflow` objects based on this (but this flow assumes they do 
not already exist).

3. A new `workflow.QueuedWorkflow` object is created by cloning the repository 
locally (if it does not already exist), checking out the new branch and parsing 
the file using built-in YAML parsing code. Intermediate visualisations and 
models are produced which may not yet be complete.

4. [cwltool](https://github.com/common-workflow-language/cwltool) is run on the 
workflow using the `--print-rdf` option to produce the RDF representation. The RDF 
will be stored in the SPARQL store and queries will extract the information 
required. Afterwards this is used to construct a Research Object Bundle for the workflow. 
This is an asynchronous operation so meanwhile...

5. The user is redirected to the main workflow page, which will use the `loading` 
template for now until cwltool has finished running. The background is the intermediate 
visualisation. An AJAX call repeatedly checks the status of cwltool saved in the 
`workflow.QueuedWorkflow` object in MongoDB.

6. The page either displays an error on the loading page or reloads to view the 
 parsed workflow on the `workflow` template. An AJAX call checks if the Research Object 
 Bundle has been created and adds it to the page when it has.
