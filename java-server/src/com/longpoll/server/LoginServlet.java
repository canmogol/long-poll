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
 * acm 12/4/12 8:34 PM
 */
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // start connection if it did not
        DB.instance().connectToDB();

        // read username and password
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        log("login message received, username: " + username + " password: " + password);

        // find user
        List<ChatUser> users = Ebean.find(ChatUser.class)
                .where()
                .eq("username", username)
                .eq("password", password)
                .findList();

        // define the responseMessage
        String responseMessage;

        // found user, log this user in
        if (users.size() == 1) {
            String sessionId = username + "|" + request.getSession(true).getId();
            responseMessage = "{\"loginStatus\":\"success\", \"sessionId\":\"" + sessionId + "\"}";
            ChatUser chatUser = users.get(0);
            chatUser.setSessionId(sessionId);
            Ebean.update(chatUser);
        }

        // no user found, refuse connection
        else {
            responseMessage = "{\"loginStatus\":\"fail\"}";
        }

        // send response
        log("write responseMessage: " + responseMessage);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(responseMessage);
        out.flush();
        out.close();

    }

    public void log(String message) {
        System.out.println("[LGIN][" + Thread.currentThread().getId() + "] " + message);
    }

}
