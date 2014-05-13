
package com.esotericsoftware.kryonetty;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import static io.netty.buffer.Unpooled.*;

/**
 * A ChannelPipelineFactory to handle Kryo messages. 
 * @author Nathan Sweet
 * */
public class KryoChannelHandler extends ChannelInitializer {
	private static final int EXECUTOR_CORE_POOL_SIZE = 10;
	private static final long EXECUTOR_KEEP_ALIVE_TIME = 60;
	final Endpoint endpoint;
	
	//TODO: Fix
	// private Executor executor = new OrderedMemoryAwareThreadPoolExecutor(
	//		EXECUTOR_CORE_POOL_SIZE, 0, 0, EXECUTOR_KEEP_ALIVE_TIME, TimeUnit.SECONDS);

	/**
	 * Construct a new pipeline factory attached to the given end-point.
	 * @param endpoint
	 */
	public KryoChannelHandler (Endpoint endpoint) {
		super();
		this.endpoint = endpoint;
	}
	@Override
	public void initChannel(Channel channel) {
		Kryo kryo = endpoint.getKryo();
		channel.pipeline().addLast("decoder", new KryoDecoder(kryo));
		channel.pipeline().addLast("encoder", new KryoEncoder(kryo, 4 * 1024, 16 * 1024));
//TODO:		channel.pipeline().addLast("executor", new ExecutionHandler(executor));
		channel.pipeline().addLast("business", new SimpleChannelInboundHandler() {
			@Override
			public void channelActive(ChannelHandlerContext ctx)
					throws Exception {
				endpoint.connected(ctx);
			}
			@Override
			public void channelInactive(ChannelHandlerContext ctx)
					throws Exception {
				endpoint.disconnected(ctx);
			}
			@Override
			protected void channelRead0 (ChannelHandlerContext ctx, Object arg1)
					throws Exception {
				endpoint.received(ctx, arg1);
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
					throws Exception {
				//Probably can't do much about this.
				//We implement the default behaviour from SimpleChannelUpstreamHandler
				//to remove the error message
				ctx.fireExceptionCaught(e);
			}
		});
	}

	static private class KryoDecoder extends MessageToMessageDecoder<ByteBuf> {
		private final Kryo kryo;
		private final Input input = new Input();
		private int length = -1;

		public KryoDecoder (Kryo kryo) {
			this.kryo = kryo;
		}

		@Override
		protected void decode (ChannelHandlerContext ctx,
					ByteBuf msg, List<Object> out)
				throws Exception {
			final byte[] array;
			final int offset;
			final int length = msg.readableBytes();
			System.err.println(length);
			if (msg.hasArray()) {
            array = msg.array();
            offset = msg.arrayOffset() + msg.readerIndex();
         } else {
            array = new byte[length];
            msg.getBytes(msg.readerIndex(), array, 0, length);
            offset = 0;
         }
			
			input.setBuffer(array, offset, length);
			Object object = kryo.readClassAndObject(input);
			if(object != null) {
				out.add(object);
			}
			// Dumps out bytes for JMeter's TCP Sampler (BinaryTCPClientImpl classname):
			// System.out.println("--");
			// for (int i = buffer.readerIndex() - 4; i < input.position(); i++) {
			// String hex = Integer.toHexString(input.getBuffer()[i] & 0xff);
			// if (hex.length() == 1) hex = "0" + hex;
			// System.out.print(hex.toUpperCase());
			// }
			// System.out.println("\n--");

			return;
		}
	}

	static private class KryoEncoder extends MessageToMessageEncoder<Object> {
		private final Output output;
		private final Kryo kryo;

		public KryoEncoder (Kryo kryo, int bufferSize, int maxBufferSize) {
			this.kryo = kryo;
			output = new Output(bufferSize, maxBufferSize);
		}

		@Override
		protected void encode (ChannelHandlerContext ctx, Object msg, List<Object> out)
				throws Exception {
			output.clear();
			output.setPosition(4);
			kryo.writeClassAndObject(output, msg);
			int total = output.position();
			output.setPosition(0);
			output.writeInt(total - 4);
			out.add(wrappedBuffer(output.getBuffer()));
			return;
		}
	}
}
