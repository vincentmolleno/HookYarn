#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_hookyarn_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "HookYarn Native Support Enabled";
    return env->NewStringUTF(hello.c_str());
}
