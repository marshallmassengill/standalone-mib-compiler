package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Decode {
    
    @XmlAttribute(name = "varbindvalue")
    private String varbindvalue;
    
    @XmlAttribute(name = "varbinddecodedstring")
    private String varbinddecodedstring;

    public Decode() {}

    public String getVarbindvalue() {
        return varbindvalue;
    }

    public void setVarbindvalue(String varbindvalue) {
        this.varbindvalue = varbindvalue;
    }

    public String getVarbinddecodedstring() {
        return varbinddecodedstring;
    }

    public void setVarbinddecodedstring(String varbinddecodedstring) {
        this.varbinddecodedstring = varbinddecodedstring;
    }
}