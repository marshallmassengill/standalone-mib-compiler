package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Mask {
    
    @XmlElement(name = "maskelement")
    private List<Maskelement> maskelements = new ArrayList<>();

    public Mask() {}

    public List<Maskelement> getMaskelements() {
        return maskelements;
    }

    public void setMaskelements(List<Maskelement> maskelements) {
        this.maskelements = maskelements;
    }

    public void addMaskelement(Maskelement maskelement) {
        this.maskelements.add(maskelement);
    }
}