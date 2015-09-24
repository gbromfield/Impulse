package com.grb.impulse;

import java.net.InetSocketAddress;

public interface Transport {
    public InetSocketAddress getClientLocalAddress();
    public InetSocketAddress getClientRemoteAddress();

    public InetSocketAddress getServerLocalAddress();
    public InetSocketAddress getServerRemoteAddress();
}
