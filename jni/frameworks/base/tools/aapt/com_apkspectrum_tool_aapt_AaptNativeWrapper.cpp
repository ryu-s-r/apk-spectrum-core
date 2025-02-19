#include <stdio.h>
#include <stdlib.h>

#include <androidfw/ResourceTypes.h>

#include "Main.h"

#include "com_apkspectrum_tool_aapt_AaptNativeWrapper.h"
#include "JniCharacterSet.h"
#include "OutLineBuffer.h"

extern int main(int argc, char* const argv[]);

JNIEXPORT jobjectArray JNICALL Java_com_apkspectrum_tool_aapt_AaptNativeWrapper_run
  (JNIEnv *env, jclass /*thiz*/, jobjectArray params)
{
    char prog[8] = "libaapt";
    jsize paramCnt = 0;
    
    if(params != NULL) {
        paramCnt = env->GetArrayLength(params);
    }
    if(paramCnt < 0) paramCnt = 0;

	char* buf[paramCnt+1];
	buf[0] = prog;

    jstring encoding = getStickyEncodingCharacterSet(env);
    jclass java_lang_String = NULL;
    jmethodID java_lang_String_getBytes = NULL;

    if(encoding != NULL) {
        java_lang_String = env->FindClass("java/lang/String");
        if(java_lang_String != NULL) {
            java_lang_String_getBytes = env->GetMethodID(java_lang_String, "getBytes", "(Ljava/lang/String;)[B");
        }
        if(java_lang_String_getBytes == NULL) {
            encoding = NULL;
            if(java_lang_String != NULL) {
                env->DeleteLocalRef(java_lang_String);
                java_lang_String = NULL;
            }
        }
    }

    for(int i = 0; i < paramCnt; i++) {
        jstring param = static_cast<jstring>(env->GetObjectArrayElement(params, i));
        if(param == NULL) {
            buf[i+1] = NULL;
            fprintf(stderr, "params[%d] is NULL\n", i);
            continue;
        }

        if(encoding != NULL) {
            jbyteArray bytes = static_cast<jbyteArray>(env->CallObjectMethod(param, java_lang_String_getBytes, encoding));
            // check UnsupportedEncodingException
            if(env->ExceptionCheck()) {
                fprintf(stderr, "UnsupportedEncodingException occurred\n");
                env->ExceptionClear();
                
                jmethodID java_lang_String_getBytes_ = env->GetMethodID(java_lang_String, "getBytes", "()[B");
                if(java_lang_String_getBytes_ != NULL) {
                    bytes = static_cast<jbyteArray>(env->CallObjectMethod(param, java_lang_String_getBytes_));
                }
            }
            if(bytes != NULL) {
                buf[i+1] = jbyteArray2cstr(env, bytes);
                env->DeleteLocalRef(bytes);
            } else {
                buf[i+1] = NULL;
                fprintf(stderr, "Failure: encoding buf[%d] is NULL\n", i);
            }
        } else {
            const char *jchar = env->GetStringUTFChars(param, 0);
            buf[i+1] = (char *)malloc(4096);
            if(buf[i+1] != NULL) {
                sprintf(buf[i+1], "%s", (char*)jchar);
            }
            env->ReleaseStringUTFChars(param, jchar);
        }

        env->DeleteLocalRef(param);
    }

    jobjectArray stringArray = NULL;
    {
        OutLineBuffer olb(env);
    	main(paramCnt+1,(char**)buf);
    	stringArray = olb.toArray();
    }

    for(int i = 0; i < paramCnt; i++) {
        if(buf[i+1] != NULL) {
            free((void*)buf[i+1]);
        }
    }

    fflush(stdout);
    fflush(stderr);
    
    return stringArray;
}

//static JNINativeMethod sMethod[] = {
    /* name, signature, funcPtr */
//    {"run", "([Ljava/lang/String;)[Ljava/lang/String;", (jobjectArray*)Java_com_apkspectrum_tool_aapt_AaptNativeWrapper_run}
//};

/*
int jniRegisterNativMethod(JNIEnv* env, const char* className, const JNINativeMethod* gMethods, int numMethods ) {
    jclass clazz;
 
    clazz = env->FindClass(className);
 
    if(clazz == NULL){
        return -1;
    }
    if(env->RegisterNatives(clazz, gMethods, numMethods) < 0){
        return -1;
    }
    return 0;
}
*/

/*
 For the Oracle JRE8u261 32bit.
 It waw ocurred fatal error at after called to the JNI_OnLoad.
 It even occurs when there is no body.
 I do not know the cause. So, I do block the called for the 32 bits.
*/
#if !defined(_WIN32) || defined(_WIN64)
jint JNI_OnLoad(JavaVM* jvm, void* /*reserved*/) {
    JNIEnv* env = NULL;
    jint result = -1;

    if(jvm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK){
        return result;
    }

    getStickyEncodingCharacterSet(env);
 
    //jniRegisterNativMethod(env, "com/apkspectrum/tool/aapt/AaptNativeWrapper", sMethod, NELEM(sMethod));
 
    return JNI_VERSION_1_6;
}
#endif

void JNI_OnUnload(JavaVM *jvm, void* /*reserved*/)
{
    JNIEnv *env;

    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6)) {
        return; 
    }

    releaseStickyEncodingCharacterSet(env);

    return; 
}
