#!/usr/bin/env bash
set -Eeuo pipefail

# Additive Core v30 module. Build against the actual installed core before any database change.
module_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="${1:-$HOME/gameserver001}"
backup_root="${2:-$HOME/gameserver001_backups}"
for tool in go psql pg_dump python3 systemctl curl; do
  command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
test -f "$project_dir/v30.go" && test -f "$project_dir/main.go" && test -f "$project_dir/.env"
test -f "$module_dir/frontier_expansion.go" && test -f "$module_dir/001_expansion.sql"
database_url="$(python3 - "$project_dir/.env" <<'PY'
import sys
from pathlib import Path
values=[line.split('=',1)[1].strip().strip('\"').strip("'") for line in Path(sys.argv[1]).read_text().splitlines() if line.startswith('DATABASE_URL=')]
if not values: raise SystemExit('DATABASE_URL missing')
print(values[-1])
PY
)"
export PGDATABASE="$database_url"
psql -v ON_ERROR_STOP=1 -Atqc "SELECT version FROM schema_migrations WHERE version=30" | python3 -c "import sys; assert sys.stdin.read().strip()=='30','Core v30 schema required'"
systemctl is-active --quiet gameserver001
curl -fsS --max-time 10 http://127.0.0.1:8080/health | python3 -c "import json,sys; x=json.load(sys.stdin); assert x['version']=='core-v30' and x['postgresql']=='ok'"
stage="$(mktemp -d)"
trap 'rm -rf -- "$stage"' EXIT
cp -a "$project_dir/." "$stage/"
cp "$module_dir/frontier_expansion.go" "$stage/frontier_expansion.go"
python3 - "$stage/main.go" <<'PY'
import sys
from pathlib import Path
p=Path(sys.argv[1]); s=p.read_text()
call='\tregisterFrontierExpansionRoutes(mux, a)\n'
if 'registerFrontierExpansionRoutes(mux, a)' not in s:
    marker='\tregisterV30Routes(mux, a)\n'
    if s.count(marker)!=1: raise SystemExit('Unexpected Core routing: no files or database changed')
    s=s.replace(marker,marker+call,1)
p.write_text(s)
PY
(
 cd "$stage"
 gofmt -w frontier_expansion.go main.go
 go test ./...
 go vet ./...
 go build -o frontier-expansion-binary .
)
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_dir="$backup_root/frontier-expansion-$stamp"
mkdir -p "$backup_dir"
chmod 700 "$backup_dir"
pg_dump -Fc > "$backup_dir/database.dump"
cp "$project_dir/gameserver001" "$backup_dir/gameserver001"
cp "$project_dir/main.go" "$backup_dir/main.go"
if [ -f "$project_dir/frontier_expansion.go" ]; then cp "$project_dir/frontier_expansion.go" "$backup_dir/frontier_expansion.go"; fi
echo "Build verified. Backup: $backup_dir"
psql -v ON_ERROR_STOP=1 -f "$module_dir/001_expansion.sql"
# Only kingdom 1 is populated; no players/accounts are fabricated.
psql -v ON_ERROR_STOP=1 -c 'BEGIN; SELECT frontier_seed(1); COMMIT;'
cp "$stage/main.go" "$project_dir/main.go"
cp "$stage/frontier_expansion.go" "$project_dir/frontier_expansion.go"
cp "$stage/frontier-expansion-binary" "$project_dir/gameserver001.next"
chmod 755 "$project_dir/gameserver001.next"
mv "$project_dir/gameserver001.next" "$project_dir/gameserver001"
services=(gameserver001)
if systemctl is-active --quiet gameserver002; then services+=(gameserver002); fi
if ! sudo systemctl restart "${services[@]}"; then
 cp "$backup_dir/gameserver001" "$project_dir/gameserver001.rollback"
 mv "$project_dir/gameserver001.rollback" "$project_dir/gameserver001"
 cp "$backup_dir/main.go" "$project_dir/main.go"
 sudo systemctl restart "${services[@]}"
 echo 'Restart failed; previous binary restored. Additive data retained.' >&2
 exit 1
fi
for attempt in 1 2 3 4 5; do
 if curl -fsS --max-time 4 http://127.0.0.1:8080/health >/dev/null; then break; fi
 if [ "$attempt" = 5 ]; then echo "Health check failed. Backup at $backup_dir" >&2; exit 1; fi
 sleep 1
done
psql -v ON_ERROR_STOP=1 -Atqc "SELECT count(*) FROM frontier_npc_cities WHERE server_id=1" | python3 -c "import sys; assert sys.stdin.read().strip()=='1000'"
echo 'World expansion installed: 1000 NPC cities, 2200 resources, 800 monsters, 12 landmarks.'
echo 'Open APK 0.6 and choose المستودع → التحقق من التحديث, or reopen the app.'
