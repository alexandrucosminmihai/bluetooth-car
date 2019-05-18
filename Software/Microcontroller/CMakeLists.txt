cmake_minimum_required(VERSION 3.14)
project(Microcontroller C)

set(CMAKE_C_STANDARD 99)

include_directories( . )

add_executable(Microcontroller main.c usart.h usart.c)