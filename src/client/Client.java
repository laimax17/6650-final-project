package client;

import common.Message;

public class Client implements CallbackClient {

    @Override
    public void showNewMessage(Message message) {
        System.out.println("New message from server: " + message);
    }
}
