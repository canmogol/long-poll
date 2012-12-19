package com.longpoll.server;

/**
 * acm 12/4/12 10:00 AM
 */
public class WaitObject {

    private boolean alive = true;

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
