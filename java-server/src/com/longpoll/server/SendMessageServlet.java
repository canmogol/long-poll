package com.longpoll.server;

import com.avaje.ebean.Ebean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

/**
 * acm 12/3/12 3:46 PM
 */
public class SendMessageServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log("new message send");

        // start connection if it did not
        DB.instance().connectToDB();

        // read sessionId
        String sessionId = request.getParameter("sessionId");

        // find user with this sessionId
        List<ChatUser> chatUsers = Ebean.find(ChatUser.class).where().eq("sessionId", sessionId).findList();

        // define the response message
        String responseMessage = "{}";

        // this user has a sessionId
        if (chatUsers.size() == 1) {

            // current chat user
            ChatUser chatUser = chatUsers.get(0);

            log("found user for this sessionId, chatUser: " + chatUser);

            // get the username that message to send
            String to = request.getParameter("to");

            // read the messageId
            String messageId = request.getParameter("messageId");

            // read the message content, it should be posted but currently it is a get parameter
            String content = request.getParameter("content");

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(content);
            chatMessage.setDate(new Date());
            chatMessage.setFrom(chatUser.getUsername());
            chatMessage.setMessageId(messageId);
            chatMessage.setStatus(ChatMessage.SERVER_SAVED_THE_MESSAGE);
            chatMessage.setTo(to);// TODO: there should be a friendship table, now everybody can send message to each other
            Ebean.save(chatMessage);

            log("a new message received, chatMessage: " + chatMessage);

            responseMessage = "{\"status\":\"stored\", \"messageId\":\"" + messageId + "\"}";

            // find the waitObject of the "to" user/friend
            if (Maps.get().cookieWaitObjectMap().containsKey(to)) {
                log("user " + to + " is online, will send message");
                synchronized (Maps.get().cookieWaitObjectMap().get(to)) {
                    try {
                        Maps.get().cookieWaitObjectMap().get(to).notify();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log("user " + to + " notified");
                }
            } else {
                log("user " + to + " is offline, will not send message");
                response.setStatus(HttpServletResponse.SC_OK);
            }

        }


        // send response
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(responseMessage);
        out.flush();
        out.close();

        log("done");

    }

    public void log(String message) {
        System.out.println("[SEND][" + Thread.currentThread().getId() + "] " + message);
    }

}
