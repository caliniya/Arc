

package arc.net;

import java.nio.ByteBuffer;

/**
 * Controls how objects are transmitted over the network.
 * <p>
 * Every client connection on the server uses a separate instance for TCP
 * transmissions and the <i>same</i> instance for UDP ones. Therefore all
 * implementing classes have to be synchronized or made thread-safe otherwise.
 * <p>
 * 控制对象如何通过网络传输。
 * 服务端上的每个客户端连接都使用一个单独的实例进行 TCP 传输,而 UDP 传输则使用<i>同一个</i>实例。因此,所有实现类都必须是同步的,或以其他方式保证线程安全。
 */
public interface NetSerializer{

    void write(ByteBuffer buffer, Object object);

    Object read(ByteBuffer buffer);

    /**
     * The fixed number of bytes that will be written by
     * {@link #writeLength(ByteBuffer, int)} and read by
     * {@link #readLength(ByteBuffer)}.
     * <p>
     * 由 {@link #writeLength(ByteBuffer, int)} 写入、并由 {@link #readLength(ByteBuffer)} 读取的固定字节数。
     */
    default int getLengthLength(){
        return 2;
    }

    default void writeLength(ByteBuffer buffer, int length){
        buffer.putShort((short)length);
    }

    default int readLength(ByteBuffer buffer){
        return buffer.getShort() & 0xffff; //convert to unsigned short for a higher max packet size
        // 转换为无符号 short,以支持更大的最大数据包大小
    }
}
