FROM mcr.microsoft.com/devcontainers/base:3.0.12-noble

COPY versions.env install-tools.sh /tmp/dvp-tools/
RUN bash /tmp/dvp-tools/install-tools.sh && rm -rf /tmp/dvp-tools
