LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := application

APP_SUBDIR := $(firstword $(patsubst $(LOCAL_PATH)/%, %, $(wildcard $(LOCAL_PATH)/onscripter*)))

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(APP_SUBDIR) \
	$(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../sdl/include \
	$(LOCAL_PATH)/../sdl_mixer \
	$(LOCAL_PATH)/../sdl_image \
	$(LOCAL_PATH)/../sdl_ttf \
	$(LOCAL_PATH)/../smpeg \
	$(LOCAL_PATH)/../lua/src \
	$(LOCAL_PATH)/../lua/etc \
	$(LOCAL_PATH)/../bzip2-1.0.5 \
	$(LOCAL_PATH)/../libmad-0.15.1b \
	$(LOCAL_PATH)/../tremor

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%) \
	-DSDL_JAVA_PACKAGE_PATH=$(SDL_JAVA_PACKAGE_PATH) \
	-DLINUX -DMP3_MAD -DPDA_AUTOSIZE -DUSE_OGG_VORBIS -DINTEGER_OGG_VORBIS -DUTF8_FILESYSTEM -DUSE_LUA -DUSE_SMPEG

#Change C++ file extension as appropriate
LOCAL_CPP_EXTENSION := .cpp

OBJSUFFIX := .o
EXT_OBJS = LUAHandler.o
include $(LOCAL_PATH)/$(APP_SUBDIR)/Makefile.onscripter
LOCAL_SRC_FILES := $(addprefix $(APP_SUBDIR)/,$(patsubst %.o, %.cpp, $(ONSCRIPTER_OBJS)))

LOCAL_SHARED_LIBRARIES := sdl sdl_mixer sdl_image sdl_ttf smpeg lua bz2 mad tremor

LOCAL_LDLIBS := -lGLESv1_CM -ldl -llog -lz -lGLESv1_CM

include $(BUILD_SHARED_LIBRARY)
