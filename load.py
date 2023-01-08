#!/usr/bin/env python3

import sys
import re
import time
import gzip
from urllib.parse import urljoin
import requests
import json

re.compile

isCommit = re.compile("^[0-9a-fA-F]{40}$")

HEADERS = {"User-Agent": "cwlviewer-load/0.0.1", "Accept": "application/json"}


def parse_gitinfos(sourceFp):
    doc = json.load(sourceFp)
    for wf in doc["content"]:
        yield wf["retrievedFrom"]


def is_not_commit(gitinfo):
    return not isCommit.match(gitinfo["branch"])


def make_requests(gitinfos):
    for git in gitinfos:
        # dict_keys(['repoUrl', 'branch', 'path', 'packedId', 'url', 'rawUrl', 'type'])
        req = {"url": git["repoUrl"], "branch": git["branch"], "path": git["path"]}
        if git["packedId"]:
            req["packedId"] = git["packedId"]
        yield req


def send(base, req):
    url = urljoin(base, "/workflows")
    r = requests.post(url, data=req, allow_redirects=False, headers=HEADERS)
    print("Posted: %s" % req, file=sys.stderr)
    if r.status_code == 202:
        location = urljoin(url, r.headers["Location"])
        print("  queued: %s" % location, file=sys.stderr)
        # need to check later
        return location
    if r.status_code == 303:
        print("  done: %s" % r.headers["Location"], file=sys.stderr)
        return None  # Already there, all OK
    print(f"Unhandled HTTP status code: {r.status_code}  {r.text}", file=sys.stderr)
    print(req)


def send_requests(base, requests):
    for req in requests:
        yield send(base, req)


def is_running(location):
    if not location:
        return True
    queued = requests.get(location, allow_redirects=False, headers=HEADERS)
    if queued.status_code == 303:
        # Done!
        return False
    if not queued.ok:
        print(f"Failed {location}: {queued.text}", file=sys.stderr)
        return False
    j = queued.json()
    if j["cwltoolStatus"] == "RUNNING":
        return True
    elif j["cwltoolStatus"] == "ERROR":
        print(f"Failed {location}: {j['message']}", file=sys.stderr)
        return False
    else:
        raise Exception(f"Unhandled queue status: {queued.status_code} {queued.text}")


MAX_CONCURRENT = 3  # Maximum number in queue
SLEEP = 0.5  # wait SLEEP seconds if queue is full


def trim_queue(queue):
    new_queue = []
    for q in queue:
        if is_running(q):
            # print("Still running %s" % q)
            new_queue.append(q)
    print(f"Trimmed queue from {len(queue)} to {len(new_queue)}", file=sys.stderr)
    return new_queue


def main(jsonfile="-", base="http://view.commonwl.org:8082/", *args):
    if jsonfile == "-":
        source = sys.stdin
    elif jsonfile.endswith(".gz"):
        source = gzip.open(jsonfile, "rb")
    else:
        source = open(jsonfile, "rb")

    gitinfos = parse_gitinfos(source)
    if "--no-commits" in args:
        gitinfos = filter(is_not_commit, gitinfos)

    requests = make_requests(gitinfos)
    queued = []
    for q in send_requests(base, requests):
        if q:
            queued.append(q)
        while len(queued) >= MAX_CONCURRENT:
            time.sleep(SLEEP)
            queued = trim_queue(queued)
    # Finish the rest of the queue
    while queued:
        queued = trim_queue(queued)


if __name__ == "__main__":
    if "-h" in sys.argv:
        print("load.py [jsonfile] [baseurl] [--no-commits]")
        sys.exit(1)

    main(*sys.argv[1:])
