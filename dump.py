import argparse
import logging
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

import requests
from math import ceil

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)
logger = logging.getLogger(__name__)

DEFAULT_PAGE = 0
DEFAULT_SIZE = 10
MAX_PAGE_SIZE = 2000


def _get_total_elements(viewer) -> int:
    """
    We need to fetch a workflows listing to figure out how many entries we
    have in the database, since the API does not contain a method to count
    the DB entries.

    :param viewer: CWL Viewer instance URL
    :return: number of total elements in the CWL Viewer instance DB
    """
    smallest_workflow_dataset: dict = _fetch_workflows_data(viewer, 0, 1).json()
    return int(smallest_workflow_dataset['totalElements'])


def _dump_all_workflows(viewer: str, output: Path) -> None:
    """
    Dump all the workflows in the database.
    :param viewer: CWL Viewer instance URL
    :param output: Local existing directory
    :return: None
    """
    total_elements = _get_total_elements(viewer)
    logger.info("Total number of workflows: %s", total_elements)
    pages = ceil(total_elements / MAX_PAGE_SIZE)
    logger.info(
        "Will create %s separate dump files of up to %s workflows each.",
        pages, MAX_PAGE_SIZE)
    for page in range(0, pages):
        _dump_workflows(viewer, output, page, MAX_PAGE_SIZE)


def _dump_workflows(viewer: str, output: Path, page: int, size: int) -> None:
    """
    Dump a certain number of workflows.

    :param viewer: CWL Viewer instance URL
    :param output: Local existing directory
    :param page: Page number (first is zero)
    :param size: Number of elements to retrieve
    :return: None
    """
    response = _fetch_workflows_data(viewer, page, size)
    file_name = f'{datetime.now().strftime("%Y-%m-%dT%H%M%S%z")}.json'
    file_output = output / file_name
    logger.debug(f'Dumping page {page}, size {size}, to {file_output}')
    with file_output.open('w', encoding='utf-8') as f:
        f.write(response.text)


def _fetch_workflows_data(viewer: str, page: int, size: int) -> requests.Response:
    """
    Fetch data for workflows. Returned object is the ``requests.Response`` object returned.

    This can be turned into JSON with a simple ``response.json()``, or to text via ``.text()``.
    :param viewer: CWL Viewer instance URL
    :param page: Page number (first is zero)
    :param size: Number of elements to retrieve
    :return: ``requests.Response`` instance
    """
    logger.debug(f'Fetching page {page}, size {size}')
    url = urljoin(viewer, f'/workflows?page={page}&size={size}')
    logger.debug(f'URL: {url}')
    response = requests.get(url, headers={
        'accept': 'application/json'
    })
    return response


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--viewer", help="server base URL", default='https://view.commonwl.org/')
    parser.add_argument("-o", "--output", help="output directory", required=True)
    parser.add_argument("-p", "--page", help="what workflows page to retrieve", type=int, default=0)
    parser.add_argument("-s", "--size", help="how many workflows to retrieve (capped at 2000)", type=int, default=10)
    parser.add_argument("-a", "--all", help="dump all the workflows", action='store_true')
    parser.add_argument("-d", "--debug", help="set logging level to debug", action='store_true')
    args = parser.parse_args()
    if args.all and (args.page > 0 or args.size != 10):
        raise ValueError('You must not specify page or size with all.')
    if args.page < 0:
        raise ValueError('Page must be 0 or greater.')
    if args.size < 1:
        raise ValueError('Size must be at least 1.')
    if args.size > MAX_PAGE_SIZE:
        raise ValueError(f'Size must not be greater than {MAX_PAGE_SIZE}')
    out_path = Path(args.output)
    if not out_path.exists() or not out_path.is_dir():
        raise ValueError(f'Invalid output directory (not a directory, or does not exist): {args.output}')
    if args.debug:
        logger.setLevel(logging.DEBUG)
    logger.info(f'Viewer URL: {args.viewer}')
    logger.info(f'Output: {args.output}')
    if args.all:
        logger.info(f'Dumping all the workflows from {args.viewer} to {out_path}')
        _dump_all_workflows(
            viewer=args.viewer,
            output=out_path
        )
    else:
        logger.info(f'Dumping workflows from {args.viewer}, page {args.page}, size {args.size} to {out_path}')
        _dump_workflows(
            viewer=args.viewer,
            output=out_path,
            page=args.page,
            size=args.size
        )


if __name__ == '__main__':
    main()
