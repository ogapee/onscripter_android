LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := ogg

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SRC_FILES := $(notdir $(wildcard $(LOCAL_PATH)/*.c))

include $(BUILD_SHARED_LIBRARY)

