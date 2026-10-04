/*
 * This file is a modified class of dnsjava, an implementation of the dns protocol in java.
 * Licensed under the BSD-3-Clause.
 */
package arc.net.dns;

import arc.files.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static arc.net.dns.ArcDns.*;

public final class ResolvConfNameserverProvider implements NameserverProvider{

    @Override
    public Ar<InetSocketAddress> getNameservers(){
        Ar<InetSocketAddress> out = new Ar<>();
        // first try the default unix config path
        if(!tryParseResolveConf("/etc/resolv.conf", out)){
            // then fallback to netware
            tryParseResolveConf("sys:/etc/resolv.cfg", out);
        }
        return out;
    }

    private boolean tryParseResolveConf(String path, Ar<InetSocketAddress> out){
        Fi conf = new Fi(path);

        if(conf.exists()){
            try(BufferedReader reader = conf.reader(1024)){
                String line;
                while((line = reader.readLine()) != null){
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    if(!tokenizer.hasMoreTokens()) continue;

                    if(tokenizer.nextToken().equals("nameserver")){
                        out.add(new InetSocketAddress(tokenizer.nextToken(), dnsResolverPort));
                    }
                }
                return true;
            }catch(Exception ignored){
            }
        }

        return false;
    }

    @Override
    public boolean isEnabled(){
        return !OS.isWindows && !OS.isAndroid;
    }
}
