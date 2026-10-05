

package arc.net;

import arc.net.FrameworkMessage.DiscoverHost;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface ServerDiscoveryHandler{
    /**
     * Called when the {@link Server} receives a {@link DiscoverHost} packet.
     * <p>
     * 当 {@link Server} 收到 {@link DiscoverHost} 数据包时调用。
     * @throws IOException from sending a response. 由发送响应抛出。
     */
    void onDiscoverReceived(InetAddress address, ReponseHandler handler) throws IOException;

    interface ReponseHandler{
        void respond(ByteBuffer buffer) throws IOException;
    }
}
