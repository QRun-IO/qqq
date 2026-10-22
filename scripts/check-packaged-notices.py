#!/usr/bin/env python3
"""Check exact root LICENSE/NOTICE bytes in release-profile reactor archives."""
import argparse
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from zipfile import BadZipFile, ZipFile

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def reactor_archives():
    parent = ET.parse(ROOT / "pom.xml")
    revision = parent.findtext("m:properties/m:revision", namespaces=NS)
    for module in parent.findall("m:modules/m:module", NS):
        directory = ROOT / module.text
        pom = ET.parse(directory / "pom.xml")
        if pom.findtext("m:packaging", default="jar", namespaces=NS) != "jar":
            continue
        artifact = pom.findtext("m:artifactId", namespaces=NS)
        version = pom.findtext("m:version", namespaces=NS) or pom.findtext("m:parent/m:version", namespaces=NS)
        version = version.replace("${revision}", revision)
        classifiers = ["", "-sources", "-javadoc"]
        if any(goal.text == "test-jar" for goal in pom.findall("m:build/m:plugins/m:plugin/m:executions/m:execution/m:goals/m:goal", NS)):
            classifiers.append("-tests")
        for classifier in classifiers:
            yield directory / "target" / f"{artifact}-{version}{classifier}.jar"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archives", nargs="*", type=Path,
                        help="explicit archives; defaults to every release-profile reactor distribution")
    args = parser.parse_args()
    archives = args.archives or list(reactor_archives())
    expected = {name: (ROOT / name).read_bytes() for name in ("LICENSE", "NOTICE")}
    failures = []
    for archive in archives:
        try:
            with ZipFile(archive) as jar:
                for name, content in expected.items():
                    entry = f"META-INF/{name}"
                    if jar.namelist().count(entry) != 1:
                        failures.append(f"{archive}: expected exactly one {entry}")
                    elif jar.read(entry) != content:
                        failures.append(f"{archive}: {entry} differs from root {name}")
        except (OSError, BadZipFile) as error:
            failures.append(f"{archive}: {error}")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(f"PASS: {len(archives)} archives contain exact root LICENSE/NOTICE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
