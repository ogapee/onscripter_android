LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := smpeg

ifndef SDL_JAVA_PACKAGE_PATH
$(error Please define SDL_JAVA_PACKAGE_PATH to the path of your Java package with dots replaced with underscores, for example "com_example_SanAngeles")
endif

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../sdl/include

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) -DNOCONTROLS

SMPEG_SRCS := \
	MPEGfilter.c \
	*.cpp \
	video/*.cpp \
	audio/*.cpp

# TODO: use libcutils for atomic operations, but it's not included in NDK

#	src/atomic/linux/*.c \
#	src/power/linux/*.c \
#	src/joystick/android/*.c \
#	src/haptic/android/*.c \
#	src/libm/*.c \

LOCAL_CPP_EXTENSION := .cpp

# Note this "simple" makefile var substitution, you can find even more complex examples in different Android projects
LOCAL_SRC_FILES := $(foreach F, $(SMPEG_SRCS), $(addprefix $(dir $(F)),$(notdir $(wildcard $(LOCAL_PATH)/$(F)))))

LOCAL_SHARED_LIBRARIES := sdl

LOCAL_LDLIBS := -lGLESv1_CM -ldl -llog

include $(BUILD_SHARED_LIBRARY)
