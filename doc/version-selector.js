// Version selector for the docs site. Injected into every page (including
// archived per-version snapshots) at site-composition time by
// .github/scripts/compose-docs-site.sh, so old snapshots always get the
// current selector. Reads /versions.json, also written at composition time.
//
// URL layout: site root serves the latest stable release's docs, /dev/ tracks
// main, /vX.Y.Z/ are immutable per-release archives.
(function () {
    'use strict';

    var script = document.currentScript;
    if (!script || !script.src) return;
    // The script sits at the site root; derive the base path from our own src
    // so nothing needs to hardcode the GitHub Pages project prefix.
    var basePath = new URL(script.src, location.href).pathname
        .replace(/version-selector\.js(\?.*)?$/, '');

    var rel = location.pathname.indexOf(basePath) === 0
        ? location.pathname.slice(basePath.length)
        : '';
    var seg = rel.split('/')[0];
    var current, pagePath;
    if (seg === 'dev' || /^v\d/.test(seg)) {
        current = seg;
        pagePath = rel.slice(seg.length + 1);
    } else {
        current = '';
        pagePath = rel;
    }

    fetch(basePath + 'versions.json')
        .then(function (r) { return r.json(); })
        .then(function (manifest) {
            var entries = [{ value: '', label: manifest.latest + ' (latest)' }];
            if (manifest.dev) entries.push({ value: 'dev', label: 'dev (unreleased)' });
            manifest.versions.forEach(function (v) {
                if (v !== manifest.latest) entries.push({ value: v, label: v });
            });

            var select = document.createElement('select');
            select.id = 'cav-version-selector';
            select.title = 'Documentation version';
            select.setAttribute('aria-label', 'Documentation version');
            entries.forEach(function (e) {
                var opt = document.createElement('option');
                opt.value = e.value;
                opt.textContent = e.label;
                select.appendChild(opt);
            });
            // A pinned snapshot may predate the manifest's version list; make
            // sure the current version is always selectable.
            if (current && !entries.some(function (e) { return e.value === current; })) {
                var extra = document.createElement('option');
                extra.value = current;
                extra.textContent = current;
                select.appendChild(extra);
            }
            select.value = current;

            // Outdated-version warning tint: anything that isn't the latest
            // stable (root) or dev gets flagged amber.
            if (current && current !== 'dev') select.classList.add('cav-outdated');

            select.addEventListener('change', function () {
                var prefix = select.value ? select.value + '/' : '';
                var target = basePath + prefix + pagePath;
                // Keep the page path when it exists in the target version,
                // otherwise land on that version's index.
                fetch(target, { method: 'HEAD' })
                    .then(function (r) { location.href = r.ok ? target + location.hash : basePath + prefix; })
                    .catch(function () { location.href = basePath + prefix; });
            });

            var style = document.createElement('style');
            style.textContent =
                '#cav-version-selector{position:fixed;right:.75rem;bottom:.75rem;z-index:1000;' +
                'padding:.3em .5em;font:inherit;font-size:.85em;border-radius:6px;' +
                'border:1px solid #bbb;background:#fff;color:#222;box-shadow:0 1px 4px rgba(0,0,0,.15);cursor:pointer}' +
                '#cav-version-selector.cav-outdated{border-color:#b8860b;box-shadow:0 1px 4px rgba(184,134,11,.4)}' +
                '@media(prefers-color-scheme:dark){#cav-version-selector{background:#222;color:#eee;border-color:#555}' +
                '#cav-version-selector.cav-outdated{border-color:#fbbf24}}';

            document.head.appendChild(style);
            document.body.appendChild(select);
        })
        .catch(function () { /* manifest missing (e.g. local preview) — no selector */ });
})();
