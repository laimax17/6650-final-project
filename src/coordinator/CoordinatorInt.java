package coordinator;

import common.Message;

import client.CallbackClient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CoordinatorInt extends Remote{
    boolean registerClient(CallbackClient callbackClient) throws RemoteException;
    List<Message> getHistory();
    List<Message> getLatest();
    Message sendMessage(String message);
}
