package arc.backend.sdl;

import arc.util.*;
import arc.util.io.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static arc.util.OS.*;

public class ANGLELoader{
    static private final Random random = new Random();
    static private File egl, gles, lastWorkingDir;

    static String randomUUID(){
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    public static String crc(InputStream input){
        if(input == null) throw new IllegalArgumentException("input cannot be null.");
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        try{
            while(true){
                int length = input.read(buffer);
                if(length == -1) break;
                crc.update(buffer, 0, length);
            }
        }catch(Exception ex){
        }finally{
            Streams.close(input);
        }
        return Long.toString(crc.getValue(), 16);
    }

    private static File extractFile(String sourcePath, File outFile){
        try{
            if(!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) throw new ArcRuntimeException(
            "Couldn't create ANGLE native library output directory " + outFile.getParentFile().getAbsolutePath());

            if(outFile.exists()) return outFile;

            try(OutputStream out = new FileOutputStream(outFile); InputStream in = readFile(sourcePath)){
                Streams.copy(in, out);
                return outFile;
            }
        }catch(Throwable t){
            throw new ArcRuntimeException("Couldn't load ANGLE shared library " + sourcePath, t);
        }
    }

    /**
     * Returns a path to a file that can be written. Tries multiple locations and verifies writing succeeds.
     * <p>
     * 返回一个可写入文件的路径。尝试多个位置并验证写入是否成功。
     * @return null if a writable path could not be found. 如果找不到可写路径则返回 null。
     */
    private static File getExtractedFile(String dirName, String fileName){
        // Temp directory with username in path.
        // 路径中包含用户名的临时目录。
        File idealFile = new File(
        System.getProperty("java.io.tmpdir") + "/arc" + System.getProperty("user.name") + "/" + dirName, fileName);
        if(canWrite(idealFile)) return idealFile;

        // System provided temp directory.
        // 系统提供的临时目录。
        try{
            File file = File.createTempFile(dirName, null);
            if(file.delete()){
                file = new File(file, fileName);
                if(canWrite(file)) return file;
            }
        }catch(IOException ignored){
        }

        // User home.
        // 用户主目录。
        File file = new File(System.getProperty("user.home") + "/.libgdx/" + dirName, fileName);
        if(canWrite(file)) return file;

        // Relative directory.
        // 相对目录。
        file = new File(".temp/" + dirName, fileName);
        if(canWrite(file)) return file;

        // We are running in the OS X sandbox.
        // 我们正在 OS X 沙盒中运行。
        if(System.getenv("APP_SANDBOX_CONTAINER_ID") != null) return idealFile;

        return null;
    }

    /**
     * Returns true if the parent directories of the file can be created and the file can be written.
     * 如果文件的父目录可以创建且文件可写,则返回 true。
     */
    private static boolean canWrite(File file){
        File parent = file.getParentFile();
        File testFile;
        if(file.exists()){
            if(!file.canWrite() || !canExecute(file)) return false;
            // Don't overwrite existing file just to check if we can write to directory.
            // 不要仅仅为了检查能否写入目录而覆盖已有文件。
            testFile = new File(parent, randomUUID());
        }else{
            parent.mkdirs();
            if(!parent.isDirectory()) return false;
            testFile = file;
        }
        try{
            new FileOutputStream(testFile).close();
            return canExecute(testFile);
        }catch(Throwable ex){
            return false;
        }finally{
            testFile.delete();
        }
    }

    private static boolean canExecute(File file){
        try{
            if(file.canExecute()) return true;

            file.setExecutable(true, false);

            return file.canExecute();
        }catch(Exception ignored){
        }
        return false;
    }

    public static boolean isCompatible(){
        String osDir = "";
        String arch = isARM ? (is64Bit ? "arm64" : "arm32") : (is64Bit ? "x64" : "x86");
        String ext = "";
        if(isWindows){
            osDir = "windows";
            ext = ".dll";
        }
        if(isLinux){
            osDir = "linux";
            ext = ".so";
        }
        if(isMac){
            osDir = "macos";
            ext = ".dylib";
        }

        String dir = osDir + "/" + arch + "/angle";

        String eglSource = dir + "/libEGL" + ext;
        String glesSource = dir + "/libGLESv2" + ext;

        return ANGLELoader.class.getClassLoader().getResource(eglSource) != null
        && ANGLELoader.class.getClassLoader().getResource(glesSource) != null;
    }

    private static InputStream readFile(String path){
        InputStream input = ANGLELoader.class.getClassLoader().getResourceAsStream(path);
        if(input == null) throw new ArcRuntimeException("Unable to read file for extraction: " + path);
        return input;
    }

    public static void load(){
        String osDir = "";
        String arch = isARM ? (is64Bit ? "arm64" : "arm32") : (is64Bit ? "x64" : "x86");
        String ext = "";
        if(isWindows){
            osDir = "windows";
            ext = ".dll";
        }
        if(isLinux){
            osDir = "linux";
            ext = ".so";
        }
        if(isMac){
            osDir = "macos";
            ext = ".dylib";
        }

        String dir = osDir + "/" + arch + "/angle";

        String eglSource = dir + "/libEGL" + ext;
        String glesSource = dir + "/libGLESv2" + ext;
        String crc = crc(readFile(eglSource)) + crc(readFile(glesSource));
        egl = getExtractedFile(crc, new File(eglSource).getName());
        gles = getExtractedFile(crc, new File(glesSource).getName());

        if(!isMac){
            extractFile(eglSource, egl);
            System.load(egl.getAbsolutePath());
            extractFile(glesSource, gles);
            System.load(gles.getAbsolutePath());
        }else{
            // On macOS, we can't preload the shared libraries. calling dlopen("path1/lib.dylib")
            // 在 macOS 上,我们无法预加载共享库。调用 dlopen("path1/lib.dylib")
            // then calling dlopen("lib.dylib") will not return the dylib loaded in the first dlopen()
            // 那么调用 dlopen("lib.dylib") 将不会返回第一次 dlopen() 加载的 dylib
            // call, but instead perform the dlopen library search algorithm anew. Since the dylibs
            // 调用,而是重新执行 dlopen 库搜索算法。由于这些 dylib
            // we extract are not in any paths dlopen knows about, GLFW fails to load them.
            // 我们解压出的文件不在 dlopen 已知的任何路径中,GLFW 无法加载它们。
            // Instead, we need to copy the shared libraries to the current working directory (which
            // 相反,我们需要将共享库复制到当前工作目录(这个目录
            // we can't temporarily change in pure Java either...). The dylibs will get deleted
            // 在纯 Java 中也无法临时更改……)。这些 dylib 将被删除
            // in postGlfwInit() once the first window has been created, and GLFW has loaded the dylibs.
            // 在 postGlfwInit() 中,即第一个窗口创建且 GLFW 加载完 dylib 之后。
            lastWorkingDir = new File(".");
            extractFile(eglSource, new File(lastWorkingDir, egl.getName()));
            extractFile(glesSource, new File(lastWorkingDir, gles.getName()));
        }
    }

    public static void postGlfwInit(){
        new File(lastWorkingDir, egl.getName()).delete();
        new File(lastWorkingDir, gles.getName()).delete();
    }
}