cmake_minimum_required(VERSION 3.22.1)
project("danzengine")

add_library(danzengine SHARED native-lib.cpp)
find_library(log-lib log)
target_link_libraries(danzengine ${log-lib})

add_executable(libdanz_daemon.so shizuku_daemon.cpp)
set_target_properties(libdanz_daemon.so PROPERTIES
    LINK_FLAGS "-pie -fPIE"
    COMPILE_FLAGS "-fPIE"
)
target_link_libraries(libdanz_daemon.so ${log-lib})
