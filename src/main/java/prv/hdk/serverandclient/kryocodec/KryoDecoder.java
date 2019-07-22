package prv.hdk.serverandclient.kryocodec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author hdk
 * 类说明：反序列化的Handler
 */
public class KryoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
                          List<Object> out) throws Exception {
        Object obj = KryoSerializer.deserialize(in);
        //数据信息处理后添加目标编码格式的数据到ByteBuf中就供后面的处理器处理。
        out.add(obj);
    }
}
