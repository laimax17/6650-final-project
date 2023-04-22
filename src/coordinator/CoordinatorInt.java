package coordinator;

import common.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CoordinatorInt extends Remote{
  List<Message> getHistory();
  List<Message> getLatest();
  Message sendMessage(String message);
}
