package arc.util;

import arc.util.io.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static arc.util.OS.*;

/**
 * Loads shared libraries from a natives jar file (desktop) or arm folders (Android). For desktop projects, have the natives jar
 * in the classpath, for Android projects put the shared libraries in the libs/armeabi and libs/armeabi-v7a folders.
 * <p>
 * 从 natives jar 文件(桌面端)或 arm 文件夹(Android)加载共享库。桌面项目需将 natives jar 放入 classpath;Android 项目需将共享库放入 libs/armeabi 和 libs/armeabi-v7a 文件夹。
 * @author mzechner
 * @author Nathan Sweet
 */
public class SharedLibraryLoader{
    private static final HashSet<String> loadedLibraries = new HashSet<>();
    private String nativesJar;

    public SharedLibraryLoader(){
    }

    /**
     * Fetches the natives from the given natives jar file. Used for testing a shared lib on the fly.
     * <p>
     * 从给定的 natives jar 文件中获取本地库。用于即时测试共享库。
     */
    public SharedLibraryLoader(String nativesJar){
        this.nativesJar = nativesJar;
    }

    /**
     * Sets the library as loaded, for when application code wants to handle libary loading itself.
     * 将库标记为已加载,用于应用程序代码想自行处理库加载的情况。
     */
    public static synchronized void setLoaded(String libraryName){
        loadedLibraries.add(libraryName);
    }

    public static synchronized boolean isLoaded(String libraryName){
        return loadedLibraries.contains(libraryName);
    }

    /**
     * Returns a CRC of the remaining bytes in the stream.
     * 返回流中剩余字节的 CRC。
     */
    public String crc(InputStream input){
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

    /**
     * Maps a platform independent library name to a platform dependent name.
     * 将平台无关的库名映射为平台相关的名称。
     */
    public String mapLibraryName(String libraryName){
        if(isWindows) return libraryName + (is64Bit ? "64.dll" : ".dll");
        if(isLinux) return "lib" + libraryName + (isARM ? "arm" : "") + (is64Bit ? "64.so" : ".so");
        if(isMac) return "lib" + libraryName + (isARM ? "arm" : "") + (is64Bit ? "64.dylib" : ".dylib");
        return libraryName;
    }

    /**
     * Loads a shared library for the platform the application is running on.
     * <p>
     * 为应用程序运行所在的平台加载共享库。
     * @param libraryName The platform independent library name. If not contain a prefix (eg lib) or suffix (eg .dll). 平台无关的库名。不含前缀(如 lib)或后缀(如 .dll)。
     */
    public void load(String libraryName){
        // in case of iOS, things have been linked statically to the executable, bail out.
        // 在 iOS 上,库已静态链接到可执行文件中,直接返回。
        if(isIos) return;

        synchronized(SharedLibraryLoader.class){
            if(isLoaded(libraryName)) return;
            String platformName = mapLibraryName(libraryName);
            try{
                if(isAndroid)
                    System.loadLibrary(platformName);
                else
                    loadFile(platformName);
                setLoaded(libraryName);
            }catch(Throwable ex){
                throw new ArcRuntimeException("Couldn't load shared library '" + platformName + "' for target: "
                + osName + (is64Bit ? ", 64-bit" : ", 32-bit"), ex);
            }
        }
    }

    protected InputStream readFile(String path){
        if(nativesJar == null){
            InputStream input = SharedLibraryLoader.class.getResourceAsStream("/" + path);
            if(input == null) throw new ArcRuntimeException("Unable to read file for extraction: " + path);
            return input;
        }

        // Read from JAR.
        // 从 JAR 中读取。
        try{
            ZipFile file = new ZipFile(nativesJar);
            ZipEntry entry = file.getEntry(path);
            if(entry == null) throw new ArcRuntimeException("Couldn't find '" + path + "' in JAR: " + nativesJar);
            return file.getInputStream(entry);
        }catch(IOException ex){
            throw new ArcRuntimeException("Error reading '" + path + "' in JAR: " + nativesJar, ex);
        }
    }

    /**
     * Extracts the specified file to the specified directory if it does not already exist or the CRC does not match. If file
     * extraction fails and the file exists at java.library.path, that file is returned.
     * <p>
     * 若指定文件尚不存在或 CRC 不匹配,则将其解压到指定目录。若解压失败且该文件存在于 java.library.path,则返回该文件。
     * @param sourcePath The file to extract from the classpath or JAR. 要从 classpath 或 JAR 中解压的文件。
     * @param dirName The name of the subdirectory where the file will be extracted. If null, the file's CRC will be used. 文件将被解压到的子目录名称。若为 null,则使用文件的 CRC。
     * @return The extracted file. 解压出的文件。
     */
    public File extractFile(String sourcePath, String dirName) throws IOException{
        try{
            String sourceCrc = crc(readFile(sourcePath));
            if(dirName == null) dirName = sourceCrc;

            File extractedFile = getExtractedFile(dirName, new File(sourcePath).getName());
            if(extractedFile == null){
                extractedFile = getExtractedFile(UUID.randomUUID().toString(), new File(sourcePath).getName());
                if(extractedFile == null) throw new ArcRuntimeException(
                "Unable to find writable path to extract file. Is the user home directory writable?");
            }
            return extractFile(sourcePath, sourceCrc, extractedFile);
        }catch(RuntimeException ex){
            // Fallback to file at java.library.path location, eg for applets.
            // 回退到 java.library.path 位置的文件,例如用于 applet。
            File file = new File(System.getProperty("java.library.path"), sourcePath);
            if(file.exists()) return file;
            throw ex;
        }
    }

    /**
     * Extracts the specified file into the temp directory if it does not already exist or the CRC does not match. If file
     * extraction fails and the file exists at java.library.path, that file is returned.
     * <p>
     * 若指定文件尚不存在或 CRC 不匹配,则将其解压到临时目录。若解压失败且该文件存在于 java.library.path,则返回该文件。
     * @param sourcePath The file to extract from the classpath or JAR. 要从 classpath 或 JAR 中解压的文件。
     * @param dir The location where the extracted file will be written. 解压文件的写入位置。
     */
    public void extractFileTo(String sourcePath, File dir) throws IOException{
        extractFile(sourcePath, crc(readFile(sourcePath)), new File(dir, new File(sourcePath).getName()));
    }

    /**
     * Returns a path to a file that can be written. Tries multiple locations and verifies writing succeeds.
     * <p>
     * 返回一个可写文件的路径。会尝试多个位置并验证写入是否成功。
     * @return null if a writable path could not be found. 若找不到可写路径则为 null。
     */
    private File getExtractedFile(String dirName, String fileName){
        // Temp directory with username in path.
        // 路径中包含用户名的临时目录。
        File idealFile = new File(
        System.getProperty("java.io.tmpdir") + "/arc" + username + "/" + dirName, fileName);
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
        File file = new File(userHome + "/.arc/" + dirName, fileName);
        if(canWrite(file)) return file;

        // Relative directory.
        // 相对目录。
        file = new File(".temp/" + dirName, fileName);
        if(canWrite(file)) return file;

        // We are running in the OS X sandbox.
        // 当前运行在 OS X 沙箱中。
        if(System.getenv("APP_SANDBOX_CONTAINER_ID") != null) return idealFile;

        return null;
    }

    /**
     * Returns true if the parent directories of the file can be created and the file can be written.
     * 若文件的父目录可以创建且文件可以写入,则返回 true。
     */
    private boolean canWrite(File file){
        File parent = file.getParentFile();
        File testFile;
        if(file.exists()){
            if(!file.canWrite() || !canExecute(file)) return false;
            // Don't overwrite existing file just to check if we can write to directory.
            // 不要为了检查能否写入目录而覆盖已有文件。
            testFile = new File(parent, UUID.randomUUID().toString());
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

    private boolean canExecute(File file){
        try{
            if(file.canExecute()) return true;
            file.setExecutable(true, false);
            return file.canExecute();
        }catch(Throwable ignored){
        }
        return false;
    }

    protected File extractFile(String sourcePath, String sourceCrc, File extractedFile) throws IOException{
        String extractedCrc = null;
        if(extractedFile.exists()){
            try{
                extractedCrc = crc(new FileInputStream(extractedFile));
            }catch(FileNotFoundException ignored){
            }
        }

        // If file doesn't exist or the CRC doesn't match, extract it to the temp dir.
        // 若文件不存在或 CRC 不匹配,则将其解压到临时目录。
        if(extractedCrc == null || !extractedCrc.equals(sourceCrc)){
            InputStream input = null;
            FileOutputStream output = null;
            try{
                input = readFile(sourcePath);
                if(extractedFile.getParentFile() != null){
                    extractedFile.getParentFile().mkdirs();
                }
                output = new FileOutputStream(extractedFile);
                byte[] buffer = new byte[4096];
                while(true){
                    int length = input.read(buffer);
                    if(length == -1) break;
                    output.write(buffer, 0, length);
                }
            }catch(IOException ex){
                throw new ArcRuntimeException("Error extracting file: " + sourcePath + "\nTo: " + extractedFile.getAbsolutePath(), ex);
            }finally{
                Streams.close(input);
                Streams.close(output);
            }
        }

        return extractedFile;
    }

    /**
     * Extracts the source file and calls System.load. Attemps to extract and load from multiple locations. Throws runtime
     * exception if all fail.
     * <p>
     * 解压源文件并调用 System.load。会尝试从多个位置解压并加载。若全部失败则抛出运行时异常。
     */
    private void loadFile(String sourcePath){
        String sourceCrc = crc(readFile(sourcePath));

        String fileName = new File(sourcePath).getName();

        // Temp directory with arc in path.
        // 路径中包含 arc 的临时目录。
        File file = new File(System.getProperty("java.io.tmpdir") + "/arc/" + sourceCrc, fileName);
        Throwable result;
        if((result = loadFile(sourcePath, sourceCrc, file)) == null) return;

        // System provided temp directory.
        // 系统提供的临时目录。
        try{
            file = File.createTempFile(sourceCrc, null);
            if(file.delete() && loadFile(sourcePath, sourceCrc, file) == null) return;
        }catch(Throwable ignored){
        }

        // User home.
        // 用户主目录。
        file = new File(userHome + "/.arc/" + sourceCrc, fileName);
        if(loadFile(sourcePath, sourceCrc, file) == null) return;

        // Relative directory.
        // 相对目录。
        file = new File(".temp/" + sourceCrc, fileName);
        if(loadFile(sourcePath, sourceCrc, file) == null) return;

        // Right next to the directory.
        // 紧邻所在目录。
        file = new File(fileName);
        if(loadFile(sourcePath, sourceCrc, file) == null) return;

        // Fallback to java.library.path location, eg for applets.
        // 回退到 java.library.path 位置,例如用于 applet。
        file = new File(System.getProperty("java.library.path"), sourcePath);
        if(file.exists()){
            System.load(file.getAbsolutePath());
            return;
        }

        throw new ArcRuntimeException(result);
    }

    /** @return null if the file was extracted and loaded. 若文件已解压并加载,则返回 null。 */
    protected Throwable loadFile(String sourcePath, String sourceCrc, File extractedFile){
        try{
            System.load(extractFile(sourcePath, sourceCrc, extractedFile).getAbsolutePath());
            return null;
        }catch(Throwable ex){
            return ex;
        }
    }
}
