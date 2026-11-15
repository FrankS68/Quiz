#!/usr/bin/env bash
# Quiz ausrollen: Stand auschecken, bauen, Volume sichern, starten, pruefen,
# bei Fehlschlag auf das vorige Image zurueckschalten (analog knownet-vaadin).
#
#   ./deploy.sh              aktuellen Branch per git pull aktualisieren und ausrollen
#   ./deploy.sh <ref>        Branch oder Tag (z.B. v1.1.0) ausrollen
#   ./deploy.sh --rollback   vorige Version (Image vaadinquiz:prev) wieder starten
#
# Erwartet im Verzeichnis des Skripts:
#   .env      COMPOSE_PROJECT_NAME, COMPOSE_FILE (compose.pi.yml bzw. compose.prod.yml),
#             optional HEALTH_URL und GITHUB_TOKEN_FILE
#   config/application-prod.properties   OAuth-Zugangsdaten, social.crypto-key, erster Admin
#   GitHub-Token (read:packages) fuer den Build, Default ./.github_token
# Umgebungsvariablen: HEALTH_URL (Default aus .env, sonst http://localhost:8083),
# HEALTH_TIMEOUT (Sekunden, 240), BACKUP_DIR (~/backups/quiz), BACKUP_KEEP (Anzahl Sicherungen, 10).
#
# Lange Laeufe mit nohup starten, sonst beendet ein SSH-Abbruch das Skript bei gestopptem Stack:
#   nohup ./deploy.sh quality > deploy.log 2>&1 &
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

IMAGE=vaadinquiz
APP=quiz-app
SERVICE=quiz
VOLUME=quiz-data
env_value() { grep -oP "^$1=\K.*" .env 2>/dev/null || true; }
HEALTH_URL=${HEALTH_URL:-$(env_value HEALTH_URL)}
HEALTH_URL=${HEALTH_URL:-http://localhost:8083}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-240}
BACKUP_DIR=${BACKUP_DIR:-$HOME/backups/quiz}
BACKUP_KEEP=${BACKUP_KEEP:-10}
PROJECT=$(env_value COMPOSE_PROJECT_NAME)
PROJECT=${PROJECT:-$(basename "$PWD")}

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
die() { log "FEHLER: $*"; exit 1; }

# Nur ein Deploy gleichzeitig
exec 9>".deploy.lock"
flock -n 9 || die "Es laeuft bereits ein Deploy."

backup_volume() {
    local stamp=$1
    mkdir -p "$BACKUP_DIR"
    if ! docker volume inspect "${PROJECT}_${VOLUME}" >/dev/null 2>&1; then
        log "Noch kein Volume ${PROJECT}_${VOLUME} - nichts zu sichern"
        return
    fi
    log "Stoppe App fuer konsistente Sicherung"
    docker compose stop >/dev/null
    docker run --rm -v "${PROJECT}_${VOLUME}:/v:ro" -v "$BACKUP_DIR:/b" alpine \
        tar czf "/b/${VOLUME}-${stamp}.tgz" -C /v .
    log "Gesichert: $BACKUP_DIR/${VOLUME}-${stamp}.tgz"
    # alte Sicherungen aufraeumen
    ls -1t "$BACKUP_DIR/${VOLUME}-"*.tgz 2>/dev/null | tail -n +$((BACKUP_KEEP + 1)) | xargs -r rm -f
}

wait_healthy() {
    local since=$1 deadline=$((SECONDS + HEALTH_TIMEOUT))
    log "Warte auf Start (max. ${HEALTH_TIMEOUT}s)"
    while (( SECONDS < deadline )); do
        if docker logs --since "$since" "$APP" 2>&1 | grep -q "Application run failed"; then
            log "App meldet 'Application run failed'"
            return 1
        fi
        if docker logs --since "$since" "$APP" 2>&1 | grep -q "Started QuizApplication"; then
            # "/" leitet ohne Anmeldung auf /login um; die Login-Seite muss antworten
            local login
            login=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL/login" || true)
            if [[ $login == 200 ]]; then
                log "App laeuft: /login -> $login"
                return 0
            fi
        fi
        sleep 5
    done
    log "Zeitlimit erreicht"
    return 1
}

start_and_check() {
    local since
    since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    docker compose up -d >/dev/null
    wait_healthy "$since"
}

rollback() {
    docker image inspect "$IMAGE:prev" >/dev/null 2>&1 || die "Kein Image $IMAGE:prev vorhanden."
    log "Rollback: $IMAGE:prev -> $IMAGE:latest"
    docker tag "$IMAGE:prev" "$IMAGE:latest"
    if start_and_check; then
        log "Rollback erfolgreich. Daten: Falls die neue Version das Schema geaendert hat,"
        log "liegt die Sicherung von vor dem Deploy in $BACKUP_DIR."
    else
        die "Auch die vorige Version startet nicht. Logs: docker logs $APP"
    fi
}

if [[ ${1:-} == "--rollback" ]]; then
    rollback
    exit 0
fi

REF=${1:-}

# 1. Code
[[ -z $(git status --porcelain --untracked-files=no) ]] || die "Arbeitsverzeichnis hat Aenderungen (git status)."
git fetch -q --tags origin
if [[ -z $REF ]]; then
    git pull -q --ff-only
    REF=$(git rev-parse --abbrev-ref HEAD)
elif git show-ref -q --verify "refs/remotes/origin/$REF"; then
    git checkout -q "$REF"
    git pull -q --ff-only
else
    git checkout -q "$REF"   # Tag oder Commit (detached)
fi
SHA=$(git rev-parse --short HEAD)
log "Stand: $REF @ $SHA ($(git log -1 --format=%s))"

# 2. Bauen (die laufende App bleibt waehrenddessen online)
log "Baue Image"
if docker image inspect "$IMAGE:latest" >/dev/null 2>&1; then
    docker tag "$IMAGE:latest" "$IMAGE:prev"
fi
docker compose build -q "$SERVICE" || die "Build fehlgeschlagen; laufende Version unveraendert."
docker tag "$IMAGE:latest" "$IMAGE:$SHA"

# 3. Sichern
STAMP="$(date +%Y%m%d-%H%M)-$SHA"
backup_volume "$STAMP"

# 4. Starten und pruefen
if start_and_check; then
    log "Deploy erfolgreich: $REF @ $SHA"
    printf '%s\t%s\t%s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$REF" "$SHA" >> deploy-history.tsv
else
    log "Neue Version startet nicht – letzte Log-Zeilen:"
    docker logs --tail 15 "$APP" 2>&1 | sed 's/^/    /'
    rollback
    exit 1
fi
