
package com.esotericsoftware.kryonetty;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Provides a skeleton Endpoint implementation using Netty IO.
 * @author Nathan Sweet
 */
public abstract class Client implements Endpoint {
	/**
	 * Netty bootstrap object used to create the channel.
	 */
	private Bootstrap bootstrap;
	/**
	 * Netty channel used to write objects on.
	 */
	private Channel channel;
	/**
	 * Connection timeout in milliseconds
	 */
	private static final int CONNECT_TIMEOUT = 5000;

	/**
	 * Create a new client connected to the given socket.
	 * @param serverAddress Server to connect to.
	 */
	public Client (SocketAddress serverAddress) {
	   EventLoopGroup workerGroup = new NioEventLoopGroup();
		ExecutorService threadPool = Executors.newCachedThreadPool();
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new KryoChannelHandler(this));
		// bootstrap.setOption("trafficClass", 0x10); // IPTOS_LOWDELAY
		ChannelFuture connect = bootstrap.connect(serverAddress);
		if (!connect.awaitUninterruptibly(CONNECT_TIMEOUT))
				throw new RuntimeException("Timeout connecting.");
		channel = connect.channel();
	}

	/**
	 * Write the given object to the channel.
	 * @param object
	 */
	public void send (Object object) {
		channel.writeAndFlush(object).awaitUninterruptibly(1000);
	}

	/**
	 * Close the channel.
	 */
	public void close () {
		channel.close();
		channel = null;
	}
}
