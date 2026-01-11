# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required for your project.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("egyptian_agent")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Check if llama.cpp exists and set flag accordingly
if(EXISTS "${CMAKE_SOURCE_DIR}/../../external/llama.cpp/CMakeLists.txt")
    set(HAVE_LLAMA_CPP TRUE)
    message(STATUS "Found llama.cpp, enabling full Llama functionality")
else()
    set(HAVE_LLAMA_CPP FALSE)
    message(WARNING "llama.cpp not found, building with mock implementation")
endif()

# Check if faster-whisper exists and set flag accordingly
if(EXISTS "${CMAKE_SOURCE_DIR}/../../external/faster-whisper/CMakeLists.txt")
    set(HAVE_FASTER_WHISPER TRUE)
    message(STATUS "Found faster-whisper, enabling Whisper ASR functionality")
else()
    set(HAVE_FASTER_WHISPER FALSE)
    message(WARNING "faster-whisper not found, building without Whisper ASR")
endif()

# Create the native libraries
add_library(
    # Sets the name of the library.
    llama_native

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    src/main/cpp/llama_native.cpp
)

# Add Whisper native library if available
if(HAVE_FASTER_WHISPER)
    add_library(
        egyptian_whisper

        SHARED

        src/main/cpp/whisper_native.cpp
    )
endif()

# Find required libraries
find_library(log-lib log)

if(HAVE_LLAMA_CPP)
    # Add llama.cpp as a subdirectory
    add_subdirectory(${CMAKE_SOURCE_DIR}/../../external/llama.cpp ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp)

    # Link with llama libraries
    target_link_libraries(
        # Specifies the target library.
        llama_native

        # Links the target library to the llama library
        llama
        ggml

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
    )

    # Define preprocessor macro to enable llama.cpp integration
    target_compile_definitions(llama_native PRIVATE USE_LLAMA_CPP)

    # Include headers
    target_include_directories(
        llama_native
        PRIVATE
        ${CMAKE_SOURCE_DIR}/../../external/llama.cpp/common
        ${CMAKE_SOURCE_DIR}/../../external/llama.cpp
    )
else()
    # Just link with log library for mock implementation
    target_link_libraries(
        # Specifies the target library.
        llama_native

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
    )
endif()

if(HAVE_FASTER_WHISPER)
    # Add faster-whisper as a subdirectory
    add_subdirectory(${CMAKE_SOURCE_DIR}/../../external/faster-whisper ${CMAKE_CURRENT_BINARY_DIR}/faster-whisper)

    # Link with Whisper libraries
    target_link_libraries(
        egyptian_whisper
        whisper
        ${log-lib}
    )

    # Define preprocessor macro to enable Whisper integration
    target_compile_definitions(egyptian_whisper PRIVATE USE_FASTER_WHISPER)

    # Include headers
    target_include_directories(
        egyptian_whisper
        PRIVATE
        ${CMAKE_SOURCE_DIR}/../../external/faster-whisper
    )
endif()