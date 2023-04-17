package paxos;

import common.Accept;
import common.AcceptResponse;
import server.KvServer;

import java.rmi.RemoteException;

public interface Learner extends KvServer {

    AcceptResponse handleLearn(Accept req) throws RemoteException;
}
