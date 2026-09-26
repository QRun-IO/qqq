#!/usr/bin/env python3
"""Exercise a copied starter and generated QBit against a disposable MySQL DB."""

import argparse
import json
import uuid
from urllib.parse import urlparse
from urllib.request import Request, urlopen


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def request(base_url, method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    with urlopen(Request(base_url + path, data=data, method=method,
                         headers={"Content-Type": "application/json"}), timeout=10) as response:
        require(response.status == 200, (method, path, response.status))
        return json.load(response)


def exercise(base_url):
    parsed = urlparse(base_url)
    if parsed.scheme != "http" or parsed.hostname not in ("localhost", "127.0.0.1"):
        raise ValueError("live acceptance is restricted to loopback HTTP")
    metadata = request(base_url, "GET", "/metaData")
    for key, names in (("tables", ("sampleTable", "orderDeskEntity", "orderDeskChildEntity")),
                       ("apps", ("sampleApp", "orderDeskApp")),
                       ("processes", ("orderDeskProcess",)),
                       ("widgets", ("orderDeskDashboardWidget",))):
        require(all(name in metadata[key] for name in names), (key, list(metadata[key])))

    original = request(base_url, "POST", "/qqq/v1/table/sampleTable/query", {})
    require(any(record["values"]["name"] == "First example" for record in original["records"]),
            "seeded starter row missing")
    suffix = uuid.uuid4().hex[:10]
    sample_id = parent_id = child_id = None
    try:
        sample = request(base_url, "POST", "/qqq/v1/table/sampleTable",
                         {"name": "Acceptance " + suffix})
        sample_id = sample["record"]["values"]["id"]
        parent = request(base_url, "POST", "/qqq/v1/table/orderDeskEntity",
                         {"name": "Order " + suffix, "status": "PENDING"})
        parent_id = parent["record"]["values"]["id"]
        child = request(base_url, "POST", "/qqq/v1/table/orderDeskChildEntity",
                        {"name": "Child " + suffix, "orderDeskEntityId": parent_id})
        child_id = child["record"]["values"]["id"]
        request(base_url, "PATCH", f"/qqq/v1/table/orderDeskEntity/{parent_id}",
                {"status": "PROCESSED"})
        parent_read = request(base_url, "GET", f"/qqq/v1/table/orderDeskEntity/{parent_id}")
        child_read = request(base_url, "GET", f"/qqq/v1/table/orderDeskChildEntity/{child_id}")
        sample_read = request(base_url, "GET", f"/qqq/v1/table/sampleTable/{sample_id}")
        require(parent_read["record"]["values"]["status"] == "PROCESSED", "parent update not persisted")
        require(child_read["record"]["values"]["orderDeskEntityId"] == parent_id, "child relation not persisted")
        require(sample_read["record"]["values"]["name"] == "Acceptance " + suffix,
                "starter insert not persisted")
        require(any(record["values"]["id"] == child_id for record in
                    request(base_url, "POST", "/qqq/v1/table/orderDeskChildEntity/query", {})["records"]),
                "child missing from query")
    finally:
        for table, record_id in (("orderDeskChildEntity", child_id),
                                 ("orderDeskEntity", parent_id), ("sampleTable", sample_id)):
            if record_id is not None:
                request(base_url, "DELETE", f"/qqq/v1/table/{table}/{record_id}")
    print("PASS: starter metadata and MySQL read/insert/update/delete; generated parent/child relationship")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8000")
    args = parser.parse_args()
    exercise(args.base_url.rstrip("/"))
