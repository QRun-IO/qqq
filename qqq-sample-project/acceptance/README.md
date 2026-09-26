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
to fail before HTTP startup. The acceptance inventory rows remain pending
until the corrected template and live starter data/auth/negative checks pass.
