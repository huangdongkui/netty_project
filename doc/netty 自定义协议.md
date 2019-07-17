#netty 自定义协议
##netty 是什么呢？
>相信很多人都被人问过这个问题。如果快速准确的回复这个问题呢？网络编程框架，netty可以让你快速和简单的开发出一个高性能的网络应用。netty是一个网络编程框架。那netty又有什么框框呢？

##框1：客户和服务的启动

>所有的服务端和客户端都是这样的姿势启动。

**服务端**

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
**客户端**

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
> 