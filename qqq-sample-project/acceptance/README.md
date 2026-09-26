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
  --maven-repo /tmp/qqq-acceptance-m2 \
  --live-mysql
```

Populate the dedicated Maven cache from a normal Maven build first. The
runner uses offline Maven to make the tested dependency set reproducible and
prints all three source commit IDs. It fails when the generated QBit is not a
valid live host integration; a successful compile alone is insufficient.
The runner copies only tracked, non-secret-like source paths from the two
first-party checkouts. Its work directory is removed on exit, including on
failure. Supply a new path under the OS temporary directory with `--workdir`
only when inspecting a failed fixture; this explicitly retains generated
files and the starter server log. Never point `--workdir` at existing data.

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

`--live-mysql` requires Docker, the local `mysql:8.4` image, Java and free
loopback ports 3306 and 8000. It creates and removes a disposable container,
seeds its schema, packages and starts the copied starter, and performs real
HTTP CRUD. Deletes must report one deleted record and leave no readable or
queryable row. A separate live JUnit test proves an allowed insert persists
and a denied insert returns 403 without a DB write. The runner then probes
missing child schema, an unavailable DB, and absent DB environment variables.
Every check is required when `--live-mysql` is selected. Without this switch,
the runner covers compilation, metadata and no-DB auth behavior only; that
shorter run is not generated-consumer acceptance. Both inventory rows remain
pending until the corrected first-party template is integrated and these
live checks are mapped to release-gate reports.
