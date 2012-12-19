package com.longpoll.server;

import com.avaje.ebean.Ebean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * acm 12/3/12 2:33 PM
 */
public class GetMessageServlet extends HttpServlet {


    private static final int MAX_LONG_POLL_TIME = 60;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("\n");

        // start connection if it did not
        DB.instance().connectToDB();

        // create a request object to wait and notify
        final WaitObject requestObject = new WaitObject();

        // read cookie(not like below) and create a request object
        String sessionId = request.getParameter("sessionId");

        // set response message as empty string, it will be changed
        String responseMessage = "{\"shutdown\":\"true\"}";

        try {
            log("will find the user with this sessionId: " + sessionId);

            // find ChatUser from this session id
            List<ChatUser> chatUsers = Ebean.find(ChatUser.class).where().eq("sessionId", sessionId).findList();

            if (chatUsers.size() == 1) {

                // there is a user with this sessionId
                ChatUser chatUser = chatUsers.get(0);
                log("found the use with this sessionId, user: " + chatUser);

                // put this cookie with request object, in put method if the exists cookie's old requestObject will be notified
                Maps.get().cookieWaitObjectMap().put(chatUser.getUsername(), requestObject);

                log("cookieWaitObjectMap map: " + Maps.get().cookieWaitObjectMap());

                // find the chat messages
                int numberOfMessages = Ebean.find(ChatMessage.class).where().eq("to", chatUser.getUsername()).lt("status", ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE).order("id").findRowCount();
                int numberOfMessagesMy = Ebean.find(ChatMessage.class).where().eq("from", chatUser.getUsername()).eq("status", ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE).order("id").findRowCount();
                int numberOfMessagesMyNotAck = Ebean.find(ChatMessage.class).where().eq("from", chatUser.getUsername()).eq("status", ChatMessage.SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE).order("id").findRowCount();

                log("number of messages to deliver, numberOfMessages: " + numberOfMessages + " numberOfMessagesMy: " + numberOfMessagesMy + " numberOfMessagesMyNotAck: " + numberOfMessagesMyNotAck);

                synchronized (requestObject) {
                    // if there are no messages in the DB, wait
                    if (numberOfMessages == 0 && numberOfMessagesMy == 0 && numberOfMessagesMyNotAck == 0) {
                        log("there are no messages in the DB to deliver, will wait");
                        // wait for it
                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                synchronized (requestObject) {
                                    log("Timer run!");
                                    if (requestObject.isAlive()) {
                                        log("Timer will notify requestObject: " + requestObject);
                                        try {
                                            requestObject.notify();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(timerTask, MAX_LONG_POLL_TIME * 1000);

                        // wait for a message
                        requestObject.wait();

                        // stop timer
                        timer.cancel();
                    } else {
                        log("there are messages in the DB to deliver");
                    }

                    if (!requestObject.isAlive()) {
                        throw new Exception("someone told to go down! wait object: " + requestObject);
                    }
                }
                log("continue");

                // empty the response message
                responseMessage = "";

                try {
                    log("find and prepare messages for user: " + chatUser);

                    List<ChatMessage> chatMessages = Ebean.find(ChatMessage.class).where().eq("to", chatUser.getUsername()).lt("status", ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE).order("id").findList(); // for me message, status 0 and 1 messages
                    log("found message send to me, status 0 and 1: " + chatMessages);

                    List<ChatMessage> chatMessagesMy = Ebean.find(ChatMessage.class).where().eq("from", chatUser.getUsername()).eq("status", ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE).order("id").findList(); // from me message, status 2 message
                    log("found message status 2: " + chatMessagesMy);

                    List<ChatMessage> chatMessagesMyNotAck = Ebean.find(ChatMessage.class).where().eq("from", chatUser.getUsername()).eq("status", ChatMessage.SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE).order("id").findList(); // from me message, status 3
                    log("found message status 3: " + chatMessagesMyNotAck);

                    // find and loop through all messages
                    for (ChatMessage m : chatMessages) {
                        // var messageDate = now.getHours() + ':' + now.getMinutes() + " " + now.getDay() + "/" + (now.getMonth() + 1) + "/" + now.getFullYear();
                        String messageDate = new SimpleDateFormat("HH:mm dd/MM/yyyy").format(m.getDate());
                        responseMessage += "{\"from\": \"" + m.getFrom() + "\", \"content\": \"" + m.getContent() + "\", \"date\": \"" + messageDate + "\", \"messageId\": \"" + m.getMessageId() + "\", \"status\": \"" + m.getStatus() + "\", \"id\":\"" + m.getId() + "\"},";

                        // this message is received from a friend but it is only stored, not delivered
                        if (m.getStatus() == ChatMessage.SERVER_SAVED_THE_MESSAGE) {
                            // change status to SERVER_WRITE_MESSAGE_TO_DESTINATION, it is delivered now
                            m.setStatus(ChatMessage.SERVER_WRITE_MESSAGE_TO_DESTINATION);
                        }

                        // update this message status
                        Ebean.update(m);
                    }

                    // find and loop through all my messages
                    for (ChatMessage m : chatMessagesMy) {
                        responseMessage += "{\"messageId\": \"" + m.getMessageId() + "\", \"status\": \"" + m.getStatus() + "\", \"id\":\"" + m.getId() + "\"},";

                        // this is the message of the friend got the message and acknowledged
                        if (m.getStatus() == ChatMessage.DESTINATION_SAID_IT_GOT_THE_MESSAGE) {
                            // SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE is the last status of a message
                            m.setStatus(ChatMessage.SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE);
                        }

                        // update this message status
                        Ebean.update(m);
                    }


                    // find and loop through all my messages
                    for (ChatMessage m : chatMessagesMyNotAck) {
                        // just add this message to response
                        responseMessage += "{\"messageId\": \"" + m.getMessageId() + "\", \"status\": \"" + m.getStatus() + "\", \"id\":\"" + m.getId() + "\"},";
                    }

                    // remove the last comma
                    if (responseMessage.endsWith(",")) {
                        responseMessage = responseMessage.substring(0, responseMessage.length() - 1);
                    }
                    responseMessage = "[" + responseMessage + "]";
                } catch (Exception e) {
                    responseMessage = "[]";
                }

                // remove request object
                Object requestObjectRemoved = Maps.get().cookieWaitObjectMap().remove(chatUser.getUsername());
                log("removed requestObject: " + requestObjectRemoved + " for cookie: " + sessionId);

            } else {
                log("there is no user with this sessionId: " + sessionId);
            }

        } catch (InterruptedException e) {
            log("requestObject interrupted, don't panic this is expected, InterruptedException: " + e + " exception message: " + e.getMessage() + " wait object: " + requestObject);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log("Exception: " + e + " exception message: " + e.getMessage() + " wait object: " + requestObject);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        // send response
        log("write messages: " + responseMessage);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(responseMessage);
        out.flush();

        // close the writer
        response.getWriter().close();

        log("done");
    }

    public void log(String message) {
        System.out.println("[GET ][" + Thread.currentThread().getId() + "] " + message);
    }

}
