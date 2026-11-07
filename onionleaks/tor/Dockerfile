# Tor from the Debian repository — no third-party images.
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends tor && rm -rf /var/lib/apt/lists/*
COPY torrc /etc/tor/torrc
RUN mkdir -p /var/lib/tor/onionleaks && chown -R debian-tor:debian-tor /var/lib/tor && chmod 700 /var/lib/tor/onionleaks
USER debian-tor
ENTRYPOINT ["tor", "-f", "/etc/tor/torrc"]
