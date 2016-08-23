package org.jenkinsci.plugins.systemcheck;

import hudson.Extension;
import hudson.scheduler.CronTab;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import antlr.ANTLRException;

@Extension
public class HistoryGlobalConfiguration extends GlobalConfiguration {

    private int RefreshRate = 2 ; // every  minutes

    private int CountsOfKeepingLoad = 4 * 60 / 2;

    private int CountsOfKeepingBuilds = 4 * 60 / 2;

    public HistoryGlobalConfiguration() {
        load();
    }

    @DataBoundConstructor
    public HistoryGlobalConfiguration(int RefreshRate, int CountsOfKeepingLoad, int CountsOfKeepingBuilds) {
        this.RefreshRate = RefreshRate;
        this.CountsOfKeepingLoad = CountsOfKeepingLoad;
        this.CountsOfKeepingBuilds = CountsOfKeepingBuilds;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws FormException {

        System.out.println(json.toString());
//        this.RefreshRate = req.bindJSON(
//                HistoryGlobalConfiguration.class, json).RefreshRate;
        this.RefreshRate = json.getInt("RefreshRate");
        this.CountsOfKeepingLoad = json.getInt("CountsOfKeepingLoad");
        this.CountsOfKeepingBuilds = json.getInt("CountsOfKeepingBuilds");

        save();
        return true;
    }
    
    public FormValidation doCheckRefreshRate(@QueryParameter String value) {
        int v = -1;
        try{
            v = Integer.parseInt(value.trim());
        }catch(NumberFormatException e) {
           return FormValidation.error("Couldn't parse Integer Value!");
        }
        if(v < 60 && v > 0) {
            return FormValidation.ok();
        } else {
            return FormValidation.error("Please insert value in Range 1 to 59 (both inclusive)");
        }
    }

    public static HistoryGlobalConfiguration get() {
        return GlobalConfiguration.all().get(
                HistoryGlobalConfiguration.class);
    }

    public int getRefreshRate() {
        return RefreshRate;
    }

    public int getCountsOfKeepingLoad() {
        return CountsOfKeepingLoad;
    }
    
    public int getCountsOfKeepingBuilds() {
        return CountsOfKeepingBuilds;
    }

}

