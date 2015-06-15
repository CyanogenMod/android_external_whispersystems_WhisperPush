LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := WhisperPush
LOCAL_PROGUARD_ENABLED := disabled

ifneq ($(DEFAULT_SYSTEM_DEV_CERTIFICATE),build/target/product/security/testkey)
  LOCAL_CERTIFICATE := cyngn-app
else
  LOCAL_CERTIFICATE := platform
endif

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --extra-packages com.google.android.gms -S $(LOCAL_PATH)/../../google/google_play_services/libproject/google-play-services_lib/res --auto-add-overlay

LOCAL_STATIC_JAVA_LIBRARIES := play axolotl-android textsecure-android

include $(BUILD_PACKAGE)
