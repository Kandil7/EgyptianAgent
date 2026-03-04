#!/usr/bin/env bash
# =============================================================================
# Egyptian Agent - Production Build Script
# =============================================================================
#
# PURPOSE:
#   Builds, optimizes, signs, and verifies a production-ready APK for the
#   Egyptian Agent application. Includes zipalign optimization, release signing,
#   and comprehensive verification steps.
#
# USAGE:
#   ./scripts/build/build_production.sh [OPTIONS]
#
# OPTIONS:
#   --keystore PATH     Path to release keystore (default: keystore/release.keystore)
#   --key-alias ALIAS   Key alias in keystore (default: egyptian-agent)
#   --output DIR        Output directory for signed APK (default: dist/production)
#   --skip-tests        Skip running tests before build
#   --skip-sign         Skip APK signing (for debugging)
#   --generate-sbom     Generate Software Bill of Materials
#   --version NAME      Version name override (e.g., 1.0.0)
#   --version-code CODE Version code override (e.g., 100)
#   --log-file PATH     Write build log to specified file
#   --ci                CI/CD mode (non-interactive output)
#   -h, --help          Show this help message
#
# EXAMPLES:
#   ./scripts/build/build_production.sh
#   ./scripts/build/build_production.sh --keystore /path/to/keystore.jks
#   ./scripts/build/build_production.sh --version 1.0.0 --version-code 100
#   ./scripts/build/build_production.sh --ci --generate-sbom
#
# ENVIRONMENT VARIABLES:
#   KEYSTORE_PASSWORD   Keystore password (or will prompt)
#   KEY_PASSWORD        Key password (or will prompt)
#   ANDROID_HOME        Android SDK location
#   JAVA_HOME           Java JDK location
#
# RETURN CODES:
#   0   Success
#   1   General error
#   2   Prerequisites missing
#   3   Build failed
#   4   Signing failed
#   5   Verification failed
#
# AUTHOR: EgyptianAgent Team
# VERSION: 2.0.0
# DATE: 2026-03-03
# =============================================================================

set -euo pipefail

# =============================================================================
# Configuration
# =============================================================================

readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly LOG_DIR="$PROJECT_DIR/build/logs"
readonly BUILD_TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

# Default configuration
KEYSTORE_PATH="keystore/release.keystore"
KEY_ALIAS="egyptian-agent"
OUTPUT_DIR="$PROJECT_DIR/dist/production"
SKIP_TESTS=false
SKIP_SIGN=false
GENERATE_SBOM=false
VERSION_NAME=""
VERSION_CODE=""
LOG_FILE=""
CI_MODE=false

# Colors
declare -A COLORS=(
    [red]='\033[0;31m'
    [green]='\033[0;32m'
    [yellow]='\033[1;33m'
    [blue]='\033[0;34m'
    [cyan]='\033[0;36m'
    [nc]='\033[0m'
)

# =============================================================================
# Logging Functions
# =============================================================================

init_logging() {
    mkdir -p "$LOG_DIR"
    mkdir -p "$OUTPUT_DIR"
    
    if [[ -n "$LOG_FILE" ]]; then
        exec > >(tee -a "$LOG_FILE") 2>&1
    fi
}

log_info() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[INFO] $*"
    else
        echo -e "${COLORS[green]}[INFO]${COLORS[nc]} $*"
    fi
}

log_warn() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[WARN] $*"
    else
        echo -e "${COLORS[yellow]}[WARN]${COLORS[nc]} $*"
    fi
}

log_error() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[ERROR] $*" >&2
    else
        echo -e "${COLORS[red]}[ERROR]${COLORS[nc]} $*" >&2
    fi
}

log_step() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[STEP] $*"
    else
        echo -e "${COLORS[blue]}[STEP]${COLORS[nc]} $*"
    fi
}

log_success() {
    if [[ "$CI_MODE" == "true" ]]; then
        echo "[SUCCESS] $*"
    else
        echo -e "${COLORS[cyan]}[SUCCESS]${COLORS[nc]} $*"
    fi
}

print_header() {
    local title="$1"
    local width=60
    
    if [[ "$CI_MODE" == "true" ]]; then
        echo "=== $title ==="
    else
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
        echo -e "${COLORS[blue]}  $title${COLORS[nc]}"
        echo -e "${COLORS[blue]}$(printf '=%.0s' $(seq 1 $width))${COLORS[nc]}"
    fi
}

# =============================================================================
# Utility Functions
# =============================================================================

detect_os() {
    case "$(uname -s 2>/dev/null || echo "Windows")" in
        Linux*)     echo "linux";;
        Darwin*)    echo "macos";;
        MINGW*|MSYS*|CYGWIN*) echo "windows";;
        *)          echo "unknown";;
    esac
}

get_gradlew() {
    if [[ -f "$PROJECT_DIR/gradlew" ]]; then
        echo "./gradlew"
    elif [[ -f "$PROJECT_DIR/gradlew.bat" ]]; then
        echo "./gradlew.bat"
    else
        return 1
    fi
}

format_size() {
    local bytes="$1"
    if [[ $bytes -ge 1073741824 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1073741824}") GB"
    elif [[ $bytes -ge 1048576 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1048576}") MB"
    elif [[ $bytes -ge 1024 ]]; then
        echo "$(awk "BEGIN {printf \"%.2f\", $bytes / 1024}") KB"
    else
        echo "$bytes B"
    fi
}

get_file_size() {
    local file="$1"
    if [[ -f "$file" ]]; then
        stat -c%s "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

compute_sha256() {
    local file="$1"
    if [[ -f "$file" ]]; then
        sha256sum "$file" 2>/dev/null | cut -d' ' -f1 || \
        shasum -a 256 "$file" 2>/dev/null | cut -d' ' -f1 || \
        echo "not_computed"
    else
        echo "file_not_found"
    fi
}

# =============================================================================
# Prerequisite Checks
# =============================================================================

check_prerequisites() {
    log_step "Checking production build prerequisites..."
    local missing=()
    
    # Check Java
    if ! command -v java &>/dev/null; then
        missing+=("Java JDK 17+")
    else
        local java_version
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$java_version" -lt 17 ]]; then
            missing+=("Java JDK 17+ (found: $java_version)")
        else
            log_info "Java: OK (version $java_version)"
        fi
    fi
    
    # Check Gradle wrapper
    if ! get_gradlew &>/dev/null; then
        missing+=("Gradle wrapper")
    else
        log_info "Gradle wrapper: OK"
    fi
    
    # Check Android SDK build-tools
    if ! command -v zipalign &>/dev/null; then
        if [[ -n "${ANDROID_HOME:-}" ]]; then
            if [[ ! -f "$ANDROID_HOME/build-tools/"*"/zipalign" ]]; then
                missing+=("Android SDK build-tools (zipalign)")
            fi
        else
            missing+=("ANDROID_HOME (for zipalign)")
        fi
    else
        log_info "zipalign: OK"
    fi
    
    if ! command -v apksigner &>/dev/null; then
        if [[ -n "${ANDROID_HOME:-}" ]]; then
            if [[ ! -f "$ANDROID_HOME/build-tools/"*"/apksigner" ]]; then
                missing+=("Android SDK build-tools (apksigner)")
            fi
        else
            missing+=("ANDROID_HOME (for apksigner)")
        fi
    else
        log_info "apksigner: OK"
    fi
    
    # Check keystore (unless skipping sign)
    if [[ "$SKIP_SIGN" != "true" ]]; then
        if [[ ! -f "$PROJECT_DIR/$KEYSTORE_PATH" ]]; then
            log_warn "Keystore not found: $KEYSTORE_PATH"
            log_warn "Will use debug keystore for signing"
            KEYSTORE_PATH="keystore/debug.keystore"
            KEY_ALIAS="androiddebugkey"
        else
            log_info "Keystore: OK ($KEYSTORE_PATH)"
        fi
    fi
    
    # Report missing
    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing prerequisites:"
        for item in "${missing[@]}"; do
            log_error "  - $item"
        done
        echo ""
        log_error "Solutions:"
        if [[ " ${missing[*]} " =~ "Java" ]]; then
            echo "  - Install JDK 17+: https://adoptium.net/"
        fi
        if [[ " ${missing[*]} " =~ "Gradle" ]]; then
            echo "  - Ensure you're in the project root directory"
        fi
        if [[ " ${missing[*]} " =~ "build-tools" ]]; then
            echo "  - Install Android SDK build-tools via Android Studio"
            echo "  - Set ANDROID_HOME environment variable"
        fi
        return 2
    fi
    
    log_success "All prerequisites satisfied"
    return 0
}

# =============================================================================
# Build Functions
# =============================================================================

run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log_info "Skipping tests (--skip-tests)"
        return 0
    fi
    
    log_step "Running tests..."
    
    local gradlew
    gradlew=$(get_gradlew)
    
    # Run unit tests
    log_info "Running unit tests..."
    if ! $gradlew test --console=plain --quiet; then
        log_error "Unit tests failed"
        return 3
    fi
    log_success "Unit tests passed"
    
    # Run lint
    log_info "Running lint checks..."
    if ! $gradlew lint --console=plain --quiet; then
        log_warn "Lint checks found issues (continuing build)"
    else
        log_success "Lint checks passed"
    fi
    
    return 0
}

build_release_apk() {
    log_step "Building release APK..."
    
    local gradlew
    gradlew=$(get_gradlew)
    
    local gradle_flags=""
    
    # Apply version overrides
    if [[ -n "$VERSION_NAME" ]]; then
        gradle_flags="$gradle_flags -PversionName=$VERSION_NAME"
    fi
    
    if [[ -n "$VERSION_CODE" ]]; then
        gradle_flags="$gradle_flags -PversionCode=$VERSION_CODE"
    fi
    
    # Build release APK
    local start_time
    start_time=$(date +%s)
    
    if ! $gradlew :app:assembleRelease $gradle_flags --no-daemon --console=plain; then
        log_error "Build failed"
        return 3
    fi
    
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_success "Build completed in ${duration}s"
    return 0
}

find_release_apk() {
    local apk_dir="$PROJECT_DIR/app/build/outputs/apk/release"
    
    if [[ ! -d "$apk_dir" ]]; then
        return 1
    fi
    
    find "$apk_dir" -name "*.apk" -type f ! -name "*unaligned*" ! -name "*signed*" | head -1
}

optimize_apk() {
    local input_apk="$1"
    local output_apk="$2"
    
    log_step "Optimizing APK with zipalign..."
    
    if ! zipalign -v -p 4 "$input_apk" "$output_apk"; then
        log_error "zipalign failed"
        return 3
    fi
    
    log_success "APK optimized: $(basename "$output_apk")"
    return 0
}

sign_apk() {
    local input_apk="$1"
    local output_apk="$2"
    
    if [[ "$SKIP_SIGN" == "true" ]]; then
        log_info "Skipping signing (--skip-sign)"
        cp "$input_apk" "$output_apk"
        return 0
    fi
    
    log_step "Signing APK..."
    
    local keystore_file="$PROJECT_DIR/$KEYSTORE_PATH"
    
    # Get passwords
    local keystore_pass="${KEYSTORE_PASSWORD:-}"
    local key_pass="${KEY_PASSWORD:-$keystore_pass}"
    
    if [[ -z "$keystore_pass" ]]; then
        if [[ "$CI_MODE" == "true" ]]; then
            log_error "KEYSTORE_PASSWORD not set in CI mode"
            return 4
        fi
        read -rsp "Enter keystore password: " keystore_pass
        echo ""
        key_pass="${KEY_PASSWORD:-$keystore_pass}"
    fi
    
    # Sign the APK
    if ! apksigner sign \
        --ks "$keystore_file" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$keystore_pass" \
        --key-pass "pass:$key_pass" \
        --out "$output_apk" \
        "$input_apk"; then
        log_error "APK signing failed"
        return 4
    fi
    
    log_success "APK signed"
    return 0
}

verify_apk_signature() {
    local apk_file="$1"
    
    log_step "Verifying APK signature..."
    
    if ! apksigner verify --verbose "$apk_file"; then
        log_error "APK signature verification failed"
        return 5
    fi
    
    log_success "APK signature verified"
    return 0
}

verify_apk_integrity() {
    local apk_file="$1"
    
    log_step "Verifying APK integrity..."
    
    # Check file exists
    if [[ ! -f "$apk_file" ]]; then
        log_error "APK not found: $apk_file"
        return 5
    fi
    
    # Check minimum size
    local size_bytes
    size_bytes=$(get_file_size "$apk_file")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    if [[ "$size_mb" -lt 30 ]]; then
        log_error "APK size too small: ${size_mb}MB (expected > 30MB)"
        return 5
    fi
    
    log_info "APK size: $(format_size "$size_bytes")"
    
    # Verify with aapt2
    if command -v aapt2 &>/dev/null; then
        if ! aapt2 dump badging "$apk_file" &>/dev/null; then
            log_error "APK integrity check failed"
            return 5
        fi
        log_info "APK structure: valid"
    fi
    
    return 0
}

generate_sbom() {
    if [[ "$GENERATE_SBOM" != "true" ]]; then
        return 0
    fi
    
    log_step "Generating Software Bill of Materials (SBOM)..."
    
    local sbom_file="$OUTPUT_DIR/egyptian_agent_sbom_$BUILD_TIMESTAMP.json"
    
    # Generate basic SBOM
    cat > "$sbom_file" << EOF
{
  "bomFormat": "CycloneDX",
  "specVersion": "1.4",
  "version": 1,
  "metadata": {
    "timestamp": "$(date -Iseconds)",
    "tools": [
      {
        "vendor": "EgyptianAgent",
        "name": "build_production.sh",
        "version": "2.0.0"
      }
    ],
    "component": {
      "type": "application",
      "name": "Egyptian Agent",
      "version": "${VERSION_NAME:-unknown}",
      "purl": "pkg:apk/com.egyptian.agent@${VERSION_NAME:-unknown}"
    }
  },
  "components": [
    {
      "type": "library",
      "name": "Android SDK",
      "version": "${ANDROID_HOME:-unknown}"
    },
    {
      "type": "library",
      "name": "Gradle",
      "version": "$(get_gradlew 2>/dev/null && ./gradlew --version 2>/dev/null | grep Gradle | cut -d' ' -f2 || echo "unknown")"
    },
    {
      "type": "library",
      "name": "Java",
      "version": "$(java -version 2>&1 | head -1 | cut -d'"' -f2 || echo "unknown")"
    }
  ]
}
EOF
    
    log_info "SBOM generated: $sbom_file"
}

generate_build_report() {
    local apk_file="$1"
    local report_file="$OUTPUT_DIR/build_report_$BUILD_TIMESTAMP.txt"
    
    log_step "Generating build report..."
    
    cat > "$report_file" << EOF
===============================================================================
Egyptian Agent - Production Build Report
===============================================================================

Build Information:
  Timestamp:       $BUILD_TIMESTAMP
  Version Name:    ${VERSION_NAME:-default}
  Version Code:    ${VERSION_CODE:-default}
  Keystore:        $KEYSTORE_PATH
  Key Alias:       $KEY_ALIAS

Environment:
  OS:              $(detect_os)
  Hostname:        $(hostname 2>/dev/null || echo "unknown")
  User:            $(whoami 2>/dev/null || echo "unknown")

Git Information:
  Branch:          $(git branch --show-current 2>/dev/null || echo "unknown")
  Commit:          $(git rev-parse HEAD 2>/dev/null || echo "unknown")
  Status:          $(git diff-index --quiet HEAD -- 2>/dev/null && echo "clean" || echo "modified")

Build Tools:
  Java:            $(java -version 2>&1 | head -1 || echo "unknown")
  Gradle:          $(get_gradlew 2>/dev/null && ./gradlew --version 2>/dev/null | head -1 || echo "unknown")
  Android SDK:     ${ANDROID_HOME:-$ANDROID_SDK_ROOT:-not set}

Build Artifacts:
  APK Path:        $apk_file
  APK Size:        $(format_size $(get_file_size "$apk_file"))
  APK SHA256:      $(compute_sha256 "$apk_file")

Build Options:
  Skip Tests:      $SKIP_TESTS
  Skip Sign:       $SKIP_SIGN
  Generate SBOM:   $GENERATE_SBOM

Build Status:      SUCCESS
===============================================================================
EOF
    
    log_info "Build report: $report_file"
}

# =============================================================================
# Help and Usage
# =============================================================================

show_help() {
    cat << EOF
Egyptian Agent - Production Build Script

USAGE:
    $SCRIPT_NAME [OPTIONS]

OPTIONS:
    --keystore PATH     Path to release keystore
                        (default: keystore/release.keystore)
    --key-alias ALIAS   Key alias in keystore (default: egyptian-agent)
    --output DIR        Output directory (default: dist/production)
    --skip-tests        Skip running tests before build
    --skip-sign         Skip APK signing (for debugging)
    --generate-sbom     Generate Software Bill of Materials
    --version NAME      Version name override (e.g., 1.0.0)
    --version-code CODE Version code override (e.g., 100)
    --log-file PATH     Write build log to specified file
    --ci                CI/CD mode (non-interactive output)
    -h, --help          Show this help message

EXAMPLES:
    # Standard production build
    $SCRIPT_NAME

    # Build with custom keystore
    $SCRIPT_NAME --keystore /path/to/keystore.jks --key-alias my-alias

    # Build with version override
    $SCRIPT_NAME --version 1.0.0 --version-code 100

    # CI/CD build with SBOM
    $SCRIPT_NAME --ci --generate-sbom --log-file build.log

    # Debug build (skip signing)
    $SCRIPT_NAME --skip-sign

ENVIRONMENT VARIABLES:
    KEYSTORE_PASSWORD   Keystore password (or will prompt interactively)
    KEY_PASSWORD        Key password (defaults to keystore password)
    ANDROID_HOME        Android SDK location
    JAVA_HOME           Java JDK location

RETURN CODES:
    0   Success
    1   General error
    2   Prerequisites missing
    3   Build failed
    4   Signing failed
    5   Verification failed

OUTPUT:
    Signed APK:  dist/production/egyptian_agent_production_*.apk
    SBOM:        dist/production/egyptian_agent_sbom_*.json (if --generate-sbom)
    Report:      dist/production/build_report_*.txt
    Logs:        build/logs/

For more information, see: docs/deployment/DEPLOYMENT_GUIDE.md
EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --keystore)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --keystore requires an argument"
                    return 5
                fi
                KEYSTORE_PATH="$2"
                shift 2
                ;;
            --key-alias)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --key-alias requires an argument"
                    return 5
                fi
                KEY_ALIAS="$2"
                shift 2
                ;;
            --output)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --output requires an argument"
                    return 5
                fi
                OUTPUT_DIR="$2"
                shift 2
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-sign)
                SKIP_SIGN=true
                shift
                ;;
            --generate-sbom)
                GENERATE_SBOM=true
                shift
                ;;
            --version)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --version requires an argument"
                    return 5
                fi
                VERSION_NAME="$2"
                shift 2
                ;;
            --version-code)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --version-code requires an argument"
                    return 5
                fi
                VERSION_CODE="$2"
                shift 2
                ;;
            --log-file)
                if [[ -z "${2:-}" ]]; then
                    log_error "Option --log-file requires an argument"
                    return 5
                fi
                LOG_FILE="$2"
                shift 2
                ;;
            --ci)
                CI_MODE=true
                COLORS=([red]='' [green]='' [yellow]='' [blue]='' [cyan]='' [nc]='')
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                echo "Use --help for usage information"
                return 5
                ;;
            *)
                log_error "Unexpected argument: $1"
                return 5
                ;;
        esac
    done
    
    return 0
}

# =============================================================================
# Main
# =============================================================================

main() {
    # Parse arguments
    if ! parse_arguments "$@"; then
        exit $?
    fi
    
    # Initialize logging
    init_logging
    
    # Print header
    print_header "Egyptian Agent Production Build"
    
    echo ""
    log_info "Build Configuration:"
    echo "  Keystore:     $KEYSTORE_PATH"
    echo "  Key Alias:    $KEY_ALIAS"
    echo "  Output Dir:   $OUTPUT_DIR"
    echo "  Skip Tests:   $SKIP_TESTS"
    echo "  Skip Sign:    $SKIP_SIGN"
    echo "  Generate SBOM: $GENERATE_SBOM"
    echo ""
    
    # Check prerequisites
    if ! check_prerequisites; then
        exit $?
    fi
    
    echo ""
    
    # Run tests
    if ! run_tests; then
        exit $?
    fi
    
    echo ""
    
    # Build release APK
    if ! build_release_apk; then
        exit $?
    fi
    
    # Find the built APK
    local raw_apk
    raw_apk=$(find_release_apk)
    
    if [[ -z "$raw_apk" ]]; then
        log_error "Release APK not found"
        return 3
    fi
    
    log_info "Found APK: $(basename "$raw_apk")"
    
    # Create output filenames
    local aligned_apk="$OUTPUT_DIR/egyptian_agent_production_${BUILD_TIMESTAMP}_aligned.apk"
    local signed_apk="$OUTPUT_DIR/egyptian_agent_production_${BUILD_TIMESTAMP}_signed.apk"
    
    echo ""
    
    # Optimize APK
    if ! optimize_apk "$raw_apk" "$aligned_apk"; then
        exit $?
    fi
    
    echo ""
    
    # Sign APK
    if ! sign_apk "$aligned_apk" "$signed_apk"; then
        exit $?
    fi
    
    echo ""
    
    # Verify signature
    if ! verify_apk_signature "$signed_apk"; then
        exit $?
    fi
    
    echo ""
    
    # Verify integrity
    if ! verify_apk_integrity "$signed_apk"; then
        exit $?
    fi
    
    echo ""
    
    # Generate SBOM if requested
    generate_sbom
    
    # Generate build report
    generate_build_report "$signed_apk"
    
    # Clean up intermediate files
    rm -f "$aligned_apk"
    
    # Print summary
    print_header "Production Build Complete"
    
    log_success "Production build completed successfully!"
    echo ""
    echo "  Signed APK:   $signed_apk"
    echo "  APK Size:     $(format_size $(get_file_size "$signed_apk"))"
    echo "  APK SHA256:   $(compute_sha256 "$signed_apk")"
    echo "  Build Report: $OUTPUT_DIR/build_report_$BUILD_TIMESTAMP.txt"
    echo ""
    
    if [[ "$GENERATE_SBOM" == "true" ]]; then
        echo "  SBOM:         $OUTPUT_DIR/egyptian_agent_sbom_$BUILD_TIMESTAMP.json"
        echo ""
    fi
    
    log_info "Next steps:"
    echo "  1. Deploy to device: ./scripts/deploy/deploy_production.sh"
    echo "  2. Verify installation: ./scripts/utils/verify_implementation.sh"
    echo "  3. Run smoke tests manually on device"
    echo ""
    
    return 0
}

# Trap errors
trap 'log_error "Build interrupted"; exit 1' INT TERM

# Run main
main "$@"
exit $?
