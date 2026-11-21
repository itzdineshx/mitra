# GIT_WORKFLOW.md — branches, commits and merging

> Replaces "one task = one branch = one pull request". That created too many branches and pull requests for a solo project. Now: **one branch per roadmap version, one commit per task, one pull request per version.**

## 1. The branches

| Branch | What it is | Who writes to it |
|---|---|---|
| `main` | Always builds and passes tests. Only receives finished versions (and tiny fixes to CI or docs). Tagged per version (`v0.1.0` …). | merges only |
| `phase<N>/<version-folder>` | The working branch for one roadmap version. Examples: `phase1/v0.1-hello-mitra`, `phase1/v0.2-trust`, `phase1/v0.3-team`, `phase1/v0.4-plugins`, `phase1/v0.5-web`, `phase1/v0.6-control-centre`, `phase2/v0.7-hardened`, `phase3/v0.9-research-preview`, `phase4/v1.0-bharat`, `phase4/v1.x-everyday-mitra`, `phase5/v2.0-platform`. The phase number is in `roadmap/README.md` (Phases table). | the developer / AI |
| `fix/<short-name>` | Only for an urgent fix on `main` (for example a broken CI). Merge it quickly and delete it. | |

Rules:
- Only **one** `phase…` branch is active at a time. Start the next one from an up-to-date `main` after the previous version is merged.
- **No branch per task.** No branches named `feat/T09-…`.
- **Dependabot is paused** (see `.github/dependabot.yml`). Never merge its pull requests during the sprint. Re-enable it after the v0.6 release (T27).

## 2. Day-to-day on a version branch

```
git switch main && git pull
git switch -c phase1/v0.1-hello-mitra        # once per version
# ... work on one task ...
./gradlew spotlessApply test assembleDirectDebug assembleFossDebug
python scripts/roadmap_status.py
git add -A
git commit -m "feat(T03): debounce snapshots and tag provenance"
git push -u origin phase1/v0.1-hello-mitra   # push after every task
```

- **One commit per finished task** (more if the task is big: `feat(T03): …`, `test(T03): …`). Every commit must build and pass the tests.
- Commit message: `type(Txx): short description` with type `feat`, `fix`, `test`, `docs`, `chore`, `refactor`. Roadmap ticks go in the same commit as the work.
- CI runs on every push to `main` and to `phase*/**` branches, so you see red or green after each task.
- **Never rewrite pushed history** (`push --force`, `reset --hard` on shared commits). If a commit is wrong, add a new commit that fixes it.

## 3. Finishing a version (merging to `main`)

When the version's **exit criteria** (top of its `roadmap/<version>/README.md`) are met and the phone checks passed:

1. `main` merged into the branch first (`git merge main`) and CI is green on the branch.
2. Open **one pull request**: `phase1/v0.1-hello-mitra` → `main`, titled `v0.1 — Hello Mitra`. The description lists the exit criteria with their evidence (numbers, screenshots).
3. Owner reviews and merges with **Create a merge commit** (not squash, so each task commit stays visible).
4. Tag: `git tag -a v0.1.0 -m "v0.1 Hello Mitra" && git push origin v0.1.0`.
5. Delete the version branch (GitHub does it if "Automatically delete head branches" is on). Create the next branch from `main`.

## 4. Safety habits (so no work is ever lost)

- Push the branch after every task. Work that exists only on one laptop is not safe.
- Never run `git reset --hard`, `git clean -fd`, `git push --force` or delete a branch without first showing the exact command and what it will remove, and getting the owner's "go".
- Before deleting a branch, prove it is merged: `git branch -r --merged origin/main` lists it, or `git diff origin/main origin/<branch>` is empty.
- If something looks lost: `git reflog` lists every recent position of HEAD; a commit can be restored from there for about 90 days.

## 5. Repository settings (owner, once)

- Settings › General › Pull Requests: allow **merge commits**, tick **Automatically delete head branches**.
- Settings › Advanced Security (Code security): turn **Dependabot version updates** off until v0.6 is released.
- Optional, Settings › Rules › main: require status checks (CI) before merging.
