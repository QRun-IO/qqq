#!/usr/bin/env python3
"""Observe only this CI job's opt-in Mongo fixtures; never alter the containers."""

import concurrent.futures
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time


run_id = os.environ.get("QQQ_MONGO_TRACE_RUN", "")
if (sys.platform != "linux" or os.geteuid() != 0
        or os.environ.get("CIRCLECI") != "true"
        or run_id != os.environ.get("CIRCLE_WORKFLOW_JOB_ID")
        or not re.fullmatch(r"[0-9a-f-]{36}", run_id)):
    sys.exit("Mongo tracing requires root on the matching opt-in CircleCI Linux job")

root = Path("/tmp/qqq-mongo-bind-diagnostics")
root.mkdir(exist_ok=True)
stop = root / "stop"
errors = []
observed = set()
captures = []


def command(arguments, pass_fds=()):
    return subprocess.run(arguments, text=True, capture_output=True, timeout=5, check=True, pass_fds=pass_fds).stdout


def identity(pid):
    process = Path(f"/proc/{pid}")
    return (process.joinpath("ns/net").stat().st_ino,
            process.joinpath("stat").read_text().rpartition(")")[2].split()[19])


def capture(container_id):
    destination = root / container_id
    destination.mkdir()
    trace = None
    namespace = None
    try:
        info = json.loads(command([
            "docker", "inspect", "--format",
            '{"id":{{json .Id}},"image":{{json .Image}},"pid":{{.State.Pid}},"running":{{.State.Running}}}', container_id]))
        pid = info["pid"]
        if not info["running"] or pid <= 0:
            raise RuntimeError("Fixture stopped before attachment")
        if container_id not in Path(f"/proc/{pid}/cgroup").read_text():
            raise RuntimeError("Host process cgroup does not match the labelled container")
        original_identity = identity(pid)
        namespace = os.open(f"/proc/{pid}/ns/net", os.O_RDONLY)
        if os.fstat(namespace).st_ino != original_identity[0]:
            raise RuntimeError("Network namespace changed before attachment")
        info.update(started_at=time.time(), network_namespace=original_identity[0],
                    process_start=original_identity[1])
        (destination / "identity.json").write_text(json.dumps(info, indent=2) + "\n")
        with (destination / "attach.log").open("w") as attach, (destination / "sockets.log").open("w") as sockets:
            if (identity(pid) != original_identity
                    or container_id not in Path(f"/proc/{pid}/cgroup").read_text()):
                raise RuntimeError("Fixture process changed before attachment")
            trace = subprocess.Popen([
                "strace", "-ff", "-ttt", "-yy", "-s", "128",
                "-e", "trace=socket,bind,listen,setsockopt,getsockopt,close,shutdown",
                "-p", str(pid), "-o", str(destination / "bind")],
                stdout=attach, stderr=attach, start_new_session=True)
            namespace_command = ["nsenter", f"--net=/proc/self/fd/{namespace}"]
            sockets.write(command(namespace_command + ["cat", "/proc/sys/net/ipv4/ip_local_port_range"], (namespace,)))
            deadline = time.monotonic() + 70
            while not stop.exists() and time.monotonic() < deadline:
                try:
                    if identity(pid) != original_identity:
                        break
                    sockets.write(f"\nTIME {time.time()}\n")
                    sockets.write(command(namespace_command + ["ss", "-H", "-tanpe",
                                           "( sport = :27017 or dport = :27017 )"], (namespace,)))
                    unix = command(namespace_command + ["ss", "-H", "-xanpe"], (namespace,))
                    sockets.write("\n".join(line for line in unix.splitlines() if "mongodb-27017.sock" in line) + "\n")
                    sockets.flush()
                except (FileNotFoundError, ProcessLookupError):
                    break
                except subprocess.CalledProcessError:
                    # Namespace disappearance is expected when the owned fixture stops.
                    if not Path(f"/proc/{pid}").exists():
                        break
                    raise
                time.sleep(0.05)
    except Exception as failure:
        errors.append(f"{container_id}: {type(failure).__name__}: {failure}")
    finally:
        if namespace is not None:
            os.close(namespace)
        if trace is not None:
            if trace.poll() is None:
                # This group contains only the tracer; tracees retain their own groups.
                try:
                    os.killpg(trace.pid, signal.SIGINT)
                except ProcessLookupError:
                    pass
            try:
                trace.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(trace.pid, signal.SIGKILL)
                trace.wait(timeout=5)
            if trace.returncode not in (0, -signal.SIGINT, 128 + signal.SIGINT):
                errors.append(f"{container_id}: tracer exit {trace.returncode}; inspect attach.log")


workers = concurrent.futures.ThreadPoolExecutor(max_workers=4)
try:
    deadline = time.monotonic() + 1800
    while not stop.exists() and time.monotonic() < deadline:
        matches = command(["docker", "ps", "--no-trunc", "--quiet", "--filter",
                           f"label=io.qrun.mongo-bind-trace={run_id}"]).splitlines()
        for container_id in matches:
            if container_id not in observed:
                observed.add(container_id)
                captures.append(workers.submit(capture, container_id))
        time.sleep(0.1)
    if not stop.exists():
        errors.append("Collector reached its 30-minute bound before the CI stop signal")
except Exception as failure:
    errors.append(f"collector: {type(failure).__name__}: {failure}")
finally:
    stop.touch()
    workers.shutdown(wait=True)
    for pending in captures:
        try:
            pending.result()
        except Exception as failure:
            errors.append(f"capture: {type(failure).__name__}: {failure}")
    (root / "result.tmp").write_text(json.dumps({
        "run_id": run_id, "observed_containers": sorted(observed), "errors": errors,
        "limitations": "Tracing changes timing; startup before attachment and brief unsampled owners may be missed."
    }, indent=2) + "\n")
    (root / "result.tmp").replace(root / "result.json")
