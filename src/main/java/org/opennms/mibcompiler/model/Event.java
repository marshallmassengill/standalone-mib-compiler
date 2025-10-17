package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event {
    
    @XmlElement(name = "uei")
    private String uei;
    
    @XmlElement(name = "event-label")
    private String eventLabel;
    
    @XmlElement(name = "descr")
    private String descr;
    
    @XmlElement(name = "logmsg")
    private Logmsg logmsg;
    
    @XmlElement(name = "severity")
    private String severity;
    
    @XmlElement(name = "mask")
    private Mask mask;
    
    @XmlElement(name = "varbindsdecode")
    private List<Varbindsdecode> varbindsdecodes;

    public Event() {}

    public String getUei() {
        return uei;
    }

    public void setUei(String uei) {
        this.uei = uei;
    }

    public String getEventLabel() {
        return eventLabel;
    }

    public void setEventLabel(String eventLabel) {
        this.eventLabel = eventLabel;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public Logmsg getLogmsg() {
        return logmsg;
    }

    public void setLogmsg(Logmsg logmsg) {
        this.logmsg = logmsg;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Mask getMask() {
        return mask;
    }

    public void setMask(Mask mask) {
        this.mask = mask;
    }

    public List<Varbindsdecode> getVarbindsdecodes() {
        return varbindsdecodes;
    }

    public void setVarbindsdecodes(List<Varbindsdecode> varbindsdecodes) {
        this.varbindsdecodes = varbindsdecodes;
    }
}