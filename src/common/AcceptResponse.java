package common;

public class AcceptResponse {

    private boolean status;
    private int proposalNum;
    private Message req;
    public AcceptResponse(boolean status, int n, Message req) {
        this.status = status;
        this.proposalNum = n;
        this.req = req;
    }

    public AcceptResponse(boolean status, int n) {
        this.status = false;
        this.proposalNum = 0;
        this.req = null;

    }

    public boolean getStatus() {
        return status;
    }
}
