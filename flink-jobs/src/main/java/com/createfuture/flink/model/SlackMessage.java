package com.createfuture.flink.model;

public class SlackMessage {
    private long timestamp;
    private String user;
    private String message;

    public SlackMessage() {
    }

    public SlackMessage(long timestamp, String user, String message) {
        this.timestamp = timestamp;
        this.user = user;
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SlackMessage{" +
                "timestamp=" + timestamp +
                ", user='" + user + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
