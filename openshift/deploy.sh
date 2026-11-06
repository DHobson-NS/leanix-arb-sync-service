#!/usr/bin/env bash
#
# Deploy leanix-arb-sync-service to OpenShift Local (CRC).
#
# Prerequisites:
#   1. CRC is installed (crc setup done) - this script starts it if it is stopped.
#   2. The platform (namespace and PostgreSQL server) is deployed: ./openshift/deploy.sh in the leanix-services repository.
#   3. ./.env holds the secrets (see .env.example); they are turned into OpenShift secrets, never written to files in git.
#
# Usage:
#   ./openshift/deploy.sh             # database, secret, build, deployment
#   ./openshift/deploy.sh build       # rebuild the image only
#   ./openshift/deploy.sh redeploy    # restart the pod with the latest image
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NAMESPACE="leanix-services"
NAME="leanix-arb-sync-service"
ACTION="${1:-full}"

# Secrets come from the environment (or ./.env), never from files in git
if [ -f "$ROOT_DIR/.env" ]; then set -a; . "$ROOT_DIR/.env"; set +a; fi

# ── OpenShift Local ──────────────────────────────────────────────────────────
if ! command -v oc >/dev/null 2>&1; then
    [ -x "$HOME/.crc/bin/oc/oc" ] && export PATH="$HOME/.crc/bin/oc:$PATH"
fi
command -v oc >/dev/null 2>&1 || { echo "ERROR: 'oc' not found. Run:  eval \$(crc oc-env)"; exit 1; }

if ! oc whoami >/dev/null 2>&1; then
    command -v crc >/dev/null 2>&1 || { echo "ERROR: not logged in to a cluster and 'crc' is not installed"; exit 1; }
    if ! crc status 2>/dev/null | grep -q "OpenShift:.*Running"; then
        echo "==> Starting OpenShift Local (this takes a few minutes)..."
        crc start
    fi
    echo "==> Logging in as kubeadmin..."
    oc login -u kubeadmin -p "$(crc console --credentials | grep kubeadmin | awk -F"'" '{print $2}')" \
        https://api.crc.testing:6443 --insecure-skip-tls-verify=true >/dev/null
fi
oc get namespace "$NAMESPACE" >/dev/null 2>&1 || { echo "ERROR: namespace $NAMESPACE not found - deploy the platform first (leanix-services repository)"; exit 1; }

# ── Helpers ──────────────────────────────────────────────────────────────────
build_image() {
    echo "==> Building $NAME from a clean copy of this folder (no .env, no build output)..."
    local stage; stage="$(mktemp -d)"
    rsync -a --exclude .env --exclude build --exclude target --exclude .gradle --exclude .git --exclude .idea "$ROOT_DIR/" "$stage/"
    oc start-build "$NAME" --from-dir="$stage" --follow -n "$NAMESPACE"
    rm -rf "$stage"
    echo "==> Build of $NAME complete."
}

redeploy() {
    echo "==> Rolling out the latest image of $NAME..."
    oc rollout restart "deployment/$NAME" -n "$NAMESPACE"
    oc rollout status  "deployment/$NAME" -n "$NAMESPACE" --timeout=180s
}

# One database and one user per service, created if they do not exist; only that user can use its database.
create_database() {   # <database> <user> <password>
    local db="$1" user="$2" password="$3"
    oc exec deployment/postgres -n "$NAMESPACE" -- psql -U postgres -v ON_ERROR_STOP=1 -q \
        -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$user') THEN CREATE ROLE $user LOGIN; END IF; END \$\$" \
        -c "ALTER ROLE $user PASSWORD '$password'"
    oc exec deployment/postgres -n "$NAMESPACE" -- sh -c "psql -U postgres -Atc \"SELECT 1 FROM pg_database WHERE datname = '$db'\" | grep -q 1 || psql -U postgres -c 'CREATE DATABASE $db OWNER $user'"
    oc exec deployment/postgres -n "$NAMESPACE" -- psql -U postgres -q -c "REVOKE ALL ON DATABASE $db FROM PUBLIC"
}

deploy() {
    local arb_token="${ARB_LEANIX_API_TOKEN:-${LEANIX_API_TOKEN:?Set LEANIX_API_TOKEN}}"
    : "${ARB_API_TOKEN:?Set ARB_API_TOKEN}" "${MANAGEMENT_API_TOKEN:?Set MANAGEMENT_API_TOKEN}" "${ARB_DB_PASSWORD:?Set ARB_DB_PASSWORD}"
    local dir="$ROOT_DIR/openshift"
    echo "==> Database, secrets and config..."
    create_database leanix_arb arb_user "$ARB_DB_PASSWORD"
    oc create secret generic arb-credentials -n "$NAMESPACE" \
        --from-literal=LEANIX_HOST="${ARB_LEANIX_HOST:-${LEANIX_HOST:?Set LEANIX_HOST}}" \
        --from-literal=LEANIX_API_TOKEN="$arb_token" --from-literal=API_TOKEN="$ARB_API_TOKEN" \
        --from-literal=MANAGEMENT_API_TOKEN="$MANAGEMENT_API_TOKEN" --from-literal=QUARKUS_DATASOURCE_PASSWORD="$ARB_DB_PASSWORD" \
        --dry-run=client -o yaml | oc apply -f -
    oc apply -f "$dir/app-configmap.yaml" -f "$dir/app-buildconfig.yaml"
    build_image
    echo "==> Application..."
    oc apply -f "$dir/app-deployment.yaml"
    oc rollout status "deployment/$NAME" -n "$NAMESPACE" --timeout=240s
}

case "$ACTION" in
    build)    build_image ;;
    redeploy) redeploy ;;
    full)     deploy; echo ""; echo "==> Deployment complete.  Logs: oc logs -f deployment/$NAME -n $NAMESPACE" ;;
    *) echo "Usage: $0 [full|build|redeploy]"; exit 1 ;;
esac
