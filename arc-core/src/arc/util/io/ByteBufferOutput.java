package arc.util.io;

import java.io.*;
import java.nio.*;

/**
 * DataOutput wrapper of ByteBuffer.
 * ByteBuffer 的 DataOutput 包装器。
 */
public class ByteBufferOutput implements DataOutput{
    public ByteBuffer buffer;

    /**
     * Wraps the specified ByteBuffer.
     * 包装指定的 ByteBuffer。
     */
    public ByteBufferOutput(ByteBuffer buffer){
        this.buffer = buffer;
    }

    /**
     * buffer must be set before this object can be used.
     * 必须先设置 buffer,此对象才能使用。
     */
    public ByteBufferOutput(){
    }

    @Override
    public void write(int i){
        buffer.put((byte) i);
    }

    @Override
    public void write(byte[] bytes){
        buffer.put(bytes);
    }

    @Override
    public void write(byte[] bytes, int off, int len){
        buffer.put(bytes, off, len);
    }

    @Override
    public void writeBoolean(boolean b){
        buffer.put(b ? (byte) 1 : 0);
    }

    @Override
    public void writeByte(int i){
        buffer.put((byte) i);
    }

    @Override
    public void writeShort(int i){
        buffer.putShort((short) i);
    }

    @Override
    public void writeChar(int i){
        buffer.putChar((char) i);
    }

    @Override
    public void writeInt(int i){
        buffer.putInt(i);
    }

    @Override
    public void writeLong(long l){
        buffer.putLong(l);
    }

    @Override
    public void writeFloat(float v){
        buffer.putFloat(v);
    }

    @Override
    public void writeDouble(double v){
        buffer.putDouble(v);
    }

    @Override
    public void writeBytes(String s){
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeChars(String s){
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeUTF(String s) throws IOException{
        final int strlen = s.length();
        int utflen = strlen; // optimized for ASCII
        // 针对 ASCII 优化

        for(int i = 0; i < strlen; i++){
            int c = s.charAt(i);
            if(c >= 0x80 || c == 0)
                utflen += (c >= 0x800) ? 2 : 1;
        }

        if(utflen > 65535 || /* overflow 溢出 */ utflen < strlen)
            throw new UTFDataFormatException("encoded string too long");

        byte[] bytearr = new byte[utflen + 2];

        int count = 0;
        bytearr[count++] = (byte)((utflen >>> 8) & 0xFF);
        bytearr[count++] = (byte)((utflen >>> 0) & 0xFF);

        int i = 0;
        for(i = 0; i < strlen; i++){ // optimized for initial run of ASCII
        // 针对开头的 ASCII 连续段优化
            int c = s.charAt(i);
            if(c >= 0x80 || c == 0) break;
            bytearr[count++] = (byte)c;
        }

        for(; i < strlen; i++){
            int c = s.charAt(i);
            if(c < 0x80 && c != 0){
                bytearr[count++] = (byte)c;
            }else if(c >= 0x800){
                bytearr[count++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
                bytearr[count++] = (byte)(0x80 | ((c >> 6) & 0x3F));
                bytearr[count++] = (byte)(0x80 | ((c >> 0) & 0x3F));
            }else{
                bytearr[count++] = (byte)(0xC0 | ((c >> 6) & 0x1F));
                bytearr[count++] = (byte)(0x80 | ((c >> 0) & 0x3F));
            }
        }
        write(bytearr, 0, utflen + 2);
    }

}
