package prv.hdk.serverandclient.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import prv.hdk.serverandclient.vo.MyMessage;

/**
 * @author hdk
 * 类说明：业务处理类
 */

public class ServerBusiHandler extends SimpleChannelInboundHandler<MyMessage> {
    private static final Log LOG = LogFactory.getLog(ServerBusiHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MyMessage msg)
            throws Exception {
        LOG.info(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info(ctx.channel().remoteAddress()+" 主动断开了连接!");
    }

}
