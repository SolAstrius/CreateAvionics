; -*- mode: Lisp;-*-

; illuaminate config — generates the docs site at https://github.com/SolAstrius/CreateAvionics
; Stubs come from cct-javadoc's LuaDoclet (run via :common:luaJavadoc).

(sources
  /doc/
  /common/build/docs/luaJavadoc/)

(doc
  (destination /build/illuaminate)
  (index doc/index.md)

  (site
    (title "Create: Avionics")
    (url https://solastrius.github.io/CreateAvionics/)
    (source-link https://github.com/SolAstrius/CreateAvionics/blob/${commit}/${path}#L${line}))

  (module-kinds
    (peripheral Peripherals)
    (guide Guides))

  (library-path
    /doc/stub/
    /common/build/docs/luaJavadoc/))

(at /
  (linters
    ;; Most of our peripheral code uses line comments; allow undocumented members
    ;; until we migrate to /** ... */ javadoc.
    -doc:undocumented
    -doc:undocumented-arg
    -doc:undocumented-return
    -var:unused-arg
    -var:unused-global)

  (lint
    (allow-toplevel-global true)))
