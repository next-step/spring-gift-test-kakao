---
name: commit
description: Create a git commit following AngularJS commit message convention. Use when the user wants to commit changes.
argument-hint: [optional message hint]
allowed-tools: Bash(git *)
---

# Git Commit Skill (AngularJS Convention)

You are a git commit assistant. Create commits following the AngularJS commit message convention below.

## Procedure

1. Run `git status` (never use `-uall` flag) and `git diff --staged` in parallel to understand current changes.
2. If there are no staged changes, run `git diff` to check unstaged changes and stage the relevant files with `git add <specific-files>`. Never use `git add -A` or `git add .`.
3. Run `git log --oneline -10` to see recent commit style for context.
4. Analyze the changes and determine the appropriate **type** and **scope**.
5. Draft a commit message following the format below.
6. Create the commit. Always use a HEREDOC to pass the message:

```bash
git commit -m "$(cat <<'EOF'
<type>(<scope>): <subject>

<body>

<footer>
EOF
)"
```

7. Run `git status` after commit to verify success.

## Commit Message Format

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

**Any line of the commit message cannot be longer than 100 characters.**

### Type (required)

Must be one of:

| Type       | Description                                        |
|------------|----------------------------------------------------|
| `feat`     | A new feature                                      |
| `fix`      | A bug fix                                          |
| `docs`     | Documentation only changes                         |
| `style`    | Formatting, missing semicolons, etc. (no logic change) |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `test`     | Adding missing tests                               |
| `chore`    | Maintenance tasks (build, CI, tooling, etc.)       |

### Scope (optional but recommended)

The scope specifies the place of the commit change. For example: a module name, file name, component name, or feature area. Use lowercase.

Examples: `auth`, `api`, `product`, `category`, `config`, `build`

If the scope is not meaningful or changes span too many areas, omit it: `<type>: <subject>`

### Subject (required)

- Use imperative, present tense: "change" not "changed" nor "changes"
- Do NOT capitalize the first letter
- Do NOT end with a period (.)
- Keep it concise and descriptive

### Body (optional)

- Use imperative, present tense
- Include the **motivation** for the change
- Contrast with **previous behavior** when relevant
- Omit the body if the subject line is self-explanatory

### Footer (optional)

Use for two purposes only:

**Breaking Changes:**
```
BREAKING CHANGE: <description of what changed>

<migration instructions>
```

**Closing Issues:**
```
Closes #123
Closes #123, #245, #992
```

## Rules

- If `$ARGUMENTS` is provided, use it as a hint for the commit message but still follow the convention strictly.
- Do NOT modify any files. This skill only creates commits.
- Do NOT push to remote unless explicitly asked.
- Do NOT amend previous commits unless explicitly asked.
- Do NOT skip hooks (no `--no-verify`).
- Always add `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>` as the last line of the commit message.
- If pre-commit hook fails, fix the issue and create a NEW commit (never amend).

## Examples

Single-line (no body/footer):
```
feat(product): add stock quantity validation
```

With body:
```
fix(auth): resolve token expiration not being checked

the middleware was skipping token validation when the
Authorization header contained a Bearer prefix with
extra whitespace

Closes #42
```

Breaking change:
```
refactor(api): change product endpoint response format

return paginated response wrapper instead of raw array
to support cursor-based pagination

BREAKING CHANGE: GET /api/products now returns
{ content: [...], page: {...} } instead of a plain array.
Clients must update to read from the content field.

Closes #89
```
