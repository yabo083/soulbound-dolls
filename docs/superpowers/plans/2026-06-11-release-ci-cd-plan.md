# Release CI/CD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add initial Git/GitHub release scaffolding for Soulbound Dolls, with local first commit only and no push.

**Architecture:** Keep build verification and publishing as separate GitHub Actions workflows. Build runs on normal pushes/PRs; publishing only runs on `v*` tags or manual dispatch and uses GitHub Secrets for Modrinth/CurseForge tokens and project IDs. A root `.gitignore` prevents generated Gradle, IDE, CodeGraph, and run artifacts from entering the first commit.

**Tech Stack:** Gradle, Java 21, NeoForge ModDev, GitHub Actions, `Kir-Antipov/mc-publish`.

---

### Task 1: Repository Hygiene and Version Metadata

**Files:**
- Create: `.gitignore`
- Modify: `gradle.properties`
- Create: `CHANGELOG.md`
- Create: `docs/release.md`

- [ ] **Step 1: Add root `.gitignore`**

Ignore generated Gradle/build/run/IDE files while preserving source, docs, resources, and wrapper files.

- [ ] **Step 2: Set release version to `0.1.0`**

Change `mod_version=0.1.0-dev` to `mod_version=0.1.0` in `gradle.properties`.

- [ ] **Step 3: Add `CHANGELOG.md` for version `0.1.0`**

Document the initial NeoForge 1.21.1 release, player-bound dolls, bound item rendering, and item-frame/inventory fixes.

- [ ] **Step 4: Add `docs/release.md`**

Document required repository secrets: `MODRINTH_PROJECT_ID`, `MODRINTH_TOKEN`, `CURSEFORGE_PROJECT_ID`, `CURSEFORGE_TOKEN`.

### Task 2: GitHub Actions Workflows

**Files:**
- Create: `.github/workflows/build.yml`
- Create: `.github/workflows/publish.yml`

- [ ] **Step 1: Add build workflow**

Use Java 21 and Gradle cache, then run `./gradlew :platforms:neoforge-1.21.1:build` and upload built jars.

- [ ] **Step 2: Add publish workflow**

Trigger on `v*` tags and manual dispatch. Build the jar, derive the release version from the tag or manual input, then publish the NeoForge 1.21.1 jar to Modrinth and CurseForge through `Kir-Antipov/mc-publish`.

### Task 3: Verify, Create Remote, Commit Locally

**Files:**
- No additional source files.

- [ ] **Step 1: Run full build**

Run: `./gradlew :platforms:neoforge-1.21.1:build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Create GitHub repository remote without push**

Use `gh repo create soulbound-dolls --public --source . --remote origin --description "Player-bound dolls for NeoForge."` if the repository does not exist. If it exists, set `origin` to that repo URL.

- [ ] **Step 3: Review staged files**

Run: `git status --short` and ensure generated folders are not staged.

- [ ] **Step 4: Create local initial commit**

Run: `git add . && git commit -m "Initial Soulbound Dolls mod"`.

Expected: local commit exists on `main`; no `git push` is run.

---

## Self-Review

- Spec coverage: version starts at `0.1.0`; CI builds; publish workflow targets Modrinth and CurseForge; GitHub remote is created without push; only a local commit is made.
- Placeholder scan: no placeholders or private tokens are embedded.
- Type consistency: workflow secret names match release documentation.
