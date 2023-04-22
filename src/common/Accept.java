package common;

public class Accept {

    private int N;
    private Message value;
    public Accept(int proposalNum, Message value) {
        this.N = proposalNum;
        this.value = value;
    }


    public int getN() {
        return N;
    }

    public Message getValue() {
        return value;
    }
}
