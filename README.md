# $`clj\LaTeX`$

### Example
See [example](./example.tex).

> [!WARNING]
> Do not use multiple clojure blocks on one source file line.

### Usage

> [!IMPORTANT]
> [babashka](https://github.com/babashka/babashka) is needed.

From **terminal**:
```bash
./cljlatex.clj  filename.tex
```
or
```bash
bb cljlatex.clj  filename.tex
```

From **TeXstudio**:
Options > Configure TeXstudio > Build > Add
```bash
bb cljlatex.clj %.tex | <build command> clj-%.tex
```
For example
```bash
bb cljlatex.clj %.tex | pdflatex -synctex=1 -interaction=nonstopmode clj-%.tex
```
