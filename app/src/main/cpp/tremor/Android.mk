LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := tremor

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libogg-1.3.3

LOCAL_CFLAGS := -DHAVE_ALLOCA_H

LOCAL_CPP_EXTENSION := .cpp

# Note this simple makefile var substitution, you can find even simpler examples in different Android projects
LOCAL_SRC_FILES := $(notdir $(wildcard $(LOCAL_PATH)/*.c))

LOCAL_SHARED_LIBRARIES := ogg

include $(BUILD_SHARED_LIBRARY)

