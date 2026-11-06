# leanix-arb-sync-service

Part of the LeanIX sync services (platform: [leanix-services](https://github.com/DHobson-NS/leanix-services)). Creates an **Architecture Review** (ARB) fact sheet in LeanIX for every new Initiative.

On its schedule it reads the Initiative/Project fact sheets from LeanIX. The schedule (the `ARB_SYNC` schedule: update frequency, value, an optional start time and whether it runs) and the review teams are configuration kept by [leanix-management-service](https://github.com/DHobson-NS/leanix-management-service) and edited on the pages of the management service. This service asks the management service every minute whether it is due and tells it when a run starts; if the management service cannot be reached it does not run. For each Initiative created after the previous run that has no ARB record yet, it:

1. creates an `ArchitectureReview` fact sheet named after the initiative (cut to 36 characters),
2. links it to the initiative,
3. stores an audit record in the `architecture_review` table (including the Initiative name, which older rows get from LeanIX at startup), and
4. sets the ARB phase to `Open` and applies the matching tag (`leanix.tag.arb-phase-open`),
5. subscribes the Initiative's IT manager to the review: the user with the Initiative's `itManagerEmail`, type ACCOUNTABLE, with the subscription role named by `leanix.role.manager-name` (default `Manager`). An Initiative without a manager email gets no subscription, and a failure is recorded on the review (column `manager_status`) without undoing it.

A new database starts from the moment it is created (migration `V2`), so initiatives that already exist are not processed. A failure on one initiative is logged and the run goes on. Nothing is deleted.

## Configuration

| Variable | Meaning |
|---|---|
| `LEANIX_HOST` | LeanIX host, for example `norfolksouthern.leanix.net` (required) |
| `LEANIX_API_TOKEN` | API token of the technical user (required, a secret) |
| `LEANIX_API_USER` | name of the workflow user (default `arbWorkflowUser`) |
| `LEANIX_TAG_ARB_PHASE_OPEN` | id of the "Open" ARB phase tag in the workspace |
| `QUARKUS_DATASOURCE_*` | PostgreSQL connection to its own database `leanix_arb` (schema `arb`; the URL ends in `?currentSchema=arb`) |
| `API_TOKEN` | bearer token callers must send to this service's API (`GET /api/status`); required, no default |
| `MANAGEMENT_SERVICE_URL`, `MANAGEMENT_API_TOKEN` | address and token of the management service; required token, no default |

The host and token have no defaults on purpose: the service fails at startup without them. In `.env` arb-sync uses the same `LEANIX_HOST` and `LEANIX_API_TOKEN` as cmdb-sync (the Sandbox workspace). `ARB_LEANIX_API_TOKEN` and `ARB_LEANIX_HOST`, if set, override them for arb-sync.

## Build

Gradle 9.1.0, Quarkus 3.39, Java 25, with public repositories:

```bash
./gradlew build
```

The `Dockerfile` in this folder builds the same jar and runs it.

## Run

Deploy the platform first (`./openshift/deploy.sh` in the leanix-services repository), then `./openshift/deploy.sh` here for OpenShift Local (secrets in this repository's `.env`, see `.env.example`); `docker compose up -d --build` in leanix-services runs everything locally. The manifests are in `openshift/` of this folder: the service runs in the `leanix-services` namespace as `leanix-arb-sync-service` with its own database `leanix_arb` on the `postgres` server, and has no route. Its API (`GET /api/status`: last run, review count and the latest reviews, for the pages of the management service) needs the bearer token. Keep `replicas: 1`: two copies would create every review twice.