package com.longpoll.server;

import com.avaje.ebean.Ebean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * acm | 12/19/12 4:05 PM
 */
public class ReceivedAckServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log("new received ack message");

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

            // get the messageId for the received message
            String messageId = request.getParameter("messageId");

            List<ChatMessage> chatMessages = Ebean.find(ChatMessage.class).where().eq("messageId", messageId).findList();
            if (chatMessages.size() == 1) {
                ChatMessage chatMessage = chatMessages.get(0);
                log("received message found, chatMessage: " + chatMessage);
                // update the status to message's last status, this message is send, received and acknowledged, notified and acknowledged
                chatMessage.setStatus(ChatMessage.SOURCE_SAID_IT_KNOWS_THIS_MESSAGE_IS_DELIVERED);
                Ebean.update(chatMessage);
                log("received message updated, chatMessage: " + chatMessage);
            }
            responseMessage = "{\"status\":\"acknowledged\", \"messageId\":\"" + messageId + "\"}";
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
        System.out.println("[RACK][" + Thread.currentThread().getId() + "] " + message);
    }

}
