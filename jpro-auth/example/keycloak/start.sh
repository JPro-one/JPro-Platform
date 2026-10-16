#!/bin/sh
# Starts Keycloak on http://localhost:8180 with the test realm "myrealm" (client jpro-example, user test/test),
# for running the oauth example by hand:
#   jpro-auth/example/keycloak/start.sh
#   . jpro-auth/example/keycloak/env.sh && ./gradlew :jpro-auth:example:jproRun -Psample=oauth
# Admin console: http://localhost:8180/admin (admin/admin). Stop with stop.sh.
set -e
cd "$(dirname "$0")"
IMPORT_DIR="$(mktemp -d)"
sed 's/${APP_PORT}/8080/g' ../src/test/resources/keycloak/myrealm.json > "$IMPORT_DIR/myrealm.json"
docker rm -f jpro-keycloak >/dev/null 2>&1 || true
docker run -d --name jpro-keycloak -p 8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v "$IMPORT_DIR:/opt/keycloak/data/import:ro" \
  quay.io/keycloak/keycloak:26.4.0 start-dev --import-realm
echo "Waiting for Keycloak ..."
for i in $(seq 1 90); do
  if curl -sf http://localhost:8180/realms/myrealm/.well-known/openid-configuration >/dev/null; then
    echo "Keycloak is ready: http://localhost:8180/realms/myrealm"
    exit 0
  fi
  sleep 1
done
echo "Keycloak did not become ready; see: docker logs jpro-keycloak" >&2
exit 1
