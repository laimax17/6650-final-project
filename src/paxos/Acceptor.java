package paxos;
import common.Accept;
import common.AcceptResponse;
import common.Prepare;
import common.Promise;
import server.KvServer;

import java.rmi.RemoteException;
public interface Acceptor extends KvServer {

    int maxProposalNumberRec = 0;
    AcceptResponse sendLearn(Accept req, Learner learner) throws RemoteException;

    Promise handlePrepare(Prepare req) throws RemoteException;

    AcceptResponse handleAccept(Accept req) throws RemoteException;
}
