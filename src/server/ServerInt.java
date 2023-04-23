package server;

import common.Message;
import common.Status;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInt extends Remote{
    List<Message> getAll() throws RemoteException;
    List<Message> getUpdate() throws RemoteException;
    Status saveMessage() throws RemoteException;

}