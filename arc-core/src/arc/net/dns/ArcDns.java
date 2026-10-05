package arc.net.dns;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Streams.*;

import java.io.*;
import java.net.*;

public final class ArcDns{

    /**
     * Default dns server port.
     * 默认 DNS 服务端口。
     */
    public static final int dnsResolverPort = 53;

    private static final Ar<InetSocketAddress> nameservers = new Ar<>(3);
    private static final Ar<NameserverProvider> nameserverProviders = Ar.with(
    new JndiContextNameserverProvider(),    // Simple JRE installation
    // 简单的 JRE 安装
    new ResolvConfNameserverProvider(),     // Unix/Linux
    new WellKnownNameserverProvider()       // Others
    // 其他
    );

    static{
        refreshNameservers();
    }

    public static Ar<NameserverProvider> getNameserverProviders(){
        return nameserverProviders;
    }

    /**
     * Set a new ordered list of resolver config providers.
     * 设置一个新的、有序的解析器配置提供者列表。
     */
    public static void setNameserverProviders(Ar<NameserverProvider> providers){
        nameserverProviders.clear();
        nameserverProviders.addAll(providers);
        refreshNameservers();
    }

    /**
     * Returns all located servers
     * 返回所有已找到的服务器
     */
    public static Ar<InetSocketAddress> getNameservers(){
        return nameservers;
    }

    public static void refreshNameservers(){
        nameservers.clear();

        if(Core.app != null){
            Core.app.getDnsServers(nameservers);
        }

        for(NameserverProvider provider : nameserverProviders){
            if(provider.isEnabled()){
                try{
                    nameservers.addAll(provider.getNameservers());
                    // Stop when a name server is found
                    // 找到域名服务器后即停止
                    if(!nameservers.isEmpty()) return;
                }catch(Exception e){
                    Log.warn("[DNS] Failed to initialize provider: @", e);
                }
            }
        }

        for(InetSocketAddress server : nameservers){
            Log.debug("[DNS] Added @ to nameservers", server);
        }

        // Add localhost as nameserver if no suitable nameserver provider found
        // 如果没有找到合适的域名服务器提供者,则将 localhost 添加为域名服务器
        nameservers.add(new InetSocketAddress(InetAddress.getLoopbackAddress(), dnsResolverPort));
    }

    /**
     * Lookup the SRV record of a domain in the format {@code _service._protocol.name}
     * with the list of nameservers from {@link #getNameservers()}.
     * <p>
     * 使用 {@link #getNameservers()} 中的域名服务器列表,查找格式为 {@code _service._protocol.name} 的域名的 SRV 记录。
     */
    public static Ar<SRVRecord> getSrvRecords(String domain){
        for(InetSocketAddress nameserver : nameservers){
            try{
                return getSrvRecords(domain, nameserver);
            }catch(IOException ignored){
            }
        }
        return new Ar<>(1);
    }

    /**
     * Lookup the SRV record of a domain in the format {@code _service._protocol.name}.
     * The results are sorted by priority, then by weight.
     * <p>
     * 查找格式为 {@code _service._protocol.name} 的域名的 SRV 记录。
     * 结果先按优先级排序,再按权重排序。
     */
    public static Ar<SRVRecord> getSrvRecords(String domain, InetSocketAddress nameserver) throws IOException{
        Ar<SRVRecord> records = new Ar<>();

        try(DatagramSocket socket = new DatagramSocket()){
            socket.setSoTimeout(2000);

            short id = (short)new Rand().nextInt(Short.MAX_VALUE);
            byte[] response = new byte[512];

            try(ByteArrayOutputStream stream = new OptimizedByteArrayOutputStream(128);
                DataOutputStream out = new DataOutputStream(stream)
            ){
                out.writeShort(id);         // Id
                // ID
                out.writeShort(0x0100);     // Flags (recursion enabled)
                // 标志(启用递归)
                out.writeShort(1);          // Questions
                // 问题数
                out.writeShort(0);          // Answers
                // 回答数
                out.writeShort(0);          // Authority
                // 权威记录数
                out.writeShort(0);          // Additional
                // 附加记录数

                // Domain
                // 域名
                for(String part : domain.split("\\.")){
                    out.writeByte(part.length());
                    out.write(part.getBytes(Strings.utf8));
                }
                out.writeByte(0);

                out.writeShort(33);         // Type (SRV)
                // 类型(SRV)
                out.writeShort(1);          // Class (Internet)
                // 类别(Internet)

                socket.send(new DatagramPacket(stream.toByteArray(), stream.size(), nameserver));
            }

            DatagramPacket packet = new DatagramPacket(response, response.length);
            socket.receive(packet);

            try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(response))){
                short responseId = in.readShort();
                if(responseId != id) throw new IOException("Invalid response from dns server " + nameserver);

                in.readShort();
                in.readShort();
                int answers = in.readUnsignedShort();
                in.readShort();
                in.readShort();

                byte len;
                while((len = in.readByte()) != 0){
                    in.skipBytes(len);
                }

                in.readShort();
                in.readShort();

                for(int i = 0; i < answers; i++){
                    in.readShort();                         // OFFSET
                    // 偏移量
                    in.readShort();                         // Type
                    // 类型
                    in.readShort();                         // Class
                    // 类别
                    long ttl = in.readInt() & 0xFFFFFFFFL;  // TTL
                    in.readShort();                         // Data length
                    // 数据长度

                    int priority = in.readUnsignedShort();
                    int weight = in.readUnsignedShort();
                    int port = in.readUnsignedShort();

                    StringBuilder builder = new StringBuilder();
                    while((len = in.readByte()) != 0){
                        for(int j = 0; j < len; j++) builder.append((char)in.readByte());
                        builder.append('.');
                    }
                    builder.delete(builder.length() - 1, builder.length());

                    records.add(new SRVRecord(ttl, priority, weight, port, builder.toString()));
                }
            }
        }

        return records.sort();
    }
}
