# Starter and application QBit source acceptance (#607, #608)

This fixture uses local checkouts of the first-party `qqq-app-starter` and
`qbit-template-application` repositories. It installs the selected QQQ 4.1
source snapshot into a dedicated Maven cache, makes disposable copies of both
consumers, follows the template's documented package/class/coordinate rename,
registers the resulting QBit in the copied starter, then runs JUnit against
the actual host. It does not edit either upstream repository.

```sh
export QQQ_STARTER_SOURCE=/path/to/qqq-app-starter
export QQQ_TEMPLATE_SOURCE=/path/to/qbit-template-application
export QQQ_SOURCE=/path/to/qqq
python3 -m unittest discover -s "$QQQ_SOURCE/qqq-sample-project/acceptance" -p 'test_starter_application.py' -v
python3 "$QQQ_SOURCE/qqq-sample-project/acceptance/run_starter_application.py" \
  --qqq-source "$QQQ_SOURCE" \
  --starter-source "$QQQ_STARTER_SOURCE" \
  --template-source "$QQQ_TEMPLATE_SOURCE" \
  --maven-repo /tmp/qqq-acceptance-m2
```

Populate the dedicated Maven cache from a normal Maven build first. The
runner uses offline Maven to make the tested dependency set reproducible and
prints all three source commit IDs. It fails when the generated QBit is not a
valid live host integration; a successful compile alone is insufficient.

Current first-party template `ebc77b9` has two observed blockers against QQQ
source `38f784fd9`: its two entity annotations only request possible-value
sources, so no table metadata is produced; and its app calls `withSection` but
does not populate `QAppMetaData.children`, causing starter metadata validation
to fail before HTTP startup. The isolated template fix branch
`feature/GH-608-template-acceptance` also adds the JavaBean setters and
`BackendStep` implementation required for valid table fields and process
metadata. The copied starter fixture now tests metadata, HTTP startup,
configured mock-auth denial, invalid configuration, duplicate registration,
and missing host dependency.

For the optional live database check, use a disposable loopback MySQL 8.4
container with database `qqq_starter_test`, user/password `test`/`test`, and
port 3306. Seed it with `starter-application-mysql.sql`, package the generated
starter from the fixture directory, and start its JAR with `RDBMS_VENDOR=mysql`,
`RDBMS_HOSTNAME=127.0.0.1`, `RDBMS_PORT=3306`,
`RDBMS_DATABASE_NAME=qqq_starter_test`, and the disposable credentials. Then:

```sh
python3 "$QQQ_SOURCE/qqq-sample-project/acceptance/live_starter_application.py" \
  --base-url http://127.0.0.1:8000
```

The live check reads, inserts, updates and deletes through the starter and
generated parent/child HTTP APIs. Stop the copied server and remove the
container afterward. Missing schema, unavailable database and absent DB
environment variables were also exercised manually against disposable state.
Both inventory rows remain pending until the corrected first-party template
is integrated and these live checks are mapped to release-gate reports.
