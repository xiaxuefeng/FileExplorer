#ifeq ($(PRIZE_FILE_EXPLORER),yes)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13 \

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PrizeFileExplorer
LOCAL_CERTIFICATE := platform
#LOCAL_PRIVILEGED_MODULE := true
LOCAL_PROGUARD_ENABLED := custom

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

#endif