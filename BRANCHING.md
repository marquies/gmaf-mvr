# Branching & Release

## Linie

`master` ist der **einzige** Langzeitbranch und immer auslieferbar. Es gibt kein `develop`.

- Arbeit passiert auf kurzlebigen Branches: `feat/…`, `fix/…`, `ci/…`, `docs/…`, `test/…`
- Keine direkten Commits auf `master` — Alles über Pull Requests, **Squash-Merge**
- Nach dem Merge wird der Branch gelöscht

## Releases

- Ein Release ist ein Tag `vX.Y.Z` auf `master`
- `.github/workflows/release.yml` leitet die Version aus dem Tag ab (`${GITHUB_REF_NAME#v}`)
- Tag-Versionen sind unveränderlich: einmal `v1.0.7` veröffentlicht, nie überschreiben

## CI-Builds (Snapshots)

GitHub Packages ist **unveränderlich**. Sobald eine Version existiert, scheitert jeder
weitere Deploy derselben Version mit `HTTP 409 Conflict`.

Deshalb veröffentlicht die CI keine feste Version, sondern pro Commit eine eindeutige:

```
Basis aus pom.xml (z.B. 1.0.7)  →  1.0.7-<sha7>
```

Der Schritt `Set unique package version for this commit` setzt das zur Laufzeit per
`mvn versions:set -DgenerateBackupPoms=false`. Die POM im Repository bleibt unverändert —
sie trägt weiterhin die Basisversion.

## Rechte

- **Agenten und Bots** bekommen Schreib-, **keine Adminrechte**
- Sie pushen **in das Repository**, nicht in einen Fork: Fork-PRs lösen das
  `action_required`-Gate aus und können keine Actions starten
- Der Agent öffnet den PR und meldet das Ergebnis — **gemerged wird von einem Menschen**
- Fremde Registries brauchen ein eigenes PAT (`PACKAGES_TOKEN`), das `github.token`
  reicht nicht über Repo-Grenzen

## Pull Requests

- **Ein Anliegen pro PR.** Kein „nebenbei noch aufgeräumt"
- Conventional Commits: `feat:`, `fix:`, `ci:`, `docs:`, `test:`, `chore:`
- Der PR-Beschreibung muss zu entnehmen sein, **warum** die Änderung nötig ist
- Bei Agenten-PRs: Diff gegen die Vorgabe prüfen. „CI grün" ist kein Beweis,
  dass die Änderung das Richtige tut

## Branch-Schutz auf `master`

Empfohlen (Repository-Settings → Branches):

- Require pull request before merging
- Require status checks: `build`
- Require linear history
- Do not allow force pushes
- Delete head branches after merge
