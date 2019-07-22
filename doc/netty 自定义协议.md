#netty 自定义协议
##netty 是什么呢？
>相信很多人都被人问过这个问题。如果快速准确的回复这个问题呢？网络编程框架，netty可以让你快速和简单的开发出一个高性能的网络应用。netty是一个网络编程框架。那netty又有什么框框呢？主要有二个框。

##框1：客户和服务的启动

> 一切通讯都有收与发，所有的服务端和客户端都是这样的姿势启动。具体的参数可以看文档。

###服务端

```
 public void bind() throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();

        
        //需要两个线程组
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new ServerInit());

        // 绑定端口，同步等待成功
        b.bind(NettyConstant.REMOTE_PORT).sync();
            LOG.info("Netty server start : "
                + (NettyConstant.REMOTE_IP + " : " + NettyConstant.REMOTE_PORT));
    }
```
###客户端

```
          Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ClientInit());
            // 发起异步连接操作
            ChannelFuture future = b.connect(
                    new InetSocketAddress(host, port)).sync();
            channel = future.sync().channel();
            
            future.channel().closeFuture().sync();

```

##框2：处理器ChannelHandler
> netty将所有接收到或发出去的数据都交给处理器处理，***hannelInboundHandler***入站处理器，***ChannelOutboundHandler***出站处理器。入站既接收数据时触发，出站是发送时触发。包括编码器和解码器分别实现的也是这两个接口。所以我们要为服务端或客户端扩展功能的话！只要对应创建 对个类去实现这两个接口添加到通道上就行了。netty作为一个框架，当然也帮我们实现了很多经常的用的处理器了，如：StringDecoder(字串解码器)，StringEncoder(字串编码器),LengthFieldPrepender（给数据添加长度解决黏包半包问题），ReadTimeoutHandler（超时检测）

### 服务端出入站处理器添加

```

**
 * @author hdk
 * 类说明：服务端入站处理器初始化类
 */
public class ServerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        /*Netty提供的日志打印Handler，可以展示发送接收出去的字节*/
        //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        /*剥离接收到的消息的长度字段，拿到实际的消息报文的字节数组*/
        ch.pipeline().addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(65535,
                        0,2,0,
                        2));
        /*给发送出去的消息增加长度字段*/
        ch.pipeline().addLast("frameEncoder",
                new LengthFieldPrepender(2));
        /*反序列化，将字节数组转换为消息实体*/
        ch.pipeline().addLast(new KryoDecoder());
        /*序列化，将消息实体转换为字节数组准备进行网络传输*/
        ch.pipeline().addLast("MessageEncoder",
                new KryoEncoder());
        /*超时检测*/
        ch.pipeline().addLast("readTimeoutHandler",
                new ReadTimeoutHandler(10));
        /*登录应答*/
        ch.pipeline().addLast(new LoginAuthRespHandler());

        /*心跳应答*/
        ch.pipeline().addLast("HeartBeatHandler",
                new HeartBeatRespHandler());

        /*服务端业务处理*/
        ch.pipeline().addLast("ServerBusiHandler",
                new ServerBusiHandler());

    }
}

```
###客户端出入站处理器添加

```
/**
 * @author hdk
 * 类说明：客户端Handler的初始化
 */
public class ClientInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        /*剥离接收到的消息的长度字段，拿到实际的消息报文的字节数组*/
        ch.pipeline().addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(65535,
                        0,2,0,
                        2));

        /*给发送出去的消息增加长度字段*/
        ch.pipeline().addLast("frameEncoder",
                new LengthFieldPrepender(2));

        /*反序列化，将字节数组转换为消息实体*/
        ch.pipeline().addLast(new KryoDecoder());
        /*序列化，将消息实体转换为字节数组准备进行网络传输*/
        ch.pipeline().addLast("MessageEncoder",
                new KryoEncoder());

        /*超时检测*/
        ch.pipeline().addLast("readTimeoutHandler",
                new ReadTimeoutHandler(10));

        /*发出登录请求*/
        ch.pipeline().addLast("LoginAuthHandler",
                new LoginAuthReqHandler());

       /*发出心跳请求*/
        ch.pipeline().addLast("HeartBeatHandler",
                new HeartBeatReqHandler());
    }
}
```

### 处理器注意事项

> 入站：在入站实现方法中ChannelInboundHandler.channelRead必需release下一个处理器会接着触发。释放netty的ByteBuf给其他处理器读取： ReferenceCountUtil.release(msg);如果不想下个处理器处理就调用ctx.fireChannelRead(msg);
>
> 出站：在入站实现方法中ChannelOutboundHandler.write同样要调用ReferenceCountUtil.release(msg)，在其他的处理器中才可以读取本处理器处理过的数据，如果

##处理器的扩展编码器与解码器
> 在网络编程中定义一种高效编码规则是很重要的，节省带宽提交传输效率。提高传输速度。所以netty封装了两种特殊的处理器叫：ByteToMessageDecoder解码器和MessageToByteEncoder编码器。使用方法也很简单，继承重写一下decode和encode就可以了.。
### 解码器 ByteToMessageDecoder

```
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
```

###编码器 MessageToByteEncoder

```

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

```

#总结
> netty为什么那么流行，个人认为有几点：
> * 1事件编程让代码变得很简洁。
> * 2提供很多协议如mqtt,http,websocket,smtp,redis等等，所有可以很快速的就可以开发出这些协议的服务端程序，不用自己去实现了。
> * ![1563789405876](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1563789405876.png)

# netty扩展-自定义协议

##Netty协议栈消息定义包含两部分：消息头、消息体

![1563791266642](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1563791266642.png)

## Header

| **名称**   | **类型**           | **长度** | **描述**                                                     |
| ---------- | ------------------ | -------- | ------------------------------------------------------------ |
| crcCode    | Int                | 32       | Netty消息校验码                                              |
| Length     | Int                | 32       | 整个消息长度                                                 |
| sessionID  | Long               | 64       | 会话ID                                                       |
| Type       | Byte               | 8        | 0:业务请求消息1：业务响应消息2：业务one way消息3握手请求消息4握手应答消息5：心跳请求消息6：心跳应答消息 |
| Priority   | Byte               | 8        | 消息优先级：0~255                                            |
| Attachment | Map<String,Object> | 变长     | 可选字段，由于推展消息头                                     |

```
/**
 * @author hdk
 * 类说明：消息头
 */
public final class MyHeader {

    private int crcCode = 0xabef0101;

    private int length;// 消息长度

    private long sessionID;// 会话ID

    private byte type;// 消息类型

    private byte priority;// 消息优先级

    private Map<String, Object> attachment = new HashMap<String, Object>(); // 附件

    public final int getCrcCode() {
    	return crcCode;
    }

    public final void setCrcCode(int crcCode) {
    	this.crcCode = crcCode;
    }

    public final int getLength() {
    	return length;
    }

    public final void setLength(int length) {
    	this.length = length;
    }

    public final long getSessionID() {
    	return sessionID;
    }

    public final void setSessionID(long sessionID) {
    	this.sessionID = sessionID;
    }

    public final byte getType() {
    	return type;
    }

    public final void setType(byte type) {
    	this.type = type;
    }

    public final byte getPriority() {
    	return priority;
    }

    public final void setPriority(byte priority) {
    	this.priority = priority;
    }

    public final Map<String, Object> getAttachment() {
    	return attachment;
    }

    public final void setAttachment(Map<String, Object> attachment) {
	    this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "MyHeader [crcCode=" + crcCode + ", length=" + length
            + ", sessionID=" + sessionID + ", type=" + type + ", priority="
            + priority + ", attachment=" + attachment + "]";
    }

}

```

```
/**
 * @author hdk
 * 类说明：消息实体类
 */
public final class MyMessage {

    private MyHeader myHeader;

    private Object body;

    public final MyHeader getMyHeader() {
    	return myHeader;
    }

    public final void setMyHeader(MyHeader myHeader) {
    	this.myHeader = myHeader;
    }

    public final Object getBody() {
    	return body;
    }

    public final void setBody(Object body) {
    	this.body = body;
    }

    @Override
    public String toString() {
    	return "MyMessage [myHeader=" + myHeader + "][body="+body+"]";
    }
}

```