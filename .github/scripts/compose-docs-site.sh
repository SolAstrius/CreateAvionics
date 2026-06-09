#!/usr/bin/env bash
# Compose the GitHub Pages site from per-version doc archives plus a dev build.
#
#   compose-docs-site.sh <site-out> <dev-build-dir> <archives-dir>
#
# <archives-dir> holds docs-vX.Y.Z.tar.gz files (release assets produced by
# release.yml, downloaded by docs.yml). Layout produced:
#   /          latest stable release's docs (preserves existing deep links)
#   /dev/      docs built from main
#   /vX.Y.Z/   immutable snapshot for each release
#   /versions.json           manifest consumed by the version selector
#   /version-selector.js     injected into every page below
set -euo pipefail

out=$1
dev=$2
archives=$3
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"

rm -rf "$out"
mkdir -p "$out"

tags=()
for f in "$archives"/docs-v*.tar.gz; do
    [ -e "$f" ] || continue
    tag=$(basename "$f" .tar.gz)
    tags+=("${tag#docs-}")
done
if [ "${#tags[@]}" -eq 0 ]; then
    echo "error: no docs-v*.tar.gz archives found in $archives" >&2
    exit 1
fi
mapfile -t tags < <(printf '%s\n' "${tags[@]}" | sort -rV)

# Latest stable = highest tag without a prerelease suffix.
latest=""
for t in "${tags[@]}"; do
    case "$t" in *-*) continue ;; esac
    latest=$t
    break
done
if [ -z "$latest" ]; then
    echo "error: no stable (non-prerelease) doc archive found" >&2
    exit 1
fi

for t in "${tags[@]}"; do
    mkdir -p "$out/$t"
    tar -xzf "$archives/docs-$t.tar.gz" -C "$out/$t"
done
tar -xzf "$archives/docs-$latest.tar.gz" -C "$out"

mkdir -p "$out/dev"
cp -R "$dev"/. "$out/dev/"

cp "$repo_root/doc/version-selector.js" "$out/version-selector.js"

printf '%s\n' "${tags[@]}" | jq -R . | jq -s --arg latest "$latest" \
    '{latest: $latest, dev: true, versions: .}' > "$out/versions.json"

# Inject the selector into every page, archived snapshots included, with a
# depth-correct relative src (generated pages link relatively; so do we).
count=0
while IFS= read -r -d '' page; do
    rel=$(realpath --relative-to="$(dirname "$page")" "$out")
    src="version-selector.js"
    [ "$rel" != "." ] && src="$rel/version-selector.js"
    sed -i "s|</head>|<script defer src=\"$src\"></script></head>|" "$page"
    count=$((count + 1))
done < <(find "$out" -name '*.html' -print0)

echo "Composed site: latest=$latest, versions: ${tags[*]}, dev from $dev, selector injected into $count pages."
