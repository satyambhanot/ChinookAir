# Builds the design report. Uses latexmk when it is installed, Tectonic otherwise.
REPORT := docs/report

ifneq ($(shell command -v latexmk 2>/dev/null),)
  BUILD = cd $(REPORT) && latexmk -pdf -interaction=nonstopmode -halt-on-error $(1)
else
  BUILD = cd $(REPORT) && tectonic $(1)
endif

.PHONY: pdf underline-test clean

pdf:
	$(call BUILD,main.tex)

# Renders one line with a single and a double underline, to prove the tooling works.
underline-test:
	$(call BUILD,underline-test.tex)

clean:
	cd $(REPORT) && rm -f *.aux *.log *.toc *.out *.fls *.fdb_latexmk *.synctex.gz *.xdv main.pdf underline-test.pdf
