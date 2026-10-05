package arc.util.io;

import arc.func.*;
import arc.util.*;

import java.io.*;
import java.nio.*;
import java.security.*;

/**
 * Provides utility methods to copy streams.
 * 提供复制流的实用方法。
 */
public final class Streams{
    public static final int defaultBufferSize = 8192;
    public static final byte[] emptyBytes = new byte[0];

    /**
     * Allocates a {@value #defaultBufferSize} byte[] for use as a temporary buffer and calls
     * {@link #copy(InputStream, OutputStream, byte[])}.
     * <p>
     * 分配 {@value #defaultBufferSize} 大小的 byte[] 作为临时缓冲区,并调用 {@link #copy(InputStream, OutputStream, byte[])}。
     */
    public static void copy(InputStream input, OutputStream output) throws IOException{
        copy(input, output, new byte[defaultBufferSize]);
    }

    /**
     * Allocates a byte[] of the specified size for use as a temporary buffer and calls
     * {@link #copy(InputStream, OutputStream, byte[])}.
     * <p>
     * 分配指定大小的 byte[] 作为临时缓冲区,并调用 {@link #copy(InputStream, OutputStream, byte[])}。
     */
    public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException{
        copy(input, output, new byte[bufferSize]);
    }

    /**
     * Copy the data from an {@link InputStream} to an {@link OutputStream}, using the specified byte[] as a temporary buffer.
     * The stream is not closed.
     * <p>
     * 使用指定的 byte[] 作为临时缓冲区,将数据从 {@link InputStream} 复制到 {@link OutputStream}。不关闭流。
     */
    public static void copy(InputStream input, OutputStream output, byte[] buffer) throws IOException{
        int bytesRead;
        while((bytesRead = input.read(buffer)) != -1){
            output.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Copy the data from an {@link InputStream} to an {@link OutputStream}, using the specified byte[] as a temporary buffer.
     * The stream is not closed.
     * Provides progress as a 0-1 value through the specified listener.
     * <p>
     * 使用指定的 byte[] 作为临时缓冲区,将数据从 {@link InputStream} 复制到 {@link OutputStream}。不关闭流。通过指定的监听器以 0-1 的值提供进度。
     * @param totalLength the total byte length of the input. 输入的总字节长度。
     */
    public static void copyProgress(InputStream input, OutputStream output, long totalLength, int bufferSize, Floatc progress) throws IOException{
        byte[] buffer = new byte[bufferSize];
        long totalRead = 0;
        int bytesRead;
        while((bytesRead = input.read(buffer)) != -1){
            totalRead += bytesRead;
            progress.get(totalRead / (float)totalLength);
            output.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Allocates a {@value #defaultBufferSize} byte[] for use as a temporary buffer and calls
     * {@link #copy(InputStream, OutputStream, byte[])}.
     * <p>
     * 分配 {@value #defaultBufferSize} 大小的 byte[] 作为临时缓冲区,并调用 {@link #copy(InputStream, OutputStream, byte[])}。
     */
    public static void copy(InputStream input, ByteBuffer output) throws IOException{
        copy(input, output, new byte[defaultBufferSize]);
    }

    /**
     * Allocates a byte[] of the specified size for use as a temporary buffer and calls
     * {@link #copy(InputStream, ByteBuffer, byte[])}.
     * <p>
     * 分配指定大小的 byte[] 作为临时缓冲区,并调用 {@link #copy(InputStream, ByteBuffer, byte[])}。
     */
    public static void copy(InputStream input, ByteBuffer output, int bufferSize) throws IOException{
        copy(input, output, new byte[bufferSize]);
    }

    /**
     * Copy the data from an {@link InputStream} to a {@link ByteBuffer}, using the specified byte[] as a temporary buffer. The
     * buffer's limit is increased by the number of bytes copied, the position is left unchanged. The stream is not closed.
     * <p>
     * 使用指定的 byte[] 作为临时缓冲区,将数据从 {@link InputStream} 复制到 {@link ByteBuffer}。缓冲区的 limit 增加所复制的字节数,position 保持不变。不关闭流。
     * @param output Must be a direct Buffer with native byte order and the buffer MUST be large enough to hold all the bytes in 必须是具有本机字节序的直接 Buffer,且缓冲区必须足够大以容纳其中的全部字节
     * the stream. No error checking is performed.
     * @return the number of bytes copied. 复制的字节数。
     */
    public static int copy(InputStream input, ByteBuffer output, byte[] buffer) throws IOException{
        int startPosition = output.position(), total = 0, bytesRead;
        while((bytesRead = input.read(buffer)) != -1){
            Buffers.copy(buffer, 0, output, bytesRead);
            total += bytesRead;
            output.position(startPosition + total);
        }
        output.position(startPosition);
        return total;
    }

    /**
     * Copy the data from an {@link InputStream} to a byte array. The stream is not closed.
     * 将数据从 {@link InputStream} 复制到 byte 数组。不关闭流。
     */
    public static byte[] copyBytes(InputStream input) throws IOException{
        return copyBytes(input, input.available());
    }

    /**
     * Copy the data from an {@link InputStream} to a byte array. The stream is not closed.
     * <p>
     * 将数据从 {@link InputStream} 复制到 byte 数组。不关闭流。
     * @param estimatedSize Used to allocate the output byte[] to possibly avoid an array copy. 用于分配输出 byte[],以尽量避免数组复制。
     */
    public static byte[] copyBytes(InputStream input, int estimatedSize) throws IOException{
        ByteArrayOutputStream baos = new OptimizedByteArrayOutputStream(Math.max(0, estimatedSize));
        copy(input, baos);
        return baos.toByteArray();
    }

    /**
     * Calls {@link #copyString(InputStream, int, String)} using the input's {@link InputStream#available() available} size
     * and the platform's default charset.
     * <p>
     * 使用输入的 {@link InputStream#available() available} 大小和平台默认字符集调用 {@link #copyString(InputStream, int, String)}。
     */
    public static String copyString(InputStream input) throws IOException{
        return copyString(input, input.available(), null);
    }

    /**
     * Calls {@link #copyString(InputStream, int, String)} using the platform's default charset.
     * 使用平台默认字符集调用 {@link #copyString(InputStream, int, String)}。
     */
    public static String copyString(InputStream input, int estimatedSize) throws IOException{
        return copyString(input, estimatedSize, null);
    }

    /**
     * Copy the data from an {@link InputStream} to a string using the specified charset.
     * <p>
     * 使用指定字符集将数据从 {@link InputStream} 复制为字符串。
     * @param estimatedSize Used to allocate the output buffer to possibly avoid an array copy. 用于分配输出缓冲区,以尽量避免数组复制。
     * @param charset May be null to use the platform's default charset. 可为 null,表示使用平台默认字符集。
     */
    public static String copyString(InputStream input, int estimatedSize, String charset) throws IOException{
        InputStreamReader reader = new InputStreamReader(input, charset == null ? "UTF-8" : charset);
        StringWriter writer = new StringWriter(Math.max(0, estimatedSize));
        char[] buffer = new char[defaultBufferSize];
        int charsRead;
        while((charsRead = reader.read(buffer)) != -1){
            writer.write(buffer, 0, charsRead);
        }
        return writer.toString();
    }

    /**
     * Close and ignore all errors.
     * 关闭并忽略所有错误。
     */
    public static void close(Closeable c){
        if(c != null){
            try{
                c.close();
            }catch(Throwable ignored){
            }
        }
    }

    /**
     * @return sha256 hash of provided byte array
     * 所提供 byte 数组的 sha256 哈希值
     */
    public static byte[] sha256(byte[] bytes){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return digest.digest();
        }catch(NoSuchAlgorithmException e){
            throw new ArcRuntimeException(e);
        }
    }

    /**
     * A ByteArrayOutputStream which avoids copying of the byte array if possible.
     * 一个 ByteArrayOutputStream,尽可能避免复制字节数组。
     */
    public static class OptimizedByteArrayOutputStream extends ByteArrayOutputStream{
        public OptimizedByteArrayOutputStream(int initialSize){
            super(initialSize);
        }

        @Override
        public synchronized byte[] toByteArray(){
            if(count == buf.length) return buf;
            return super.toByteArray();
        }

        public byte[] getBuffer(){
            return buf;
        }
    }
}
