package client;

import common.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallbackClient extends Remote {
    void showNewMessage(Message message) throws RemoteException;
}
