/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Locker;

import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class Locker {

    protected ArrayList<String> sentRequest;
    protected HashMap<String, Object> receivedRequest;
    protected boolean active;
    
    public Locker() {
        clear();
    }

    public final Locker clear() {
        sentRequest = new ArrayList();
        receivedRequest = new HashMap();
        active = false;
        return this;
    }
    
    public boolean isActive(){
        return active;
    }
   
    public Locker activate() {
        active = true;
        return this;
    }
    
    public Locker deactivate() {
        active = false;
        return this;
    }
    
    
    public int countArrived() {
        return receivedRequest.size();
    }
    
    public int countSent() {
        return sentRequest.size();
    }
    
    public ArrayList<String> getSentID() {
        return (ArrayList <String>) sentRequest.clone();
    }
    
    public ArrayList<String> getReceivedID() {
        return new ArrayList(receivedRequest.keySet());
    }
    
    public Locker addSent(String id) {
        sentRequest.add(id);
        return this;
    }
    
    public Locker addReceivedObject(String id, Object o) {
        receivedRequest.put(id, o);
        return this;
    }
    
    public Object getReceivedObject(String id) {
        return receivedRequest.get(id);
    }
}
