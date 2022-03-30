# CWL Viewer MongoDB migration to PostgreSQL

This directory holds documentation that may be useful for other CWL Viewer
developers (probably not for users.)

## Spring Boot & Hibernate upgrade

Issue: <https://github.com/common-workflow-language/cwlviewer/issues/254>

## Jupyter Notebook, `mongo_to_pg.ipynb`

Issue: <https://github.com/common-workflow-language/cwlviewer/issues/395>

After upgrading the Spring Boot and Hibernate code, we were left with the
production MongoDB database in AWS. Even though we could create a new
environment with PostgreSQL, we still needed to migrate the production
database.

The first step to work on the issue was to obtain a sample of the data
exported from the system. Instead of relying on someone being able to
access MongoDB in AWS, the first try was via the existing `dump.sh` script
(`load.py` is much slower.)

With the data on disk, a Jupyter Notebook was created to process the data
with Pandas, and produce a CSV to be `COPY`ed by PostgreSQL, importing
the data directly into the `workflow` table (the only other table in
CWL Viewer is `queued_workflow`, but it only holds data for workflows being
processed, can probably be ignored.)

The code of the Notebook is in this directory, and can be used to review
what was done, or to modify it for other use cases. The Python script with
the same name (but `.py` extension) was created based on the Notebook, to
be used in the command line.

To install the dependencies, use `pip install -r requirements.txt
in a virtual environment to get the dependencies to run both the
Notebook and the Python script.
