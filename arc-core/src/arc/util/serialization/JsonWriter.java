package arc.util.serialization;

import java.io.*;

public interface JsonWriter extends Closeable{
    JsonWriter name(String name);

    JsonWriter object();

    JsonWriter array();

    JsonWriter value(Object value);

    JsonWriter object(String name);

    JsonWriter array(String name);

    JsonWriter set(String name, Object value);

    JsonWriter pop();

    /**
     * Starts a new object. Equivalent to {@link #object()}; provided for readability at call sites.
     * 开始一个新对象。等价于 {@link #object()};为提高调用处的可读性而提供。
     */
    default JsonWriter writeObjectStart(){
        return object();
    }

    /**
     * Starts a new named object. Equivalent to {@link #object(String)}.
     * 开始一个新的具名对象。等价于 {@link #object(String)}。
     */
    default JsonWriter writeObjectStart(String name){
        return object(name);
    }

    /**
     * Ends the current object. Equivalent to {@link #pop()}.
     * 结束当前对象。等价于 {@link #pop()}。
     */
    default JsonWriter writeObjectEnd(){
        return pop();
    }

    /**
     * Starts a new array. Equivalent to {@link #array()}.
     * 开始一个新数组。等价于 {@link #array()}。
     */
    default JsonWriter writeArrayStart(){
        return array();
    }

    /**
     * Starts a new named array. Equivalent to {@link #array(String)}.
     * 开始一个新的具名数组。等价于 {@link #array(String)}。
     */
    default JsonWriter writeArrayStart(String name){
        return array(name);
    }

    /**
     * Ends the current array. Equivalent to {@link #pop()}.
     * 结束当前数组。等价于 {@link #pop()}。
     */
    default JsonWriter writeArrayEnd(){
        return pop();
    }
}