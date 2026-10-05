package arc.util.io;

import java.io.*;

/**
 * A wrapper for DataOutput with more concise method names and no IOExceptions.
 * DataOutput 的封装,方法名更简洁且不抛出 IOException。
 */
public class Writes implements Closeable{
    public DataOutput output;

    public Writes(DataOutput output){
        this.output = output;
    }

    /**
     * write long
     * 写出 long
     */
    public void l(long i){
        try{
            output.writeLong(i);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write int
     * 写出 int
     */
    public void i(int i){
        try{
            output.writeInt(i);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write byte
     * 写出 byte
     */
    public void b(int i){
        try{
            output.writeByte(i);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write bytes
     * 写出 byte 数组
     */
    public void b(byte[] array, int offset, int length){
        try{
            output.write(array, offset, length);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write bytes
     * 写出 byte 数组
     */
    public void b(byte[] array){
        b(array, 0, array.length);
    }

    /**
     * write boolean (writes a byte internally)
     * 写出 boolean(内部写一个 byte)
     */
    public void bool(boolean b){
        b(b ? 1 : 0);
    }

    /**
     * write short
     * 写出 short
     */
    public void s(int i){
        try{
            output.writeShort(i);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write float
     * 写出 float
     */
    public void f(float f){
        try{
            output.writeFloat(f);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * write double
     * 写出 double
     */
    public void d(double d){
        try{
            output.writeDouble(d);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * writes a string (UTF)
     * 写出字符串(UTF)
     */
    public void str(String str){
        try{
            output.writeUTF(str);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(){
        if(output instanceof Closeable){
            try{
                ((Closeable)output).close();
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }
    }
}
