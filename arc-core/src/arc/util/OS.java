package arc.util;

import arc.*;
import arc.files.*;

import java.io.*;

public class OS{
    public static final int cores = Runtime.getRuntime().availableProcessors();
    /**
     * User's account name.
     * 用户的账户名。
     */
    public static final String username = prop("user.name");
    /**
     * User's home directory.
     * 用户的主目录。
     */
    public static final String userHome = prop("user.home");
    /**
     * Name of the OS being used.
     * 正在使用的操作系统名称。
     */
    public static final String osName = prop("os.name");
    /**
     * Version of the OS being used; format varies based on OS.
     * 正在使用的操作系统版本;格式因操作系统而异。
     */
    public static final String osVersion = prop("os.version");
    /**
     * Operating system architecture, e.g. "amd64"
     * 操作系统架构,例如 "amd64"
     */
    public static final String osArch = propNoNull("os.arch");
    /**
     * Either 32 or 64.
     * 值为 32 或 64。
     */
    public static final String osArchBits = propNoNull("sun.arch.data.model");
    /**
     * JVM version; may contain underscores. Examples: 1.8.0_211 (Java 8 update 211), 12.0.1 (Java 12)
     * JVM 版本;可能包含下划线。例如:1.8.0_211(Java 8 update 211)、12.0.1(Java 12)
     */
    public static final String javaVersion = propNoNull("java.version");
    /**
     * Java version as a single number; 0 on iOS or Android. Examples: 8, 10, 17
     * 以单个数字表示的 Java 版本;iOS 或 Android 上为 0。例如:8、10、17
     */
    public static final int javaVersionNumber = OS.isAndroid || OS.isIos ? 0 :
        javaVersion.startsWith("1.") ? 8 :
        Strings.parseInt(javaVersion.replaceAll("^(\\d+).*", "$1"), 8);

    public static boolean isWindows = propNoNull("os.name").contains("Windows");
    public static boolean isLinux = propNoNull("os.name").contains("Linux") || propNoNull("os.name").contains("BSD");
    public static boolean isMac = propNoNull("os.name").contains("Mac");
    public static boolean isIos = false;
    public static boolean isAndroid = false;
    public static boolean isARM = propNoNull("os.arch").startsWith("arm") || propNoNull("os.arch").startsWith("aarch64");
    public static boolean is64Bit = propNoNull("os.arch").contains("64") || propNoNull("os.arch").startsWith("armv8");
    public static boolean isMobile;
    public static boolean isDesktop;

    static{
        if(propNoNull("java.runtime.name").contains("Android Runtime") || propNoNull("java.vm.vendor").contains("The Android Project") || propNoNull("java.vendor").contains("The Android Project")){
            isAndroid = true;
            isWindows = false;
            isLinux = false;
            isMac = false;
            is64Bit = false;
        }
        //if it's none of the standard operating systems, it's iOS
        // 如果不是标准操作系统之一,则为 iOS
        if(propNoNull("moe.platform.name").equals("iOS") || (!isAndroid && !isWindows && !isLinux && !isMac)){
            isIos = true;
            isAndroid = false;
            isWindows = false;
            isLinux = false;
            isMac = false;
            is64Bit = false;
        }
        isMobile = isAndroid || isIos;
        isDesktop = !isMobile;
    }

    public static String getAppDataDirectoryString(String appname){
        if(OS.isWindows){
            return env("AppData") + "\\\\" + appname;
        }else if(isIos || isAndroid){
            return Core.files.getLocalStoragePath();
        }else if(OS.isLinux){
            if(System.getenv("XDG_DATA_HOME") != null){
                String dir = System.getenv("XDG_DATA_HOME");
                if(!dir.endsWith("/")) dir += "/";
                return dir + appname + "/";
            }
            return userHome + "/.local/share/" + appname + "/";
        }else if(OS.isMac){
            return userHome + "/Library/Application Support/" + appname + "/";
        }else{ //else, probably web
        // 否则,可能是 web
            return "";
        }
    }

    public static String getWindowsTmpDir(){
        String temp = env("TEMP");
        if(temp != null) return temp;
        String tmp = env("TMP");
        if(tmp != null) return tmp;
        return "C:\\Windows\\TEMP";
    }

    /**
     * Executes, returns the result output string with the err output optionally tacked on.
     * 执行命令,返回结果输出字符串,可选地附加错误输出。
     */
    public static String exec(boolean logErr, String... args){
        try{
            Process process = Runtime.getRuntime().exec(args);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), Strings.utf8));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = in.readLine()) != null){
                result.append(line).append("\n");
            }

            if(logErr){
                BufferedReader inerr = new BufferedReader(new InputStreamReader(process.getErrorStream(), Strings.utf8));
                while((line = inerr.readLine()) != null){
                    result.append(line).append("\n");
                }
            }

            //trim trailing newline
            // 去除末尾换行符
            if(result.length() > 0 && result.charAt(result.length() - 1) == '\n') result.setLength(result.length() - 1);
            return result.toString();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a process. Does not include the error output stream.
     * 执行一个进程。不包含错误输出流。
     */
    public static String exec(String... args){
        return exec(false, args);
    }

    public static boolean execSafe(String command){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream(), Strings.utf8));
            String line;
            while((line = in.readLine()) != null){
                System.out.println(line);
            }
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    public static boolean execSafe(String... command){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream(), Strings.utf8));
            String line;
            while((line = in.readLine()) != null){
                System.out.println(line);
            }
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    public static Fi getAppDataDirectory(String appname){
        return Core.files.absolute(getAppDataDirectoryString(appname));
    }

    public static boolean hasProp(String name){
        return System.getProperty(name) != null;
    }

    public static String prop(String name){
        return System.getProperty(name);
    }

    /** @return whether the specified environment variable exists and is set to "1". 指定的环境变量是否存在且被设置为 "1"。 */
    public static boolean hasEnvFlag(String name){
        return "1".equals(System.getenv(name));
    }

    public static boolean hasEnv(String name){
        return System.getenv(name) != null;
    }

    public static String env(String name){
        return System.getenv(name);
    }

    public static String propNoNull(String name){
        String s = prop(name);
        return s == null ? "" : s;
    }
}
