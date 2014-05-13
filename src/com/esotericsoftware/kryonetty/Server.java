
package com.esotericsoftware.kryonetty;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
//TODO: import io.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Skeleton Kryo server implementation using Netty.
 * @author Nathan Sweet
 */
public abstract class Server implements Endpoint {
	private ServerBootstrap bootstrap;
	private Channel channel;

	public Server (int port) {
		ExecutorService threadPool = Executors.newCachedThreadPool();
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
      EventLoopGroup workerGroup = new NioEventLoopGroup();
		bootstrap = new ServerBootstrap();
		bootstrap
			.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
	         @Override
	         public void initChannel(SocketChannel ch) throws Exception {
	             ch.pipeline().addLast(new KryoChannelHandler(Server.this));
	         }
		   })
		   .option(ChannelOption.TCP_NODELAY, true);
//TODO:		bootstrap.setOption("child.reuseAddress", true);
		channel = bootstrap.bind(new InetSocketAddress(port)).channel();
	}

	public void close () {
		channel.close();
		channel = null;
	}
}
