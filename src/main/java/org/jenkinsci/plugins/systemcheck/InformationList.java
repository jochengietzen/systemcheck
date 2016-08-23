package org.jenkinsci.plugins.systemcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InformationList {
    private String name;
    private List<Object> informationList;
    private HashMap<String,String> attributes;

    public InformationList(String name, InformationList i) {
        this.name = name;
        if (this.informationList == null) {
            this.informationList = new ArrayList<Object>();
        }
        informationList.add(i);
        this.attributes = new HashMap<String, String>();
    }

    public InformationList(String name, Information i) {
        this.name = name;
        if (this.informationList == null) {
            this.informationList = new ArrayList<Object>();
        }
        informationList.add(i);
        this.attributes = new HashMap<String, String>();
    }

    public InformationList(String name) {
        this.name = name;
        this.informationList = new ArrayList<Object>();
        this.attributes = new HashMap<String, String>();
    }
    
    public void putValue(String key, Object value) {
        this.attributes.put(key, value.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
    }
    
    public void add(Information i) {
        this.informationList.add(i);
    }

    public void add(InformationList i) {
        this.informationList.add(i);
    }
    
    public String toJSON() {
        String r = "\"";
        r += this.name + "\":{";
        String attr = "";
        for(String key : attributes.keySet()) {
            attr += "\"" + key + "\":\"" + attributes.get(key) + "\",";
        }
//        if(attr.length() > 0) {
//            attr.substring(0, attr.length());
//        }
        r += attr + "\"kids\":{";
        for(Object o : informationList) {
            if(o.getClass() == this.getClass()) {
                r += ((InformationList) o).toJSON() + ",";
            } else if (o.getClass() == Information.class) {
                r += ((Information) o).toJSON() + ",";
            } else {
                r += "'Something went wrong!'";
            }
        }
        if(informationList.isEmpty()) r += ",";
        r = r.substring(0, r.length() - 1) + "}}";
        return r;  
    }
    
    public String toXML() {
        String r = "<" + this.name + ">";
        r += "<name>" + this.name + "</name>";
        for(String key : attributes.keySet()) {
            r += "<" + key.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + ">" + attributes.get(key) + "</" + key.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + ">";
        }
        if(!informationList.isEmpty()) {
//            r += "<kids>";
            for(Object o : informationList) {
                if(o.getClass() == this.getClass()) {
                    r += ((InformationList) o).toXML();
                } else if (o.getClass() == Information.class) {
                    r += ((Information) o).toXML();
                } else {
                    r += "'Something went wrong!'";
                }
            }
//            r += "</kids>";
        }
        r += "</" + this.name + ">";
        return r;  
    }
}
