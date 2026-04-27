#!/usr/bin/env bash
# ============================================================
#  FinanzLeben – Automatisches Setup-Skript für Ubuntu
#  Unterstützt: Ubuntu 20.04 / 22.04 / 24.04
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Farben & Ausgabefunktionen ───────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${CYAN}  ℹ  $*${NC}"; }
success() { echo -e "${GREEN}  ✓  $*${NC}"; }
warn()    { echo -e "${YELLOW}  ⚠  $*${NC}"; }
err()     { echo -e "${RED}  ✗  $*${NC}" >&2; }
header()  { echo -e "\n${BOLD}${BLUE}┌─ $* ─────────────────────────────────────${NC}\n"; }
line()    { echo -e "${BLUE}──────────────────────────────────────────────────────${NC}"; }

prompt() {
    # prompt VAR "Frage" "Standard"
    local _var=$1 _q=$2 _def=${3:-}
    local _val
    if [[ -n "$_def" ]]; then
        read -rp "$(echo -e "  ${BOLD}${YELLOW}?  ${_q} [${_def}]: ${NC}")" _val
        _val="${_val:-$_def}"
    else
        while true; do
            read -rp "$(echo -e "  ${BOLD}${YELLOW}?  ${_q}: ${NC}")" _val
            [[ -n "$_val" ]] && break
            warn "Eingabe darf nicht leer sein."
        done
    fi
    printf -v "$_var" '%s' "$_val"
}

prompt_password() {
    local _var=$1 _q=$2 _val
    read -rsp "$(echo -e "  ${BOLD}${YELLOW}?  ${_q}: ${NC}")" _val
    echo
    printf -v "$_var" '%s' "$_val"
}

prompt_yn() {
    local _q=$1 _ans
    read -rp "$(echo -e "  ${BOLD}${YELLOW}?  ${_q} [j/N]: ${NC}")" _ans
    [[ "${_ans,,}" == "j" || "${_ans,,}" == "ja" || "${_ans,,}" == "y" || "${_ans,,}" == "yes" ]]
}

generate_secret() {
    openssl rand -base64 64 | tr -dc 'A-Za-z0-9' | head -c 64
}

is_port_free() {
    ! ss -tlnp 2>/dev/null | grep -qE ":${1}\s"
}

find_free_port() {
    local p=$1
    while ! is_port_free "$p"; do
        p=$((p + 1))
    done
    echo "$p"
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        err "Dieses Skript muss mit root-Rechten ausgeführt werden."
        echo -e "  Bitte neu starten mit:  ${BOLD}sudo bash $0${NC}"
        exit 1
    fi
}

# ── Docker installieren ──────────────────────────────────────
install_docker() {
    if command -v docker &>/dev/null && docker compose version &>/dev/null 2>&1; then
        success "Docker $(docker --version | grep -oP '\d+\.\d+\.\d+' | head -1) & Compose bereits vorhanden"
        return
    fi

    info "Installiere Docker Engine + Compose-Plugin..."
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg lsb-release

    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
        > /etc/apt/sources.list.d/docker.list

    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        docker-ce docker-ce-cli containerd.io docker-compose-plugin

    systemctl enable --now docker
    success "Docker installiert"
}

# ── cloudflared installieren ─────────────────────────────────
install_cloudflared() {
    if command -v cloudflared &>/dev/null; then
        success "cloudflared bereits vorhanden"
        return
    fi

    info "Installiere cloudflared..."
    local arch
    arch=$(dpkg --print-architecture)

    curl -fsSL \
        "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${arch}.deb" \
        -o /tmp/cloudflared.deb

    dpkg -i /tmp/cloudflared.deb
    rm -f /tmp/cloudflared.deb
    success "cloudflared installiert"
}

# ════════════════════════════════════════════════════════════
#  HAUPTPROGRAMM
# ════════════════════════════════════════════════════════════
main() {
    clear
    echo -e "${BOLD}${BLUE}"
    echo "  ╔══════════════════════════════════════════════════╗"
    echo "  ║         FinanzLeben – Automatisches Setup        ║"
    echo "  ║         Ubuntu 20.04 / 22.04 / 24.04            ║"
    echo "  ╚══════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo "  Dieses Skript installiert und konfiguriert alles automatisch."
    echo "  Bestehende .env-Datei wird gesichert falls vorhanden."
    line

    check_root

    # ── 1. Systempakete & Docker ─────────────────────────────
    header "1 / 6  Systempakete & Docker"
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        curl openssl net-tools iproute2 git

    install_docker

    # ── 2. Ports ─────────────────────────────────────────────
    header "2 / 6  Port-Konfiguration"

    info "Scanne belegte Ports..."
    SUG_BACKEND=$(find_free_port 8080)
    SUG_FRONTEND=$(find_free_port 3000)

    [[ "$SUG_BACKEND"  != "8080" ]] && warn "Port 8080 belegt → schlage $SUG_BACKEND vor"
    [[ "$SUG_FRONTEND" != "3000" ]] && warn "Port 3000 belegt → schlage $SUG_FRONTEND vor"

    prompt BACKEND_PORT  "Host-Port für das Backend"  "$SUG_BACKEND"
    prompt FRONTEND_PORT "Host-Port für das Frontend" "$SUG_FRONTEND"

    for PORT in "$BACKEND_PORT" "$FRONTEND_PORT"; do
        if ! [[ "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1024 || PORT > 65535 )); then
            err "Ungültiger Port: $PORT (erlaubt: 1024–65535)"
            exit 1
        fi
    done

    if [[ "$BACKEND_PORT" == "$FRONTEND_PORT" ]]; then
        err "Backend- und Frontend-Port dürfen nicht gleich sein."
        exit 1
    fi

    for PORT in "$BACKEND_PORT" "$FRONTEND_PORT"; do
        if ! is_port_free "$PORT"; then
            err "Port $PORT ist bereits belegt. Bitte einen anderen Port wählen."
            exit 1
        fi
    done

    success "Ports OK: Backend=$BACKEND_PORT  Frontend=$FRONTEND_PORT"

    # ── 3. Datenbank ─────────────────────────────────────────
    header "3 / 6  Datenbank-Konfiguration"

    prompt DB_NAME     "Datenbankname"               "financegame"
    prompt DB_USER     "Datenbankbenutzer"            "financegame"
    prompt DB_DATA_PATH "Speicherort der DB-Daten auf diesem Server" \
                        "${SCRIPT_DIR}/data/postgres"

    # Absoluten Pfad sicherstellen
    DB_DATA_PATH="$(realpath -m "$DB_DATA_PATH")"

    # Verzeichnis anlegen und Berechtigungen setzen (Postgres läuft als UID 999)
    mkdir -p "$DB_DATA_PATH"
    chown -R 999:999 "$DB_DATA_PATH" 2>/dev/null || true

    DB_PASSWORD="$(generate_secret | head -c 32)"
    JWT_SECRET="$(generate_secret)"

    success "DB-Pfad: $DB_DATA_PATH"
    success "Passwort & JWT-Secret automatisch generiert"

    # ── 4. Cloudflare Tunnel ──────────────────────────────────
    header "4 / 6  Cloudflare Tunnel"

    USE_CLOUDFLARE=false
    CF_TUNNEL_TOKEN=""
    FRONTEND_PUBLIC_URL="http://localhost:${FRONTEND_PORT}"
    BACKEND_PUBLIC_URL="http://localhost:${BACKEND_PORT}"

    echo "  Mit einem Cloudflare Tunnel ist die App sicher über das Internet"
    echo "  erreichbar – ohne Router-Portfreigaben oder feste IP."
    echo

    if prompt_yn "Cloudflare Tunnel einrichten?"; then
        USE_CLOUDFLARE=true
        install_cloudflared

        echo
        echo -e "  ${BOLD}${CYAN}So holst du deinen Tunnel-Token:${NC}"
        echo "  ┌─────────────────────────────────────────────────────────────┐"
        echo "  │ 1. Öffne: https://one.dash.cloudflare.com                   │"
        echo "  │ 2. Navigiere zu: Networks → Tunnels                         │"
        echo "  │ 3. Klicke: 'Create a tunnel' → Typ: Cloudflared             │"
        echo "  │ 4. Vergib einen Namen (z.B. 'financegame')                  │"
        echo "  │ 5. Kopiere den Token und füge ihn unten ein                 │"
        echo "  │                                                              │"
        echo "  │  Routen im Cloudflare-Dashboard konfigurieren:              │"
        echo "  │                                                              │"
        echo "  │  Frontend-Route:                                             │"
        echo "  │    Public hostname: spiel.deine-domain.com                  │"
        echo "  │    Service:         http://localhost:${FRONTEND_PORT}$(printf '%*s' $((25 - ${#FRONTEND_PORT})) '')│"
        echo "  │                                                              │"
        echo "  │  Backend-Route:                                              │"
        echo "  │    Public hostname: api.deine-domain.com                    │"
        echo "  │    Service:         http://localhost:${BACKEND_PORT}$(printf '%*s' $((25 - ${#BACKEND_PORT})) '')│"
        echo "  └─────────────────────────────────────────────────────────────┘"
        echo

        prompt_password CF_TUNNEL_TOKEN "Cloudflare Tunnel Token einfügen"

        if [[ -z "$CF_TUNNEL_TOKEN" ]]; then
            warn "Kein Token eingegeben – Cloudflare Tunnel wird übersprungen."
            USE_CLOUDFLARE=false
        else
            echo
            echo "  Jetzt die öffentlichen URLs eingeben (wie im Cloudflare Dashboard konfiguriert)."
            echo "  Beispiel Frontend: https://spiel.example.com"
            echo "  Beispiel Backend:  https://api.example.com"
            echo

            prompt FRONTEND_PUBLIC_URL "Öffentliche Frontend-URL (https://...)" ""
            prompt BACKEND_PUBLIC_URL  "Öffentliche Backend-URL  (https://...)" ""

            FRONTEND_PUBLIC_URL="${FRONTEND_PUBLIC_URL%/}"
            BACKEND_PUBLIC_URL="${BACKEND_PUBLIC_URL%/}"

            if [[ ! "$FRONTEND_PUBLIC_URL" =~ ^https?:// ]]; then
                err "Frontend-URL muss mit http:// oder https:// beginnen."
                exit 1
            fi
            if [[ ! "$BACKEND_PUBLIC_URL" =~ ^https?:// ]]; then
                err "Backend-URL muss mit http:// oder https:// beginnen."
                exit 1
            fi
        fi
    fi

    # CORS: Bei Cloudflare öffentliche Frontend-URL + localhost kombinieren,
    # damit sowohl der Tunnel als auch lokale Entwicklung funktionieren.
    if [[ "$USE_CLOUDFLARE" == "true" ]]; then
        CORS_ALLOWED_ORIGINS="${FRONTEND_PUBLIC_URL},http://localhost:${FRONTEND_PORT}"
    else
        CORS_ALLOWED_ORIGINS="http://localhost:${FRONTEND_PORT}"
    fi

    # ── 5. Zusammenfassung ────────────────────────────────────
    header "5 / 6  Konfigurationsübersicht"

    echo -e "  Host-Port Backend:     ${BOLD}${BACKEND_PORT}${NC}"
    echo -e "  Host-Port Frontend:    ${BOLD}${FRONTEND_PORT}${NC}"
    echo -e "  Datenbankname:         ${BOLD}${DB_NAME}${NC}"
    echo -e "  Datenbankbenutzer:     ${BOLD}${DB_USER}${NC}"
    echo -e "  DB-Datenpfad:          ${BOLD}${DB_DATA_PATH}${NC}"
    echo -e "  DB-Passwort:           ${BOLD}(automatisch generiert)${NC}"
    echo -e "  JWT-Secret:            ${BOLD}(automatisch generiert, 64 Zeichen)${NC}"
    echo -e "  Frontend-URL (public): ${BOLD}${FRONTEND_PUBLIC_URL}${NC}"
    echo -e "  Backend-URL  (public): ${BOLD}${BACKEND_PUBLIC_URL}${NC}"
    echo -e "  CORS erlaubte Origins: ${BOLD}${CORS_ALLOWED_ORIGINS}${NC}"
    echo -e "  Cloudflare Tunnel:     ${BOLD}$([ "$USE_CLOUDFLARE" == "true" ] && echo "Ja ✓" || echo "Nein")${NC}"
    echo

    if ! prompt_yn "Sieht alles korrekt aus? Jetzt installieren?"; then
        warn "Abgebrochen. Skript einfach neu starten."
        exit 0
    fi

    # ── 6. Installation ───────────────────────────────────────
    header "6 / 6  Installation"

    # .env sichern falls vorhanden
    if [[ -f .env ]]; then
        BACKUP=".env.backup.$(date +%Y%m%d_%H%M%S)"
        cp .env "$BACKUP"
        warn "Vorhandene .env gesichert als: $BACKUP"
    fi

    # .env schreiben – alle Werte explizit, keine Fallbacks auf docker-compose angewiesen
    cat > .env <<EOF
# Automatisch generiert von install.sh – $(date)
# ACHTUNG: Diese Datei enthält sensible Daten – niemals in Git committen!

# Datenbank
DB_NAME=${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
DB_DATA_PATH=${DB_DATA_PATH}

# JWT
JWT_SECRET=${JWT_SECRET}

# Ports (Host-seitig)
BACKEND_PORT=${BACKEND_PORT}
FRONTEND_PORT=${FRONTEND_PORT}

# CORS: kommagetrennte Liste erlaubter Frontend-Ursprünge
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}

# API-URLs
NUXT_PUBLIC_API_BASE=${BACKEND_PUBLIC_URL}
NUXT_INTERNAL_API_BASE=http://backend:8080
EOF

    # .env in .gitignore sicherstellen
    if ! grep -qxF '.env' .gitignore 2>/dev/null; then
        echo '.env' >> .gitignore
        info ".env zu .gitignore hinzugefügt"
    fi

    success ".env erstellt"

    # Laufende Container stoppen
    if docker compose ps --quiet 2>/dev/null | grep -q .; then
        info "Stoppe laufende Container..."
        docker compose down
    fi

    # Images bauen
    info "Baue Docker-Images (kann 3–10 Minuten dauern)..."
    docker compose build --no-cache

    # Starten
    info "Starte alle Dienste..."
    docker compose up -d

    success "Container gestartet"

    # ── Health-Check Backend ──────────────────────────────────
    echo
    info "Warte auf Backend-Start (max. 90 Sekunden)..."
    WAITED=0
    BACKEND_OK=false
    while [[ $WAITED -lt 90 ]]; do
        if curl -sf "http://localhost:${BACKEND_PORT}/api/health" &>/dev/null; then
            BACKEND_OK=true
            break
        fi
        printf "."
        sleep 3
        WAITED=$((WAITED + 3))
    done
    echo

    if [[ "$BACKEND_OK" == "true" ]]; then
        success "Backend antwortet auf http://localhost:${BACKEND_PORT}/api/health"
    else
        warn "Backend antwortet noch nicht. Beim ersten Start normal (Maven-Build)."
        warn "Logs prüfen mit:  docker compose logs -f backend"
    fi

    # ── Cloudflare Tunnel als Systemd-Service ─────────────────
    if [[ "$USE_CLOUDFLARE" == "true" && -n "$CF_TUNNEL_TOKEN" ]]; then
        echo
        info "Richte Cloudflare Tunnel als Systemd-Service ein..."

        if systemctl is-active --quiet cloudflared 2>/dev/null; then
            systemctl stop cloudflared
            cloudflared service uninstall 2>/dev/null || true
        fi

        cloudflared service install "$CF_TUNNEL_TOKEN"
        systemctl enable --now cloudflared

        sleep 3
        if systemctl is-active --quiet cloudflared; then
            success "Cloudflare Tunnel aktiv"
        else
            warn "Status unklar – prüfen mit: systemctl status cloudflared"
        fi
    fi

    # ── Abschlussbericht ──────────────────────────────────────
    echo
    line
    echo -e "${BOLD}${GREEN}"
    echo "  ✓  Installation erfolgreich abgeschlossen!"
    echo -e "${NC}"
    line
    echo
    echo -e "  ${BOLD}Deine App ist erreichbar unter:${NC}"
    echo
    echo -e "    🌐 Frontend:    ${BOLD}${CYAN}${FRONTEND_PUBLIC_URL}${NC}"
    echo -e "    🔧 Backend:     ${BOLD}${CYAN}${BACKEND_PUBLIC_URL}${NC}"
    echo
    if [[ "$USE_CLOUDFLARE" == "true" ]]; then
        echo -e "  ${BOLD}Cloudflare Tunnel:${NC}  aktiv  (systemctl status cloudflared)"
        echo
    fi
    echo -e "  ${BOLD}Datenbank-Daten:${NC}"
    echo "    $DB_DATA_PATH"
    echo
    line
    echo
    echo -e "  ${BOLD}Nützliche Befehle:${NC}"
    echo "    docker compose logs -f              # Alle Logs"
    echo "    docker compose logs -f backend      # Nur Backend"
    echo "    docker compose logs -f frontend     # Nur Frontend"
    echo "    docker compose restart backend      # Backend neu starten"
    echo "    docker compose down                 # Alle Container stoppen"
    echo "    docker compose up -d                # Alle Container starten"
    echo "    docker compose ps                   # Status aller Container"
    echo
    echo -e "  ${BOLD}Update (neue Codeversion):${NC}"
    echo "    git pull && sudo bash install.sh"
    echo
    line
    echo
    echo -e "  ${YELLOW}⚠  Sicherheitshinweis:${NC}"
    echo "    Die Datei .env enthält Passwörter und Secrets."
    echo "    Sie ist bereits in .gitignore – niemals manuell committen!"
    echo
}

main "$@"
