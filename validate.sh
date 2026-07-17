#!/bin/bash
# validate.sh - Android Resource Validation Script
# Run BEFORE git commit to catch common build-breaking errors
# Usage: bash validate.sh [project-root]

set -e

ROOT="${1:-.}"
cd "$ROOT"
echo "=== VALIDATION: $ROOT ==="

# 1. XML Well-formedness Check (no content after </resources>)
echo ""
echo "[1/4] Checking XML well-formedness..."
BAD_XML=0
while IFS= read -r -d '' xmlfile; do
    if grep -q '</resources>' "$xmlfile" 2>/dev/null; then
        AFTER=$(sed -n '/<\/resources>/,$p' "$xmlfile" | tail -n +2 | grep -c '[^[:space:]]' || true)
        if [ "$AFTER" -gt 0 ]; then
            echo "  WARNING: Content after </resources> in $xmlfile"
            BAD_XML=$((BAD_XML + 1))
        fi
    fi
    if command -v xmllint &>/dev/null; then
        if ! xmllint --noout "$xmlfile" 2>/dev/null; then
            echo "  INVALID XML: $xmlfile"
            BAD_XML=$((BAD_XML + 1))
        fi
    fi
done < <(find "$ROOT" -name '*.xml' -path '*/res/*' -type f -print0 2>/dev/null || true)
if [ "$BAD_XML" -eq 0 ]; then echo "  PASS: All XML files are valid"; fi

# 2. Duplicate Resource Names
echo ""
echo "[2/4] Checking for duplicate resource names..."
DUPS=0
for dir in "$ROOT/app/src/main/res/values" $(find "$ROOT/app/src/main/res" -maxdepth 1 -type d 2>/dev/null); do
    if [ -d "$dir" ]; then
        for pattern in 'color name=' 'string name=' 'dimen name=' 'style name=' 'attr name=' 'drawable name='; do
            found=$(grep -rhn "$pattern" "$dir/"*.xml 2>/dev/null | grep -oP "$pattern\"\K[^\"]+" | sort || true)
            dupes=$(echo "$found" | uniq -d || true)
            if [ -n "$dupes" ]; then
                echo "  DUPLICATE in $dir ($pattern):"
                echo "$dupes"
                DUPS=$((DUPS + 1))
            fi
        done
    fi
done
if [ "$DUPS" -eq 0 ]; then echo "  PASS: No duplicate resources found"; fi

# 3. Resource Reference Validation (using temp file to avoid subshell counter bug)
echo ""
echo "[3/4] Checking resource references..."
TEMP_FILE=$(mktemp /tmp/validate_missing.XXXXXX 2>/dev/null || echo "/tmp/validate_missing.txt")
> "$TEMP_FILE"
while IFS= read -r -d '' xmlfile; do
    grep -oP '@(color|string|dimen|drawable|mipmap)/\K[a-zA-Z_][a-zA-Z0-9_]*' "$xmlfile" 2>/dev/null | sort -u | while IFS= read -r ref; do
        if ! grep -qr "name=\"$ref\"" "$ROOT/app/src/main/res/" 2>/dev/null; then
            echo "  MISSING REF: $ref referenced in $xmlfile but not defined anywhere"
            echo "$ref" >> "$TEMP_FILE"
        fi
    done
done < <(find "$ROOT" -name '*.xml' -path '*/res/*' ! -path '*/res/values/*' -type f -print0 2>/dev/null || true)
MISSING=$(wc -l < "$TEMP_FILE" 2>/dev/null || echo 0)
rm -f "$TEMP_FILE" 2>/dev/null || true
if [ "$MISSING" -eq 0 ]; then echo "  PASS: All resource references resolve"; fi

# Summary
echo ""
echo "=== VALIDATION COMPLETE ==="
echo "XML issues: $BAD_XML | Duplicate resources: $DUPS | Missing refs: $MISSING"
if [ "$BAD_XML" -gt 0 ] || [ "$DUPS" -gt 0 ] || [ "$MISSING" -gt 0 ]; then
    echo "FAIL: Fix the issues above before committing!"
    exit 1
else
    echo "PASS: Project is clean, safe to commit."
fi
