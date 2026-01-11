# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required for your project.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("llama_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Create the native library
add_library(
    # Sets the name of the library.
    llama_native

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    src/main/cpp/llama_native.cpp
)

# Find required libraries
find_library(log-lib log)

# Link required libraries
target_link_libraries(
    # Specifies the target library.
    llama_native

    # Links the target library to the log library
    # included in the NDK.
    ${log-lib}
)