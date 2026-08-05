# CMakeLists.txt for FunctionGemma Native Library
# Builds llama.cpp integration for FunctionGemma-270M-IT inference engine
#
# Target: Android (armeabi-v7a, arm64-v8a)
# Model: google/functiongemma-270m-it (GGUF Q4_K_M quantized)
#
# Usage:
#   cmake -S . -B build \
#     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
#     -DANDROID_ABI=arm64-v8a \
#     -DANDROID_PLATFORM=android-21 \
#     -DCMAKE_BUILD_TYPE=Release

cmake_minimum_required(VERSION 3.22.1)
project("functiongemma_jni" VERSION 1.0.0 LANGUAGES CXX C)

# ============================================================================
# C++ Standard and Compiler Flags
# ============================================================================
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Compiler optimization flags for mobile performance
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -ffast-math -fno-strict-aliasing")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g -DDEBUG")

# Android-specific flags
if(ANDROID)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
    
    # Optimize for ARM NEON (available on all modern Android devices)
    if(${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "armeabi-v7a")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+fp+simd")
    endif()
endif()

# ============================================================================
# Build Options
# ============================================================================
option(USE_LLAMA_CPP "Enable llama.cpp integration for FunctionGemma" ON)
option(USE_OPENMP "Enable OpenMP for multi-threading" OFF)
option(BUILD_SHARED_LIBS "Build shared libraries" ON)
option(FUNCTIONGEMMA_VERBOSE_LOGGING "Enable verbose logging for debugging" OFF)

# ============================================================================
# Find Required Libraries
# ============================================================================
find_library(log-lib log)
find_library(android-lib android)

# ============================================================================
# llama.cpp Integration
# ============================================================================
if(USE_LLAMA_CPP)
    message(STATUS "==============================================")
    message(STATUS "Building FunctionGemma with llama.cpp backend")
    message(STATUS "==============================================")

    # Check if llama.cpp exists as submodule or external directory
    set(LLAMA_CPP_PATHS
        "${CMAKE_SOURCE_DIR}/../../../external/llama.cpp"
        "${CMAKE_SOURCE_DIR}/../../../../external/llama.cpp"
        "${CMAKE_SOURCE_DIR}/llama.cpp"
        "${CMAKE_SOURCE_DIR}/external/llama.cpp"
    )

    set(LLAMA_CPP_FOUND FALSE)
    foreach(LLAMA_PATH ${LLAMA_CPP_PATHS})
        if(EXISTS "${LLAMA_PATH}/CMakeLists.txt")
            set(LLAMA_CPP_DIR "${LLAMA_PATH}")
            set(LLAMA_CPP_FOUND TRUE)
            message(STATUS "Found llama.cpp at: ${LLAMA_CPP_DIR}")
            break()
        endif()
    endforeach()

    if(LLAMA_CPP_FOUND)
        # Configure llama.cpp build options for mobile optimization
        set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build llama.cpp as static lib" FORCE)
        set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build llama.cpp tests" FORCE)
        set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build llama.cpp examples" FORCE)
        set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build llama.cpp server" FORCE)
        set(LLAMA_CURL OFF CACHE BOOL "Build with curl support" FORCE)
        
        # Mobile-specific optimizations
        set(LLAMA_NATIVE OFF CACHE BOOL "Disable native optimizations for cross-compile" FORCE)
        set(LLAMA_OPENMP ${USE_OPENMP} CACHE BOOL "Enable OpenMP" FORCE)
        
        # Quantization support for GGUF models
        set(LLAMA_K_QUANTS ON CACHE BOOL "Enable K-quants for Q4_K_M models" FORCE)
        
        # Add llama.cpp as subdirectory
        add_subdirectory(${LLAMA_CPP_DIR} ${CMAKE_BINARY_DIR}/llama.cpp)

        # Create FunctionGemma JNI library
        add_library(
            functiongemma_jni
            SHARED
            functiongemma_jni.cpp
        )

        # Link against llama.cpp libraries
        target_link_libraries(
            functiongemma_jni
            llama
            ggml_base
            ggml-cpu
            ${log-lib}
            ${android-lib}
        )

        # Include directories for llama.cpp headers
        target_include_directories(
            functiongemma_jni
            PRIVATE
            ${LLAMA_CPP_DIR}
            ${LLAMA_CPP_DIR}/common
            ${LLAMA_CPP_DIR}/ggml/include
            ${LLAMA_CPP_DIR}/include
        )

        # Compile definitions
        target_compile_definitions(
            functiongemma_jni
            PRIVATE
            USE_LLAMA_CPP=1
            GGML_USE_CPU=1
            $<$<CONFIG:Debug>:FUNCTIONGEMMA_DEBUG=1>
            $<$<BOOL:${FUNCTIONGEMMA_VERBOSE_LOGGING}>:FUNCTIONGEMMA_VERBOSE=1>
        )

        # Set library properties
        set_target_properties(
            functiongemma_jni
            PROPERTIES
            OUTPUT_NAME "functiongemma"
            VERSION ${PROJECT_VERSION}
            SOVERSION 1
            CXX_STANDARD 17
            CXX_STANDARD_REQUIRED ON
        )

        message(STATUS "FunctionGemma JNI library configured successfully")
        message(STATUS "  Output: libfunctiongemma.so")
        message(STATUS "  llama.cpp: ${LLAMA_CPP_DIR}")
        message(STATUS "  OpenMP: ${USE_OPENMP}")

    else()
        message(WARNING "==============================================")
        message(WARNING "llama.cpp NOT FOUND - Building MOCK implementation")
        message(WARNING "==============================================")
        message(WARNING "To enable full FunctionGemma functionality:")
        message(WARNING "  git submodule update --init --recursive")
        message(WARNING "Or manually clone llama.cpp:")
        message(WARNING "  git clone https://github.com/ggerganov/llama.cpp external/llama.cpp")
        message(WARNING "==============================================")

        # Fall through to mock implementation
        set(USE_LLAMA_CPP OFF)
    endif()
endif()

# ============================================================================
# Mock Implementation (Fallback when llama.cpp not available)
# ============================================================================
if(NOT USE_LLAMA_CPP)
    message(STATUS "==============================================")
    message(STATUS "Building FunctionGemma MOCK implementation")
    message(STATUS "==============================================")
    message(STATUS "This provides fallback behavior for testing")
    message(STATUS "without the full llama.cpp backend")
    message(STATUS "==============================================")

    add_library(
        functiongemma_jni
        SHARED
        functiongemma_jni.cpp
    )

    target_link_libraries(
        functiongemma_jni
        ${log-lib}
        ${android-lib}
    )

    target_compile_definitions(
        functiongemma_jni
        PRIVATE
        USE_LLAMA_CPP=0
        FUNCTIONGEMMA_MOCK=1
    )

    set_target_properties(
        functiongemma_jni
        PROPERTIES
        OUTPUT_NAME "functiongemma"
        VERSION ${PROJECT_VERSION}
        SOVERSION 1
    )

    message(STATUS "Mock FunctionGemma JNI library configured")
    message(STATUS "  Output: libfunctiongemma.so (mock mode)")
endif()

# ============================================================================
# Installation Rules
# ============================================================================
install(
    TARGETS functiongemma_jni
    LIBRARY DESTINATION ${CMAKE_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
    ARCHIVE DESTINATION ${CMAKE_SOURCE_DIR}/../../jniLibs/${ANDROID_ABI}
)

# ============================================================================
# Build Summary
# ============================================================================
message(STATUS "==============================================")
message(STATUS "FunctionGemma Build Configuration Summary")
message(STATUS "==============================================")
message(STATUS "Project: ${PROJECT_NAME} v${PROJECT_VERSION}")
message(STATUS "Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "Android ABI: ${ANDROID_ABI}")
message(STATUS "Android Platform: ${ANDROID_PLATFORM}")
message(STATUS "C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "USE_LLAMA_CPP: ${USE_LLAMA_CPP}")
message(STATUS "USE_OPENMP: ${USE_OPENMP}")
message(STATUS "VERBOSE_LOGGING: ${FUNCTIONGEMMA_VERBOSE_LOGGING}")
message(STATUS "==============================================")
