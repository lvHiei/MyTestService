#include <jni.h>
#include <string>

#include "ffmpegHeader.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_lvhiei_mytestservice_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    av_register_all();

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
