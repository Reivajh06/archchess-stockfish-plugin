# Archchess Stockfish plugin

A simple [Archchess](https://github.com/ReivajH06) plugin that lets the CPU play with [Stockfish](https://stockfishchess.org/).

## Install

1. Download `stockfish.jar` from the [latest release](../../releases/latest) (requires Java 25).
2. Drop the jar in Archchess' plugins folder. That's it: the jar is self-contained.

Stockfish (official `sf_19` Windows x86-64 build) is bundled inside the jar and extracted once to
`<tmp>/archchess-stockfish/` on first start. On other operating systems set `STOCKFISH_PATH` to your own
Stockfish executable (or `-Dstockfish.path=...`); this also overrides the bundled engine on Windows.

## Build

```bash
bash scripts/fetch-stockfish.sh   # downloads Stockfish into src/main/resources/engines (not committed: ~100 MB)
mvn package                       # produces target/stockfish.jar (~80 MB, runnable, library included)
```

The Archchess plugins library (`archchess-plugins 1.0`) is vendored in `lib/`, so no extra setup is needed.

## Release

Push a tag and GitHub Actions builds the jar and attaches it to a GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Protocol

The plugin speaks Archchess' line protocol over stdin/stdout (replies are prefixed with `$archchess:`):

| Input | Reply |
| --- | --- |
| `name` | `$archchess:Stockfish` |
| `move <FEN>` | `$archchess:{"from":"e2","to":"e4"}` (+ `"promote_to":"queen"` when promoting) |
| `config <EASY\|MEDIUM\|HARD\|MAX> [moveTimeMs]` | `$archchess:{"ok":"true"}` |
| `exit` | stops Stockfish and exits |

Errors are returned as `{"error":"..."}`. Difficulty maps to Stockfish's *Skill Level* (1 / 8 / 15 / 20);
the default is full strength with 1000 ms per move.


## License

This plugin bundles [Stockfish](https://github.com/official-stockfish/Stockfish) (GPLv3, license text in the jar as `engines/STOCKFISH-COPYING.txt`). Its source code is available at that URL. Because the jar redistributes Stockfish, keep this notice and license your own plugin code under a GPL-compatible license.
