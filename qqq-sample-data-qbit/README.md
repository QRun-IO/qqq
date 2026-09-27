# Generated reference-data QBit

This separate JAR is a first-party consumer derived from `qbit-template-data` at `30bcb3c52fb9ad54657691706d21e6c2d7f65c2f` for QQQ 4.1 acceptance. Its config, natural-key sync, bundled JSON, and tokenized Liquibase generator adapt that template's corresponding sources; explicit table metadata replaces the template's obsolete 0.35 annotation path. The source-only `data-qbit-acceptance` profile adds this locally installed JAR and six host tests to `qqq-sample-project`. Those tests register the producer, run the QQQ sync process, and perform a real H2 Liquibase migration. The original 0.35 template is not claimed to compile unchanged on QQQ 4.1. This fixture is outside the root release reactor and is not published; the default sample and published-candidate path do not depend on it.

After installing the framework reactor into a disposable Maven repository, run:

```sh
mvn -B -Drevision=4.1.0-SNAPSHOT -f qqq-sample-data-qbit/pom.xml clean install
mvn -B -Drevision=4.1.0-SNAPSHOT -f qqq-sample-project/pom.xml \
  -Pacceptance-tests,data-qbit-acceptance clean verify
```

The source CI job uses its own exact internal revision and Maven settings for
both commands. The `data-qbit-acceptance` profile must not be enabled in the
published-candidate sample run: that run resolves only public framework artifacts.
