import java.io.Serializable;

public abstract class taskTemplate implements Serializable {
    private static final long serialVersionUID = 1533987323839107531L;
    int id;
    String name;
    int execute_time;
    int period;
    int deadline;
    int arrival_time;
    int remainExT;

    public taskTemplate(int id, int execute_time, int period, int arrival_time,int deadline) {
        this.id = id;
        this.name = "Task_" + id;
        this.execute_time = execute_time;
        this.period = period;
        this.arrival_time = arrival_time;
        this.deadline = deadline;
        this.remainExT = execute_time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getExecute_time() {
        return execute_time;
    }

    public void setExecute_time(int execute_time) {
        this.execute_time = execute_time;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getArrival_time() {
        return arrival_time;
    }

    public void setArrival_time(int arrival_time) {
        this.arrival_time = arrival_time;
    }

    public int getRemainExT() {
        return this.remainExT;
    }

    public void setRemainExT(int remainExT) {
        this.remainExT = remainExT;
    }

}
