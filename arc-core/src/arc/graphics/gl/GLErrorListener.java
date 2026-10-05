package arc.graphics.gl;

import arc.util.*;

/**
 * Listener for GL errors detected by {@link GLProfiler}.
 * <p>
 * {@link GLProfiler} 检测到的 GL 错误的监听器。
 * @author Jan Polák
 * @see GLProfiler
 */
public interface GLErrorListener{

    /**
     * Listener that will log using Log.error GL error name and GL function.
     * 监听器,将使用 Log.error 记录 GL 错误名称和 GL 函数。
     */
    GLErrorListener loggingListener = error -> {
        String place = getCallName();

        if(place != null){
            Log.err(new RuntimeException(Strings.format("[GLProfiler] Error @ from @", error, place)));
        }else{
            Log.err(new RuntimeException(Strings.format("[GLProfiler] Error @", error)));
        }
    };

    /**
     * Listener that will throw a ArcRuntimeException with error name.
     * 监听器,将抛出带有错误名称的 ArcRuntimeException。
     */
    GLErrorListener throwingListener = error -> {
        String place = getCallName();

        if(place != null){
            throw new RuntimeException(Strings.format("[GLProfiler] Error @ from @", error, place));
        }else{
            throw new RuntimeException(Strings.format("[GLProfiler] Error @", error));
        }
    };

    /**
     * Put your error logging code here.
     * 在此编写你的错误日志代码。
     */
    void onError(String error);

    static @Nullable String getCallName(){
        String place = null;
        try{
            final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for(int i = 0; i < stack.length; i++){
                if("check".equals(stack[i].getMethodName())){
                    if(i + 1 < stack.length){
                        final StackTraceElement glMethod = stack[i + 1];
                        place = glMethod.getMethodName();
                    }
                    break;
                }
            }
        }catch(Exception ignored){
        }
        return place;
    }
}
