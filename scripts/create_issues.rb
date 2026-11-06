#!/usr/bin/env ruby
# Reads docs/backlog/*.md, checks the backlog is consistent and, with --create,
# turns every entry into a GitHub issue (with milestone and labels).
#
#   ruby scripts/create_issues.rb              dry run: validate + summary
#   ruby scripts/create_issues.rb --preview P5-04   print one finished issue body
#   ruby scripts/create_issues.rb --create     create milestones, labels and issues

require "open3"

Encoding.default_external = Encoding::UTF_8

REPO = "satyambhanot/ChinookAir".freeze
BACKLOG = File.expand_path("../docs/backlog/*.md", __dir__)
REQUIRED_META = ["Issue Title", "Milestone/Phase", "Labels", "Estimate", "Depends on", "Kind", "Rubric check"].freeze
REQUIRED_SECTIONS = ["Context & Objective", "Implementation Guide", "Testing Strategy"].freeze
MAX_HOURS = 4.0

DOD = {
  "code" => [
    "Code linter run and passed. (`mvn verify` shows 0 Checkstyle violations)",
    "Unit tests written and passing. (`mvn test`, every case from the Testing Strategy is a test)",
    "Adheres to strict OOP and project coding standards. (layers as in CONTRIBUTING.md, names match the class diagram)",
  ],
  "doc" => [
    "Code linter run and passed. (nothing to lint in a doc or diagram: `mvn verify` still passes and file names follow `docs/diagrams/README.md`)",
    "Unit tests written and passing. (every check in the Testing Strategy is ticked off, including the grep and count commands)",
    "Adheres to strict OOP and project coding standards. (class, method and state names match the glossary and the class diagram exactly)",
  ],
}.freeze

Issue = Struct.new(:id, :meta, :sections, :file, keyword_init: true) do
  def title = meta["Issue Title"]
  def phase = meta["Milestone/Phase"]
  def hours = meta["Estimate"].to_f
  def kind = meta["Kind"]
  def labels = meta["Labels"].split(",").map(&:strip)
  def deps = meta["Depends on"] == "none" ? [] : meta["Depends on"].split(",").map(&:strip)
end

def parse
  issues = []
  Dir[BACKLOG].sort.each do |path|
    next if File.basename(path) == "README.md"
    text = File.read(path)
    text.split(/^## (?=P\d+-\d+\s*$)/).drop(1).each do |block|
      id, rest = block.split("\n", 2)
      head, *secs = rest.split(/^### /)
      meta = head.scan(/^\*\*(.+?):\*\* (.*)$/).to_h
      sections = secs.to_h { |s| n, body = s.split("\n", 2); [n.strip, body.to_s.strip] }
      issues << Issue.new(id: id.strip, meta: meta, sections: sections, file: File.basename(path))
    end
  end
  issues
end

def validate(issues)
  problems = []
  seen = {}
  issues.each_with_index do |i, idx|
    problems << "#{i.id}: duplicate id" if seen[i.id]
    seen[i.id] = idx
    REQUIRED_META.each { |k| problems << "#{i.id}: missing #{k}" if i.meta[k].to_s.empty? }
    REQUIRED_SECTIONS.each { |s| problems << "#{i.id}: missing section #{s}" if i.sections[s].to_s.empty? }
    problems << "#{i.id}: kind must be code or doc" unless DOD.key?(i.kind)
    problems << "#{i.id}: estimate #{i.hours}h is over #{MAX_HOURS}h" if i.hours > MAX_HOURS
    problems << "#{i.id}: estimate missing" if i.hours <= 0
    prose = i.sections.values.join.gsub(/`[^`]*`/, "")
    problems << "#{i.id}: contains TODO or TBD" if prose.match?(/\b(TODO|TBD)\b/)
  end
  issues.each_with_index do |i, idx|
    i.deps.each do |d|
      if !seen.key?(d) then problems << "#{i.id}: depends on unknown #{d}"
      elsif seen[d] >= idx then problems << "#{i.id}: depends on #{d}, which comes later in the sequence"
      end
    end
  end
  problems
end

def body_for(issue, numbers = {})
  deps = issue.deps.map { |d| numbers[d] ? "#{d} (##{numbers[d]})" : d }
  facts = [
    "- **Milestone/Phase:** #{issue.phase}",
    "- **Estimate:** #{issue.meta['Estimate']}",
    "- **Depends on:** #{deps.empty? ? 'none' : deps.join(', ')}",
  ].join("\n")
  checklist = (DOD.fetch(issue.kind) + ["Rubric requirement explicitly verified: #{issue.meta['Rubric check']}"])
              .map { |line| "- [ ] #{line}" }.join("\n")
  paths = issue.kind == "code" ? " Paths are relative to `src/main/java/ca/ucalgary/chinookair/` unless they start with `src/`." : ""
  [
    facts,
    "### Context & Objective\n#{issue.sections['Context & Objective']}",
    "### Implementation Guide\n#{issue.sections['Implementation Guide']}",
    "### Testing Strategy\n#{issue.sections['Testing Strategy']}",
    "### Definition of Done (Pre-Commit Checklist)\n#{checklist}",
    "From the [master plan](https://github.com/#{REPO}/blob/dev/docs/master-plan.md), issue #{issue.id}.#{paths}",
  ].join("\n\n")
end

def gh(*args, stdin: nil)
  out, err, st = Open3.capture3("gh", *args, stdin_data: stdin.to_s)
  abort "gh #{args.first(3).join(' ')} failed: #{err}" unless st.success?
  out.strip
end

def summary(issues)
  puts format("%-45s %6s %6s", "Phase", "Issues", "Hours")
  issues.group_by(&:phase).each do |phase, list|
    puts format("%-45s %6d %6.1f", phase, list.size, list.sum(&:hours))
  end
  puts format("%-45s %6d %6.1f", "TOTAL", issues.size, issues.sum(&:hours))
  design, impl = issues.partition { |i| i.phase.match?(/Phase [0-4]:/) }
  puts "Design side: #{design.size} issues, #{design.sum(&:hours)}h. Implementation side: #{impl.size} issues, #{impl.sum(&:hours)}h."
end

issues = parse
problems = validate(issues)

if ARGV.first == "--preview"
  found = issues.find { |i| i.id == ARGV[1] } or abort "no such issue #{ARGV[1]}"
  puts "TITLE: #{found.id}: #{found.title}", "MILESTONE: #{found.phase}", "LABELS: #{found.labels.join(', ')}", "", body_for(found)
  exit
end

if ARGV.first == "--index"
  issues.group_by(&:phase).each do |phase, list|
    puts "", "#### #{phase}", "", "| ID | Issue | Hours | Depends on |", "|---|---|---|---|"
    list.each { |i| puts "| #{i.id} | #{i.title} | #{i.hours.to_s.sub(/\.0\z/, '')} | #{i.deps.empty? ? '-' : i.deps.join(', ')} |" }
  end
  exit
end

summary(issues)
if problems.any?
  puts "\n#{problems.size} problem(s):"
  problems.each { |p| puts "  - #{p}" }
  exit 1
end
puts "\nBacklog is consistent: all dependencies point backwards, every issue is #{MAX_HOURS}h or less."
exit unless ARGV.first == "--create"

abort "Refusing to create: repo has open issues already labelled from this backlog." if gh("issue", "list", "--repo", REPO, "--search", "in:title P0-01", "--json", "number", "-q", "length") != "0"

existing = gh("api", "repos/#{REPO}/milestones?state=all&per_page=100", "-q", ".[] | .title").split("\n")
issues.map(&:phase).uniq.each do |phase|
  next if existing.include?(phase)
  gh("api", "repos/#{REPO}/milestones", "-f", "title=#{phase}")
  puts "milestone: #{phase}"
end
have = gh("label", "list", "--repo", REPO, "--limit", "100", "--json", "name", "-q", ".[].name").split("\n")
(issues.flat_map(&:labels).uniq - have).each do |l|
  gh("label", "create", l, "--repo", REPO)
  puts "label: #{l}"
end

numbers = {}
issues.each do |i|
  url = gh("issue", "create", "--repo", REPO, "--title", "#{i.id}: #{i.title}", "--body-file", "-",
           "--milestone", i.phase, "--label", i.labels.join(","), stdin: body_for(i, numbers))
  numbers[i.id] = url[/\d+\z/]
  puts "#{i.id} -> #{url}"
end
puts "created #{numbers.size} issues"
