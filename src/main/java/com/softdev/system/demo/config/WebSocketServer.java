package com.softdev.system.demo.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.softdev.system.demo.vo.ClientVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * WebSocketServer WebSocket服务端代码，包含接收消息，推送消息等接口
 * @author zhengkai.blog.csdn.net
 */
@Slf4j
@ServerEndpoint("/mwc/client/{type}/{uuid}/{clientId}")
@Component
public class WebSocketServer {

    /**静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。*/
    private static AtomicInteger onlineCount = new AtomicInteger();
    /**concurrent包的线程安全Map，用来存放每个客户端对应的MyWebSocket对象。*/
    private static ConcurrentHashMap<String,WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;
    /**接收userId*/
    private String userId="";

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session, @PathParam("type") String type, @PathParam("uuid") String uuid) {
        log.info("onOpen:");
        this.session = session;

        // 判断类型是手机还是pc，如果是pc，则直接入库；如果是手机，根据clientId查询client表clientId字段，
        // 是否存在已连接的记录，如果存在，则提示用户已被xx连接；如果不存在，则直接入库
        if (type.equalsIgnoreCase("pc")) {
            // 入库
        } else {
            // 根据clientId查询client表clientId字段，如果存在则根据记录的uuid查询map中链接的有效性
            //如果有效，提示用户；如果无效则允许手机链接，入库
        }

        if(webSocketMap.containsKey(uuid)){
            webSocketMap.remove(uuid);
            webSocketMap.put(uuid,this);
            //加入set中
        }else{
            webSocketMap.put(uuid,this);
            //加入set中
            addOnlineCount();
            //在线数加1
        }
        ClientVO clientVO = new ClientVO();
        clientVO.setType(type);
        clientVO.setUuid(uuid);


        log.info("用户连接:"+uuid+",当前在线人数为:" + getOnlineCount());

        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户:"+uuid+",网络异常!!!!!!");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if(webSocketMap.containsKey(userId)){
            webSocketMap.remove(userId);
            //从set中删除
            subOnlineCount();
        }
        log.info("用户退出:"+userId+",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息:"+userId+",报文:"+message);
        //可以群发消息
        //消息保存到数据库、redis
        if(StringUtils.isNotBlank(message)){
            try {
                //解析发送的报文
                JSONObject jsonObject = JSON.parseObject(message);
                //追加发送人(防止串改)
                jsonObject.put("fromUserId",this.userId);
                String toUserId=jsonObject.getString("toUserId");
                //传送给对应toUserId用户的websocket
                if(StringUtils.isNotBlank(toUserId)&&webSocketMap.containsKey(toUserId)){
                    webSocketMap.get(toUserId).sendMessage(jsonObject.toJSONString());
                }else{
                    log.error("请求的userId:"+toUserId+"不在该服务器上");
                    //否则不在这个服务器上，发送到mysql或者redis
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:"+this.userId+",原因:"+error.getMessage());
        error.printStackTrace();
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 发送自定义消息
     * */
    public static void sendInfo(String message,@PathParam("userId") String userId) throws IOException {
        log.info("发送消息到:"+userId+"，报文:"+message);
        if(StringUtils.isNotBlank(userId)&&webSocketMap.containsKey(userId)){
            webSocketMap.get(userId).sendMessage(message);
        }else{
            log.error("用户"+userId+",不在线！");
        }
    }

    public static synchronized AtomicInteger getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount.incrementAndGet();
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount.decrementAndGet();
    }
}