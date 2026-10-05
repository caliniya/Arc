

package arc.net;

import java.net.DatagramPacket;

public interface ClientDiscoveryHandler{
    /**
     * Implementations of this method should return a new {@link DatagramPacket}
     * that the {@link Client} will use to fill with the incoming packet data
     * sent by the {@link ServerDiscoveryHandler}.
     * <p>
     * 此方法的实现应返回一个新的 {@link DatagramPacket},供 {@link Client} 填充由 {@link ServerDiscoveryHandler} 发来的传入数据包数据。
     * @return a new {@link DatagramPacket} 一个新的 {@link DatagramPacket}。
     */
    DatagramPacket newDatagramPacket();

    /**
     * Called when the {@link Client} discovers a host.
     * <p>
     * 当 {@link Client} 发现主机时调用。
     * @param datagramPacket the same {@link DatagramPacket} from
     * {@link #newDatagramPacket()}, after being filled with
     * the incoming packet data. 来自 {@link #newDatagramPacket()} 的同一个 {@link DatagramPacket},已被填充为传入的数据包数据。
     */
    void discoveredHost(DatagramPacket datagramPacket);

    /**
     * Called right before the {@link Client#discoverHost(int, int)} or
     * {@link Client#discoverHosts(int, int)} method exits. This allows the
     * implementation to clean up any resources used.
     * <p>
     * 在 {@link Client#discoverHost(int, int)} 或 {@link Client#discoverHosts(int, int)} 方法退出前调用。这允许实现清理其使用的任何资源。
     */
    void finish();

}
