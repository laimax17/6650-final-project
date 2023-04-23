package coordinator;

import common.Message;

import client.CallbackClient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CoordinatorInt extends Remote{
    boolean registerClient(CallbackClient callbackClient) throws RemoteException;
    List<Message> getHistory() throws RemoteException;
    List<Message> getLatest() throws RemoteException;
    Message sendMessage(String message) throws RemoteException;
}
