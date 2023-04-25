package paxos;
import common.Accept;
import common.AcceptResponse;
import common.Prepare;
import common.Promise;
import server.ServerInt;
import java.rmi.RemoteException;
public interface Acceptor extends ServerInt {

    int maxProposalNumberRec = 0;
    AcceptResponse sendLearn(Accept req, Learner learner) throws RemoteException;

    Promise handlePrepare(Prepare req, Proposer proposer) throws RemoteException;

    AcceptResponse handleAccept(Accept req) throws RemoteException;
}
