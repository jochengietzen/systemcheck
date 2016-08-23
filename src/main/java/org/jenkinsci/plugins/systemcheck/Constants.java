package org.jenkinsci.plugins.systemcheck;

import java.io.File;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

public class Constants {

    private Constants() {
        // holds constants
    }


    public static final String rootURL = getRootUrl();



//    public static long REFRESH_RATE = 1000 * 60 * 5 ; // every 5 minutes
//    public long REFRESH_RATE = this.getDescriptor().getDefaultREFRESH_RATE();

//    public static final int CountsOfKeeping = 4 * 60 / 2;
//    public int CountsOfKeeping = this.getDescriptor().getDefaultCountsOfKeeping();

//    public long getREFRESH_RATE() {
//        return this.getDescriptor().getDefaultREFRESH_RATE();
//    }

    public static final String UserDir = "systemcheck/";
            
    public static final String BASE = Constants.getHome()
            + "userContent/" + getDirectory();
    public static final String history_file_Load = BASE + "loadHistory";
    public static final String history_file_Build = BASE + "buildHistory";
    public static final String file_tmp = BASE + "tmp";


    public static final String IndexStart = "i_";

    public static final String IndexEnd= "_i";

    public static final String Delimiter = " ";
    
    public static final String Delimiter2 = ":";
    
    
    private static String getHome() {
        String s = Jenkins.getInstance().getRootDir().getPath();
        if (s == null) {
            s = System.getProperty("JENKINS_HOME");
        }
        if (s == null) {
            s = System.getProperty("HUDSON_HOME");
        }
        if (s == null) {
            s = (new File(System.getProperty("user.dir"))).getParent();
        }
        if (s != null && !s.endsWith("/"))
            s = s + "/";
        if (s != null && !s.startsWith("/"))
            s = "/" + s;
        return s;
    }

    private static String getDirectory() {
        File f = new File(Constants.getHome() + "userContent/" + UserDir);
        if(!f.exists()) {
            f.mkdirs();
        }
        return UserDir;
    }

    private static String getRootUrl() {
        String s = Jenkins.getInstance().getRootUrl();
        if (s == null) {
            s = JenkinsLocationConfiguration.get().getUrl();
        }
        // other methods just return null, done just for running under developer mode
        // hardcoded and obviously working only when jenkins runs on 8080
        // info
        // https://wiki.jenkins-ci.org/display/JENKINS/Hyperlinks+in+HTML
        if(s == null) {
            s = "http://localhost:8080/jenkins";
        }
        if (s != null && !s.endsWith("/"))
            s = s + "/";
        return s;
    }
    
}
