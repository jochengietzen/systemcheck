package org.jenkinsci.plugins.systemcheck;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Queue.Item;
import hudson.remoting.Callable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension
public class SystemCheckRootAction implements UnprotectedRootAction {

    private int oks;
    private int warnings;
    private int failures;
    private int unkowns;
    //    private ArrayList<CheckDetails> list = new ArrayList<CheckDetails>();
    private InformationList info;

    private Map<String, Long> waitingSince = new HashMap<String, Long>();
    private Map<String, Boolean> nodesRequired = new HashMap<String, Boolean>();

    /**
     * Constructor
     */
    public SystemCheckRootAction() {
        super();
    }

    public InformationList getInfo() {
        return info;
    }

    public String getInfoJSON() {
        check();
        return "{" + info.toJSON() + "}";
    }

    public String getInfoJSONAsXML() {
        return "<?xml version='1.0' encoding='UTF-8'?><json>" + getInfoJSON() + "</json>";
    }

    public String getInfoXML() {
        return "<?xml version='1.0' encoding='UTF-8'?>" + info.toXML();
    }

    public String getDisplayName() {

        return null;
    }

    public String getIconFileName() {

        return null;
    }

    public String getUrlName() {

        return "status";
    }

    public boolean check() {
        info = new InformationList("SystemCheck");
        failures = 0;
        oks = 0;
        warnings = 0;
        unkowns = 0;
        info.putValue("RefreshRate", LoadHistory.getRefreshRate());
        info.putValue("CountofKeeping", LoadHistory.getCountsOfKeeping());
        long d = (System.currentTimeMillis() / 1000L);

        // Queue check
        checkQueueOfJobs(d);

        // Slave offline check
        checkSlaveOffline(d);

        // Master load check
        checkMasterLoad(d);

        return true;
    }

    private void checkQueueOfJobs(long d) {
        d = (System.currentTimeMillis() / 1000L);
        for (Node n : Hudson.getInstance().getNodes()) {
            nodesRequired.put(n.getNodeName(), false);
        }
        Item[] buildables = Hudson.getInstance().getQueue().getItems();
        InformationList waitingJobs = new InformationList("waitingJobs");
        InformationList unblockedList = new InformationList("unblocked");
        InformationList blockedList = new InformationList("blocked");
        int blocked = 0;
        int unblocked = 0;
        for(Item b : buildables) {
            Information acc = new Information("JobBlockedCheck", b.isStuck(), (System.currentTimeMillis() / 1000L), "Information about this Job in the Queue");
            acc.putValue("stuck", b.isStuck());
            acc.putValue("blocked", b.isBlocked());
            try {
                acc.putValue("blockedReason", b.getCauseOfBlockage().getShortDescription());
            }catch(NullPointerException e) {
                acc.putValue("blockedReason", "NULL");
            }
            acc.putValue("waitingFor", (System.currentTimeMillis() - b.getInQueueSince())/1000L);
            acc.putValue("waitingSince", b.getInQueueSince()/1000L);
            acc.putValue("jobname", b.task.getName());
            acc.putValue("url", Hudson.getInstance().getRootUrl() + b.task.getUrl());
            if(b.isStuck()) {
                for(Node n:b.getAssignedLabel().getNodes()) {
                    if(waitingSince.containsKey(n.getNodeName())) {
                        long oldSince = waitingSince.get(n.getNodeName());
                        waitingSince.put(n.getNodeName(), Math.min(oldSince, b.getInQueueSince()/1000L));
                    } else {
                        waitingSince.put(n.getNodeName(),b.getInQueueSince()/1000L);
                    }
                    nodesRequired.put(n.getNodeName(), true);
                }
            }
            String theseNodes = "";
            try{
                for(Node n : b.getAssignedLabel().getNodes()) {
                    theseNodes += n.getNodeName() + "|";
                }
            }catch(NullPointerException e) {
                theseNodes = "NULL";
            }
            if(theseNodes.endsWith("|")) theseNodes = theseNodes.substring(0, theseNodes.length() - 1);
            acc.putValue("Nodes", theseNodes);
            if(b.isBlocked() || b.isStuck()) {
                blockedList.add(acc);
                blocked++;
            } else {
                unblockedList.add(acc);
                unblocked++;
            }
        }
        waitingJobs.putValue("blockedCount", blocked);
        waitingJobs.putValue("unblockedCount", unblocked);
        waitingJobs.add(blockedList);
        waitingJobs.add(unblockedList);
        info.add(waitingJobs);
        InformationList buildsHistory = new InformationList("buildsHistory");
        for(String s : BuildsHistory.getLastWrittenData()) {
            Information i = new Information("BuildHistoryValue", s, System.currentTimeMillis() / 1000L, "History value of Build Queue");
            String[] r = s.split(Constants.Delimiter2);
            try{
                i.putValue("blocked", r[0]);
                i.putValue("stuck", r[1]);
                i.putValue("blockedAndStuck", r[2]);
                i.putValue("fine", r[3]);
                i.putValue("total", r[4]);
            }catch(Exception e) {
                i.putValue("ERROR", e.getMessage());
            }
            buildsHistory.add(i);
        }
        info.add(buildsHistory);
    }

    private void checkMasterLoad(long d) {
        d = (System.currentTimeMillis() / 1000L);
        Computer c;
        c = Hudson.getInstance().getComputer("(master)");
        String load = "0";
        boolean loadCheckOkay = true;
        try {
            load = c.getChannel().call(new MonitorTask());
        } catch (IOException e) {
            unkowns++;
            loadCheckOkay = false;
            e.printStackTrace();
        } catch (RuntimeException e) {
            unkowns++;
            loadCheckOkay = false;
            e.printStackTrace();
        } catch (InterruptedException e) {
            unkowns++;
            loadCheckOkay = false;
            e.printStackTrace();
        }
        InformationList inf = new InformationList("MasterLoadCheck");
        if (loadCheckOkay) {
            Information i = new Information("MasterLoadCurrent", load, d, "System Load of Master is " + load);
            inf.putValue("load", load);
            inf.putValue("failure", false);
            inf.add(i);
        } else {
            Information i = new Information("MasterLoadCurrent", "UNKOWN", d, "System Load of Master is unkown");
            inf.putValue("load", "UNKOWN");
            inf.putValue("failure", true);
            inf.add(i);
        }
        InformationList list = new InformationList("MasterLoadHistory");
        for(String s : LoadHistory.getLastWrittenData()) {
            Information i = new Information("MasterLoadHistoryValue", s, System.currentTimeMillis() / 1000L, "History value of Master's load");
            i.putValue("load", s);
            list.add(i);
        }
        inf.add(list);
        info.add(inf);
    }

    private void checkSlaveOffline(long d) {
        d = (System.currentTimeMillis() / 1000L);
        int count = 0;
        InformationList i = new InformationList("AllSlaveCheck");
        for (Node n : Hudson.getInstance().getNodes()) {
            Information no = new Information("SlaveCheck", n.toComputer().isOnline(), d, (n.toComputer().isOnline()) ? "Slave " + n.toComputer().getName() + " is online" : "Slave " + n.toComputer().getName() + " is offline");
            no.putValue("offline", n.toComputer().isOffline());
            no.putValue("offlineCause", n.toComputer().getOfflineCauseReason());
            no.putValue("url", Hudson.getInstance().getRootUrl() + n.toComputer().getUrl());
            no.putValue("connectedSince", n.toComputer().getConnectTime());
            no.putValue("slaveName", n.toComputer().getDisplayName());
            no.putValue("offlineAndRequired", ((nodesRequired.containsKey(n.getNodeName())?nodesRequired.get(n.getNodeName()) : false)));
            no.putValue("longestWaitingSince", ((waitingSince.containsKey(n.getNodeName())?waitingSince.get(n.getNodeName()) : -1)));
            no.putValue("idle", n.toComputer().isIdle());
            String[] monitors = {"hudson.node_monitors.SwapSpaceMonitor","hudson.node_monitors.DiskSpaceMonitor"};
            String[] monitorXML = {"swapSpaceMonitor","diskSpaceMonitor"};
            for(int j = 0; j < monitors.length; j++) {
                String s = monitors[j];
                Object curr = n.toComputer().getMonitorData().get(s);
                curr = ((curr != null) ? curr.toString() : -1);
                no.putValue(monitorXML[j], curr);
            }

            // Returns true if this computer has some idle executors that can take more workload.
            no.putValue("partiallyIdle", n.toComputer().isPartiallyIdle());
            if (n.toComputer().isOffline()) {
                count++;
            }
            i.add(no);
        }        
        i.putValue("count", count);
        info.add(i);
    }

    public String getSummary() {
        return failures == 0 ? "OK" : "FAILURE";
    }

    public String getOks() {
        return String.valueOf(oks);

    }

    public String getWarnings() {
        return String.valueOf(warnings);
    }

    public String getUnkowns() {
        return String.valueOf(unkowns);
    }

    public String getFailures() {
        return String.valueOf(failures);
    }

    //    public ArrayList<CheckDetails> getDetails() {
    //        return list;
    //    }

    //    public List<Node> getNumberOfOfflineSlaves() {
    //        int count = 0;
    //        List<Node> ret = new ArrayList<Node>();
    //        for (Node n : Hudson.getInstance().getNodes()) {
    //            if (n.toComputer().isOffline()) {
    //                count++;
    //            }
    //        }
    //        return count;
    //    }

    public int getNumbersOfSlaves() {

        List <Node> slaves = Hudson.getInstance().getNodes();

        return slaves.size();
    }

    /**
     * Task which returns the SystemLoadAverage.
     */
    static final class MonitorTask implements Callable<String, RuntimeException> {
        private static final long serialVersionUID = 1L;

        /**
         * Detect the System Load Average.
         */
        public String call() {
            final OperatingSystemMXBean opsysMXbean = ManagementFactory.getOperatingSystemMXBean();
            return String.valueOf(opsysMXbean.getSystemLoadAverage());
        }
    }


}
