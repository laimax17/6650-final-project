package paxos;

import common.Accept;
import common.AcceptResponse;
import server.ServerInt;

import java.rmi.RemoteException;

public interface Learner extends ServerInt {

    AcceptResponse handleLearn(Accept req) throws RemoteException;
}
