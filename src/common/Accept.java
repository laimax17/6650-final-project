package common;

public class Accept {

    private int N;
    private String value;
    public Accept(int proposalNum, String value) {
        this.N = proposalNum;
        this.value = value;
    }

    public int getN() {
        return N;
    }

    public String getValue() {
        return value;
    }
}
