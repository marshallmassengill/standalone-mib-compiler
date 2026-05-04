package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Varbindsdecode {

    @XmlElement(name = "parmid")
    private String parmid;
    
    @XmlElement(name = "decode")
    private List<Decode> decodes = new ArrayList<>();

    public Varbindsdecode() {}

    public String getParmid() {
        return parmid;
    }

    public void setParmid(String parmid) {
        this.parmid = parmid;
    }

    public List<Decode> getDecodes() {
        return decodes;
    }

    public void setDecodes(List<Decode> decodes) {
        this.decodes = decodes;
    }

    public void addDecode(Decode decode) {
        this.decodes.add(decode);
    }
}