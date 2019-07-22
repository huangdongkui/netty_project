package prv.hdk.serverandclient.kryocodec;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import prv.hdk.serverandclient.vo.MyMessage;

/**
 * @author hdk
 * 类说明：序列化的Handler
 */
public class KryoEncoder  extends MessageToByteEncoder<MyMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MyMessage message,
                          ByteBuf out) throws Exception {
        KryoSerializer.serialize(message, out);
        //发送ByteBuf到目标IP
        ctx.flush();
    }
}
