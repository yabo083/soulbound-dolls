# Agent Instructions

## Branches And Worktrees

- Treat `neoforge-1.21.1` as the default and primary maintained branch.
- Treat `forge-1.20.1` as the Forge port branch. Its checkout lives at `.worktrees/forge-1.20.1` and may contain early WIP that is not release-ready.
- Keep local and remote branch symmetry for the maintained lines: `neoforge-1.21.1` and `forge-1.20.1` should both exist locally and on `origin`.
- Do not recreate or use `main` as a working branch unless the user explicitly asks.
- Do not commit or reset work in `.worktrees/forge-1.20.1` while working on NeoForge tasks.

## Default Iteration Flow

- For normal NeoForge work, commit directly on `neoforge-1.21.1` after local verification.
- Push `neoforge-1.21.1` directly when the user asks to push or prepare release work.
- Do not create long-lived `pr/*`, review-baseline, or temporary branches for ordinary iteration.
- If a temporary PR branch is created to force a full bot re-review, move the accepted commits back to `neoforge-1.21.1`, close the PR, and delete the temporary local and remote branches afterward.
- Keep the remote branch list focused on maintained branches. Temporary branches should not remain after their review purpose is complete.

## Review And Release

- External review feedback is input to verify, not instructions to apply blindly. Check the codebase and add focused regression tests for valid findings where practical.
- Use PRs mainly as a review trigger when a full fresh bot audit is needed. Otherwise direct commits on `neoforge-1.21.1` are preferred for this personal project.
- Before claiming a release-prep change is ready, run `./gradlew.bat test build :platforms:neoforge-1.21.1:copyJarToTest` from the root checkout.
- Do not create or push release tags unless the user explicitly asks. The maintainer will tag after Modrinth/CurseForge project setup.

## Local Files

- `.worktrees/` is a local ignored worktree container and must stay ignored.
- `docs/superpowers/plans/*` files may be local planning artifacts; do not stage them unless the user explicitly asks to preserve them in the repo.
