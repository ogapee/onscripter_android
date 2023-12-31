LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdl_mixer

LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/.. $(LOCAL_PATH)/../sdl/include \
	$(LOCAL_PATH)/../libmad-0.15.1b $(LOCAL_PATH)/../libogg-1.3.3

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) \
	-DWAV_MUSIC -DMP3_MAD_MUSIC -DOGG_USE_TREMOR -DOGG_MUSIC

LOCAL_CPP_EXTENSION := .cpp

# Note this simple makefile var substitution, you can find even simpler examples in different Android projects
LOCAL_SRC_FILES := $(notdir $(wildcard $(LOCAL_PATH)/*.c))

LOCAL_SHARED_LIBRARIES := sdl ogg tremor mad

include $(BUILD_SHARED_LIBRARY)

