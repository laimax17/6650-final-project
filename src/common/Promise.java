package common;

public class Promise {

    private int maxN;
    private String value;
    boolean status;
    public Promise(boolean s,int maxN, String value){
        this.status = s;
        this.maxN = maxN;
        this.value = value;
    }

    public boolean getStatus(){
        return status;
    }
    public int getMaxN() {
        return maxN;
    }
    public String getV() {
        return value;
    }
}
