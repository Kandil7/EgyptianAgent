#!/bin/bash
# =============================================================================
# FunctionGemma Test Execution Script
# =============================================================================
# 
# Usage:
#   ./scripts/run_functiongemma_tests.sh [OPTIONS]
#
# Options:
#   --all           Run all tests (unit + integration)
#   --unit          Run unit tests only
#   --integration   Run integration tests only (requires device)
#   --coverage      Generate code coverage report
#   --class NAME    Run specific test class
#   --category CAT  Run tests by category (CALL_CONTACT, SEND_WHATSAPP, etc.)
#   --help          Show this help message
#
# Examples:
#   ./scripts/run_functiongemma_tests.sh --all
#   ./scripts/run_functiongemma_tests.sh --unit --coverage
#   ./scripts/run_functiongemma_tests.sh --class FunctionGemmaIntentEngineTest
#   ./scripts/run_functiongemma_tests.sh --category CALL_CONTACT
#
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Test configuration
TEST_REPORT_DIR="$PROJECT_DIR/build/reports/tests"
COVERAGE_REPORT_DIR="$PROJECT_DIR/build/reports/coverage"
JUNIT_XML_DIR="$PROJECT_DIR/build/test-results"

# Default options
RUN_UNIT=false
RUN_INTEGRATION=false
RUN_COVERAGE=false
TEST_CLASS=""
TEST_CATEGORY=""

# =============================================================================
# Helper Functions
# =============================================================================

print_header() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

show_help() {
    cat << EOF
FunctionGemma Test Execution Script

Usage:
  $0 [OPTIONS]

Options:
  --all           Run all tests (unit + integration)
  --unit          Run unit tests only
  --integration   Run integration tests only (requires connected device)
  --coverage      Generate code coverage report
  --class NAME    Run specific test class
  --category CAT  Run tests by category
  --help          Show this help message

Examples:
  $0 --all                          # Run all tests
  $0 --unit --coverage              # Run unit tests with coverage
  $0 --class FunctionGemmaEngineTest  # Run specific test class
  $0 --category CALL_CONTACT        # Run tests for specific category

Output:
  Test Reports:  $TEST_REPORT_DIR
  Coverage:      $COVERAGE_REPORT_DIR
  JUnit XML:     $JUNIT_XML_DIR

EOF
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed"
        exit 1
    fi
    print_success "Java found: $(java -version 2>&1 | head -n 1)"
    
    # Check Gradle
    if [ ! -f "$PROJECT_DIR/gradlew" ]; then
        print_error "Gradle wrapper not found"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    # Check Android SDK (for integration tests)
    if [ "$RUN_INTEGRATION" = true ] || [ "$RUN_ALL" = true ]; then
        if [ -z "$ANDROID_HOME" ]; then
            print_error "ANDROID_HOME not set"
            exit 1
        fi
        print_success "Android SDK found: $ANDROID_HOME"
        
        # Check for connected device
        if ! adb devices | grep -q "device$"; then
            print_error "No Android device connected"
            exit 1
        fi
        print_success "Android device connected"
    fi
}

# =============================================================================
# Test Execution Functions
# =============================================================================

run_unit_tests() {
    print_header "Running Unit Tests"
    
    local gradle_args="testDebugUnitTest"
    
    if [ -n "$TEST_CLASS" ]; then
        gradle_args="$gradle_args --tests $TEST_CLASS"
        print_info "Running test class: $TEST_CLASS"
    fi
    
    if [ -n "$TEST_CATEGORY" ]; then
        gradle_args="$gradle_args --tests *$TEST_CATEGORY*"
        print_info "Running test category: $TEST_CATEGORY"
    fi
    
    cd "$PROJECT_DIR"
    
    print_info "Executing: ./gradlew $gradle_args"
    ./gradlew $gradle_args --info
    
    print_success "Unit tests completed"
}

run_integration_tests() {
    print_header "Running Integration Tests"
    
    cd "$PROJECT_DIR"
    
    print_info "Connecting to device..."
    adb wait-for-device
    
    print_info "Executing instrumented tests..."
    ./gradlew connectedAndroidTest --info
    
    print_success "Integration tests completed"
}

generate_coverage() {
    print_header "Generating Code Coverage Report"
    
    cd "$PROJECT_DIR"
    
    # Run tests with coverage
    ./gradlew testDebugUnitTest jacocoTestReport --info
    
    # Check if report was generated
    if [ -f "$COVERAGE_REPORT_DIR/html/index.html" ]; then
        print_success "Coverage report generated: $COVERAGE_REPORT_DIR/html/index.html"
        print_info "Open in browser to view detailed coverage"
    else
        print_error "Coverage report not found"
    fi
}

# =============================================================================
# Report Generation
# =============================================================================

generate_summary_report() {
    print_header "Test Summary Report"
    
    local test_results_dir="$JUNIT_XML_DIR/testDebugUnitTest"
    
    if [ -d "$test_results_dir" ]; then
        local total_tests=$(grep -r "tests=" "$test_results_dir"/*.xml 2>/dev/null | wc -l || echo "0")
        local failures=$(grep -r "failures=" "$test_results_dir"/*.xml 2>/dev/null | grep -v "failures=\"0\"" | wc -l || echo "0")
        local errors=$(grep -r "errors=" "$test_results_dir"/*.xml 2>/dev/null | grep -v "errors=\"0\"" | wc -l || echo "0")
        
        echo ""
        echo "Test Results Summary:"
        echo "  Total Test Classes: $total_tests"
        echo "  Classes with Failures: $failures"
        echo "  Classes with Errors: $errors"
        echo ""
        
        if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
            print_success "All tests passed!"
        else
            print_error "Some tests failed. Check detailed reports at: $TEST_REPORT_DIR"
        fi
    else
        print_info "No test results found yet"
    fi
}

# =============================================================================
# Main Execution
# =============================================================================

# Parse command line arguments
RUN_ALL=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            RUN_ALL=true
            RUN_UNIT=true
            RUN_INTEGRATION=true
            shift
            ;;
        --unit)
            RUN_UNIT=true
            shift
            ;;
        --integration)
            RUN_INTEGRATION=true
            shift
            ;;
        --coverage)
            RUN_COVERAGE=true
            shift
            ;;
        --class)
            TEST_CLASS="$2"
            shift 2
            ;;
        --category)
            TEST_CATEGORY="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# If no options specified, show help
if [ "$RUN_UNIT" = false ] && [ "$RUN_INTEGRATION" = false ] && [ "$RUN_COVERAGE" = false ]; then
    show_help
    exit 0
fi

# Main execution
print_header "FunctionGemma Test Suite"
echo "Project Directory: $PROJECT_DIR"
echo "Date: $(date)"
echo ""

check_prerequisites

if [ "$RUN_UNIT" = true ]; then
    run_unit_tests
fi

if [ "$RUN_INTEGRATION" = true ]; then
    run_integration_tests
fi

if [ "$RUN_COVERAGE" = true ]; then
    generate_coverage
fi

generate_summary_report

print_header "Test Execution Complete"
print_info "View detailed reports at:"
print_info "  - HTML Report: $TEST_REPORT_DIR"
print_info "  - Coverage: $COVERAGE_REPORT_DIR/html/index.html"
print_info "  - JUnit XML: $JUNIT_XML_DIR"
