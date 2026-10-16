#!/usr/bin/env bash
# Boots a real Paper server with the built jar and asserts it loads, generates its
# configs and answers its commands. Catches the class of breakage unit tests cannot:
# bad plugin.yml, config generation, module init, command registration.
#
#   ./scripts/smoke-test.sh [path/to/OptimizeIsEasy.jar]
#
# Keeps the downloaded server in .smoke/ (gitignored). Exits non-zero on any failure.
set -uo pipefail

MC="${MC:-1.21.1}"
RUN=".smoke"
SERVER="$RUN/server"
LOG="$RUN/server.log"
BOOT_WAIT="${BOOT_WAIT:-25}"

JAR="${1:-$(ls build/libs/OptimizeIsEasy-*.jar 2>/dev/null | head -1)}"
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "smoke: no jar. Run ./gradlew build first, or pass a path." >&2
  exit 1
fi
echo "smoke: $JAR on Paper $MC"

fail() { echo "smoke: FAIL - $*" >&2; [ -f "$LOG" ] && tail -30 "$LOG" >&2; exit 1; }

mkdir -p "$RUN"
if [ ! -f "$RUN/paper.jar" ]; then
  URL=$(curl -sS "https://fill.papermc.io/v3/projects/paper/versions/$MC/builds/latest" \
        | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])') \
    || fail "could not resolve Paper $MC download url"
  echo "smoke: downloading $URL"
  curl -fsS -o "$RUN/paper.jar" "$URL" || fail "download failed"
fi

rm -rf "$SERVER"; mkdir -p "$SERVER/plugins"
cp "$JAR" "$SERVER/plugins/"

cat > "$SERVER/eula.txt" <<'EOF'
eula=true
EOF

cat > "$SERVER/server.properties" <<'EOF'
online-mode=false
level-type=minecraft\:flat
level-name=smoke
spawn-protection=0
max-players=5
view-distance=4
simulation-distance=4
max-world-size=1024
EOF

echo "smoke: booting (${BOOT_WAIT}s wait, expect ~60-90s total)"
(
  cd "$SERVER" || exit 1
  # Commands are fed on stdin after boot; anything the server reads early is queued.
  {
    sleep "$BOOT_WAIT"
    echo "optimize status";   sleep 4
    echo "exploitfix status"; sleep 4
    echo "optimize edb check"; sleep 6
    echo "stop";              sleep 5
  } | timeout 420 java -Xms512M -Xmx1G -jar ../paper.jar --nogui
) > "$LOG" 2>&1

grep -q "OptimizeIsEasy enabled"  "$LOG" || fail "plugin never enabled"
grep -q "OptimizeIsEasy disabled" "$LOG" || fail "no clean shutdown"
grep -q "OptimizeIsEasy .*Status" "$LOG" || fail "/optimize status produced no output"
grep -q "ExploitDB"               "$LOG" || fail "/exploitfix or /optimize edb produced no output"
grep -Eq "SEVERE|Could not load|Caused by" "$LOG" && fail "exceptions in the server log"

# Every file the jar ships under a plugin subdirectory must have been written out.
python3 - "$JAR" "$SERVER/plugins/OptimizeIsEasy" <<'PY' || fail "plugin data files incomplete"
import os, sys, zipfile
jar, out = sys.argv[1], sys.argv[2]
if not os.path.isdir(out):
    sys.exit(f"smoke: no plugin data dir {out}")
names = zipfile.ZipFile(jar).namelist()
bad = False
for sub in sorted(d for d in os.listdir(out) if os.path.isdir(os.path.join(out, d))):
    shipped = sorted(os.path.basename(n) for n in names
                     if n.startswith(sub + "/") and n.count("/") == 1 and not n.endswith("/"))
    have = set(os.listdir(os.path.join(out, sub)))
    missing = [f for f in shipped if f not in have]
    if shipped:
        print(f"smoke: {len(shipped) - len(missing)}/{len(shipped)} files generated in {sub}/")
    if missing:
        print(f"smoke: MISSING in {sub}/: " + ", ".join(missing), file=sys.stderr)
        bad = True
if bad:
    sys.exit(1)
PY

echo "smoke: OK - enabled, jar data files written, commands answered, clean stop"
