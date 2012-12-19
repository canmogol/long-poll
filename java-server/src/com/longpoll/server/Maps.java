package com.longpoll.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * acm 12/3/12 3:27 PM
 */
public class Maps {

    private static Maps instance = null;
    private ConcurrentMap<String, WaitObject> cookieWaitObjectMap;

    private Maps() {
    }

    public static Maps get() {
        if (instance == null) {
            instance = new Maps();
        }
        return instance;
    }

    public ConcurrentMap<String, WaitObject> cookieWaitObjectMap() {
        if (cookieWaitObjectMap == null) {
            cookieWaitObjectMap = new ConcurrentHashMap<String, WaitObject>() {
                @Override
                public WaitObject put(String cookie, WaitObject waitObject) {
                    // if this cookie is already in the map, notify the old waitObject
                    if (this.containsKey(cookie)) {
                        synchronized (this.get(cookie)) {
                            System.out.println("[PUT ][" + Thread.currentThread().getId() + "] will set alive to FALSE and notify the old wait object of cookie: " + cookie + " wait object: " + this.get(cookie));
                            this.get(cookie).setAlive(false);
                            try {
                                this.get(cookie).notify();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return super.put(cookie, waitObject);
                }
            };
        }
        return cookieWaitObjectMap;
    }

}
