# CMakeLists.txt for EgyptianAgent Native Libraries
# Supports both llama.cpp integration and mock fallback

cmake_minimum_required(VERSION 3.22.1)
project("egyptian_agent_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -ffast-math")

# Option to enable llama.cpp (set via gradle build config)
option(USE_LLAMA_CPP "Enable llama.cpp integration" OFF)

# Log library from NDK
find_library(log-lib log)

# ============================================================================
# llama.cpp Integration (Optional)
# ============================================================================
if(USE_LLAMA_CPP)
    message(STATUS "Building with llama.cpp integration")
    
    # Check if llama.cpp submodule exists
    if(EXISTS "${CMAKE_SOURCE_DIR}/external/llama.cpp/CMakeLists.txt")
        # Add llama.cpp as subdirectory
        set(BUILD_SHARED_LIBS OFF)
        set(LLAMA_BUILD_TESTS OFF)
        set(LLAMA_BUILD_EXAMPLES OFF)
        set(LLAMA_BUILD_SERVER OFF)
        
        add_subdirectory(external/llama.cpp llama.cpp)
        
        # Create the native library with llama.cpp
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
            android
        )
        
        target_include_directories(
            llama_native
            PRIVATE
            external/llama.cpp
            external/llama.cpp/common
            external/llama.cpp/ggml/include
        )
        
        target_compile_definitions(
            llama_native
            PRIVATE
            USE_LLAMA_CPP=1
        )
    else()
        message(WARNING "llama.cpp not found at external/llama.cpp")
        message(WARNING "Building with MOCK implementation only")
        message(WARNING "To enable full Llama functionality:")
        message(WARNING "  git submodule add https://github.com/ggerganov/llama.cpp external/llama.cpp")
        message(WARNING "  git submodule update --init --recursive")
        
        # Fall through to mock implementation
        set(USE_LLAMA_CPP OFF)
    endif()
endif()

# ============================================================================
# Mock Implementation (Default when llama.cpp not available)
# ============================================================================
if(NOT USE_LLAMA_CPP)
    message(STATUS "Building MOCK llama_native implementation (llama.cpp not enabled)")
    
    add_library(
        llama_native
        SHARED
        src/main/cpp/llama_native.cpp
    )
    
    target_link_libraries(
        llama_native
        ${log-lib}
        android
    )
    
    target_compile_definitions(
        llama_native
        PRIVATE
        USE_LLAMA_CPP=0
    )
endif()

# ============================================================================
# Whisper Native (Optional - similar pattern)
# ============================================================================
option(USE_WHISPER "Enable whisper.cpp integration" OFF)

if(USE_WHISPER)
    message(STATUS "Building with whisper.cpp integration")
    
    if(EXISTS "${CMAKE_SOURCE_DIR}/external/whisper.cpp/CMakeLists.txt")
        set(WHISPER_BUILD_TESTS OFF)
        set(WHISPER_BUILD_EXAMPLES OFF)
        
        add_subdirectory(external/whisper.cpp whisper.cpp)
        
        add_library(
            whisper_native
            SHARED
            src/main/cpp/whisper_native.cpp
        )
        
        target_link_libraries(
            whisper_native
            whisper
            ggml
            ${log-lib}
            android
        )
        
        target_include_directories(
            whisper_native
            PRIVATE
            external/whisper.cpp
        )
    else()
        message(WARNING "whisper.cpp not found, building mock implementation")
        set(USE_WHISPER OFF)
    endif()
endif()

if(NOT USE_WHISPER)
    message(STATUS "Building MOCK whisper_native implementation")
    
    # whisper_native mock would be implemented similarly
endif()
