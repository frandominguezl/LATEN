/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Events;

/**
 *
 * @author lcv
 */
public interface EventListener {

    public void doNotify(Event e);

    public Event doLastEvent();
    
    public void doSubscribeEvent(EventHandler h, String [] types);

}
