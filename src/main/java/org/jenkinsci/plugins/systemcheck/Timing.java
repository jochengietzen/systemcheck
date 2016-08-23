package org.jenkinsci.plugins.systemcheck;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import antlr.ANTLRException;

@Extension
public class Timing extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(Timing.class.getName());


    protected Timing(String name) {
        super(name);
    }

    public Timing() {
        super("Timing");
    }


    @Override
    protected void execute(TaskListener tasklistener) throws IOException, InterruptedException {
        LOGGER.setLevel(Level.WARNING);
        final String cronTime = "*/" + HistoryGlobalConfiguration.get().getRefreshRate() + " * * * *";
        final int CountOfKeepingLoad = HistoryGlobalConfiguration.get().getCountsOfKeepingLoad();
        final int CountOfKeepingBuilds = HistoryGlobalConfiguration.get().getCountsOfKeepingBuilds();
        final long currentTime = System.currentTimeMillis();
        CronTab cronTab;
        try {
            cronTab = new CronTab(cronTime);
            if ((cronTab.ceil(currentTime).getTimeInMillis() - currentTime) == 0) {
                LOGGER.info("Record load entry");
                LoadHistory.recordCurrentEntry(CountOfKeepingLoad);
                BuildsHistory.recordCurrentEntry(CountOfKeepingBuilds);
            }


        } catch (ANTLRException e) {
            LOGGER.warning("Could not parse Cron");
            e.printStackTrace();
        }
    }

    protected Level getNormalLoggingLevel() {
        return Level.FINEST;
    }
    
    @Override
    public long getRecurrencePeriod() {
//        return TimeUnit.SECONDS.toMillis(2);
        return MIN;
    }
}