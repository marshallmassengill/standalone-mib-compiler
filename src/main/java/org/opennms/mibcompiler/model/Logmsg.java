package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Logmsg {
    
    @XmlAttribute(name = "dest")
    private String dest;
    
    @XmlValue
    private String content;

    public Logmsg() {}

    public Logmsg(String dest, String content) {
        this.dest = dest;
        this.content = content;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}