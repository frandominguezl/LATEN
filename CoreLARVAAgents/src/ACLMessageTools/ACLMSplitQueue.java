/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ACLMessageTools;

import PlainAgent.PlainAgent.myBehaviour;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.HashMap;
import java.util.Set;
import AdminReport.ReportableObject;

/**
 *
 * @author lcv
 */
public class ACLMSplitQueue implements ReportableObject {

    protected HashMap<String, ACLMessageQueue> _queueList;
    protected HashMap<String, myBehaviour> _serviceList;

    public ACLMSplitQueue() {
        _queueList = new HashMap<>();
        _serviceList = new HashMap<>();
//        ACLMessageQueue regular = new ACLMessageQueue();
//        _queueList.put("REGULAR", regular);
    }

    public void addList(String protocol, myBehaviour service) {
        _queueList.put(protocol, new ACLMessageQueue());
        _serviceList.put(protocol, service);
    }

    public synchronized void Push(ACLMessage msg) {
        String protocol = (msg.getProtocol() == null ? "REGULAR" : msg.getProtocol());
//        if (!_queueList.keySet().contains(protocol)) {
//            _queueList.put(protocol, new ACLMessageQueue());
//        }
//        _queueList.get(protocol).Push(msg);
        if (_queueList.keySet().contains(protocol)) {
            _queueList.get(protocol).Push(msg);
            _serviceList.get(protocol).allowBehaviour();
        }
    }

    public ACLMessage Pop(String protocol) {
        if (!_queueList.keySet().contains(protocol)) {
            return null;
        } else if (_queueList.get(protocol).isEmpty()) {
            return null;
        } else {
            return _queueList.get(protocol).Pop();
        }
    }

    public ACLMessage Pop() {
        return Pop("REGULAR");
    }

    public boolean isEmpty(String protocol) {
        if (!_queueList.keySet().contains(protocol)) {
            return true;
        } else {
            return _queueList.get(protocol).isEmpty();
        }
    }

    public boolean isEmpty() {
        return isEmpty("REGULAR");
    }

    public boolean isAllEmpty() {
        boolean res = true;
        for (String prot : _queueList.keySet()) {
            res &= isEmpty(prot);
        }

        return res;
    }

    public ACLMessageQueue getQueue(String protocol) {
        return _queueList.get(protocol);
    }

    public myBehaviour getService(String protocol) {
        return _serviceList.get(protocol);
    }

    public Set<String> getKeySet() {
        return _queueList.keySet();
    }

    public String fancyStatus() {
        String res = "|";
        for (String prot : _queueList.keySet()) {
            res += prot + ": " + getQueue(prot).size() + "|";
        }
        return res;
    }

    public JsonObject reportStatus() {
        JsonObject res = new JsonObject().add("report", "mailqueues");
        JsonArray list = new JsonArray();
        for (String prot : _queueList.keySet()) {
            list.add(new JsonObject().add("objectid", prot).
                    add("size", getQueue(prot).size()));
        }
        return res.add("mailqueues", list);
    }

    @Override
    public String defReportType() {
        return "mailqueues";
    }

    @Override
    public String[] defReportableObjectList() {
        return _queueList.keySet().toArray(new String [_queueList.keySet().size()]);
    }

    @Override
    public String reportObjectDate(String objectid) {
        return this.getQueue(objectid).lastRead();
    }

    @Override
    public String reportObjectStatus(String objectid) {
        return this.getQueue(objectid).lastReadMessage();
    }

    @Override
    public String reportObjectValue(String objectid) {
        return this.getQueue(objectid).size()+"";
    }
}
