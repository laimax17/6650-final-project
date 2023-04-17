package common;

public class AcceptResponse {

    private boolean status;
    private int proposalNum;
    private String req;
    public AcceptResponse(boolean status, int n, String req) {
        this.status = status;
        this.proposalNum = n;
        this.req = req;
    }

    public boolean getStatus() {
        return status;
    }
}
