package paxos;

import common.*;
import server.ServerInt;

import java.rmi.RemoteException;
import java.util.List;

public interface Proposer extends ServerInt {

    Promise sendPrepare(Prepare req, Acceptor acceptor) throws RemoteException;

    AcceptResponse sendAccept(Accept req, Acceptor acceptor) throws RemoteException;

    int sendProposal(int num, Message message) throws RemoteException;

    void setReplicaList(List<ServerInt> replicaList) throws RemoteException;

    AcceptResponse sendLearn(Accept req, Learner learner) throws RemoteException;

    List<Message> recoverAcceptor(int acceptorRound, int currentPaxosRound) throws RemoteException;

    int getId() throws RemoteException;

}
