package com.longpoll.server.test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * acm 12/6/12 11:24 AM
 */
public class DateTest {

    public static void main(String[] args) {
        new DateTest();
    }

    public DateTest() {
        runTest();
    }

    private void runTest() {
        runDateTest();
    }

    private void runDateTest() {
        String messageDate = new SimpleDateFormat("HH:mm dd/MM/yyyy").format(new Date());
        System.out.println("messageDate: " + messageDate);
    }
}
