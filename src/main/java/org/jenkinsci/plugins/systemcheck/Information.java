package org.jenkinsci.plugins.systemcheck;

import java.util.HashMap;

public class Information {
    private String name;
    private String value;
    private long date;
    private String description;
    private HashMap<String,String> attributes;

    public Information(String name, Object value, long date, String description) {
        this.name = name;
        this.value = value.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        this.date = date;
        this.description = description.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        this.attributes = new HashMap<String, String>();
    }

    public Information(String name, Object value, long date, String description, HashMap hm) {
        this.name = name;
        this.value = value.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        this.date = date;
        this.description = description.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        this.attributes = hm;
    }

    public void putValue(String key, String value) {
        this.attributes.put(key, value.replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
    }

    public void putValue(String key, Object value) {
        this.attributes.put(key, value.toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
    }

    public Information getInformations(){
        return this;
    }
    public String getName() {
        return name;
    }
    public String getValue() {
        return value;
    }
    public long getDate() {
        return date;
    }
    public String getDescription() {
        return description;
    }

    public String toJSON() {
        String r = "\"" + this.name + "\":{";
        String attr = "\"name\":\"" + this.name + "\",\"value\":\"" + this.value + "\",\"date\":\"" + this.date + "\",\"description\":\"" + this.description +"\",";
        for(String key : attributes.keySet()) {
            attr += "\"" + key + "\":\"" + attributes.get(key) + "\",";
        }
        r += attr.substring(0, attr.length() - 1) + "}";
        return r;  
    }

    public String toXML() {
        String r = "<" + this.name + ">";
        r += "<name>" + this.name + "</name><value>" + this.value + "</value><date>" + this.date + "</date><description>" 
                + this.description +"</description>";
        for(String key : attributes.keySet()) {
            r += "<" + key.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + ">" + attributes.get(key) + "</" + key.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + ">";
        }
        r += "</" + this.name + ">";
        return r;
    }
}
