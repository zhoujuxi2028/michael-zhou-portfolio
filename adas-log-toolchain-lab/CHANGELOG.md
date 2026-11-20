# Changelog

All notable changes to this project are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [0.1.0] - Unreleased

Ruleset version: 1.0.0

### Added

- `adas-lab gen`: seeded synthetic ACC / APA / HMI logs (JSON Lines) with truth labels
- `adas-lab parse`: streaming anomaly detection with data quality report and exit-code gates (`--fail-on`, `--strict`)
- Rules ACC-01, ACC-02, APA-01, APA-02, HMI-01, HMI-02
- `adas-lab cases boundary`: boundary value cases generated from rule thresholds
- GitHub Actions job and a GitLab CI example
