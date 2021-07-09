/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LarvaAgent;

import static ACLMessageTools.ACLMessageTools.isJsonString;
import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author lcv
 */
public class LarvaAgent extends IntegratedAgent {

    protected int _milestone = 0, _assignmentID = -1, _problemID = 0, _milestoneID = -1, _userID = -1, _groupID = -1;
    protected String _date;
    protected boolean _initSession = false;
    int swait = 5000;

    @Override
    public void setup() {
        super.setup();
        _exitRequested = true;
        System("Configure agent");
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        doFullCheckout();

    }

    protected boolean doFullCheckin() {
        boolean res = false;

        Info("\nHACKATHON AND EXAM PROTOCOL\n");
        if (!doCheckinPlatform()) {
            return false;
        }
        _initSession = true;
        this.doCheckinLARVA();
        _assignmentID = _config.get("analytics").asObject().get("assignmentid").asInt();
        switch (this._analyticsAgent) {
            case "Mutenroshi":
                _problemID = 0;
                break;
            case "Goku":
                _problemID = 1;
                break;
            case "Songoanda":
                _problemID = 2;
                break;
            case "Freezer":
                _problemID = 14;
                break;
            case "Jake":
                _problemID = 15;
                break;
            default:
                _problemID = -1;
                break;
        }
        return true;
    }

    protected void doFullCheckout() {

        if (swait > 0) {
            Info("Waiting " + swait + " msecs for the agent to die ");
            try {
                Thread.sleep(swait);
            } catch (Exception ex) {
            }
        }
        this.doCheckoutLARVA();
        this.doCheckoutPlatform();

    }

    @Override
    protected void setState(String s) {
        super.setState(s);
        registerMilestone();
    }

    @Override
    protected void Error(String message) {
        super.Error(message);
//        this.reportFailure(message);
    }



    protected JsonObject reportMilestone() {
        JsonObject res = new JsonObject().add("report", "milestone"),
                content = new JsonObject();
        JsonArray list = new JsonArray();

        String behaviourname = (this._currentBehaviour != null
                ? this._currentBehaviour.getBehaviourName() : "UNKNOWN");
        content.add("behaviourname", behaviourname).
                add("assignmentid", _assignmentID).
                add("problemid", _problemID).
                add("userid", this._myCardID.getShortID()).
                add("date", _stateDate);
        if (isJsonString(_state)) {
            JsonObject aux = Json.parse(_state).asObject();
            if (aux.get("info") != null) {
                content.add("objectid", aux.getString("info", "unknown")).
                        add("json", aux.get("value").asObject());
            } else if (aux.get("report") != null) {
                content.add("objectid", aux.getString("report", "unknown")).
                        add("json", aux.get(aux.getString("report", "unknown")));

            } else {
                content.add("objectid", "unkown").
                        add("json", aux);
            }
        } else {
            content.add("objectid", _state);

        }
        list.add(content);
        return res.add("milestone", list);

    }

    public void registerMilestone() {
        // The first try to register a milestone, even if it is deep in the class hiearchy, initializes 
        // The reporting infrastructure

        if (_shutdownRequested) {
            return;
        }
        if (!_initSession) {
            doFullCheckin();
        }
        String auxtitle = _state;
        if (isJsonString(_state)) {
            JsonObject aux = Json.parse(_state).asObject();
            if (aux.get("info") != null) {
                auxtitle = aux.getString("info", "unknown");
            }
        }

        // Then report the milestone reached
        if (_myCardID != null && _myCardID.isValid()) {
            // Search in the DF which agent is monitoring this exercise

            ACLMessage msg = new ACLMessage();
            msg.setSender(getAID());
            msg.addReceiver(new AID(_analyticsAgent, AID.ISLOCALNAME));
            msg.setProtocol("ANALYTICS");
            msg.setContent(this.reportMilestone().toString());
            msg.setEncoding(_myCardID.getShortID());
            msg.setPerformative(ACLMessage.INFORM);
            this.send(msg);
        }

    }

    protected void doCheckForErrors(String state) {

    }

}

//    protected void startSession() {
//        this.doCheckinPlatform();
//        ACLMessage msg = new ACLMessage();
//        msg.setSender(getAID());
//        msg.addReceiver(new AID(_monitorAgent, AID.ISLOCALNAME));
//        msg.setProtocol("ANALYTICS");
//        msg.setContent("" + _singleBehaviour);
////        if (_myCardID != null && _myCardID.isValid()) {
////            msg.setEncoding(_myCardID.getCardID());
////        }
//        msg.setPerformative(ACLMessage.SUBSCRIBE);
//        this.send(msg);
//        _initSession = false;
//    }
//
//    protected void endSession() {
//        this.doCheckoutPlatform();
//        ACLMessage msg = new ACLMessage();
//        msg.setSender(getAID());
//        msg.addReceiver(new AID(_monitorAgent, AID.ISLOCALNAME));
//        msg.setProtocol("ANALYTICS");
//        msg.setContent("");
//        msg.setPerformative(ACLMessage.CANCEL);
//        this.send(msg);
//        _initSession = false;
//
//    }
//
//    protected void badNotification(String cause) {
//        if (_myCardID != null && _myCardID.isValid() && !_monitorAgent.equals("")) {
//            ACLMessage msg = new ACLMessage();
//            msg.setSender(getAID());
//            msg.addReceiver(new AID(_monitorAgent, AID.ISLOCALNAME));
//            msg.setProtocol("ANALYTICS");
//            msg.setContent(cause);
//            msg.setPerformative(ACLMessage.FAILURE);
//            this.send(msg);
//        } else {
//            if (!_monitorAgent.equals("")) {
//                ACLMessage msg = new ACLMessage();
//                msg.setSender(getAID());
//                msg.addReceiver(new AID(_monitorAgent, AID.ISLOCALNAME));
//                msg.setProtocol("ANALYTICS");
//                msg.setContent(cause);
//                msg.setPerformative(ACLMessage.FAILURE);
//                this.send(msg);
//            }
//            System.err.println("!!!!!!!!!!!!!!!!!!!!!!" + cause);
//            abortSession();
//
//        }
//    }
//
//    protected boolean plugAnalytics() {
//        try {
//            _assignmentID = _config.get("analytics").asObject().get("assignmentid").asInt();
//
//            ArrayList<String> servers = this.getAllServiceAgents("Analytics assignmentid " + _assignmentID);
//            if (servers != null && servers.size() > 0) {
//                _monitorAgent = servers.get(0);
//            } else {
//                badNotification(defText(red) + "ABORT! No agent registered to monitor this assignment. Please ask the teacher.");
//            }
//        } catch (Exception Ex) {
//            badNotification(defText(red) + "ABORT! Analytics configuration is missing or wrong \n" + defText(gray) + "Please locate the config file into the following folder\n"
//                    + "<NetBeans Project>\n"
//                    + "   |\n"
//                    + "   +-config/\n"
//                    + "      |\n"
//                    + "      +-default.json\n"
//                    + "and add the following section \"analytics\": {\"assignmentid\": XXX}\n"
//                    + "where XXX is the integer number that follows the title of each assignment in LARVA Web page");
//            abortSession();
//        }
//        switch (_monitorAgent) {
//            case "AgentHelloworld":
//                _problemID = 0;
//                break;
//            case "Goku":
//                _problemID = 1;
//                break;
//            case "Songoanda":
//                _problemID = 2;
//                break;
//            case "Freezer":
//                _problemID = 14;
//                break;
//            case "Jake":
//                _problemID = 15;
//                break;
//            default:
//                _problemID = -1;
//                break;
//        }
//
//        return true;
//    }
