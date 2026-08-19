# PR: Chronicle-Values#42 (VALID_FEATURE)

**Track C — Feature/docs PR**  ·  priority tier 6  ·  base `ea`  ·  branch `feat/Chronicle-Values-42-opt-in-generated-source-dump-dir-c`
Issue: https://github.com/OpenHFT/Chronicle-Values/issues/42

## Planned change
Opt-in generated-source dump dir; compile from files (Wire's pattern); verify debugger stepping/source mapping.

## Quality bar (must clear before this PR merges)
- One issue, one PR; link with `Fixes #42`; scope limited to this issue.
- Regression test that fails before / passes after (re-enable the ignored test where one exists, else add one).
- Cross-repo discipline: Chronicle-Queue exposes integration points only; retention/roll-maintenance policy lives in CQE.
- Do not fork the in-flight QUEUE-143/144 PRs; branch off current `ea` and rebase.
- Docs + changelog updated; author credit retained on any rebase; CI proven green.

## Status
Ready to implement on this branch.
_This PR-NOTES commit is the local addressment scaffold; the code change lands on top._
