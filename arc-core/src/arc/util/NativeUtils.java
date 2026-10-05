package arc.util;

public class NativeUtils{

    /*JNI

    #include <stdlib.h>

     */

    /** @return 0 on success, -1 on failure (errno set on the native side). Does nothing on Windows. 成功返回 0,失败返回 -1(本机侧会设置 errno)。在 Windows 上不做任何事。 */
    public static native int setEnv(String name, String value, boolean overwrite); /*
        #if defined(__linux__) && !defined(__ANDROID__)
            return setenv(name, value, overwrite ? 1 : 0);
        #else
            return -1;
        #endif
    */

    /** @return 0 on success, -1 on failure. Does nothing on Windows. 成功返回 0,失败返回 -1。在 Windows 上不做任何事。 */
    public static native int unsetEnv(String name); /*
        #if defined(__linux__) && !defined(__ANDROID__)
            return unsetenv(name);
        #else
            return -1;
        #endif
    */

    /**
     * Wraps getenv. This may differ from System.getenv.
     * <p>
     * 封装 getenv。结果可能与 System.getenv 不同。
     * @return the environment variable, the empty string. 环境变量的值,或空字符串。
     */
    public static native String getEnv(String name); /*
        #if defined(__linux__) && !defined(__ANDROID__)
            char* val = getenv(name);
            if(val == NULL){
                return env->NewStringUTF("");
            }
            return env->NewStringUTF(val);
        #else
            return env->NewStringUTF("");
        #endif
    */

    /**
     * Fixes LC_ALL and LANG on Steam to avoid issues with Zenity. This doesn't really fix all of them, but at least the dialog opens.
     * 在 Steam 上修正 LC_ALL 和 LANG 以避免 Zenity 的问题。这并不能真正解决所有问题,但至少对话框能打开。
     */
    public static void forceUtf8Locale(){
        if(!OS.isLinux) return;
        setEnv("LC_ALL", "en_US.UTF-8", true);
        setEnv("LANG", "en_US.UTF-8", true);
    }
}
