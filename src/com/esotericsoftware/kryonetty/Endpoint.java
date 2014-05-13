
package com.esotericsoftware.kryonetty;

import com.esotericsoftware.kryo.Kryo;

import io.netty.channel.ChannelHandlerContext;

/** @author Nathan Sweet */
public interface Endpoint {
	void connected (ChannelHandlerContext ctx);

	void disconnected (ChannelHandlerContext ctx);

	void received (ChannelHandlerContext ctx, Object object);

	Kryo getKryo ();
}
