FROM grafana/k6:latest

COPY ./scripts /scripts

ENTRYPOINT ["sh", "-c", "k6 run /scripts/${K6_SCRIPT}"]