package server;

import common.Message;
import common.Status;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInt extends Remote{
  List<Message> getAll();
  List<Message> getUpdate();
  Status saveMessage();

}