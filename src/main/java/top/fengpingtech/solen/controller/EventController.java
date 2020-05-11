package top.fengpingtech.solen.controller;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@ServerEndpoint("/event/{deviceId}")
public class EventController {
    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            while (true) {
                sessions.forEach( (d, s) -> {
                    try {
                        s.getBasicRemote().sendText("text");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setName("text-sender");
        thread.start();
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceId") String deviceId) {
        System.out.println("onOpen, device: " + deviceId);
        sessions.putIfAbsent(deviceId, session);
    }

    @OnClose
    public void onClose() {
        System.out.println("close");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("on error");
    }
}
