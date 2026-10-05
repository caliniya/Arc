package arc.backend.sdl;

import arc.Files.*;
import arc.graphics.*;

public class SdlConfig{
    public int r = 8, g = 8, b = 8, a = 8;
    public int depth = 0, stencil = 0;
    public int samples = 0;
    public HdpiUtils.HdpiMode hdpiMode = HdpiUtils.HdpiMode.logical;

    public int width = 640;
    public int height = 480;
    public boolean resizable = true;
    public boolean decorated = true;
    public boolean maximized = false;
    public boolean fullscreen = false;
    public boolean disableAudio = false;
    /**
     * For MacOS, this is always forced to 'true'.
     * 在 MacOS 上,此项始终强制为 'true'。
     */
    public boolean coreProfile = false;
    /**
     * Requested OpenGL versions, in order of priority.
     * 请求的 OpenGL 版本,按优先级排序。
     */
    public int[][] glVersions = {{3, 0}};
    /**
     * If true, ANGLE is used on Windows.
     * 如果为 true,则在 Windows 上使用 ANGLE。
     */
    public boolean useAngle = true;

    public String title = "Arc Application";
    public Color initialBackgroundColor = Color.black;
    public boolean initialVisible = true;
    public boolean vSyncEnabled = true;
    public String appName, appVersion, appIdentifier;

    public FileType windowIconFileType;
    public String[] windowIconPaths;

    public void setWindowIcon(FileType fileType, String... filePaths){
        windowIconFileType = fileType;
        windowIconPaths = filePaths;
    }
}
