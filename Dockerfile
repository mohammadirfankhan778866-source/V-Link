# Multi-stage Dockerfile for Pulse Chat Erlang Backend
FROM erlang:26-alpine as builder

WORKDIR /app
COPY backend/erlang /app

RUN rebar3 compile

FROM erlang:26-alpine
WORKDIR /app
COPY --from=builder /app/_build/default/lib /app/lib

EXPOSE 8080
CMD ["erl", "-pa", "lib/*/ebin", "-s", "pulse_app", "start"]
