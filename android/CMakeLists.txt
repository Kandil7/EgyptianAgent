# CMakeLists.txt for EgyptianAgent Native Libraries
# Supports both llama.cpp integration and mock fallback
# Target: Honor X6c (MediaTek Helio G81 Ultra)

cmake_minimum_required(VERSION 3.22.1)
project("egyptian_agent" VERSION 1.1.0 LANGUAGES C CXX)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Compiler flags for optimization on mobile
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -ffast-math -fno-exceptions -fno-rtti")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0 -DDEBUG")

# Android-specific flags
if(ANDROID)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
    
    # Enable NEON for ARM processors (Helio G81 Ultra)
    if(${ANDROID_ABI} MATCHES "armeabi")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
    endif()
endif()

# Define project root for path resolution
set(PROJECT_ROOT "${CMAKE_SOURCE_DIR}/..")
# CMAKE_SOURCE_DIR is the app/ module, so PROJECT_ROOT is the repo root and
# external/ sits directly beneath it. (Was "${PROJECT_ROOT}/../external",
# which pointed one level *outside* the repo and could never resolve.)
set(EXTERNAL_DIR "${PROJECT_ROOT}/external")

# Log library from NDK
find_library(log-lib log)
find_library(android-lib android)

# ============================================================================
# Build Options
# ============================================================================
option(USE_LLAMA_CPP "Enable llama.cpp integration" OFF)
option(USE_WHISPER "Enable whisper.cpp integration" OFF)
option(BUILD_SHARED_LIBS "Build shared libraries" ON)

# Mock native inference produces a build that LOOKS successful but cannot run
# any model. It must therefore be opted into explicitly; otherwise a missing
# submodule is a hard error rather than a warning nobody reads.
option(ALLOW_MOCK_NATIVE "Permit falling back to stub native inference" OFF)

message(STATUS "Egyptian Agent Native Build")
message(STATUS "  Project Root: ${PROJECT_ROOT}")
message(STATUS "  External Dir: ${EXTERNAL_DIR}")
message(STATUS "  Android ABI: ${ANDROID_ABI}")
message(STATUS "  USE_LLAMA_CPP: ${USE_LLAMA_CPP}")
message(STATUS "  USE_WHISPER: ${USE_WHISPER}")

# ============================================================================
# llama.cpp Integration (Optional)
# ============================================================================
if(USE_LLAMA_CPP)
    set(LLAMA_CPP_DIR "${EXTERNAL_DIR}/llama.cpp")
    
    if(EXISTS "${LLAMA_CPP_DIR}/CMakeLists.txt")
        message(STATUS "Building with llama.cpp integration")
        
        # llama.cpp build options - optimized for mobile
        set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
        set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
        set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
        set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
        set(LLAMA_NATIVE OFF CACHE BOOL "" FORCE)
        set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
        set(LLAMA_METAL OFF CACHE BOOL "" FORCE)
        set(LLAMA_CUDA OFF CACHE BOOL "" FORCE)
        set(LLAMA_VULKAN OFF CACHE BOOL "" FORCE)
        set(LLAMA_OPENMP OFF CACHE BOOL "" FORCE)
        
        # Add llama.cpp as subdirectory
        add_subdirectory(${LLAMA_CPP_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp)
        
        # Create the native library wrapper
        add_library(
            llama_native
            SHARED
            src/main/cpp/llama_native.cpp
        )
        
        target_link_libraries(
            llama_native
            llama
            ggml
            ${log-lib}
            ${android-lib}
        )
        
        target_include_directories(
            llama_native
            PRIVATE
            ${LLAMA_CPP_DIR}
            ${LLAMA_CPP_DIR}/common
            ${LLAMA_CPP_DIR}/ggml/include
            ${LLAMA_CPP_DIR}/include
        )
        
        target_compile_definitions(
            llama_native
            PRIVATE
            USE_LLAMA_CPP=1
            GGML_USE_CPU=1
        )
        
        # Set target properties for Android
        set_target_properties(llama_native PROPERTIES
            CXX_STANDARD 17
            POSITION_INDEPENDENT_CODE ON
        )
        
    elseif(ALLOW_MOCK_NATIVE)
        message(WARNING "llama.cpp not found at ${LLAMA_CPP_DIR}")
        message(WARNING "Falling back to MOCK implementation (ALLOW_MOCK_NATIVE=ON)")
        set(USE_LLAMA_CPP OFF)
    else()
        message(FATAL_ERROR
            "llama.cpp not found at ${LLAMA_CPP_DIR}.\n"
            "Run: git submodule update --init --recursive\n"
            "Or configure with -DALLOW_MOCK_NATIVE=ON to build a non-functional stub.")
    endif()
endif()

# ============================================================================
# Mock llama.cpp Implementation (Default)
# ============================================================================
if(NOT USE_LLAMA_CPP)
    message(STATUS "Building MOCK llama_native implementation")
    
    add_library(
        llama_native
        SHARED
        src/main/cpp/llama_native.cpp
    )
    
    target_link_libraries(
        llama_native
        ${log-lib}
        ${android-lib}
    )
    
    target_compile_definitions(
        llama_native
        PRIVATE
        USE_LLAMA_CPP=0
        MOCK_IMPLEMENTATION=1
    )
    
    target_include_directories(
        llama_native
        PRIVATE
        src/main/cpp
    )
endif()

# ============================================================================
# whisper.cpp Integration (Optional)
# ============================================================================
# Check if whisper.cpp exists
set(WHISPER_CPP_DIR "${EXTERNAL_DIR}/whisper.cpp")

# Respect USE_WHISPER: presence of the submodule must not silently opt a stub
# build into compiling it (mirrors the llama.cpp branch above).
if(USE_WHISPER AND EXISTS "${WHISPER_CPP_DIR}/CMakeLists.txt")
    message(STATUS "Found whisper.cpp, enabling Whisper ASR")

    # Disable tests and examples for production build
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(WHISPER_METAL OFF CACHE BOOL "" FORCE)
    set(WHISPER_CUDA OFF CACHE BOOL "" FORCE)
    set(WHISPER_SYCL OFF CACHE BOOL "" FORCE)
    set(WHISPER_OPENVINO OFF CACHE BOOL "" FORCE)
    set(WHISPER_COREML OFF CACHE BOOL "" FORCE)
    
    # Add whisper.cpp as subdirectory
    add_subdirectory(${WHISPER_CPP_DIR} ${CMAKE_CURRENT_BINARY_DIR}/whisper.cpp)
    
    # Create whisper_native library
    add_library(whisper_native SHARED src/main/cpp/whisper_native.cpp)
    
    target_link_libraries(whisper_native 
        whisper
        ggml
        ${log-lib}
        ${android-lib}
    )
    
    target_include_directories(whisper_native PRIVATE
        ${WHISPER_CPP_DIR}
        ${WHISPER_CPP_DIR}/include
        ${WHISPER_CPP_DIR}/ggml/include
    )
    
    target_compile_definitions(whisper_native PRIVATE
        USE_WHISPER_CPP=1
        WHISPER_BUILD=1
    )
    
    # Set target properties for Android
    set_target_properties(whisper_native PROPERTIES
        CXX_STANDARD 17
        POSITION_INDEPENDENT_CODE ON
    )
    
    message(STATUS "  whisper_native: FULL implementation with whisper.cpp")
else()
    # USE_WHISPER was requested but the submodule is absent -> hard failure.
    if(USE_WHISPER AND NOT ALLOW_MOCK_NATIVE)
        message(FATAL_ERROR
            "whisper.cpp not found at ${WHISPER_CPP_DIR}.\n"
            "Run: git submodule update --init --recursive\n"
            "Or configure with -DALLOW_MOCK_NATIVE=ON to build a non-functional stub.")
    endif()
    message(WARNING "Building MOCK whisper_native - ASR will NOT work "
                    "(USE_WHISPER=${USE_WHISPER}, ALLOW_MOCK_NATIVE=${ALLOW_MOCK_NATIVE})")
    set(USE_WHISPER OFF)

    # Build mock whisper_native
    add_library(whisper_native SHARED src/main/cpp/whisper_native.cpp)
    target_link_libraries(whisper_native ${log-lib} ${android-lib})
    target_compile_definitions(whisper_native PRIVATE
        USE_WHISPER_CPP=0
        MOCK_IMPLEMENTATION=1
    )
endif()

# ============================================================================
# Output Summary
# ============================================================================
message(STATUS "")
if(USE_LLAMA_CPP)
    message(STATUS "  llama_native: Will be built (full implementation)")
else()
    message(STATUS "  llama_native: Will be built (mock implementation)")
endif()
if(USE_WHISPER)
    message(STATUS "  whisper_native: Will be built (full implementation)")
else()
    message(STATUS "  whisper_native: Will be built (mock implementation)")
endif()
message(STATUS "  Optimization: ${CMAKE_BUILD_TYPE}")
message(STATUS "====================================")