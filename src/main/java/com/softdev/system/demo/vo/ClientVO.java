package com.softdev.system.demo.vo;

import com.softdev.system.demo.config.WebSocketServer;
import lombok.Data;

import java.util.Date;

/**
 * @className: ClientVO
 * @description:
 * @author: jerico
 * @date: 2020-02-22 05:56
 * @version:
 */
@Data
public class ClientVO {
    private String id;
    private String uuid;
    private String type;
    private String area;
    private Date onlineDate;
    private Date offlineDate;
    private String status;
    /**
     *     与服务器的链接
     */
    private WebSocketServer  webSocketServer;
    /**
     *     被控制的客户端id
     */
    private String clientId;

    /**
     *     被控制的客户端连接
     */
    private WebSocketServer clientWebSocketServer;


}
