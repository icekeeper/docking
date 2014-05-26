package ru.ifmo.docking.util;

public class Timer {

    long startTime = -1;
    long stopTime = -1;

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void stop() {
        this.stopTime = System.currentTimeMillis();
    }

    public long getTime() {
        if (startTime == -1) {
            throw new IllegalStateException("Times hasn't been started");
        } else if (stopTime == -1) {
            throw new IllegalStateException("Times hasn't been stopped");
        } else {
            return stopTime - startTime;
        }
    }
}
