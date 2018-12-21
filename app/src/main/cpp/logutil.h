//
// Created by mj on 18-12-21.
//

#ifndef MYTESTSERVICE_LOGUTIL_H
#define MYTESTSERVICE_LOGUTIL_H

#include <android/log.h>
#define TAG "JNI_SERVICE"

#define LOGD(LOGFMT, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG ,LOGFMT, ##__VA_ARGS__)
#define LOGI(LOGFMT, ...) __android_log_print(ANDROID_LOG_INFO, TAG ,LOGFMT, ##__VA_ARGS__)
#define LOGW(LOGFMT, ...) __android_log_print(ANDROID_LOG_WARN, TAG ,LOGFMT, ##__VA_ARGS__)
#define LOGE(LOGFMT, ...) __android_log_print(ANDROID_LOG_ERROR, TAG ,LOGFMT, ##__VA_ARGS__)

#endif //MYTESTSERVICE_LOGUTIL_H
