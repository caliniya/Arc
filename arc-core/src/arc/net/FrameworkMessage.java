package arc.net;

/**
 * Marker interface for internal messages.
 * <p>
 * 内部消息的标记接口。
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface FrameworkMessage{
    KeepAlive keepAlive = new KeepAlive();
    DiscoverHost discoverHost = new DiscoverHost();

    /**
     * Internal message to give the client the server assigned connection ID.
     * <p>
     * 向客户端提供服务端分配的连接 ID 的内部消息。
     */
    class RegisterTCP implements FrameworkMessage{
        public int connectionID;
    }

    /**
     * Internal message to give the server the client's UDP port.
     * <p>
     * 向服务端提供客户端 UDP 端口的内部消息。
     */
    class RegisterUDP implements FrameworkMessage{
        public int connectionID;
    }

    /**
     * Internal message to keep connections alive.
     * <p>
     * 保持连接存活的内部消息。
     */
    class KeepAlive implements FrameworkMessage{
    }

    /**
     * Internal message to discover running servers.
     * <p>
     * 发现正在运行的服务端的内部消息。
     */
    class DiscoverHost implements FrameworkMessage{
    }

    /**
     * Internal message to determine round trip time.
     * <p>
     * 确定往返时间的内部消息。
     */
    class Ping implements FrameworkMessage{
        public int id;
        public boolean isReply;
    }
}
