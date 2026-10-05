/*
 * This file is a modified class of dnsjava, an implementation of the dns protocol in java.
 * Licensed under the BSD-3-Clause.
 * 本文件是 dnsjava(一个用 Java 实现的 DNS 协议库)的修改版类。
 * 采用 BSD-3-Clause 许可。
 */
package arc.net.dns;

import arc.struct.*;

import java.net.*;

public interface NameserverProvider{
    /**
     * Returns all located servers, which may be empty.
     * 返回所有已找到的服务器,可能为空。
     */
    Ar<InetSocketAddress> getNameservers();

    /**
     * Determines if this provider is enabled.
     * 确定此提供者是否已启用。
     */
    default boolean isEnabled(){
        return true;
    }
}
