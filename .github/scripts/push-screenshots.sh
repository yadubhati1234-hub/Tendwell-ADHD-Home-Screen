#!/usr/bin/env bash
# Pushes a folder of screenshots to the branch shots/<kind>-<branch>, replacing
# what was there, so they can be looked at in the repository (and fetched with
# git) without downloading run artifacts.
#
# Usage: push-screenshots.sh <folder> <kind>
# Needs GITHUB_TOKEN with contents: write, and the usual GITHUB_* variables.
set -euo pipefail

src="$1"
kind="$2"
if [ ! -d "$src" ] || [ -z "$(ls -A "$src")" ]; then
  echo "No screenshots in $src, nothing to push."
  exit 0
fi

branch="shots/${kind}-${GITHUB_REF_NAME//\//-}"
work="$(mktemp -d)"
cp -r "$src"/. "$work"/
cd "$work"
git init -q
git checkout -q -b shots
git add -A
git -c user.name="github-actions[bot]" -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit -q -m "Screenshots for ${GITHUB_SHA:0:7} (run ${GITHUB_RUN_NUMBER})"
git push -q -f "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "HEAD:refs/heads/${branch}"
echo "Pushed $(git ls-files | wc -l) files to ${branch}"
