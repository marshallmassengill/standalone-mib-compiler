package org.opennms.mibcompiler.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "events")
@XmlAccessorType(XmlAccessType.FIELD)
public class Events {
    
    @XmlElement(name = "event")
    private List<Event> events = new ArrayList<>();

    public Events() {}

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public void addEvent(Event event) {
        this.events.add(event);
    }
}