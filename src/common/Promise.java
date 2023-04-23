package common;

public class Promise {

    private int maxN;
    private Message value;
    boolean status;
    public Promise(boolean s,int maxN, Message value){
        this.status = s;
        this.maxN = maxN;
        this.value = value;
    }

    public Promise(boolean s,int maxN){
        this.status = false;
        this.maxN = 0;
        this.value = null;
    }

    public boolean getStatus(){
        return status;
    }
    public int getMaxN() {
        return maxN;
    }
    public Message getV() {
        return value;
    }
}
