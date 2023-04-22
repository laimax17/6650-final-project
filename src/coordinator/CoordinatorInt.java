package coordinator;

import client.CallbackClient;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CoordinatorInt extends Remote{
    boolean registerClient(CallbackClient callbackClient) throws RemoteException;
}
