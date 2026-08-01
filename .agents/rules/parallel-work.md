# Codex + Antigravity parallel work

This rule applies to every task in this repository.

## Sources of truth

- Notion: task intent, owner, scope, branch, status, and verification evidence.
- GitHub: branch, commit, checks, deployment workflow, and deployed revision.
- Chat history and unpushed local files are not shared state.

The only authorized repositories are `xogh3198/MONEY_PROJECT_FRONT` and
`xogh3198/MONEY_PROJECT`. Do not access another GitHub repository or use broad account-level GitHub
listing/search.

The only authorized project dashboard is `이태호의 Notion → 기술 블로그 → 개인프로젝트 →
개인프로젝트 통합 작업 대시보드`, using `notion_inha` after verifying
`xogh3198@inha.edu` and workspace `이태호의 Notion`.

## Before editing

1. Verify the exact GitHub remote and fetch `origin/main`.
2. Check the unified Notion dashboard for the same task and overlapping paths.
3. Stop if another agent owns the task or overlapping files.
4. Claim the task as `진행 중`. Record agent, repository, branch, planned paths, and start time.
5. Work in a separate Git worktree. Codex and Antigravity must never edit the same checkout.
6. Use `agent/codex/<task-slug>` or `agent/antigravity/<task-slug>`.

Only the recorded owner edits claimed paths and the active Notion task. A handoff must record commit,
dirty files, tests, remaining work, and new owner. Do not take over an ambiguous or stale task without
checking GitHub and asking the user.

## Delivery and deployment

Both agents may deliver verified work to `main` and use existing GitHub-triggered Vercel or EC2
workflows. Before pushing `main`, fetch again, reconcile new commits, rerun tests, and confirm no overlap.
Do not start a second backend deployment while one is running.

AWS direct access is Codex-only. Antigravity must not use AWS CLI, AWS Console, EC2 SSH, SSM, Secrets
Manager, or direct AWS APIs. Antigravity may dispatch and inspect the existing GitHub Actions deployment
workflow. Never change credentials, secrets, OAuth scopes, or external permissions without explicit
user approval.

When finished, push the verified commit and update the same Notion task to `완료` with commit hashes,
checks, deployment evidence, and verification date. Incomplete work remains `진행 중` with a concrete
handoff.

When the sibling frontend repository is present, read its `docs/AI_COLLABORATION.md` before the first
parallel task.
