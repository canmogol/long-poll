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
 * acm 12/5/12 10:48 AM
 */
public class AckMessageServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // start connection if it did not
        DB.instance().connectToDB();

        // response JSON to send client
        String responseMessage = "{}";

        // get sessionId for this user
        String sessionId = request.getParameter("sessionId");

        log("new acknowledgement message received with sessionId: " + sessionId);

        // find user with this sessionId
        List<ChatUser> chatUsers = Ebean.find(ChatUser.class).where().eq("sessionId", sessionId).findList();

        // this user has a sessionId
        if (chatUsers.size() == 1) {

            // current chat user
            ChatUser chatUser = chatUsers.get(0);

            log("found user for this sessionId, chatUser: " + chatUser);

            // get the messageId from request
            String messageId = request.getParameter("messageId");

            // there should be only one message but anyway
            List<ChatMessage> chatMessages = Ebean.find(ChatMessage.class).where()
                    .eq("to", chatUser.getUsername())
                    .eq("messageId", messageId)
                    .lt("status", ChatMessage.SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE)
                    .findList();

            // there is a message for this user that needs to be acknowledged
            if (chatMessages.size() > 0) {
                responseMessage = "{\"status\":\"updated\", \"messageId\":\"" + messageId + "\"}";
            }

            // loop all messages, update status and notify the sender
            for (ChatMessage chatMessage : chatMessages) {

                // update it as DESTINATION_SAID_IT_GOT_THE_MESSAGE
                chatMessage.setStatus(ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE);
                Ebean.update(chatMessage);
                log("message updated with this messageId: " + messageId + " currently chat message: " + chatMessage);

                // find the user who send this message, and notify that user
                List<ChatUser> senders = Ebean.find(ChatUser.class).where().eq("username", chatMessage.getFrom()).findList();
                if (senders.size() == 1) {
                    ChatUser senderUser = senders.get(0);
                    log("found the user who send this message, sender: " + senderUser);
                    if (Maps.get().cookieWaitObjectMap().containsKey(senderUser.getUsername())) {
                        log("the sender user is online, will notify");
                        WaitObject waitObject = Maps.get().cookieWaitObjectMap().get(senderUser.getUsername());
                        if (waitObject != null) {
                            synchronized (waitObject) {
                                try {
                                    waitObject.notify();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } else {
                        log("the sender user is offline");
                    }
                } else {
                    log("seriously? the sender is not a user anymore :)");
                }
            }


        } else {
            // there is no user with this sessionId, do nothing
            log("there is no user with this sessionId: " + sessionId);
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
        System.out.println("[ACKN][" + Thread.currentThread().getId() + "] " + message);
    }

}
