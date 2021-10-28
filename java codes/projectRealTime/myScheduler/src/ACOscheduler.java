import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class ACOscheduler {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int method;
        int dataRange;
        while (true) {
            System.out.println("Please select use fixed data or randomly generate data: (A/B) e——(exit)");
            String s = scanner.next();
            if (s.equals("A")) {
                method = 1;
                System.out.println("Please select how many data: (300,600,900,1200,1500,1800,2100,1000,2000,3000,4000,5000,10000,2000)");
                try {
                    dataRange = new Integer(scanner.next());
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                try {
                    ACOData ad = new ACOData();
                    new scheduler1(ad.geneTask(method, dataRange), ad.genePro()).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (s.equals("B")) {
                method = 2;
                System.out.println("Please type in the number of task sequence");
                int seed = scanner.nextInt();
                System.out.println("Please type in the number of tasks in each sequence");
                int loop = scanner.nextInt();
                System.out.println("Please type in the number of processors");
                int ACOProcessorNum = scanner.nextInt();
                System.out.println("Please type in the threshold of arrival time");
                int arrival_thd = scanner.nextInt();
                System.out.println("Please type in the threshold of period time");
                int period_thd = scanner.nextInt();
                System.out.println("Please type in the threshold of execute time");
                int execute_thd = scanner.nextInt();
                try {
                    ACOData ad  = new ACOData(seed, loop, ACOProcessorNum, arrival_thd, period_thd, execute_thd);
                    new scheduler1(ad.geneTask(method, 0), ad.genePro()).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (s.equals("e")) break;
            else System.out.println("Please type right format!");
        }


//        int seed,int loop,int ACOProcessorNum,int arrival_thd,int period_thd,int execute_thd
//        try {
//            new scheduler(new ACOData(21,100,2,3,3,3).geneTask(2,0),new ACOData().genePro()).run();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    }
}

class scheduler1 {
    long runningTime = 0;
    static int timer = 0;
    List<ACOtask> allTask = new LinkedList<>();
    List<ACOtask> curRunTask = new LinkedList<>();
    List<ACOprocessor> allProcessor = new LinkedList<>();
    LinkedList<ACOtask> missTask = new LinkedList<>();
    int minRunTime;

    public scheduler1(LinkedList<ACOtask> allTask,LinkedList<ACOprocessor> allProcessor) {
        this.allTask = allTask;
        Collections.sort(allTask, new Comparator<ACOtask>() {
            @Override
            public int compare(ACOtask t1, ACOtask t2) {
                return t1.arrival_time - t2.arrival_time;
            }
        });

        this.allProcessor = allProcessor;
    }

    public boolean newArr(){
        for (ACOtask t:allTask){
            if (t.getArrival_time()==timer) return true;
        }
        return false;
    }

    public void newArrJobQ(){
        for (ACOtask t:allTask){
            if (minRunTime<t.getDeadline()) minRunTime = t.getDeadline();
            if (t.arrival_time==timer) curRunTask.add(t);
        }
        sortJob();
    }

    public void sortJob(){
        Collections.sort(curRunTask, new Comparator<ACOtask>() {
            @Override
            public int compare(ACOtask t1, ACOtask t2) {
                if (t1.probability>t2.probability) return -1;
                else if (t1.probability==t2.probability) return 0;
                else return 1;
            }
        });
    }

    public void remDelTask(LinkedList<ACOtask> readyDel){
        for (ACOtask t:readyDel){
            curRunTask.remove(t);
        }
    }

    public void run(){
        timer=0;
        boolean jobComplete = false;
        LinkedList<ACOtask> readyDel = new LinkedList<>();
        long t1 = System.currentTimeMillis();
        while (timer<=minRunTime || timer==0){
            newArrJobQ();
            if (!curRunTask.isEmpty()) {
                readyDel.clear();
                for (ACOtask t : curRunTask) {
                    if (timer > t.getDeadline()) {
                        missTask.add(t);
                        readyDel.add(t);
                    }
                }
                if (!readyDel.isEmpty()) remDelTask(readyDel);
                if (newArr() || jobComplete) {
                    for (ACOtask t : curRunTask) {
                        t.updateProbability((LinkedList<ACOtask>) curRunTask);
                    }
                    sortJob();
                    Ant ant = new Ant((LinkedList<ACOtask>) curRunTask);
                    LinkedList<ACOtask> bestPath = ant.bestPath();
                    int maxSucc = ant.maxSucc;
                    evaporation();
                    updateBest(bestPath, curRunTask.size() - maxSucc);
                    sortJob();
                    jobComplete = assignJob(true);
                } else {
                    jobComplete = assignJob(false);
                }
            }
            timer++;
        }
        runningTime = System.currentTimeMillis()-t1;
        prtMatrics();
    }

    private boolean assignJob(boolean reRun) {
        boolean jobCompelete = false;
        if (reRun){
            if (allProcessor.size()>=curRunTask.size()){
                for (int i=0;i<curRunTask.size();i++){
                    allProcessor.get(i).setCurTask(curRunTask.get(i));
                }
                for (ACOprocessor p:allProcessor){
                    if (p.getCurTask()==null) continue;
                    p.getCurTask().setRemainExT(p.getCurTask().getRemainExT()-1);
                    if (p.getCurTask().getRemainExT()==0){
                        p.setSuccessTime(p.getSuccessTime()+ (double) p.getCurTask().getExecute_time()/p.getCurTask().getPeriod());
                        curRunTask.remove(p.getCurTask());
                        p.setCurTask(null);
                        jobCompelete = true;
                    }else if(timer>p.getCurTask().getDeadline()){
                        p.missTask.add(p.getCurTask());
                        curRunTask.remove(p.getCurTask());
                        p.setCurTask(null);
                        jobCompelete = true;
                    }
                }
            }else {
                for (int j=0;j<allProcessor.size();j++){
                    allProcessor.get(j).setCurTask(curRunTask.get(j));
                }
                for (ACOprocessor p:allProcessor){
                    p.getCurTask().setRemainExT(p.getCurTask().getRemainExT()-1);
                    if (p.getCurTask().getRemainExT()==0){
                        p.setSuccessTime(p.getSuccessTime()+ (double) p.getCurTask().getExecute_time()/p.getCurTask().getPeriod());
                        curRunTask.remove(p.getCurTask());
                        p.setCurTask(null);
                        jobCompelete = true;
                    }else if(timer>p.getCurTask().getDeadline()){
                        p.missTask.add(p.getCurTask());
                        curRunTask.remove(p.getCurTask());
                        p.setCurTask(null);
                        jobCompelete = true;
                    }
                }
            }
        }else {
            for (ACOprocessor p:allProcessor){
                if (p.getCurTask()==null) continue;
                p.getCurTask().setRemainExT(p.getCurTask().getRemainExT()-1);
                if (p.getCurTask().getRemainExT()==0){
                    p.setSuccessTime(p.getSuccessTime()+ (double) p.getCurTask().getExecute_time()/p.getCurTask().getPeriod());
                    curRunTask.remove(p.getCurTask());
                    p.setCurTask(null);
                    jobCompelete = true;
                }else if(timer>p.getCurTask().getDeadline()){
                    p.missTask.add(p.getCurTask());
                    curRunTask.remove(p.getCurTask());
                    p.setCurTask(null);
                    jobCompelete = true;
                }
            }
        }
        return jobCompelete;
    }

    public void evaporation(){
        double rou = 0.3;
        for (ACOtask t: curRunTask){
            t.setPhe(t.phe*(1-rou));
        }
    }

    public void updateBest(LinkedList<ACOtask> best, int failure){
        double C = 0.1;
        for (int i=1;i<=best.size();i++){
            best.get(i-1).setPhe(best.get(i-1).phe+(double) ((curRunTask.size()-failure)/(failure+1)) * C / i);
        }
    }

    public void prtMatrics(){
        int failuer = 0;
        for (ACOprocessor p:allProcessor){
            failuer+=p.missTask.size();
            System.out.println("Processor "+p.getProcessorId()+" 的失败任务有 "+p.missTask.size()+" 个");
        }
        System.out.println("ACO失败任务有："+missTask.size()+" 个");
        failuer+=missTask.size();
        System.out.println("任务总数："+allTask.size());
        System.out.println("失败率："+ (double)failuer/allTask.size());
        System.out.println("成功率："+ (double)(allTask.size()-failuer)/allTask.size());
        double CPUTime = 0;
        for (ACOprocessor p:allProcessor) CPUTime+=p.getSuccessTime();
        System.out.println("ECU: "+ CPUTime/allProcessor.size());
        System.out.println("Time running the progress: "+(int)runningTime+"ms");
    }
}

class ACOprocessor extends processorTamplate{
    ACOtask curTask;

    public ACOprocessor(int processorId) {
        super(processorId);
    }

    @Override
    public void localScheduler() {

    }

    public void setCurTask(ACOtask curTask) {
        this.curTask = curTask;
    }

    public ACOtask getCurTask() {
        return curTask;
    }
}

class Ant{
    LinkedList<ACOtask> curTask;
    Map<ACOtask,LinkedList<ACOtask>> map = new HashMap<>();
    int maxSucc;

    public Ant(LinkedList<ACOtask> curTask){
        this.curTask = curTask;
        maxSucc = Integer.MIN_VALUE;
        ACOtask temp;
        int thr;
        if (curTask.size()<10) thr = curTask.size();
        else thr = curTask.size()/5;
        for (int a=0;a<thr;a++){
            LinkedList<ACOtask> tmpArr = new LinkedList<>();
            temp = curTask.get(a);
            tmpArr.add(temp);
            for (int b = 0;b<curTask.size();b++){
                if (b==a) continue;
                tmpArr.add(curTask.get(b));
            }
            map.put(temp,tmpArr);
        }
    }

    public LinkedList<ACOtask> bestPath(){
        LinkedList<ACOtask> res = null;
        int n;
        for (Map.Entry<ACOtask,LinkedList<ACOtask>> en: map.entrySet()){
            LinkedList<ACOtask> tmp = new LinkedList<>();
            tmp.addAll(en.getValue());
            n = curTask.size()-failure((LinkedList<ACOprocessor>) new ACOData().genePro(), en.getValue());
            if(n > maxSucc) {
                maxSucc = n;
                res = tmp;
            }
        }
        return res;
    }

    public int failure(LinkedList<ACOprocessor> pros,LinkedList<ACOtask> tasks){
        int fa = 0;
        if (pros.size()>=tasks.size()){
            for (ACOprocessor p:pros){
                if (tasks.isEmpty()) break;
                p.setCurTask(tasks.pollFirst());
            }
            int count = 0;
            while (true){
                if (processorRun(pros)) break;
                for (ACOprocessor p:pros){
                    if (p.getCurTask()==null) continue;
                    p.getCurTask().setRemainExT(p.getCurTask().getRemainExT()-1);
                    if (p.getCurTask().getRemainExT()==0) p.setCurTask(null);
                    else if(count>p.getCurTask().getDeadline()) {
                        fa++;
                        p.setCurTask(null);
                    }
                }
                count++;
            }
        }else if (pros.size()<tasks.size()){
            for (ACOprocessor p:pros){
                p.setCurTask(tasks.pollFirst());
            }
            int count = 0;
            while (true){
                if (processorRun(pros)) break;
                for (ACOprocessor p:pros){
                    if (p.getCurTask()==null) continue;
                    p.getCurTask().setRemainExT(p.getCurTask().getRemainExT()-1);
                    if (p.getCurTask().getRemainExT()==0){
                        if (tasks.isEmpty()) p.setCurTask(null);
                        else p.setCurTask(tasks.pollFirst());
                    }else if(count>p.getCurTask().getDeadline()){
                        fa++;
                        if (tasks.isEmpty()) p.setCurTask(null);
                        else p.setCurTask(tasks.pollFirst());
                    }
                }
                count++;
            }
        }
        return fa;
    }

    public boolean processorRun(LinkedList<ACOprocessor> pros){
        for (ACOprocessor p:pros){
            if (p.getCurTask()!=null) return true;
        }
        return false;
    }
}

class ACOtask extends taskTemplate{
    int K = 10;
    int alpha = 1;
    int beta = 2;
    double phe;
    double probability;

    public ACOtask(int id, int execute_time, int period, int arrival_time, int deadline) {
        super(id, execute_time, period, arrival_time, deadline);
        phe = 1.0;
        probability=0;
    }

    public void updateProbability(LinkedList<ACOtask> curTask){
        probability = Math.pow(phe,alpha) * Math.pow(getYita(this),beta) / globalSum(curTask);
    }

    public double globalSum(LinkedList<ACOtask> curTask){
        double sum = 0;
        for (ACOtask t:curTask){
            sum+=(Math.pow(t.phe,alpha) * Math.pow(getYita(t),beta));
        }
        return sum;
    }

    public double getYita(ACOtask task){
        return (double) K/(task.getDeadline()-scheduler1.timer);
    }

    public void setPhe(double phe) {
        this.phe = phe;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Task{" +
                ", probability=" + probability +
                ", id=" + id +
                ", execute_time=" + execute_time +
                ", period=" + period +
                ", deadline=" + deadline +
                ", arrival_time=" + arrival_time +
                '}';
    }
}


class ACOData {
    int seed;
    int loop;
    int ACOProcessorNum;
    int arrival_thd;
    int period_thd;
    int execute_thd;
    LinkedList<ACOtask> geneACOTask = new LinkedList<>();
    LinkedList<ACOprocessor> genePro = new LinkedList<>();

    public ACOData(){
        seed = 21;
        loop = 100;
        ACOProcessorNum = 10;
        arrival_thd = 5;
        period_thd = 5;
        execute_thd = 5;
    }

    public ACOData(int seed,int loop,int ACOProcessorNum,int arrival_thd,int period_thd,int execute_thd){
        this.seed = seed;
        this.loop = loop;
        this.ACOProcessorNum = ACOProcessorNum;
        this.arrival_thd = arrival_thd;
        this.period_thd = period_thd;
        this.execute_thd = execute_thd;
    }


    public LinkedList<ACOtask> geneTask(int method, int dataRange) throws Exception {
        if (method==2){
            int arrival_time;
            int period;
            int execute_time;
            for (int i=0;i<seed;i++){
                arrival_time = new Random().nextInt(arrival_thd);
                period = new Random().nextInt(period_thd)+1;
                execute_time = new Random().nextInt((int)period/execute_thd+1)+1;
                for (int j=0;j<loop;j++){
                    geneACOTask.add(new ACOtask(i+1,execute_time,period,arrival_time+j*period,arrival_time+(j+1)*period));
                }
            }
        }else if (method==1){
            FileInputStream fileIn=new FileInputStream("./Task_"+dataRange+".txt");
            ObjectInputStream ois=new ObjectInputStream(fileIn);
            geneACOTask = changeTask((LinkedList<edfrmTask>) ois.readObject());
        }
        return geneACOTask;
    }

    public LinkedList<ACOprocessor> genePro(){
        for (int i=0;i<ACOProcessorNum;i++){
            genePro.add(new ACOprocessor(i+1));
        }
        return genePro;
    }

    public LinkedList<ACOtask> changeTask(LinkedList<edfrmTask> edfrmTasks){
        LinkedList<ACOtask> acOtasks = new LinkedList<>();
        for (edfrmTask e:edfrmTasks){
            acOtasks.add(new ACOtask(e.getId(),e.getExecute_time(),e.getPeriod(),e.arrival_time,e.getDeadline()));
        }
        return acOtasks;
    }
}

