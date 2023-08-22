LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := mad
LOCAL_SRC_FILES := $(notdir $(wildcard $(LOCAL_PATH)/*.c))

include $(BUILD_SHARED_LIBRARY)
