# CMakeList.txt : CMake project for net-camera-viewer, include source and define
# project specific logic here.
#
cmake_minimum_required (VERSION 3.8)

# Enable Hot Reload for MSVC compilers if supported.
if (POLICY CMP0141)
  cmake_policy(SET CMP0141 NEW)
  set(CMAKE_MSVC_DEBUG_INFORMATION_FORMAT "$<IF:$<AND:$<C_COMPILER_ID:MSVC>,$<CXX_COMPILER_ID:MSVC>>,$<$<CONFIG:Debug,RelWithDebInfo>:EditAndContinue>,$<$<CONFIG:Debug,RelWithDebInfo>:ProgramDatabase>>")
endif()

project ("net-camera-viewer")

include_directories("third-part/ffmpeg/include")

list(APPEND CMAKE_PREFIX_PATH "${CMAKE_SOURCE_DIR}/third-part")
find_package(SDL2 2 REQUIRED)
include_directories(${SDL2_INCLUDE_DIRS})

# Add source to this project's executable.
add_executable(net-camera-viewer "net-camera-viewer.cpp" "net-camera-viewer.h" "h264-decoder.h" "h264-decoder.cpp" "packet-receiver.h" "packet-receiver.cpp" "yuv-render.cpp" "yuv-render.h" "performance.cpp" "performance.h")

target_link_directories(net-camera-viewer PUBLIC "${CMAKE_SOURCE_DIR}/third-part/ffmpeg/lib")
target_link_libraries(net-camera-viewer avdevice avcodec avformat swscale avutil)
target_link_libraries(net-camera-viewer ${SDL2_LIBRARIES})

if (CMAKE_VERSION VERSION_GREATER 3.12)
  set_property(TARGET net-camera-viewer PROPERTY CXX_STANDARD 20)
endif()

# TODO: Add tests and install targets if needed.
