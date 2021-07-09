/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Events;

import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class EventQueue implements EventListener{
    protected ArrayList <Event> eventQueue;

    public EventQueue() {
        eventQueue = new ArrayList();
    }
    
    synchronized public void push(Event e) {
        eventQueue.add(e);
    }
    
    @Override
    public void doNotify(Event e) {
        push(e);
    }

    @Override
    public Event doLastEvent() {
        return eventQueue.get(eventQueue.size()-1);
    }

    @Override
    public void doSubscribeEvent(EventHandler h, String [] types) {
        h.doSubscribe(this, types);
    }
}
