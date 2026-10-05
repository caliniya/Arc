/*
 * This file is a modified class of dnsjava, an implementation of the dns protocol in java.
 * Licensed under the BSD-3-Clause.
 * 本文件是 dnsjava(一个用 Java 实现的 DNS 协议库)的修改版类。
 * 采用 BSD-3-Clause 许可。
 */
package arc.net.dns;

import arc.struct.*;
import arc.util.*;

import javax.naming.*;
import javax.naming.directory.*;
import java.net.*;
import java.util.*;

import static arc.net.dns.ArcDns.dnsResolverPort;

/**
 * Resolver config provider that tries to extract the system's DNS servers from the
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html">JNDI DNS Service Provider</a>.
 * <p>
 * 解析器配置提供者,尝试从 <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html">JNDI DNS 服务提供者</a> 中提取系统的 DNS 服务器。
 */
public final class JndiContextNameserverProvider implements NameserverProvider{

    @Override
    public Ar<InetSocketAddress> getNameservers(){
        try{
            return new Inner().getNameservers();
        }catch(Throwable t){
            return new Ar<>();
        }
    }

    @Override
    public boolean isEnabled(){
        return !OS.isAndroid && !OS.isIos;
    }

    //loading a class fails if it refers to unknown classes (javax.naming is not available on some platforms), so put it in a separate inner class
    // 如果类引用了未知的类,加载会失败(某些平台上没有 javax.naming),因此将其放在单独的内部类中
    static class Inner implements NameserverProvider{

        @Override
        public Ar<InetSocketAddress> getNameservers(){
            Ar<InetSocketAddress> result = new Ar<>();

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            // http://mail.openjdk.java.net/pipermail/net-dev/2017-March/010695.html
            env.put("java.naming.provider.url", "dns://");

            String servers = null;
            try{
                DirContext ctx = new InitialDirContext(env);
                servers = (String)ctx.getEnvironment().get("java.naming.provider.url");
                ctx.close();
            }catch(NamingException ignored){
            }

            if(servers != null){
                StringTokenizer st = new StringTokenizer(servers, " ");
                while(st.hasMoreTokens()){
                    String server = st.nextToken();
                    try{
                        URI serverUri = new URI(server);
                        String host = serverUri.getHost();
                        if(host == null || host.isEmpty()){
                            // skip the fallback server to localhost
                            // 跳过回退到 localhost 的服务器
                            continue;
                        }

                        int port = serverUri.getPort();
                        if(port == -1){
                            port = dnsResolverPort;
                        }

                        result.add(new InetSocketAddress(host, port));
                    }catch(URISyntaxException e){
                        Log.debug("[DNS] Could not parse @ as a dns server, ignoring: @", server, e);
                    }
                }
            }

            return result;
        }
    }
}
