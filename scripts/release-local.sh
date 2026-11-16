#!/usr/bin/env bash
# ==============================================================================
# ZiroMaps Local APK Release Pipeline
# ==============================================================================
# Menjalankan build rilis lokal mandiri tanpa ketergantungan CI/CD remote.
# Alur:
# 1. Jalankan unit test seluruh modul (testDebugUnitTest)
# 2. Jalankan analisis static Android Lint (lintDebug)
# 3. Bangun APK rilis (assembleRelease) dengan signing config lokal
# 4. Salin APK ke dist/ZiroMaps-v<versionName>.apk dan buat checksum SHA-256
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"

echo "========================================================"
echo " Starting ZiroMaps Local Release Pipeline"
echo " Project root: ${PROJECT_ROOT}"
echo "========================================================"

# 1. Ambil versionName dari app/build.gradle.kts
VERSION_NAME=$(grep 'versionName =' app/build.gradle.kts | head -n 1 | sed -E 's/.*versionName = "([^"]+)".*/\1/')
if [ -z "${VERSION_NAME}" ]; then
    VERSION_NAME="1.0"
fi
echo "[INFO] Target Release Version: v${VERSION_NAME}"

# 2. Jalankan Unit Test Seluruh Modul
echo ""
echo "--> [1/4] Running Unit Tests (./gradlew testDebugUnitTest)..."
./gradlew testDebugUnitTest

# 3. Jalankan Android Lint
echo ""
echo "--> [2/4] Running Android Lint (./gradlew lintDebug)..."
./gradlew lintDebug

# 4. Bangun APK Release
echo ""
echo "--> [3/4] Assembling Release APK (./gradlew assembleRelease)..."
./gradlew assembleRelease

# 5. Salin APK ke folder dist/ dan generate SHA-256
echo ""
echo "--> [4/4] Packaging to dist/ directory..."
DIST_DIR="${PROJECT_ROOT}/dist"
mkdir -p "${DIST_DIR}"

RELEASE_APK_DIR="${PROJECT_ROOT}/app/build/outputs/apk/release"
FOUND_APK=$(find "${RELEASE_APK_DIR}" -maxdepth 1 -type f -name "*.apk" | head -n 1)

if [ -z "${FOUND_APK}" ] || [ ! -f "${FOUND_APK}" ]; then
    echo "[ERROR] Release APK not found in ${RELEASE_APK_DIR}" >&2
    exit 1
fi

TARGET_APK_NAME="ZiroMaps-v${VERSION_NAME}.apk"
TARGET_APK_PATH="${DIST_DIR}/${TARGET_APK_NAME}"
TARGET_SHA_PATH="${DIST_DIR}/${TARGET_APK_NAME}.sha256"

cp -f "${FOUND_APK}" "${TARGET_APK_PATH}"

# Generate SHA-256 checksum (relative path inside dist for portability)
(cd "${DIST_DIR}" && sha256sum "${TARGET_APK_NAME}" > "${TARGET_APK_NAME}.sha256")

APK_SIZE=$(du -h "${TARGET_APK_PATH}" | cut -f1)
APK_HASH=$(cut -d' ' -f1 "${TARGET_SHA_PATH}")

echo ""
echo "========================================================"
echo " Local Release Build Succeeded!"
echo "========================================================"
echo " APK Output : ${TARGET_APK_PATH} (${APK_SIZE})"
echo " SHA-256    : ${APK_HASH}"
echo " Checksum   : ${TARGET_SHA_PATH}"
echo "========================================================"
