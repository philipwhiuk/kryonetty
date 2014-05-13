package com.esotericsoftware.kryonetty;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;

import com.esotericsoftware.kryo.Kryo;

/**
 * An implementation of Client that uses the listener model as per KryoNet.
 * @author Philip Whitehouse
 *
 */
public class ClientListenerImpl extends Client {
	private List<Listener> listeners;
	private Kryo kryo;


	public ClientListenerImpl (SocketAddress serverAddress) {
		super(serverAddress);
		listeners = new ArrayList<Listener>();
	}
	
	public void addListener(Listener l) {
		listeners.add(l);
	}
	
	@Override
	public void connected (ChannelHandlerContext ctx) {
		for(Listener listener : listeners) {
			listener.connected(ctx);
		}
	}

	@Override
	public void disconnected (ChannelHandlerContext ctx) {
		for(Listener listener : listeners) {
			listener.disconnected(ctx);
		}
	}

	@Override
	public void received (ChannelHandlerContext ctx, Object object) {
		for(Listener listener : listeners) {
			listener.received(ctx, object);
		}
	}

	@Override
	public Kryo getKryo () {
		return kryo;
	}

}
