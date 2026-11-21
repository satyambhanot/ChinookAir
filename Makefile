# Builds the design report. Uses latexmk when it is installed, Tectonic otherwise.
REPORT := docs/report

ifneq ($(shell command -v latexmk 2>/dev/null),)
  BUILD = cd $(REPORT) && latexmk -pdf -interaction=nonstopmode -halt-on-error $(1)
else
  BUILD = cd $(REPORT) && tectonic $(1)
endif

.PHONY: pdf clean submission

pdf:
	$(call BUILD,main.tex)

clean:
	cd $(REPORT) && rm -f *.aux *.log *.toc *.out *.fls *.fdb_latexmk *.synctex.gz *.xdv main.pdf

# The implementation hand-in (section 7.2 of the guideline): one ZIP that holds the compiled jar and a
# ZIP of all the source files. The design PDF is a separate hand-in and is copied next to it.
DIST := dist
submission:
	mvn -B -q clean verify
	rm -rf $(DIST) && mkdir -p $(DIST)/staging
	zip -q -r $(DIST)/staging/ChinookAir-source.zip src pom.xml checkstyle.xml README.md
	cp target/ChinookAir.jar $(DIST)/staging/
	cd $(DIST)/staging && zip -q ../ChinookAir-submission.zip ChinookAir.jar ChinookAir-source.zip
	rm -rf $(DIST)/staging
	@if [ -f $(REPORT)/main.pdf ]; then cp $(REPORT)/main.pdf $(DIST)/ChinookAir-design.pdf; fi
	@echo "Wrote $(DIST)/ChinookAir-submission.zip"
