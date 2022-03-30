#!/usr/bin/env python3

"""
Script created from the mongo_to_pg.ipynb Jupyter Notebook.
"""

import argparse
import json
from uuid import uuid4

import numpy as np
import pandas as pd
from tqdm import tqdm


def _to_camel_case(snake_str):
    components = snake_str.split('_')
    # We capitalize the first letter of each component except the first one
    # with the 'title' method and join them together.
    return components[0] + ''.join(x.title() for x in components[1:])


def mongo_to_pg(file, out):
    df = pd.read_json(file)
    df = df[['content']]
    df = pd.json_normalize(df.content, max_level=0)

    # Rename columns.
    # columns copied from CWL Viewer Java code (db migration)
    # NOTE: in Java: ro_bundle_path, but Mongo had roBundle
    workflow_columns = ['cwltool_version', 'doc', 'docker_link', 'inputs', 'label', 'last_commit', 'license_link',
                        'outputs', 'retrieved_from', 'retrieved_on', 'steps', 'visualisation_dot']
    workflow_columns = {
        _to_camel_case(k): k for k in workflow_columns
    }
    workflow_columns['roBundle'] = 'ro_bundle_path'
    df = df.rename(columns=workflow_columns)

    # Keep only the DB columns
    df = df[workflow_columns.values()]

    # Pre-generate the IDs
    pd.options.mode.chained_assignment = None  # default='warn'
    df['id'] = df.apply(lambda x: str(uuid4()), axis=1)
    df = df.set_index('id')

    # JSON columns must be output as JSON, so that it's {"id": 1} and not {'id': 1} (plain text in CSV)

    json_columns = [
        'retrieved_from',
        'inputs',
        'outputs',
        'steps'
    ]
    for column in json_columns:
        df[column] = df[column].apply(json.dumps)

    df['retrieved_on'] = pd.to_datetime(df['retrieved_on'], unit='ms')

    # Convert to CSV

    # from: https://stackoverflow.com/a/68258386
    chunks = np.array_split(df.index, 100)  # chunks of 100 rows

    for chunk, subset in enumerate(tqdm(chunks)):
        if chunk == 0:  # first row
            df.loc[subset].to_csv(out, mode='w', index=True)
        else:
            df.loc[subset].to_csv(out, header=None, mode='a', index=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--input", help="input JSON file", required=True)
    parser.add_argument("-o", "--output", help="output CSV file", required=True)
    args = parser.parse_args()
    file = args.input
    out = args.output
    mongo_to_pg(file, out)


if __name__ == '__main__':
    main()
