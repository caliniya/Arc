package arc.util.serialization;

/**
 * Indicates an error during serialization due to misconfiguration or during deserialization due to invalid input data.
 * <p>
 * 表示序列化期间因配置错误、或反序列化期间因输入数据无效而产生的错误。
 * @author Nathan Sweet
 */
public class SerializationException extends RuntimeException{
    private StringBuilder trace;

    public SerializationException(){
        super();
    }

    public SerializationException(String message, Throwable cause){
        super(message, cause);
    }

    public SerializationException(String message){
        super(message);
    }

    public SerializationException(Throwable cause){
        super("", cause);
    }

    /**
     * Returns true if any of the exceptions that caused this exception are of the specified type.
     * 如果导致此异常的任一异常属于指定类型,则返回 true。
     */
    public boolean causedBy(Class type){
        return causedBy(this, type);
    }

    private boolean causedBy(Throwable ex, Class<?> type){
        Throwable cause = ex.getCause();
        if(cause == null || cause == ex) return false;
        if(type.isAssignableFrom(cause.getClass())) return true;
        return causedBy(cause, type);
    }

    public String getMessage(){
        if(trace == null) return super.getMessage();
        StringBuilder sb = new StringBuilder(512);
        sb.append(super.getMessage());
        if(sb.length() > 0) sb.append('\n');
        sb.append("Serialization trace:");
        sb.append(trace);
        return sb.toString();
    }

    /**
     * Adds information to the exception message about where in the the object graph serialization failure occurred. Serializers
     * can catch {@link SerializationException}, add trace information, and rethrow the exception.
     * <p>
     * 向异常消息中添加有关对象图中序列化失败发生位置的信息。序列化器可以捕获 {@link SerializationException},添加跟踪信息后重新抛出异常。
     */
    public void addTrace(String info){
        if(info == null) throw new IllegalArgumentException("info cannot be null.");
        if(trace == null) trace = new StringBuilder(512);
        trace.append('\n');
        trace.append(info);
    }
}
