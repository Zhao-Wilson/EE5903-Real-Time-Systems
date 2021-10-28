import java.util.LinkedList;
import java.util.List;

public abstract class processorTamplate {
    int processorId;
    List<taskTemplate> downTask = new LinkedList<>();
    List<taskTemplate> missTask = new LinkedList<>();
    double successTime = 0;

    public processorTamplate(int processorId) {
        this.processorId = processorId;
    }

    public int getProcessorId() {
        return processorId;
    }

    public abstract void localScheduler();

    public double getSuccessTime() {
        return successTime;
    }

    public void setSuccessTime(double successTime) {
        this.successTime = successTime;
    }
}
