package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Maskelement {
    
    @XmlElement(name = "mename")
    private String mename;
    
    @XmlElement(name = "mevalue")
    private List<String> mevalues = new ArrayList<>();

    public Maskelement() {}

    public String getMename() {
        return mename;
    }

    public void setMename(String mename) {
        this.mename = mename;
    }

    public List<String> getMevalues() {
        return mevalues;
    }

    public void setMevalues(List<String> mevalues) {
        this.mevalues = mevalues;
    }

    public void addMevalue(String mevalue) {
        this.mevalues.add(mevalue);
    }
}