package org.jenkinsci.plugins.systemcheck;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Queue.Item;
import hudson.remoting.Callable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author Jochen Gietzen
 * 
 */
@ExportedBean
public class BuildsHistory {

    //    private File historyFile;
    //    private String historyFileString;
    //    private Hudson hudson;
    //    protected Computer computer;

    private static final Logger LOGGER = Logger.getLogger(LoadHistory.class
            .getName());

    public BuildsHistory() {
    }

    public String getHistoryString() {
        return createHistoryFiles(HistoryGlobalConfiguration.get().getCountsOfKeepingLoad());
    }

    public static int getRefreshRate() {
        return HistoryGlobalConfiguration.get().getRefreshRate();
    }

    public static int getCountsOfKeeping() {
        return HistoryGlobalConfiguration.get().getCountsOfKeepingLoad();
    }


    protected Level getNormalLoggingLevel() {
        return Level.WARNING;
    }


    public static void recordCurrentEntry(int countOfKeepings) {
        LOGGER.setLevel(Level.WARNING);
        LOGGER.log(Level.FINE, "Starting BuildQueue History");

        String historyFileString = createHistoryFiles(countOfKeepings);
        LOGGER.log(Level.FINE, "Get new BuildQueue...");
        
        Item[] buildables = Hudson.getInstance().getQueue().getItems();
        int blocked = 0;
        int stucked = 0;
        int blockedStucked = 0;
        int fine = 0;
        int total = 0;
        for(Item b : buildables) {
            if(b.isBlocked()) blocked++;
            if(b.isStuck()) stucked++;
            if(b.isBlocked() && b.isStuck()) blockedStucked++;
            if(!b.isBlocked() && !b.isStuck()) fine++;
            total++;
        }
        String info = (String.format("%d" + Constants.Delimiter2 + "%d" + Constants.Delimiter2 + "%d" + Constants.Delimiter2 + "%d" + Constants.Delimiter2 + "%d", blocked, stucked, blockedStucked, fine, total));
//        System.out.println(info);
        boolean work = true;
        try {
            writeData(info, countOfKeepings, historyFileString);
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            work = false;
        }
        if (work) LOGGER.log(Level.FINE, "History file was written successfully");
    }

    private static void writeData(String load, int countOfKeepings, String historyFileString) throws IOException {
        if (load == null) load = "-1.0";
//        TODO: Eventuell nochmal abfangen, ob die Anzahl noch stimmt!
//        if(HistoryGlobalConfiguration.get().getCountsOfKeeping() != getCount(countOfKeepings)) {
//
//        }
        String[] values = historyFileString.trim().split(Constants.Delimiter);
        int currIndex = getCurrentIndex(historyFileString);
        values[currIndex] = load;
        historyFileString = String.join(Constants.Delimiter, values);
        historyFileString = riseCurrentIndex(historyFileString, countOfKeepings);
        saveHistoryFile(historyFileString);
    }

    private static int getCurrentIndex(String string) {
        int start = string.indexOf(Constants.IndexStart) + Constants.IndexStart.length();
        int end = string.indexOf(Constants.IndexEnd);
        int nr = Integer.parseInt(string.substring(start,end));
        return nr + 1;
    }

    private static void saveHistoryFile(String historyFileString) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(Constants.history_file_Build)));
        bw.write(historyFileString.trim());
        bw.close();
    }

    private static int getCount(int countOfKeepings) {
        String historyFileString = createHistoryFiles(countOfKeepings);
        String[] values = historyFileString.split(Constants.Delimiter);
        return values.length - 1;
    }

    private static String riseCurrentIndex(String historyFileString, int countOfKeepings) {
        int start = historyFileString.indexOf(Constants.IndexStart); 
        int length = Constants.IndexStart.length();
        int end = historyFileString.indexOf(Constants.IndexEnd);
        int nr = Integer.parseInt(historyFileString.substring(start + length,end));
        int oldNr = nr;
        nr++;
        nr = nr % countOfKeepings;
        return historyFileString.replaceAll(Constants.IndexStart + (oldNr) + Constants.IndexEnd, Constants.IndexStart + (nr) + Constants.IndexEnd);
    }

    private static String createHistoryFiles(int countOfKeepings) {
        File historyFile = new File(Constants.history_file_Build);
        String historyFileString = "";
        try {
            if (!historyFile.exists()) {
                String s = Constants.IndexStart + 0 + Constants.IndexEnd;
                for(int i = 0; i < countOfKeepings; i++) {
                    s += Constants.Delimiter + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0";
                }
                historyFile.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(historyFile));
                bw.write(s);
                bw.close();
                historyFileString = s;
            }else {
                BufferedReader br = new BufferedReader(new FileReader(historyFile));
                String s = "";
                String tmp = "";
                while ((s = br.readLine()) != null) {
                    tmp += " " + s.trim();
                }
                br.close();
                historyFileString = tmp;
                String[] values = tmp.trim().split(Constants.Delimiter);
                if(values.length - 1 > countOfKeepings) {
                    String s2 = Constants.IndexStart + 0 + Constants.IndexEnd;
                    List<String> list = getLastWrittenData(tmp.trim(), countOfKeepings);
                    for(int i = list.size() - 1; i >= 0; i--) {
                        s2 += Constants.Delimiter + list.get(i);
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(historyFile));
                    bw.write(s2);
                    bw.close();
                    historyFileString = s2;
                } else if (values.length - 1 < countOfKeepings) {
                    int currIndex = (getCurrentIndex(tmp.trim()));
                    String s2 = Constants.IndexStart + (currIndex - 1)+ Constants.IndexEnd;
                    for(int i =  1; i < currIndex; i++) {
                        s2 += Constants.Delimiter + values[i];
                    }
                    for(int i = 0; i <= countOfKeepings - values.length; i++) {
                        s2 += Constants.Delimiter + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0" + Constants.Delimiter2 + "0";
                    }
                    for(int i =  currIndex; i < values.length; i++) {
                        s2 += Constants.Delimiter + values[i];
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(historyFile));
                    bw.write(s2);
                    bw.close();
                    historyFileString = s2;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return historyFileString;
    }

    public static List<String> getLastWrittenData(String string, int countOfKeepings) {
        String[] values = string.substring(string.indexOf(Constants.IndexEnd) + Constants.IndexEnd.length()).trim().split(Constants.Delimiter);
        int currIndex = getCurrentIndex(string) - 2;
        currIndex = (currIndex >= 0) ? currIndex : countOfKeepings + currIndex;
        List<String> ret = new ArrayList<String>();
        for(int i = 0; i < values.length && ret.size() < countOfKeepings; i++) {
            ret.add(values[currIndex-- % values.length]);
            if(currIndex < 0) {
                currIndex = values.length - 1;
            }
        }
        return ret;
    }
    
    public static List<String> getLastWrittenData() {
        int countOfKeepings = HistoryGlobalConfiguration.get().getCountsOfKeepingLoad();
        String string = createHistoryFiles(countOfKeepings);
        String[] values = string.substring(string.indexOf(Constants.IndexEnd) + Constants.IndexEnd.length()).trim().split(Constants.Delimiter);
        int currIndex = getCurrentIndex(string) - 2;
        currIndex = (currIndex >= 2) ? currIndex : 0;
        List<String> ret = new ArrayList<String>();
        for(int i = 0; i < values.length && ret.size() < countOfKeepings; i++) {
            ret.add(values[currIndex-- % values.length]);
            if(currIndex < 0) {
                currIndex = values.length - 1;
            }
        }
        return ret;
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
