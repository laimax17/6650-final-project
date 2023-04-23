package common;

import java.io.Serializable;

public class Message implements Serializable {

    private String time;
    private String content;
    private String userName;

    public Message(String userName, String content) {
        this.content = content;
        this.userName = userName;
    }

    public Message(String time, String userName, String content) {
        this.content = content;
        this.time = time;
        this.userName = userName;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s : %s",this.time, this.userName, this.content);
    }
}
