/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Events;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class EventHandler {

    EventQueue queue;
    HashMap<String, ArrayList<EventListener>> subscriptions;

    EventHandler() {
        queue = new EventQueue();
        subscriptions = new HashMap();
    }

    protected void checkList(String eventtype) {
        if (subscriptions.get(eventtype) == null) {
            subscriptions.put(eventtype, new ArrayList());
        }

    }

    public void doSubscribe(EventListener obj, String[] eventtypes) {
        for (String e : eventtypes) {
            checkList(e);
            subscriptions.get(e).add(obj);
        }
    }

    public void doPublish(Event e) {
        checkList(e.type);
        for (EventListener el : subscriptions.get(e)) {
            el.doNotify(e);
        }
    }
}
