---
name: GitHub push authentication
description: Token-authenticated Git pushes from this workspace
---

GitHub API bearer authentication can succeed while Git smart HTTP rejects a bearer header. Use Basic authentication with the username `x-access-token` and the configured token as the password for Git pushes.

**Why:** GitHub accepted the token through its API but rejected the first Git push method; the Basic form succeeded.

**How to apply:** Build the Basic authorization header in memory or the shell without printing the token, push to the configured remote, and verify the remote branch SHA.