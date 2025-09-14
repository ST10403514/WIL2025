#!/usr/bin/env bash
set -euo pipefail

# ESP32 full-flash backup helper
# - Detects serial port (macOS/Linux), queries flash size, then dumps entire flash.
# - Requires: python3, esptool (will try to install via pip --user if missing)

usage() {
  cat <<EOF
Usage: $0 [--port <serial-dev>] [--baud <baud>] [--chip <auto|esp32|esp32s2|esp32s3|esp32c3>] [--outdir <dir>]

Examples:
  $0                                 # auto-detect port and chip, pick sane defaults
  $0 --port /dev/cu.usbserial-0001   # specify port explicitly (macOS typical)

Outputs:
  - backups/esp32/<timestamp>/flash-backup.bin
  - backups/esp32/<timestamp>/flash-backup.bin.sha256
  - backups/esp32/<timestamp>/metadata.json
EOF
}

PORT=""
BAUD="921600"
CHIP="auto"
OUTDIR_BASE="backups/esp32"

while [ "$#" -gt 0 ]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --port) PORT=${2:-}; shift 2 ;;
    --baud) BAUD=${2:-}; shift 2 ;;
    --chip) CHIP=${2:-}; shift 2 ;;
    --outdir) OUTDIR_BASE=${2:-}; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

# Ensure python3 exists
if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 not found in PATH. Please install Python 3." >&2
  exit 1
fi

# Ensure esptool is available
if python3 -m esptool --help >/dev/null 2>&1; then
  ESPTOOL=(python3 -m esptool)
elif command -v esptool.py >/dev/null 2>&1; then
  ESPTOOL=(esptool.py)
else
  echo "Installing esptool for current user ..."
  python3 -m pip install --user --upgrade esptool
  if python3 -m esptool --help >/dev/null 2>&1; then
    ESPTOOL=(python3 -m esptool)
  elif command -v esptool.py >/dev/null 2>&1; then
    ESPTOOL=(esptool.py)
  else
    echo "Failed to install esptool." >&2
    exit 1
  fi
fi

detect_ports() {
  # macOS devices begin with /dev/cu.*; Linux with /dev/ttyUSB* or /dev/ttyACM*
  local list
  list=""
  for pat in \
    /dev/cu.SLAB_* /dev/cu.usbserial* /dev/cu.usbmodem* /dev/cu.wchusbserial* \
    /dev/ttyUSB* /dev/ttyACM* /dev/tty.SLAB_* /dev/tty.usbserial* /dev/tty.usbmodem* /dev/tty.wchusbserial*; do
    for d in $pat; do
      [ -e "$d" ] && list+="$d\n" || true
    done
  done
  printf "%b" "$list" | sed '/^$/d' | sort -u
}

if [ -z "$PORT" ]; then
  CANDIDATES=$(detect_ports || true)
  if [ -z "$CANDIDATES" ]; then
    echo "No serial ports detected. Connect the ESP32 and ensure no other app is using it." >&2
    exit 1
  fi
  # Prefer macOS /dev/cu.* over /dev/tty.* if both exist
  PORT=$(printf "%s\n" "$CANDIDATES" | grep '/dev/cu\.' || true | head -n1)
  if [ -z "$PORT" ]; then
    PORT=$(printf "%s\n" "$CANDIDATES" | head -n1)
  fi
  echo "Detected serial port: $PORT"
fi

TS=$(date +%Y%m%d-%H%M%S)
OUTDIR="$OUTDIR_BASE/$TS"
mkdir -p "$OUTDIR"

echo "Querying flash chip info (port=$PORT baud=$BAUD) ..."
FLASH_ID_OUT="$OUTDIR/flash_id.log"
set +e
"${ESPTOOL[@]}" --chip "$CHIP" --port "$PORT" --baud "$BAUD" flash_id | tee "$FLASH_ID_OUT"
ES=$?
set -e
if [ $ES -ne 0 ]; then
  echo "flash_id failed at $BAUD, retrying at 460800 ..."
  BAUD=460800
  "${ESPTOOL[@]}" --chip "$CHIP" --port "$PORT" --baud "$BAUD" flash_id | tee -a "$FLASH_ID_OUT"
fi

# Parse detected flash size (e.g., "Detected flash size: 4MB")
SIZE_MB=$(grep -Eo 'Detected flash size: *[0-9]+' "$FLASH_ID_OUT" | awk '{print $4}' | tail -n1)
if [ -z "$SIZE_MB" ]; then
  echo "Could not parse flash size from esptool output; defaulting to 4 MB" >&2
  SIZE_MB=4
fi

# Convert MB to bytes and hex length string
SIZE_BYTES=$(python3 - "$SIZE_MB" <<'PY'
import sys
mb=int(float(sys.argv[1]))
print(mb*1024*1024)
PY
)
LEN_HEX=$(python3 - "$SIZE_BYTES" <<'PY'
import sys
n=int(sys.argv[1])
print(hex(n))
PY
)

echo "Detected flash size: ${SIZE_MB} MB (${LEN_HEX})"

OUTBIN="$OUTDIR/flash-backup.bin"

echo "Reading entire flash to $OUTBIN ... this may take a few minutes."
set +e
"${ESPTOOL[@]}" --chip "$CHIP" --port "$PORT" --baud "$BAUD" read_flash 0x000000 "$LEN_HEX" "$OUTBIN"
ES=$?
set -e
if [ $ES -ne 0 ]; then
  echo "read_flash failed at $BAUD, retrying at 230400 ..."
  BAUD=230400
  "${ESPTOOL[@]}" --chip "$CHIP" --port "$PORT" --baud "$BAUD" read_flash 0x000000 "$LEN_HEX" "$OUTBIN"
fi

# Compute SHA-256
if command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$OUTBIN" > "$OUTBIN.sha256"
elif command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$OUTBIN" > "$OUTBIN.sha256"
else
  python3 - "$OUTBIN" <<'PY' > "$OUTBIN.sha256"
import hashlib,sys
p=sys.argv[1]
h=hashlib.sha256()
with open(p,'rb') as f:
  for chunk in iter(lambda:f.read(1<<20), b''):
    h.update(chunk)
print(h.hexdigest(), p)
PY
fi

# Metadata
cat > "$OUTDIR/metadata.json" <<META
{
  "timestamp": "$TS",
  "port": "$PORT",
  "baud": "$BAUD",
  "chip": "$CHIP",
  "detected_flash_size_mb": $SIZE_MB,
  "length_hex": "$LEN_HEX",
  "flash_id_log": "$(python3 - <<'PY'
import json
print(json.dumps(open('$FLASH_ID_OUT').read()))
PY
)"
}
META

echo "Backup complete: $OUTBIN"
echo "SHA256: $(cut -d' ' -f1 "$OUTBIN.sha256")"
echo "Metadata: $OUTDIR/metadata.json"
