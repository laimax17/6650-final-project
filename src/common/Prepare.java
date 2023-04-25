package common;

public class Prepare {

    int proposalNum;

    int currentPaxosRound;

    public Prepare(int n, int currentPaxosRound) {
        this.proposalNum = n;
        this.currentPaxosRound = currentPaxosRound;
    }

    public int getProposalNum() {
        return proposalNum;
    }

    public int getCurrentPaxosRound() {
        return currentPaxosRound;
    }
}
