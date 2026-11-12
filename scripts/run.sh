#!/usr/bin/env bash

# ---------- Colors ----------
if [ -t 1 ]; then
  RED=$'\e[31m'; GREEN=$'\e[32m'; YELLOW=$'\e[33m'; BLUE=$'\e[34m'
  MAGENTA=$'\e[35m'; CYAN=$'\e[36m'; BOLD=$'\e[1m'; DIM=$'\e[2m'; RESET=$'\e[0m'
else
  RED=""; GREEN=""; YELLOW=""; BLUE=""; MAGENTA=""; CYAN=""; BOLD=""; DIM=""; RESET=""
fi

info()  { echo "${CYAN}➜${RESET} $*"; }
ok()    { echo "${GREEN}✔${RESET} $*"; }
warn()  { echo "${YELLOW}!${RESET} $*"; }
err()   { echo "${RED}✘ $*${RESET}"; }

# ---------- Env ----------
if [ -f .env ]; then
  ENV_FILE=".env"
else
  err "Error: .env file not found in root!"
  exit 1
fi

info "Exporting environment variables from ${BOLD}$ENV_FILE${RESET}..."
set -a
source "$ENV_FILE"
set +a

info "Waiting for discovery-server (8761) and Kafka (9092) to be ready..."
until (echo > /dev/tcp/localhost/8761) >/dev/null 2>&1 && (echo > /dev/tcp/localhost/9092) >/dev/null 2>&1; do
  sleep 1
done
ok "Infrastructure is ready! Starting services..."

# ---------- Logs ----------
LOG_DIR=$(mktemp -d -t buy01-logs-XXXXXX)
ln -sfn "$LOG_DIR" /tmp/buy01-logs

echo "${DIM}Temporary logs folder: $LOG_DIR${RESET}"
echo "${DIM}---------------------------------------------------${RESET}"
echo "Watch any log in another terminal with:"
echo "  ${BLUE}make logs s=api-gateway${RESET}"
echo "  ${BLUE}make logs s=order-service${RESET}"
echo "${DIM}---------------------------------------------------${RESET}"

# ---------- Service management ----------
SERVICES=(
  "api-gateway"
  "user-service"
  "product-service"
  "order-service"
  "media-service"
  "frontend"
)

declare -A PID_OF

start_service() {
  local name=$1 dir cmd

  if [ "$name" = "frontend" ]; then
    dir="frontend"
    cmd="ng serve"
  else
    dir="backend/$name"
    if [ -f "$dir/mvnw" ]; then cmd="./mvnw spring-boot:run"; else cmd="mvn spring-boot:run"; fi
  fi

  echo "=== $(date '+%H:%M:%S') starting $name ===" >> "$LOG_DIR/$name.log"
  # setsid -> own process group, so we can kill the whole tree with kill -- -PID
  setsid bash -c "cd '$dir' && exec $cmd" >> "$LOG_DIR/$name.log" 2>&1 &
  PID_OF[$name]=$!
  ok "Started ${BOLD}$name${RESET} ${DIM}(PID: ${PID_OF[$name]})${RESET}"
}

stop_service() {
  local name=$1
  local pid=${PID_OF[$name]}
  [ -z "$pid" ] && return 0

  kill -TERM -- "-$pid" 2>/dev/null
  # wait up to 10s for graceful shutdown, then force kill
  for _ in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 0.5
  done
  kill -0 "$pid" 2>/dev/null && kill -KILL -- "-$pid" 2>/dev/null
  wait "$pid" 2>/dev/null
  unset "PID_OF[$name]"
}

restart_service() {
  local name=$1
  warn "Restarting ${BOLD}$name${RESET}..."
  stop_service "$name"
  start_service "$name"
}

is_running() {
  [ -n "${PID_OF[$1]}" ] && kill -0 "${PID_OF[$1]}" 2>/dev/null
}

# Accepts exact name or unique prefix (e.g. "order" -> order-service)
resolve_service() {
  local q=$1 match=() s
  [ -z "$q" ] && return 1
  for s in "${SERVICES[@]}"; do
    [ "$s" = "$q" ] && { echo "$s"; return 0; }
    [[ "$s" == "$q"* ]] && match+=("$s")
  done
  [ ${#match[@]} -eq 1 ] && { echo "${match[0]}"; return 0; }
  return 1
}

show_status() {
  local s
  for s in "${SERVICES[@]}"; do
    if is_running "$s"; then
      echo "  ${GREEN}●${RESET} $s ${DIM}(PID ${PID_OF[$s]})${RESET}"
    else
      echo "  ${RED}●${RESET} $s ${DIM}(stopped)${RESET}"
    fi
  done
}

show_help() {
  echo "${BOLD}Commands:${RESET}"
  echo "  ${MAGENTA}r <service>${RESET}       restart a service (prefix ok: ${DIM}r order${RESET})"
  echo "  ${MAGENTA}r all${RESET}             restart everything"
  echo "  ${MAGENTA}stop <service>${RESET}    stop a service"
  echo "  ${MAGENTA}start <service>${RESET}   start a stopped service"
  echo "  ${MAGENTA}logs <service> [n]${RESET} follow a service's logs (last n lines, default 40)"
  echo "  ${MAGENTA}logs all${RESET}          follow all services at once"
  echo "  ${MAGENTA}ls${RESET}                show status"
  echo "  ${MAGENTA}h${RESET}                 show this help"
  echo "  ${MAGENTA}q${RESET} / Ctrl+C        stop everything and exit"
}

# ---------- Log viewing ----------
# Highlight common log levels
colorize() {
  sed -u \
    -e "s/\(ERROR\|FATAL\|Exception\|FAILED\)/${RED}${BOLD}\1${RESET}/g" \
    -e "s/\(WARN\)/${YELLOW}\1${RESET}/g" \
    -e "s/\(INFO\)/${GREEN}\1${RESET}/g" \
    -e "s/\(DEBUG\|TRACE\)/${DIM}\1${RESET}/g" \
    -e "s/\(Started [A-Za-z]* in [0-9.]* seconds\)/${GREEN}${BOLD}\1${RESET}/g" \
    -e "s/^\(===.*===\)$/${MAGENTA}\1${RESET}/"
}

# logs <service> [lines]
tail_logs() {
  local name=$1 n=${2:-40}
  local file="$LOG_DIR/$name.log"
  [ -f "$file" ] || { err "No log file for $name"; return; }

  echo "${DIM}--- $name logs (Ctrl+C to go back to the prompt) ---${RESET}"
  trap ':' INT                       # Ctrl+C only stops tail, not the script
  tail -n "$n" -F "$file" 2>/dev/null | colorize
  trap cleanup INT                   # restore normal Ctrl+C behavior
  echo ""
  echo "${DIM}--- back to prompt ---${RESET}"
}

# logs all: every service, with a colored [name] prefix
tail_all_logs() {
  local colors=("$RED" "$GREEN" "$YELLOW" "$BLUE" "$MAGENTA" "$CYAN")
  local pids=() i=0 s c p

  echo "${DIM}--- all logs (Ctrl+C to go back to the prompt) ---${RESET}"
  for s in "${SERVICES[@]}"; do
    c=${colors[$((i % ${#colors[@]}))]}
    setsid bash -c 'tail -n 0 -F "$0" 2>/dev/null | sed -u "s/^/$1/"' \
      "$LOG_DIR/$s.log" "${c}[$s]${RESET} " &
    pids+=($!)
    i=$((i + 1))
  done

  trap ':' INT
  wait "${pids[@]}" 2>/dev/null      # returns when Ctrl+C arrives
  trap cleanup INT

  for p in "${pids[@]}"; do kill -TERM -- "-$p" 2>/dev/null; done
  echo ""
  echo "${DIM}--- back to prompt ---${RESET}"
}

# ---------- Cleanup ----------
CLEANING=0
cleanup() {
  [ "$CLEANING" -eq 1 ] && return
  CLEANING=1
  echo ""
  warn "Stopping all services..."
  local s
  for s in "${SERVICES[@]}"; do
    stop_service "$s"
  done
  info "Removing temporary logs ($LOG_DIR)..."
  rm -rf "$LOG_DIR" /tmp/buy01-logs
  ok "All stopped and cleaned up!"
  exit 0
}
trap cleanup SIGINT SIGTERM EXIT

# ---------- Start everything ----------
for service in "${SERVICES[@]}"; do
  start_service "$service"
done

echo "${DIM}---------------------------------------------------${RESET}"
ok "${BOLD}All 5 backend services + frontend are running!${RESET}"
show_help
echo "${DIM}---------------------------------------------------${RESET}"

# ---------- Command loop ----------
# \001 and \002 mark the color codes as non-printing so line editing works correctly
PROMPT=$'\001'"${BOLD}${GREEN}"$'\002'"buy01>"$'\001'"${RESET}"$'\002'" "

while true; do
  read -e -r -p "$PROMPT" cmd arg arg2 || break   # EOF (Ctrl+D) -> exit
  case "$cmd" in
    r|restart)
      if [ "$arg" = "all" ]; then
        for s in "${SERVICES[@]}"; do restart_service "$s"; done
      elif svc=$(resolve_service "$arg"); then
        restart_service "$svc"
      else
        err "Unknown or ambiguous service: '$arg'"; show_status
      fi
      ;;
    stop)
      if svc=$(resolve_service "$arg"); then
        stop_service "$svc"; warn "Stopped $svc"
      else
        err "Unknown or ambiguous service: '$arg'"
      fi
      ;;
    start)
      if svc=$(resolve_service "$arg"); then
        is_running "$svc" && warn "$svc is already running" || start_service "$svc"
      else
        err "Unknown or ambiguous service: '$arg'"
      fi
      ;;
    l|logs)
      if [ "$arg" = "all" ]; then
        tail_all_logs
      elif svc=$(resolve_service "$arg"); then
        tail_logs "$svc" "$arg2"
      else
        err "Unknown or ambiguous service: '$arg'"; show_status
      fi
      ;;
    ls|status) show_status ;;
    h|help|"?") show_help ;;
    q|quit|exit) break ;;
    "") ;;
    *) err "Unknown command: $cmd"; show_help ;;
  esac
done