"""Run the real launcher with owned command fixtures; not public-artifact evidence."""

import json
import os
from pathlib import Path
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import unittest


SCRIPT = Path(__file__).resolve().parent.parent / "quickstart.sh"


class QuickstartLauncherTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="qqq-launcher-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.script = self.root / "quickstart.sh"
        shutil.copyfile(SCRIPT, self.script)
        self.env = {"PATH": str(self.bin), "QQQ_NO_BROWSER": "true", "LC_ALL": "C"}
        for name in ("head", "sed", "dirname", "tail", "sleep"):
            (self.bin / name).symlink_to(shutil.which(name))
        self.command("java", 'printf \'openjdk version "21.0.1"\\n\' >&2')
        for name in ("javac", "unzip"):
            self.command(name, "exit 0")
        self.command("git", "echo 'unexpected git invocation' >&2; exit 79")
        self.command("curl", "exit 1")

    def command(self, name, body):
        file = self.bin / name
        file.write_text("#!/bin/sh\n" + body + "\n")
        file.chmod(0o755)

    def run_script(self, *arguments):
        return subprocess.run(["/bin/bash", str(self.script), *arguments],
                              cwd=self.root, env=self.env, capture_output=True,
                              text=True, timeout=8)

    def assert_rejected(self, expected, *arguments):
        result = self.run_script(*arguments)
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn(expected, result.stderr)
        self.assertNotIn("[1/3]", result.stdout)
        self.assertFalse((self.root / "qqq-sample").exists())

    def test_help_and_usage_need_no_installed_prerequisites(self):
        self.env["PATH"] = ""
        result = self.run_script("--help")
        self.assertEqual(0, result.returncode)
        self.assertIn("JDK 21", result.stdout)
        self.assert_rejected("Usage:", "one", "two")

    def test_missing_prerequisites_are_actionable_and_do_not_clone(self):
        for name, expected in (("git", "Install Git"), ("java", "Install JDK 21"),
                               ("javac", "A JDK is required"), ("curl", "Install curl"),
                               ("unzip", "Install unzip")):
            with self.subTest(command=name):
                original = self.bin / name
                hidden = self.bin / (name + ".hidden")
                original.rename(hidden)
                try:
                    self.assert_rejected(expected)
                finally:
                    hidden.rename(original)

    def test_old_java_and_wrong_java_home_are_rejected(self):
        self.command("java", 'printf \'openjdk version "17.0.1"\\n\' >&2')
        self.assert_rejected("Java 21 or later is required")
        self.env["JAVA_HOME"] = str(self.root / "missing jdk")
        self.assert_rejected("Install JDK 21 and check JAVA_HOME")

    def test_invalid_frontend_is_rejected_before_downloading(self):
        self.env["QQQ_FRONTEND"] = "typo"
        self.assert_rejected("QQQ_FRONTEND must be next or material")

    def test_occupied_port_is_preserved_and_reported(self):
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 8000))
            listener.listen()
            self.assert_rejected("Port 8000 is already in use")
            self.assertEqual(8000, listener.getsockname()[1])

    def test_existing_destination_and_its_contents_are_preserved(self):
        destination = self.root / "existing app"
        destination.mkdir()
        marker = destination / "unrelated.txt"
        marker.write_text("keep this")
        result = self.run_script(str(destination))
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Directory already exists:", result.stderr)
        self.assertEqual("keep this", marker.read_text())
        self.assertNotIn("unexpected git invocation", result.stderr)

    def test_clone_failure_is_nonzero_and_does_not_start_a_build(self):
        self.command("git", "echo 'fixture clone failed' >&2; exit 78")
        result = self.run_script("new app")
        self.assertEqual(78, result.returncode)
        self.assertIn("fixture clone failed", result.stderr)
        self.assertNotIn("[2/3]", result.stdout)
        self.assertFalse((self.root / "new app").exists())

    def checkout(self, body):
        project = self.root / "editable app"
        project.mkdir()
        (project / "qqq-sample-project").mkdir()
        (project / "qqq-sample-project/pom.xml").write_text("<project/>")
        self.script = project / "quickstart.sh"
        shutil.copyfile(SCRIPT, self.script)
        wrapper = project / "mvnw"
        wrapper.write_text(body)
        wrapper.chmod(0o755)
        return project

    def test_compile_failure_keeps_diagnostics_and_source_for_retry(self):
        project = self.checkout("#!/bin/sh\necho 'fixture compile failed'; exit 3\n")
        marker = project / "local-edit.txt"
        marker.write_text("keep my changes")
        result = self.run_script()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("fixture compile failed", result.stderr)
        self.assertIn("Application stopped before it was ready", result.stderr)
        self.assertIn("fixture compile failed", (project / "quickstart.log").read_text())
        self.assertEqual("keep my changes", marker.read_text())

    def test_signal_cleanup_and_restart_preserve_edits_and_frontend_choice(self):
        project = self.checkout(f"#!{sys.executable}\n" + '''
import json
import os
from pathlib import Path
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"{}")

server = HTTPServer(("127.0.0.1", 8000), Handler)
server.timeout = 0.1
Path("fixture.json").write_text(json.dumps({"pid": os.getpid(), "args": sys.argv[1:]}))
while not Path("fixture-exit").exists():
    server.handle_request()
server.server_close()
sys.exit(int(Path("fixture-exit").read_text()))
''')
        (self.bin / "curl").unlink()
        (self.bin / "curl").symlink_to(shutil.which("curl"))
        marker = project / "local-edit.txt"
        marker.write_text("keep my changes")
        cases = (("next", signal.SIGINT, 0), ("material", signal.SIGTERM, 0),
                 ("next", None, 0), ("next", None, 42))
        for number, (frontend, stop, expected_exit) in enumerate(cases):
            with self.subTest(frontend=frontend, signal=stop):
                (project / "fixture-exit").unlink(missing_ok=True)
                self.env["QQQ_FRONTEND"] = frontend
                output = project / f"launcher-{number}.log"
                with output.open("w") as stream:
                    launcher = subprocess.Popen(["/bin/bash", str(self.script)],
                                                cwd=self.root, env=self.env,
                                                stdout=stream, stderr=subprocess.STDOUT,
                                                start_new_session=True)
                    try:
                        deadline = time.monotonic() + 10
                        while "Ready in " not in output.read_text():
                            self.assertIsNone(launcher.poll(), output.read_text())
                            self.assertLess(time.monotonic(), deadline, output.read_text())
                            time.sleep(0.05)
                        fixture = json.loads((project / "fixture.json").read_text())
                        self.assertIn(f"-Dqqq.javalin.frontend={frontend}", fixture["args"])
                        self.assertIn("compile", fixture["args"])
                        self.assertIn("exec:java", fixture["args"])
                        conflict = self.run_script()
                        self.assertNotEqual(0, conflict.returncode)
                        self.assertIn("Port 8000 is already in use", conflict.stderr)
                        self.assertIsNone(launcher.poll())
                        if stop is None:
                            (project / "fixture-exit").write_text(str(expected_exit))
                        else:
                            launcher.send_signal(stop)
                        self.assertEqual(expected_exit, launcher.wait(timeout=5), output.read_text())
                        deadline = time.monotonic() + 5
                        while self.process_exists(fixture["pid"]):
                            self.assertLess(time.monotonic(), deadline, "application orphaned")
                            time.sleep(0.05)
                        with socket.socket() as probe:
                            self.assertNotEqual(0, probe.connect_ex(("127.0.0.1", 8000)))
                        self.assertEqual("keep my changes", marker.read_text())
                    finally:
                        try:
                            os.killpg(launcher.pid, signal.SIGKILL)
                        except ProcessLookupError:
                            pass
                        launcher.wait(timeout=5)

    @staticmethod
    def process_exists(pid):
        try:
            os.kill(pid, 0)
            return True
        except ProcessLookupError:
            return False


if __name__ == "__main__":
    unittest.main()
