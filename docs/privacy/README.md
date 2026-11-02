# Privacy policy page

`index.md` is the source for the public privacy policy URL that Play Console requires.

The main repo is private, and GitHub Pages on a private repo needs a paid GitHub plan, so the page is published from a separate **public** repo instead:

1. Create a public repo (for example `launcher-privacy`) under the same GitHub account.
2. Copy `index.md` into its root and fill in the `[BRACKETED]` placeholders.
3. Repo Settings, Pages, "Deploy from a branch", branch `main`, folder `/ (root)`. Save.
4. After about a minute the policy is live at `https://<github-user>.github.io/<repo-name>/`. Paste that URL into Play Console, App content, Privacy policy.

Keep this copy and the public copy in sync when features change (for example when analytics events are added).
