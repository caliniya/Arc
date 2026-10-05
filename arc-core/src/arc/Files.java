package arc;

import arc.audio.Audio;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

/**
 * Provides standard access to the filesystem, classpath, Android SD card, and Android assets directory.
 * <p>
 * 提供对文件系统、类路径、Android SD 卡和 Android assets 目录的标准访问。
 * @author mzechner
 * @author Nathan Sweet
 */
public interface Files{
    /**
     * Returns a handle representing a file or directory.
     * <p>
     * 返回表示一个文件或目录的句柄。
     * @param type Determines how the path is resolved. 决定路径如何解析。
     * @throws ArcRuntimeException if the type is classpath or internal and the file does not exist. 如果类型为 classpath 或 internal 且文件不存在。
     * @see FileType
     */
    Fi get(String path, FileType type);

    /**
     * Convenience method that returns a {@link FileType#classpath} file handle.
     * 便捷方法,返回一个 {@link FileType#classpath} 文件句柄。
     */
    default Fi classpath(String path){
        return get(path, FileType.classpath);
    }

    /**
     * Convenience method that returns a {@link FileType#internal} file handle.
     * 便捷方法,返回一个 {@link FileType#internal} 文件句柄。
     */
    default Fi internal(String path){
        return get(path, FileType.internal);
    }

    /**
     * Convenience method that returns a {@link FileType#external} file handle.
     * 便捷方法,返回一个 {@link FileType#external} 文件句柄。
     */
    default Fi external(String path){
        return get(path, FileType.external);
    }

    /**
     * Convenience method that returns a {@link FileType#absolute} file handle.
     * 便捷方法,返回一个 {@link FileType#absolute} 文件句柄。
     */
    default Fi absolute(String path){
        return get(path, FileType.absolute);
    }

    /**
     * Convenience method that returns a {@link FileType#local} file handle.
     * 便捷方法,返回一个 {@link FileType#local} 文件句柄。
     */
    default Fi local(String path){
        return get(path, FileType.local);
    }

    /**
     * Convenience method that returns a cache file handle.
     * 便捷方法,返回一个缓存文件句柄。
     */
    default Fi cache(String path){
        return get(getCachePath(), FileType.absolute).child(path);
    }

    /**
     * @return absolute path to cache directory. 缓存目录的绝对路径。
     */
    default String getCachePath(){
        return local("cache").absolutePath();
    }

    /**
     * @return on IOS, the internal path to the assets directory. Not used on other platforms. 在 iOS 上为 assets 目录的内部路径。其他平台不使用。
     */
    default String getInternalStoragePath(){
        return "";
    }

    /**
     * @return the external storage path directory. This is the SD card on Android and the home directory of the current user on
     * the desktop. 外部存储路径目录。在 Android 上为 SD 卡,在桌面上为当前用户的主目录。
     */
    String getExternalStoragePath();

    /**
     * @return true if the external storage is ready for file IO. Eg, on Android, the SD card is not available when mounted for use
     * with a PC. 如果外部存储已就绪、可进行文件 IO,则返回 true。例如在 Android 上,SD 卡被挂载给 PC 使用时不可用。
     */
    boolean isExternalStorageAvailable();

    /**
     * @return the local storage path directory. This is the private files directory on Android and the directory of the jar on the
     * desktop. 本地存储路径目录。在 Android 上为私有文件目录,在桌面上为 jar 所在目录。
     */
    String getLocalStoragePath();

    /**
     * @return true if the local storage is ready for file IO. 如果本地存储已就绪、可进行文件 IO,则返回 true。
     */
    boolean isLocalStorageAvailable();

    /**
     * Indicates how to resolve a path to a file.
     * <p>
     * 指示如何解析文件路径。
     * @author mzechner
     * @author Nathan Sweet
     */
    enum FileType{
        /**
         * Path relative to the root of the classpath. Classpath files are always readonly. Note that classpath files are not
         * compatible with some functionality on Android, such as {@link arc.audio.Audio#newSound(Fi)} and
         * {@link Audio#newMusic(Fi)}.
         * <p>
         * 相对于类路径根目录的路径。类路径文件始终是只读的。
         * 注意,类路径文件与 Android 上的某些功能不兼容,例如 {@link arc.audio.Audio#newSound(Fi)} 和 {@link Audio#newMusic(Fi)}。
         */
        classpath,

        /**
         * Path relative to the asset directory on Android and to the application's root directory on the desktop. On the desktop,
         * if the file is not found, then the classpath is checked. This enables files to be found when using JWS or applets.
         * Internal files are always readonly.
         * <p>
         * 相对于 Android 上 assets 目录以及桌面上应用根目录的路径。在桌面上,如果找不到文件,则会检查类路径。
         * 这使得在使用 JWS 或 applet 时也能找到文件。内部文件始终是只读的。
         */
        internal,

        /**
         * Path relative to the root of the SD card on Android and to the home directory of the current user on the desktop.
         * 相对于 Android 上 SD 卡根目录以及桌面上当前用户主目录的路径。
         */
        external,

        /**
         * Path that is a fully qualified, absolute filesystem path. To ensure portability across platforms use absolute files only
         * when absolutely (heh) necessary.
         * <p>
         * 完全限定的绝对文件系统路径。为确保跨平台可移植性,只在绝对(嘿嘿)必要时才使用绝对文件。
         */
        absolute,

        /**
         * Path relative to the private files directory on Android and to the application's root directory on the desktop.
         * 相对于 Android 上私有文件目录以及桌面上应用根目录的路径。
         */
        local
    }
}
