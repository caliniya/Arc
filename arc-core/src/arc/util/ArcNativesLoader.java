package arc.util;

public class ArcNativesLoader{
    public static boolean disableNativesLoading = false;
    public static boolean loaded;

    /**
     * Loads the arc native libraries if they have not already been loaded.
     * 如果 arc 本地库尚未加载,则加载它们。
     */
    public static synchronized void load(){
        if(loaded) return;
        loaded = true;

        if(disableNativesLoading) return;

        new SharedLibraryLoader().load("arc");
    }
}
