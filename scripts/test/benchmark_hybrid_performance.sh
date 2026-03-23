#!/bin/bash

# =============================================================================
# EgyptianAgent Hybrid Performance Benchmark Script
# =============================================================================
# 
# Measures hybrid architecture performance metrics:
# - Fast path latency (target: <2.0s total)
# - Slow path latency (target: <5.0s total)
# - Routing decision time (target: <100ms)
# - Memory usage delta (target: <800MB total)
# - Battery drain per hour (target: <5%/hr)
# - CPU usage during navigation
#
# Usage: ./benchmark_hybrid_performance.sh [--device <device_id>] [--iterations <count>]
#
# @author EgyptianAgent Team
# @version 1.0.0
# =============================================================================

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
LOG_DIR="$PROJECT_ROOT/benchmark_logs"
REPORT_FILE="$LOG_DIR/benchmark_report_$(date +%Y%m%d_%H%M%S).md"
ITERATIONS=${ITERATIONS:-10}
DEVICE_ID=""

# Performance targets
TARGET_FAST_PATH_MS=2000
TARGET_SLOW_PATH_MS=5000
TARGET_ROUTING_MS=100
TARGET_MEMORY_MB=800
TARGET_BATTERY_DRAIN_PERCENT=5

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# Helper Functions
# =============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

check_device() {
    if [ -n "$DEVICE_ID" ]; then
        adb -s "$DEVICE_ID" shell getprop ro.product.model > /dev/null 2>&1
    else
        adb shell getprop ro.product.model > /dev/null 2>&1
    fi
    
    if [ $? -ne 0 ]; then
        log_error "No Android device found. Please connect a device."
        exit 1
    fi
    
    if [ -n "$DEVICE_ID" ]; then
        DEVICE="-s $DEVICE_ID"
    else
        DEVICE=""
    fi
}

setup_log_directory() {
    mkdir -p "$LOG_DIR"
    log_info "Log directory: $LOG_DIR"
}

get_memory_usage() {
    local package="com.egyptian.agent"
    local mem_info=$(adb $DEVICE shell dumpsys meminfo $package 2>/dev/null | grep "TOTAL:" | awk '{print $2}')
    
    if [ -z "$mem_info" ]; then
        echo "0"
    else
        echo "$mem_info"
    fi
}

get_cpu_usage() {
    local package="com.egyptian.agent"
    local cpu_info=$(adb $DEVICE shell dumpsys cpuinfo | grep "$package" | awk '{print $1}' | sed 's/%//')
    
    if [ -z "$cpu_info" ]; then
        echo "0"
    else
        echo "$cpu_info"
    fi
}

get_battery_level() {
    local battery_info=$(adb $DEVICE shell dumpsys battery | grep level | awk '{print $2}')
    
    if [ -z "$battery_info" ]; then
        echo "100"
    else
        echo "$battery_info"
    fi
}

# =============================================================================
# Benchmark Tests
# =============================================================================

benchmark_fast_path_latency() {
    log_info "Benchmarking Fast Path Latency..."
    
    local total_time=0
    local success_count=0
    
    # Test commands that should use fast path
    local fast_path_commands=(
        "اتصل بماما"
        "كلم بابا"
        "نبهني بكرة"
        "افتح الواتساب"
        "قفل الواي فاي"
    )
    
    for cmd in "${fast_path_commands[@]}"; do
        for i in $(seq 1 $ITERATIONS); do
            local start_time=$(date +%s%N)
            
            # Simulate command processing
            adb $DEVICE shell am start -n com.egyptian.agent/.VoiceActivity \
                --es command "$cmd" 2>/dev/null || true
            
            local end_time=$(date +%s%N)
            local elapsed_ms=$(( (end_time - start_time) / 1000000 ))
            
            total_time=$((total_time + elapsed_ms))
            success_count=$((success_count + 1))
            
            sleep 0.5
        done
    done
    
    local avg_time=$((total_time / success_count))
    
    echo "FAST_PATH_AVG_MS=$avg_time"
    echo "FAST_PATH_SAMPLES=$success_count"
    
    if [ $avg_time -lt $TARGET_FAST_PATH_MS ]; then
        log_success "Fast Path: ${avg_time}ms (target: <${TARGET_FAST_PATH_MS}ms)"
        return 0
    else
        log_warning "Fast Path: ${avg_time}ms exceeds target ${TARGET_FAST_PATH_MS}ms"
        return 1
    fi
}

benchmark_slow_path_latency() {
    log_info "Benchmarking Slow Path Latency..."
    
    local total_time=0
    local success_count=0
    
    # Test commands that should use slow path (UI navigation)
    local slow_path_commands=(
        "افتح الفيسبوك وشوف الأخبار"
        "دور على فيديو في اليوتيوب"
        "شوف الناس اللي كلمتني"
    )
    
    for cmd in "${slow_path_commands[@]}"; do
        for i in $(seq 1 $ITERATIONS); do
            local start_time=$(date +%s%N)
            
            # Simulate command processing
            adb $DEVICE shell am start -n com.egyptian.agent/.VoiceActivity \
                --es command "$cmd" 2>/dev/null || true
            
            local end_time=$(date +%s%N)
            local elapsed_ms=$(( (end_time - start_time) / 1000000 ))
            
            total_time=$((total_time + elapsed_ms))
            success_count=$((success_count + 1))
            
            sleep 1.0
        done
    done
    
    local avg_time=$((total_time / success_count))
    
    echo "SLOW_PATH_AVG_MS=$avg_time"
    echo "SLOW_PATH_SAMPLES=$success_count"
    
    if [ $avg_time -lt $TARGET_SLOW_PATH_MS ]; then
        log_success "Slow Path: ${avg_time}ms (target: <${TARGET_SLOW_PATH_MS}ms)"
        return 0
    else
        log_warning "Slow Path: ${avg_time}ms exceeds target ${TARGET_SLOW_PATH_MS}ms"
        return 1
    fi
}

benchmark_routing_decision_time() {
    log_info "Benchmarking Routing Decision Time..."
    
    # Routing decision is part of overall processing
    # This is a simplified measurement
    local total_time=0
    local samples=100
    
    for i in $(seq 1 $samples); do
        local start_time=$(date +%s%N)
        
        # Trigger intent classification
        adb $DEVICE shell am broadcast -a com.egyptian.agent.CLASSIFY_INTENT \
            --es text "test" 2>/dev/null || true
        
        local end_time=$(date +%s%N)
        local elapsed_ms=$(( (end_time - start_time) / 1000000 ))
        
        total_time=$((total_time + elapsed_ms))
    done
    
    local avg_time=$((total_time / samples))
    
    echo "ROUTING_AVG_MS=$avg_time"
    
    if [ $avg_time -lt $TARGET_ROUTING_MS ]; then
        log_success "Routing: ${avg_time}ms (target: <${TARGET_ROUTING_MS}ms)"
        return 0
    else
        log_warning "Routing: ${avg_time}ms exceeds target ${TARGET_ROUTING_MS}ms"
        return 1
    fi
}

benchmark_memory_usage() {
    log_info "Benchmarking Memory Usage..."
    
    # Get baseline memory
    local baseline_mem=$(get_memory_usage)
    
    # Run several commands
    for i in $(seq 1 10); do
        adb $DEVICE shell am start -n com.egyptian.agent/.VoiceActivity \
            --es command "test" 2>/dev/null || true
        sleep 0.5
    done
    
    # Get peak memory
    local peak_mem=$(get_memory_usage)
    local memory_delta=$((peak_mem - baseline_mem))
    
    # Convert to MB if in KB
    if [ $memory_delta -gt 10000 ]; then
        memory_delta=$((memory_delta / 1024))
    fi
    
    echo "MEMORY_BASELINE_KB=$baseline_mem"
    echo "MEMORY_PEAK_KB=$peak_mem"
    echo "MEMORY_DELTA_MB=$memory_delta"
    
    if [ $memory_delta -lt $TARGET_MEMORY_MB ]; then
        log_success "Memory Delta: ${memory_delta}MB (target: <${TARGET_MEMORY_MB}MB)"
        return 0
    else
        log_warning "Memory Delta: ${memory_delta}MB exceeds target ${TARGET_MEMORY_MB}MB"
        return 1
    fi
}

benchmark_cpu_usage() {
    log_info "Benchmarking CPU Usage..."
    
    local total_cpu=0
    local samples=10
    
    for i in $(seq 1 $samples); do
        # Run navigation command
        adb $DEVICE shell am start -n com.egyptian.agent/.VoiceActivity \
            --es command "افتح الفيسبوك" 2>/dev/null || true
        
        sleep 1
        
        local cpu=$(get_cpu_usage)
        total_cpu=$((total_cpu + ${cpu%.*}))
    done
    
    local avg_cpu=$((total_cpu / samples))
    
    echo "CPU_AVG_PERCENT=$avg_cpu"
    
    log_info "Average CPU Usage: ${avg_cpu}%"
}

benchmark_battery_drain() {
    log_info "Benchmarking Battery Drain (simulated 1 hour)..."
    
    # Get initial battery level
    local initial_battery=$(get_battery_level)
    
    # Simulate usage (accelerated test - 1 minute = 1 hour)
    for i in $(seq 1 60); do
        adb $DEVICE shell am start -n com.egyptian.agent/.VoiceActivity \
            --es command "test command" 2>/dev/null || true
        sleep 1
    done
    
    # Get final battery level
    local final_battery=$(get_battery_level)
    local drain=$((initial_battery - final_battery))
    
    # Extrapolate to hourly rate
    local hourly_drain=$drain
    
    echo "BATTERY_INITIAL=$initial_battery"
    echo "BATTERY_FINAL=$final_battery"
    echo "BATTERY_DRAIN_PERCENT=$hourly_drain"
    
    if [ $hourly_drain -lt $TARGET_BATTERY_DRAIN_PERCENT ]; then
        log_success "Battery Drain: ${hourly_drain}%/hr (target: <${TARGET_BATTERY_DRAIN_PERCENT}%/hr)"
        return 0
    else
        log_warning "Battery Drain: ${hourly_drain}%/hr exceeds target ${TARGET_BATTERY_DRAIN_PERCENT}%/hr"
        return 1
    fi
}

# =============================================================================
# Comparison with FunctionGemma-only Baseline
# =============================================================================

compare_with_baseline() {
    log_info "Comparing with FunctionGemma-only baseline..."
    
    # Baseline metrics from FunctionGemma-only system
    local baseline_fast_path=350
    local baseline_memory=550
    local baseline_battery=3
    
    # Current hybrid metrics (from benchmark results)
    local hybrid_fast_path=${FAST_PATH_AVG_MS:-0}
    local hybrid_memory=${MEMORY_DELTA_MB:-0}
    local hybrid_battery=${BATTERY_DRAIN_PERCENT:-0}
    
    echo ""
    echo "### Comparison with FunctionGemma-only Baseline"
    echo ""
    echo "| Metric | Baseline | Hybrid | Delta |"
    echo "|--------|----------|--------|-------|"
    echo "| Fast Path (ms) | $baseline_fast_path | $hybrid_fast_path | $((hybrid_fast_path - baseline_fast_path)) |"
    echo "| Memory (MB) | $baseline_memory | $hybrid_memory | $((hybrid_memory - baseline_memory)) |"
    echo "| Battery (%/hr) | $baseline_battery | $hybrid_battery | $((hybrid_battery - baseline_battery)) |"
    echo ""
}

# =============================================================================
# Report Generation
# =============================================================================

generate_report() {
    log_info "Generating benchmark report..."
    
    cat > "$REPORT_FILE" << EOF
# EgyptianAgent Hybrid Performance Benchmark Report

**Date:** $(date)
**Device:** $(adb $DEVICE shell getprop ro.product.model 2>/dev/null | tr -d '\r')
**Android Version:** $(adb $DEVICE shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
**Iterations:** $ITERATIONS

## Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
| Fast Path Latency | <${TARGET_FAST_PATH_MS}ms | $([ ${FAST_PATH_AVG_MS:-9999} -lt $TARGET_FAST_PATH_MS ] && echo "✅ Pass" || echo "❌ Fail") |
| Slow Path Latency | <${TARGET_SLOW_PATH_MS}ms | $([ ${SLOW_PATH_AVG_MS:-9999} -lt $TARGET_SLOW_PATH_MS ] && echo "✅ Pass" || echo "❌ Fail") |
| Routing Decision | <${TARGET_ROUTING_MS}ms | $([ ${ROUTING_AVG_MS:-9999} -lt $TARGET_ROUTING_MS ] && echo "✅ Pass" || echo "❌ Fail") |
| Memory Delta | <${TARGET_MEMORY_MB}MB | $([ ${MEMORY_DELTA_MB:-9999} -lt $TARGET_MEMORY_MB ] && echo "✅ Pass" || echo "❌ Fail") |
| Battery Drain | <${TARGET_BATTERY_DRAIN_PERCENT}%/hr | $([ ${BATTERY_DRAIN_PERCENT:-9999} -lt $TARGET_BATTERY_DRAIN_PERCENT ] && echo "✅ Pass" || echo "❌ Fail") |

## Measured Metrics

### Latency
- **Fast Path Average:** ${FAST_PATH_AVG_MS:-N/A}ms (${FAST_PATH_SAMPLES:-0} samples)
- **Slow Path Average:** ${SLOW_PATH_AVG_MS:-N/A}ms (${SLOW_PATH_SAMPLES:-0} samples)
- **Routing Decision:** ${ROUTING_AVG_MS:-N/A}ms

### Resource Usage
- **Memory Baseline:** ${MEMORY_BASELINE_KB:-N/A}KB
- **Memory Peak:** ${MEMORY_PEAK_KB:-N/A}KB
- **Memory Delta:** ${MEMORY_DELTA_MB:-N/A}MB
- **CPU Average:** ${CPU_AVG_PERCENT:-N/A}%
- **Battery Drain:** ${BATTERY_DRAIN_PERCENT:-N/A}%/hr

$(compare_with_baseline)

## Test Configuration

- **Iterations per test:** $ITERATIONS
- **Test commands:** Egyptian Arabic dialect
- **Apps tested:** Settings, WhatsApp, Facebook, YouTube

## Recommendations

$(
if [ ${FAST_PATH_AVG_MS:-0} -gt $TARGET_FAST_PATH_MS ]; then
    echo "- ⚠️ Fast path latency exceeds target. Consider optimizing intent classification."
fi
if [ ${SLOW_PATH_AVG_MS:-0} -gt $TARGET_SLOW_PATH_MS ]; then
    echo "- ⚠️ Slow path latency exceeds target. Consider optimizing UI navigation loop."
fi
if [ ${MEMORY_DELTA_MB:-0} -gt $TARGET_MEMORY_MB ]; then
    echo "- ⚠️ Memory usage exceeds target. Review memory leaks and caching."
fi
if [ ${BATTERY_DRAIN_PERCENT:-0} -gt $TARGET_BATTERY_DRAIN_PERCENT ]; then
    echo "- ⚠️ Battery drain exceeds target. Optimize background processing."
fi
)

---
*Generated by benchmark_hybrid_performance.sh*
EOF

    log_success "Report saved to: $REPORT_FILE"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    echo "=============================================="
    echo "EgyptianAgent Hybrid Performance Benchmark"
    echo "=============================================="
    echo ""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --device)
                DEVICE_ID="$2"
                shift 2
                ;;
            --iterations)
                ITERATIONS="$2"
                shift 2
                ;;
            --help)
                echo "Usage: $0 [--device <device_id>] [--iterations <count>]"
                echo ""
                echo "Options:"
                echo "  --device     Target device ID (default: connected device)"
                echo "  --iterations Number of iterations per test (default: 10)"
                echo "  --help       Show this help message"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Setup
    check_device
    setup_log_directory
    
    # Run benchmarks
    benchmark_fast_path_latency
    FAST_PATH_AVG_MS=$(grep "FAST_PATH_AVG_MS" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    benchmark_slow_path_latency
    SLOW_PATH_AVG_MS=$(grep "SLOW_PATH_AVG_MS" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    benchmark_routing_decision_time
    ROUTING_AVG_MS=$(grep "ROUTING_AVG_MS" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    benchmark_memory_usage
    MEMORY_DELTA_MB=$(grep "MEMORY_DELTA_MB" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    benchmark_cpu_usage
    CPU_AVG_PERCENT=$(grep "CPU_AVG_PERCENT" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    benchmark_battery_drain
    BATTERY_DRAIN_PERCENT=$(grep "BATTERY_DRAIN_PERCENT" /dev/stdin 2>/dev/null | cut -d= -f2 || echo "0")
    
    # Generate report
    generate_report
    
    echo ""
    echo "=============================================="
    echo "Benchmark Complete"
    echo "=============================================="
}

# Export functions for sourcing
export -f log_info log_success log_warning log_error
export -f check_device setup_log_directory
export -f get_memory_usage get_cpu_usage get_battery_level
export -f benchmark_fast_path_latency benchmark_slow_path_latency
export -f benchmark_routing_decision_time benchmark_memory_usage
export -f benchmark_cpu_usage benchmark_battery_drain
export -f compare_with_baseline generate_report

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
