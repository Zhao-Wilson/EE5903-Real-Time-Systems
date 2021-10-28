import java.io.*;
import java.util.*;

public class globalScheduler{
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        int method;
        int dataRange;
        while (true){
            System.out.println("Please select use fixed data or randomly generate data: (A/B) e——(exit)");
            String s = scanner.next();
            if (s.equals("A")) {
                method = 1;
                System.out.println("Please select how many data: (300,600,900,1200,1500,1800,2100,1000,2000,3000,4000,5000,10000,20000)");
                try {
                    dataRange = new Integer(scanner.next());
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                try {
                    edfrmData ed = new edfrmData();
                    new Scheduler(ed.geneTask(method, dataRange), ed.genePro()).run();
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
                int edfrmProcessorNum = scanner.nextInt();
                System.out.println("Please type in the threshold of arrival time");
                int arrival_thd = scanner.nextInt();
                System.out.println("Please type in the threshold of period time");
                int period_thd = scanner.nextInt();
                System.out.println("Please type in the threshold of execute time");
                int execute_thd = scanner.nextInt();
                try {
                    edfrmData ed = new edfrmData(seed, loop, edfrmProcessorNum, arrival_thd, period_thd, execute_thd);
                    new Scheduler(ed.geneTask(method, 0), ed.genePro()).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (s.equals("e")) break;
            else System.out.println("Please type right format!");
        }

//        try {
//            edfrmData d = new edfrmData();
//            new Scheduler(d.geneTask(2,0),d.genePro()).run();
//            d.save();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}

class Scheduler {
    long runningTime = 0;
    static int timer = 0;
    List<edfrmTask> allTask = new LinkedList<>();
    List<edfrmTask> curRunTask = new LinkedList<>();
    List<edfrmProcessor> allProcessor = new LinkedList<>();
    LinkedList<edfrmTask> missTask = new LinkedList<>();

    public List<edfrmTask> updateCurTask(){
        curRunTask.clear();
        for (edfrmTask t: this.allTask){
            if (t.arrival_time==timer) curRunTask.add(t);
        }
        Collections.sort(curRunTask, new Comparator<edfrmTask>() {
            @Override
            public int compare(edfrmTask t1, edfrmTask t2) {
                return t1.period-t2.period;
            }
        });
        return curRunTask;
    }

    public Scheduler(LinkedList<edfrmTask> allTask,LinkedList<edfrmProcessor> allProcessor) {
        this.allTask = allTask;
        Collections.sort(allTask, new Comparator<edfrmTask>() {
            @Override
            public int compare(edfrmTask t1, edfrmTask t2) {
                return t1.arrival_time - t2.arrival_time;
            }
        });

        this.allProcessor = allProcessor;

        //
//        System.out.println(allProcessor.toString());
//        System.out.println("\n");
        //System.out.println(allTask.toString());
        //System.out.println("\n");
    }

    public void updateProcessor(){
        Collections.sort(allProcessor, new Comparator<edfrmProcessor>() {
            @Override
            public int compare(edfrmProcessor p1, edfrmProcessor p2) {
                if (p1.getPutilization() > p2.getPutilization()) return 1;
                else if (p1.getPutilization() == p2.getPutilization()) return 0;
                else return -1;
            }
        });

    }

    public double muSigma(){
        return getnTask() * (double)(Math.pow(2,(double) 1.0/getnTask())-1)*10;
    }

    public int getnTask(){
        return curRunTask.size();
    }

    public void pSelection(edfrmTask edfrmTask){
        edfrmProcessor edfrmProcessor = allProcessor.get(new Random().nextInt(allProcessor.size()));
        if (edfrmProcessor.Taskmigration(edfrmTask)) {
            updateProcessor();
            edfrmProcessor minUPro = getMinUPro();
            if (minUPro.getProcessorId() != edfrmProcessor.getProcessorId()){
                if(minUPro.Taskmigration(edfrmTask)){
                    missTask.add(edfrmTask);
                }else minUPro.addPQueue(edfrmTask);
            }else {
                missTask.add(edfrmTask);
            }
        }else edfrmProcessor.addPQueue(edfrmTask);
    }

    public edfrmProcessor getMinUPro(){
        return allProcessor.get(0);
    }

    public void run(){
        timer=0;
        long t1 = System.currentTimeMillis();
        while (timer<=allTask.get(allTask.size()-1).deadline){
            updateCurTask();
            double mu = muSigma();
            if (!curRunTask.isEmpty()){
                for (edfrmTask t:curRunTask){
                    if ((double)t.execute_time/t.period>mu) missTask.add(t);
                    else pSelection(t);
                }
            }
            for (edfrmProcessor p:allProcessor) p.localScheduler();
            timer++;
        }
        runningTime = System.currentTimeMillis()-t1;
        prtMatrics();
    }

    public void prtMatrics(){
        int failuer = 0;
        for (edfrmProcessor p:allProcessor){
            failuer+=p.missTask.size();
            System.out.println("Processor "+p.getProcessorId()+" 的失败任务有 "+p.missTask.size()+" 个");
        }
        System.out.println("RMS失败任务有："+missTask.size()+" 个");
        failuer+=missTask.size();
        System.out.println("任务总数："+allTask.size());
        System.out.println("失败率："+ (double)failuer/allTask.size());
        System.out.println("成功率："+ (double)(allTask.size()-failuer)/allTask.size());
        double CPUTime = 0;
        for (edfrmProcessor p:allProcessor) CPUTime+=p.getSuccessTime();
        System.out.println("ECU: "+CPUTime/allProcessor.size());
        System.out.println("Time running the progress: "+(int)runningTime+"ms");
    }
}


class edfrmProcessor extends processorTamplate{
    double Putilization;
    static double upperBound=0.81;
    LinkedList<edfrmTask> waitingTask = new LinkedList<>();
    List<edfrmTask> curRunTask = new LinkedList<>();

    public edfrmProcessor(int ProcessorId) {
        super(ProcessorId);
    }

    public int getProcessorId() {
        return super.getProcessorId();
    }

    public double getPutilization() {
        return culPutilization();
    }

    public double culPutilization() {
        Putilization = 0;
        for (edfrmTask t:waitingTask){
            Putilization+=(double) t.remainExT/t.period;
        }
        return Putilization;
    }

    public void addPQueue(edfrmTask t) {
        this.waitingTask.add(t);
        culPutilization();
    }

    public boolean Taskmigration(edfrmTask t){
        if(getPutilization() + (double) t.execute_time/t.period > upperBound) return true;
        return false;
    }

    public void sortWaitTask(){
        Collections.sort(waitingTask, new Comparator<edfrmTask>() {
            @Override
            public int compare(edfrmTask t1, edfrmTask t2) {
                return t1.deadline-t2.deadline;
            }
        });
    }

    public void localScheduler(){
        if (!waitingTask.isEmpty()){
            sortWaitTask();
            LinkedList<edfrmTask> readyDeleteT = new LinkedList<>();
            for (edfrmTask t: waitingTask){
                if (Scheduler.timer>t.deadline && t.getRemainExT()>0){
                    missTask.add(t);
                    readyDeleteT.add(t);
                    continue;
                }
                t.setRemainExT(t.getExecute_time()-1);
                curRunTask.add(t);
                if (t.getRemainExT()<=0){
                    this.setSuccessTime(this.getSuccessTime()+(double) t.getExecute_time()/t.getPeriod());
                    downTask.add(t);
                    readyDeleteT.add(t);
                }
            }

            for (edfrmTask t:readyDeleteT){
                waitingTask.remove(t);
            }
        }
    }

    @Override
    public String toString() {
        return "Processor{" +
                "ProcessorId=" + processorId +
                ", Putilization=" + Putilization +
                '}';
    }
}


class edfrmTask extends taskTemplate {

    public edfrmTask(int id, int execute_time, int period, int arrival_time, int deadline) {
        super(id, execute_time, period, arrival_time, deadline);
    }
}



class edfrmData {
    int seed;
    int loop;
    int edfrmProcessorNum;
    int arrival_thd;
    int period_thd;
    int execute_thd;
    LinkedList<edfrmTask> geneedfrmTask = new LinkedList<>();
    LinkedList<edfrmProcessor> genePro = new LinkedList<>();

    public edfrmData(){
        seed = 200;
        loop = 100;
        edfrmProcessorNum = 10;
        arrival_thd = 5;
        period_thd = 5;
        execute_thd = 5;
    }

    public edfrmData(int seed,int loop,int edfrmProcessorNum,int arrival_thd,int period_thd,int execute_thd){
        this.seed = seed;
        this.loop = loop;
        this.edfrmProcessorNum = edfrmProcessorNum;
        this.arrival_thd = arrival_thd;
        this.period_thd = period_thd;
        this.execute_thd = execute_thd;
    }


    public LinkedList<edfrmTask> geneTask(int method, int dataRange) throws Exception {
        if (method==2){
            int arrival_time;
            int period;
            int execute_time;
            for (int i=0;i<seed;i++){
                arrival_time = new Random().nextInt(arrival_thd);
                period = new Random().nextInt(period_thd)+1;
                execute_time = new Random().nextInt((int)period/execute_thd+1)+1;
                for (int j=0;j<loop;j++){
                    geneedfrmTask.add(new edfrmTask(i+1,execute_time,period,arrival_time+j*period,arrival_time+(j+1)*period));
                }
            }
        }else if (method==1){
            FileInputStream fileIn=new FileInputStream("./Task_"+dataRange+".txt");
            ObjectInputStream ois=new ObjectInputStream(fileIn);
            geneedfrmTask = (LinkedList<edfrmTask>) ois.readObject();
        }

        return geneedfrmTask;
    }

    public void save() throws Exception {
        FileOutputStream fileOut=new FileOutputStream("./Task_20000.txt");
        ObjectOutputStream oos=new ObjectOutputStream(fileOut);
        oos.writeObject(geneedfrmTask);
    }

    public LinkedList<edfrmProcessor> genePro(){
        for (int i=0;i<edfrmProcessorNum;i++){
            genePro.add(new edfrmProcessor(i+1));
        }
        return genePro;
    }
}