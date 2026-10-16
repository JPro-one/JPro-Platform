# Source before starting the oauth example against start.sh's Keycloak: . jpro-auth/example/keycloak/env.sh
export KEYCLOAK_TEST_SITE='http://localhost:8180/realms/{realm}'
export KEYCLOAK_TEST_CLIENT_ID=jpro-example
export KEYCLOAK_TEST_CLIENT_SECRET=jpro-example-secret
# The example also creates the Google and Microsoft providers, which need some client to start.
export GOOGLE_TEST_CLIENT_ID="${GOOGLE_TEST_CLIENT_ID:-unused}"
export GOOGLE_TEST_CLIENT_SECRET="${GOOGLE_TEST_CLIENT_SECRET:-unused}"
export AZURE_TEST_CLIENT_ID="${AZURE_TEST_CLIENT_ID:-unused}"
export AZURE_TEST_CLIENT_SECRET="${AZURE_TEST_CLIENT_SECRET:-unused}"
