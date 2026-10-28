## Buy-02: Workflow and Quality

### Branching
- `main` is protected: PR required, 1 approval, green Jenkins pipeline.
- Branch names: `feature/<name>`, `fix/<name>`, `docs/<name>`.
- Commit messages: `feat(order): add cart endpoints`, `fix(product): ...`

### Pull request process
1. Create a branch from `main`, make small focused changes.
2. Open a PR using the template.
3. Jenkins runs: unit tests, SonarQube analysis, quality gate (build/deploy only on `main`).
4. A reviewer checks security, performance, readability, then approves.
5. Merge (squash), delete the branch.

### Code review checklist
- Security: authZ, ownership checks, no data leaks
- Performance: indexes, pagination, no N+1 calls
- Readability: naming, small methods, tests

### Environments
Staging: the Oracle Cloud server, deployed by Jenkins on every merge to `main`.

### SonarQube improvements log
| PR | Rule / issue | Fix |
|----|--------------|-----|
|    |              |     |