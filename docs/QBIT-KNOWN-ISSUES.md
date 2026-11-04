# QBit integration known issues

Updated: 2026-11-04. Unresolved at closeout.

- The reconciliation is unfinished. Review the complete diff and its provenance before using it as a release candidate; do not assume the original private cohort's test results cover every current change.
- Complete Core/RDBMS transaction, durable-job, permission and lifecycle verification is outstanding. Confirm rollback callbacks execute only after confirmed rollback and never weaken the fail-closed paths.
- The Scripting, Collaboration and Record Files dependency chain has not completed a clean build and end-to-end verification on this exact candidate.
- Private qualification versions/build outputs are not installation coordinates. A normal public version and release must precede standalone dependent package publication.
- Shared CI/CD design work is outside this source checkpoint and remains unfinished. Existing canonical pipeline files were preserved.
