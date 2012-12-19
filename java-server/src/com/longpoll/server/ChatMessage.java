package com.longpoll.server;

import javax.persistence.*;
import java.util.Date;

/**
 * acm 12/4/12 8:18 PM
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    public final static int SERVER_SAVED_THE_MESSAGE = 0; // someone send a new message, stored to DB
    public final static int SERVER_WRITE_MESSAGE_TO_DESTINATION = 1; // server wrote this message to recipient but we don't know if recipient got it
    public final static int DESTINATION_SAID_IT_GOT_THE_MESSAGE = 2; // recipient said the server that it got this message
    public final static int SERVER_SAID_TO_SOURCE_DESTINATION_GOT_THE_MESSAGE = 3; // server wrote to the source that recipient got the message, but server does not know if the source got this
    public final static int SOURCE_SAID_IT_KNOWS_THIS_MESSAGE_IS_DELIVERED = 4; // source said that it got it

    @Id
    @GeneratedValue
    private Long id;

    @Temporal(value = TemporalType.TIMESTAMP)
    private Date date;

    @Column(name = "from_username")
    private String from;

    @Column(name = "to_username")
    private String to;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "message_content")
    private String content;

    @Column(name = "message_status")
    private int status = SERVER_SAVED_THE_MESSAGE;

    public ChatMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", date=" + date +
                ", messageId='" + messageId + '\'' +
                ", content='" + content + '\'' +
                ", status=" + status +
                '}';
    }
}
