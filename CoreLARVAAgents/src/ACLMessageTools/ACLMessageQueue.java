/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ACLMessageTools;

import static ACLMessageTools.ACLMessageTools.isAnswerTo;
import static ACLMessageTools.ACLMessageTools.toJsonACLM;
import TimeHandler.TimeHandler;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class ACLMessageQueue {

    protected ArrayList<ACLMessage> queue; // Cola de mensajes
    protected String lastread="", lastmessage="";

    public ACLMessageQueue() {
        queue = new ArrayList();
        flush();
    }

    public final void flush() {
        synchronized (this) {   // Cerrojo para acceso exclusivo
            queue.clear();
        }
    }
    // Devuelve true si la cola está vacía, false en otro caso

    public boolean isEmpty() {
        return this.size() == 0;
    }

    // Devuelve el número de elementos útiles en la cola
    public int size() {
        synchronized (this) {
            return queue.size();
        }
    }

    // Devuelve el número de elementos útiles en la cola
    public ACLMessage get(int position) {
        synchronized (this) {   // Cerrojo para acceso exclusivo
            if (0 <= position && position < size()) {
                return queue.get(position);
            } else {
                return null;
            }
        }
    }

    // Devuelve el número de elementos útiles en la cola
    public void set(int position, ACLMessage m) {
        synchronized (this) {   // Cerrojo para acceso exclusivo
            if (0 <= position && position < size()) {
                queue.set(position, m);
            }

        }
    }

    // Devuelve el número de elementos útiles en la cola
    public ACLMessage Pop(int position) {
        synchronized (this) {   // Cerrojo para acceso exclusivo
            if (0 <= position && position < size()) {
                ACLMessage aux = get(0);                
                queue.remove(0);
                this.lastmessage=toJsonACLM(aux).toString();
                this.lastread = new TimeHandler().toString();
                return aux;
            } else {
                return null;
            }
        }
    }

    // Extrae el primer mensaje de la cola
    public ACLMessage Pop() {
        return Pop(0);
    }

    public void unPop(ACLMessage msg) {
        synchronized (this) {   // Cerrojo para acceso exclusivo
            queue.add(msg);
        }
    }

    // Extrae el primer mensaje de la cola
    public ACLMessage PopAnswer(ACLMessage sent) {
        ACLMessage ret = null;
        synchronized (this) {   // Cerrojo para acceso exclusivo
            for (int i = 0; i < size() && ret == null; i++) {
                if (isAnswerTo(get(i), sent)) {
                    ret = Pop(i);
                }
            }
        }
        return ret;
    }

    public ACLMessage PopSender(AID sender) {
        ACLMessage ret = null;
        synchronized (this) {   // Cerrojo para acceso exclusivo
            for (int i = 0; i < size() && ret == null; i++) {
                if (get(i).getSender().equals(sender)) {
                    ret = Pop(i);
                }
            }
        }
        return ret;
    }

    // Pone un mensaje al final de la cola
    public void Push(ACLMessage msg) {
        synchronized (this) {        // Cerrojo para acceso exclusivo
            queue.add(msg);
//        System.out.println("\nQUEUE <"+msg.getContent()+"> out of "+size()+" FROM: "+msg.getSender().getFullName()+"-->  TO: "+msg.getReceiver().getFullName());
        }
    }

    public String lastRead() {
        return lastread;
    }
    public String lastReadMessage() {
        return this.lastmessage;
    }
}
