package arc.graphics.gl;

import arc.Application.*;
import arc.util.*;

import java.util.*;
import java.util.regex.*;

import static arc.Application.ApplicationType.*;

public class GLVersion{
    public final String vendorString;
    public final String rendererString;
    public final String versionString;
    public final GlType type;
    public int majorVersion;
    public int minorVersion;
    public int releaseVersion;

    public GLVersion(ApplicationType appType, String versionString, String vendorString, String rendererString){
        if(appType == android || appType == iOS || versionString.toLowerCase(Locale.ROOT).startsWith("opengl es")) this.type = GlType.GLES;
        else if(appType == desktop) this.type = GlType.OpenGL;
        else if(appType == web) this.type = GlType.WebGL;
        else this.type = GlType.NONE;

        if(type == GlType.GLES){
            //OpenGL<space>ES<space><version number><space><vendor-specific information>.
            // OpenGL<空格>ES<空格><版本号><空格><厂商特定信息>。
            extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString);
        }else if(type == GlType.WebGL){
            //WebGL<space><version number><space><vendor-specific information>
            // WebGL<空格><版本号><空格><厂商特定信息>
            extractVersion("WebGL (\\d(\\.\\d){0,2})", versionString);
        }else if(type == GlType.OpenGL){
            //<version number><space><vendor-specific information>
            // <版本号><空格><厂商特定信息>
            extractVersion("(\\d(\\.\\d){0,2})", versionString);
        }else{
            majorVersion = -1;
            minorVersion = -1;
            releaseVersion = -1;
            vendorString = "";
            rendererString = "";
        }

        this.versionString = versionString;
        this.vendorString = vendorString;
        this.rendererString = rendererString;
    }

    private void extractVersion(String patternString, String versionString){
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(versionString);
        boolean found = matcher.find();
        if(found){
            String result = matcher.group(1);
            String[] resultSplit = result.split("\\.");
            majorVersion = parseInt(resultSplit[0], 2);
            minorVersion = resultSplit.length < 2 ? 0 : parseInt(resultSplit[1], 0);
            releaseVersion = resultSplit.length < 3 ? 0 : parseInt(resultSplit[2], 0);
        }else{
            Log.err("[Arc GL] Invalid version string: " + versionString);
            majorVersion = 2;
            minorVersion = 0;
            releaseVersion = 0;
        }
    }

    /**
     * Forgiving parsing of gl major, minor and release versions as some manufacturers don't adhere to spec *
     * 宽容地解析 GL 主版本、次版本和发布版本,因为有些厂商不遵守规范 *
     */
    private int parseInt(String v, int defaultValue){
        try{
            return Integer.parseInt(v);
        }catch(NumberFormatException nfe){
            Log.err("[Arc GL] Error parsing number: " + v + ", assuming: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Checks to see if the current GL connection version is higher, or equal to the provided test versions.
     * <p>
     * 检查当前 GL 连接版本是否高于或等于给定的测试版本。
     * @param testMajorVersion the major version to test against 要测试的主版本号
     * @param testMinorVersion the minor version to test against 要测试的次版本号
     * @return true if the current version is higher or equal to the test version 当前版本高于或等于测试版本时为 true
     */
    public boolean atLeast(int testMajorVersion, int testMinorVersion){
        return majorVersion > testMajorVersion || (majorVersion == testMajorVersion && minorVersion >= testMinorVersion);
    }

    /**
     * @return a string with the current GL connection data 包含当前 GL 连接信息的字符串
     */
    public String getDebugVersionString(){
        return "Type: " + type + "\n" +
        "Version: " + majorVersion + ":" + minorVersion + ":" + releaseVersion + "\n" +
        "Vendor: " + vendorString + "\n" +
        "Renderer: " + rendererString;
    }

    @Override
    public String toString(){
        return type + " " + majorVersion + "." + minorVersion + "." + releaseVersion + " / " + vendorString + " / " + rendererString + " / " + versionString;
    }

    public enum GlType{
        OpenGL,
        GLES,
        WebGL,
        NONE
    }
}
