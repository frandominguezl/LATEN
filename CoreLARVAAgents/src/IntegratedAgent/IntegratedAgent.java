/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IntegratedAgent;

import ACLMessageTools.ACLMSplitQueue;
import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.white;
import Logger.Logger;
import PlainAgent.PlainAgent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import static ACLMessageTools.ACLMessageTools.toJsonACLM;
import static ConsoleAnsi.ConsoleAnsi.defText;
import static ConsoleAnsi.ConsoleAnsi.gray;
import static ConsoleAnsi.ConsoleAnsi.lightblue;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static FileUtils.FileUtils.listFiles;
import PublicKeys.PublicCardID;
import TimeHandler.TimeHandler;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import jade.core.AID;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import AdminReport.ReportableObject;
import AdminReport.Reports;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class IntegratedAgent extends PlainAgent implements ReportableObject {

    protected Logger _dlogger;
    protected JsonObject _config;
    protected boolean _exitRequested, _shutdownRequested, _ignoreAMS, _singleBehaviour, _shuttingDown, _debug, _system;
    protected int _colorText, _colorDefault = white, _colorBackground;
    protected ACLMSplitQueue _splitQueue;
    protected PublicCardID _myCardID;
    protected String _state, _stateDate, _shortID;
    protected myPostOffice _mailServer;
    protected String _identitymanager;
    protected final String _lockfilename = ".DeleteThisToReset.lock";
    protected Reports fullreport;

    // Analytics management
    protected String _analyticsAgent = "";

    public IntegratedAgent() {
        super();
        fullreport = new Reports();
        _singleBehaviour = true;
        // Controls the exit of the agent in all behaviours
        _exitRequested = true;
        _shuttingDown = _shutdownRequested = false;
        _debug = false;
    }

    //
    // Agent lyfecycle
    //
    @Override
    public void setup() {
        boolean loadconf;
        // Inherits configuration
        super.setup();
        // Remove deprecated inherited plain behaviour
        this._myBehavioursList.remove(_plainBehaviour);
        this.removeBehaviour(_plainBehaviour);

        // Sets up basic logging service on screen
        _dlogger = new Logger();
        _dlogger.setOwner(this.getAID().getLocalName()); // Every subagent label his own log

        // Loads JSON configuration parameters or an empty one
        loadconf = false;
        _config = new JsonObject();
        if (this.getArguments() != null) {
            try {
                _config = Json.parse(this.getArguments()[0].toString()).asObject();
                if (this._identitymanager == null || this._identitymanager.equals("")) {
                    this._identitymanager = _config.getString("identitymanager", "");
                }
                if (_config.getBoolean("showbehaviour", false)) {
                    _config.set("log", true);
                    _config.set("system", true);
                }
                // if silent, no Info()/Error() is shown on screen
                if (_config.getBoolean("silent", false)) {
                    _dlogger.disableEcho();
                } else {
                    _dlogger.enableEcho();
                }
                // Backups logging service on disk
                if (_config.getBoolean("log", false)) {
//                    String logfile = "./logs/" + getLocalName() + "_log.json"; // Every agent uses its own log file
                    String logfile = "./logs/" + _config.getString("name", "noname") + "_log.json"; // All agents use the same file
                    _dlogger.setLoggerFileName(logfile);
                }

//            System("Setting up agent colours ");
                // Sets up logging colors on screen
                // If section  "appearance" is missing, default color for text and background
                JsonArray ct = new JsonArray().add(1).add(1).add(1), cb = new JsonArray().add(0).add(0).add(0);
                if (_config.get("appearance") != null) {
                    ct = _config.get("appearance").asObject().get("text").asArray();
                    cb = _config.get("appearance").asObject().get("background").asArray();
                }
                _colorText = ConsoleAnsi.defColor(ct.get(0).asDouble(), ct.get(1).asDouble(), ct.get(2).asDouble());
                _colorBackground = ConsoleAnsi.defColor(cb.get(0).asDouble(), cb.get(1).asDouble(), cb.get(2).asDouble());
                _dlogger.setText(_colorText);
                if (_config.size() > 1 || _config.get("system") == null) {
                    loadconf = true;
                }
            } catch (Exception ex) {
                this.Exception(ex);
            }
        }

        // Load cardID if any
        String myCard;
        try {
            myCard = listFiles("cardID", "cardid")[0];
            _myCardID = new PublicCardID();
            if (_myCardID.fromFile("./cardID/" + myCard)) {
                _shortID = _myCardID.getShortID();
            } else {
                _myCardID = null;
            }
        } catch (Exception ex) {
        }

        if (loadconf) {
            System("Load configuration");

        } else {
            Error("Failed to load configuration file");

        }

        if (_myCardID != null && _myCardID.isValid()) {
            System("Found CardID " + _shortID);
            if (!_identitymanager.equals("")) {
                System(this.whoAmI());
            }
        } else {
            System("BAD CardID: No CardID found");
        }
        // Ignore messages from AMS
        _ignoreAMS = true;

        _singleBehaviour = _config.getBoolean("singlebehaviour", true);
        if (_singleBehaviour) {
            System("Single behaviour agent. Adding plainBehaviour.");
            plainIntegratedBehaviour plain = new plainIntegratedBehaviour(this, "REGULAR", _config.getBoolean("showbehaviour", false));
            this.addBehaviour(plain);
        } else {
            // Sets up the internal messaging based on multiple queues upon ACLM protocols
            // A default protocol "REGULAR" is defined
            System("Multiple behaviour agent. Setting up ACLM Message queue server");
            _splitQueue = new ACLMSplitQueue();
            fullreport.addReportable(_splitQueue);
            _mailServer = new myPostOffice(this, "MAIL", _config.getBoolean("showbehaviour", false));
            addBehaviour(_mailServer);
            System("Setting up protocols listeners");
        }
        fullreport.addReportable(this);
        _system =_config.getBoolean("system", false);
    }

    @Override
    protected void takeDown() {
        super.takeDown();

        System("Take down");
//        if (_config.getBoolean("showbehaviour", false)) {
//            System(this.reportAllBehaviours());
//        }
    }

    protected void wrapperExecute() {
        plainExecute();
        if (canExit()) {
            doExit();
        }
    }

    protected final void abortSession() {
        this._exitRequested = true;
        this._shutdownRequested = true;
        doExit();
        this.deactivateLock();
        Info("Forced wait while deleting container");
        try {
            Thread.sleep(10000);
        } catch (Exception ex) {
        }
    }

    protected String getMyStackTrace() {
        return getMyStackTrace(100);
    }

    protected String getMyStackTrace(int level) {
        String res = "";
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            if (level > 0) {
                if (i >= level) {
                    break;
                }
            } else {
                if (i < -level) {
                    continue;
                }
                if (i > -level) {
                    break;
                }
            }
            StackTraceElement s = elements[i];
            res += "at " + s.getClassName() + "." + s.getMethodName()
                    + "(" + s.getFileName() + ":" + s.getLineNumber() + ")";
        }
        return res;
    }

    public boolean activateLock() {
        FileWriter _lockfile;

        try {
            _lockfile = new FileWriter(new File(_lockfilename));
            _lockfile.close();
            return true;
        } catch (IOException ex) {
            System.err.println(ex.toString());
            return false;
        }
    }

    public boolean checkLock() {
        return new File(_lockfilename).exists();
    }

    public boolean deactivateLock() {
        if (!checkLock()) {
            return true;
        } else {
            new File(_lockfilename).delete();
            return true;
        }
    }

    protected void doPause() {
        String message=_state;
        if (!_debug) {
            return;
        }
        ConsoleAnsi myc = new ConsoleAnsi(getLocalName(), 50, 5, 8);
        myc.println(new TimeHandler().toString() + message);
        myc.waitToClose();
    }

    @Override
    protected void doExit() {
        if (_singleBehaviour) {
            if (!_shutdownRequested) {
                System("Execute body");
            }
            _shuttingDown = true;
            System("Single shutting down");
            doDelete();
        } else {
            if (!_shuttingDown) {
                _shuttingDown = true;
                System("Coordinated shutting down");
                this.flushAllMessageQueues();
                doDelete();
            }
        }
    }

    protected boolean canShutDown() {
        return _shutdownRequested;
    }

    @Override
    protected boolean canExit() {
        return _exitRequested; // && _splitQueue.isEmpty("ADMIN") && _splitQueue.isEmpty("XUI");
    }

    @Override
    protected void doBlock() {

        if (((myIntegratedBehaviour) _currentBehaviour)._showdetails) {
            System("Behaviour " + _currentBehaviour.getBehaviourName() + " blocks");
        }
        this._currentBehaviour.block();
    }

    @Override
    protected void doBlock(int milis) {

        if (((myIntegratedBehaviour) _currentBehaviour)._showdetails) {
            System("Behaviour " + _currentBehaviour.getBehaviourName() + " blocks for " + milis + " ms");
        }
        this._currentBehaviour.block(milis);
    }

    @Override
    protected void doSleep(int milis) {
        doBlock(milis);
    }

    //
    // ACLMessaging wrappers
    //
    protected void reportFailure(String cause) {
        Info("Reporting FAILURE " + cause);
        ACLMessage msg = new ACLMessage();
        msg.setSender(getAID());
        msg.addReceiver(new AID(_analyticsAgent, AID.ISLOCALNAME));
        msg.setProtocol("ANALYTICS");
        msg.setContent(cause);
        msg.setPerformative(ACLMessage.FAILURE);
        this.send(msg);
    }

    // Wrappers for single queue
    protected void sendServer(ACLMessage msg) {
        System(new JsonObject().add("info", "acl_send_REGULAR").add("value", toJsonACLM(msg)));
        this.send(msg);
        String receiver = ((AID) (msg.getAllReceiver().next())).getLocalName();
        if (!receiver.equals(this._analyticsAgent)) {
            this.reportFailure("THe receiver of of the message should be " + _analyticsAgent + " but it acutally is " + receiver);
        }
    }

    protected ACLMessage receiveServer() {
        boolean exit = false;
        ACLMessage res = null;
        while (!exit) {
            res = blockingReceive();
            if (res.getSender().getLocalName().equals(this._analyticsAgent)) {
//                System(new JsonObject().add("info", "acl_queue_REGULAR").add("value", toJsonACLM(res)));
                System(new JsonObject().add("info", "acl_receive_REGULAR").add("value", toJsonACLM(res)));
                exit = true;
            }
        }
        return res;
    }

    // Wrappers for SplitQueues
    protected ACLMessage pullACLM(String protocol) {
        ACLMessage res = null;
//        System("Waiting for incoming messages at protocol "+protocol);
        if (_splitQueue.getQueue(protocol) != null && !_splitQueue.isEmpty(protocol)) {
            res = _splitQueue.Pop(protocol);
        }
        // If protocol is ANALYTICS and it is a student' agent, then do not report
        if (res != null && (_myCardID == null || !protocol.equals("ANALYTICS"))) {
            System(new JsonObject().add("info", "acl_receive_" + protocol).add("value", toJsonACLM(res)));
        }
        return res;
    }

    protected ACLMessage pullACLM() {
        return pullACLM("REGULAR");
    }

    protected void pushACLM(ACLMessage out) {
        String protocol = (out.getProtocol() == null ? "REGULAR" : out.getProtocol());
        out.setProtocol(protocol);
        if (_myCardID != null && _myCardID.isValid()) {
            out.setEncoding(_myCardID.getShortID());
        }
        if (_myCardID == null || !protocol.equals("ANALYTICS")) {
            System(new JsonObject().add("info", "acl_send_" + (out.getProtocol() == null ? "REGULAR" : out.getProtocol())).add("value", toJsonACLM(out)));
        }
        this.send(out);
    }

    protected void flushAllMessageQueues() {
        int maxrepetitions = 25;
        System("Flushing mail server");

        if (_splitQueue != null) {
            while (!_splitQueue.isAllEmpty() && maxrepetitions > 0) {
                System("Flushing mail server countdown " + maxrepetitions);
                _mailServer.forceDispatch();
                maxrepetitions--;
            }
            System("Mailing report: " + this._splitQueue.fancyStatus());
        }
    }

    protected void doBanSenderAgent(String name, int milis) {
        Info("Banning out " + name + " for " + milis + " miliseconds");
        this._mailServer.banSenderAgent(name, milis);
    }

    //
    // Console output
    //
    protected void Error(String message) {
        qualifyBehaviour();
        _dlogger.LogError(message);
    }

    protected void Error(JsonObject o) {
        qualifyBehaviour();
        _dlogger.LogError(o);
    }

    protected void Info(String message) {
        qualifyBehaviour();
        _dlogger.LogMessage(message);
    }

    protected void Info(JsonObject o) {
        qualifyBehaviour();
        _dlogger.LogMessage(o);
    }

    protected void System(String message) {
        setState(message);
        if (_system) { //_config.getBoolean("system", false)) {
            qualifyBehaviour();
            _dlogger.LogMessage(message);
        }
    }

    protected void System(JsonObject o) {
        //setState(o.toString());
        if (_system) { //_config.getBoolean("system", false)) {
            qualifyBehaviour();
            _dlogger.LogMessage(o);
        }
    }

    protected void Exception(Exception ex) {

//        qualifyBehaviour();
        _dlogger.logException(ex);

    }

    protected void setState(String s) {
        _state = s;
        _stateDate = new TimeHandler().toString();
        if (this._currentBehaviour != null) {
            ((myBehaviour) this._currentBehaviour).setBehaviourStatus(s);
        }
    }

    protected void addState(String s) {
        setState(_state + "-" + s);
    }

    @Override
    protected void plainExecute() {
    }

    //
    // Connect to course infrastructure
    //
    protected void checkIdentityManager() {
        if (_identitymanager.equals("")) {
            Exception(new Exception("Trying to access Identity Manager services but variable _identitymanager is not intialilized. Closing session"));
            abortSession();
        }
    }

    protected String whoAmI() {
        String res = "";

        checkIdentityManager();
        try {
            ACLMessage msg = new ACLMessage();
            msg.setSender(getAID());
            msg.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
            msg.setProtocol("ANALYTICS");
            msg.setPerformative(ACLMessage.QUERY_IF);
            msg.setEncoding(_myCardID.getCardID());
            msg.setContent("");
            this.send(msg);
            ACLMessage answer = blockingReceive(2000);
            if (answer == null || answer.getSender().getLocalName().equals("ams")) {
                res = "ERROR Agent IdentityManager does not answer, please ask the teacher";
            } else {
                res = getDetailsLARVA(answer);
//                if (answer.getPerformative() == ACLMessage.INFORM) {
//                    if (ACLMessageTools.ACLMessageTools.isJsonACLM(msg)) {
//                        res = Json.parse(answer.getContent()).asObject().getString("details", "Unable to find the owner of this card");
//                    } else {
//                        res = answer.getContent();
//                    }
//                } else {
//                    if (ACLMessageTools.ACLMessageTools.isJsonACLM(msg)) {
//                        res = Json.parse(answer.getContent()).asObject().getString("details", "Unable to find the owner of this card");
//                    } else {
//                        res = answer.getContent();
//                    }
//                }
            }
        } catch (Exception Ex) {
            Exception(Ex);
        }
        return res;
    }

    // At the end of life of agents
    protected boolean doCheckoutPlatform() {
        boolean res = true;
        String monitoragent;

        checkIdentityManager();
        try {
            Info("Trying to checkout from the platform");
            ACLMessage msg = new ACLMessage();
            msg.setSender(getAID());
            msg.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
            msg.setProtocol("ANALYTICS");
            msg.setPerformative(ACLMessage.CANCEL);
            this.send(msg);
            Info("Checked-out from the platform");
        } catch (Exception Ex) {
            Exception(Ex);
        }
        return res;
    }

    // At the end of setup for student's agents
    protected boolean doCheckinPlatform() {
        boolean res = false;
        String monitoragent;
        ACLMessage msg = new ACLMessage();

        checkIdentityManager();
        try {
            Info("Trying to checkin in the platform");
            if (_myCardID == null || !_myCardID.isValid()) {
                Error("Missing CardID\n" + "Please place your private CardID into the following folder\n"
                        + "<NetBeans Project>\n"
                        + "   |\n"
                        + "   +-cardID/\n"
                        + "      |\n"
                        + "      +-" + defText(lightblue) + "NAME_SURNAME.cardID\n");
                abortSession();
            } else {
                Info("Requesting checkin to " + _identitymanager);
                msg = new ACLMessage();
                msg.setSender(getAID());
                msg.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
                msg.setProtocol("ANALYTICS");
                msg.setContent("");
                msg.setEncoding(_myCardID.getCardID());
                msg.setPerformative(ACLMessage.SUBSCRIBE);
                this.send(msg);
                ACLMessage confirm = this.blockingReceive(2000);
                if (confirm != null) {
                    if (confirm.getPerformative() == ACLMessage.CONFIRM
                            || confirm.getPerformative() == ACLMessage.INFORM) {
                        Info("Chekin confirmed in the platform");
                        return true;
                    }
                }
                Error("Agent " + _identitymanager + " does not answer");
                abortSession();
            }
        } catch (Exception Ex) {
            Exception(Ex);
            Error("Couldn't confirm checkin in the server");
            abortSession();
        }
        return res;
    }

    //
    // Connect to analtics infrastructure
    //
    protected boolean doCheckoutLARVA() {
        boolean res = true;
        Info("Requesting unregistration in LARVA");
        ACLMessage msg = new ACLMessage();
        msg.setSender(getAID());
        msg.addReceiver(new AID(_analyticsAgent, AID.ISLOCALNAME));
        msg.setProtocol("ANALYTICS");
        msg.setContent("");
        msg.setPerformative(ACLMessage.CANCEL);
        this.send(msg);
        int count = 0, maxcount = 3;
        boolean exit = false;
        while (!exit) {
            ACLMessage confirm = this.blockingReceive(2000);
//            if (confirm != null && confirm.getSender().getLocalName().equals("ams")) {
//                confirm = this.blockingReceive(2000);
//            }
            if (confirm != null) {
                if (confirm.getSender().getLocalName().equals((this._analyticsAgent))
                        && confirm.getPerformative() == ACLMessage.CONFIRM) {
                    Info("Succesfully unregistered in LARVA");
                    exit = true;
                    res = true;
                } else {
                    Info("Unexpected message received " + confirm.toString());
                }
            }
            count++;

            exit = (count >= maxcount);
        }
        if (!res) {
            Error("Couldn't confirm unregistration in LARVA");
        }
        return res;
    }

    protected boolean doCheckinLARVA() {
        boolean res = false;
        String whatid;
        try {
            Info("Trying to register in LARVA");
            if (_config.get("analytics").asObject().get("play") != null) {
                whatid = "PLAY " + _config.get("analytics").asObject().get("play").asString();
                Info("Found  " + whatid);
                _analyticsAgent = this.doQueryLARVAService(whatid);
            } else if (_config.get("analytics").asObject().get("group") != null) {
                whatid = "GROUP " + _config.get("analytics").asObject().get("group").asString();
                Info("Found " + whatid);
                _analyticsAgent = this.doQueryLARVAService("Analytics " + whatid);
            } else if (_config.get("analytics").asObject().get("problemid") != null) {
                whatid = "PROBLEMID " + _config.get("analytics").asObject().get("problemid").asInt();
                Info("Found " + whatid);
                _analyticsAgent = this.doQueryLARVAService("Analytics " + whatid);
            } else {
                whatid = "ASSIGNMENTID " + _config.get("analytics").asObject().get("assignmentid").asInt();
                Info("Found " + whatid);
                _analyticsAgent = this.doQueryLARVAService("Analytics " + whatid);

            }

            if (!_analyticsAgent.equals("")) {
                Info("LARVA Agent " + _analyticsAgent + " found. Requesting registration");
                ACLMessage msg = new ACLMessage();
                msg.setSender(getAID());
                msg.addReceiver(new AID(_analyticsAgent, AID.ISLOCALNAME));
                msg.setProtocol("ANALYTICS");
                msg.setContent("");
                msg.setPerformative(ACLMessage.SUBSCRIBE);
                this.send(msg);
                ACLMessage confirm = this.blockingReceive(2000);
                if (confirm != null) {
                    if (confirm.getPerformative() == ACLMessage.CONFIRM) {
                        Info(defText(lightblue) + "Succesfully registered in LARVA");
                        return true;
                    }
                }
                Error("Couldn't confirm registration in LARVA. Reason: " + confirm.getContent());
                abortSession();
                return true;
            } else {
                Error("ABORT! NO LARVA Agent found for this assignment. Please ask the teacher.");
                abortSession();
            }
        } catch (Exception Ex) {
            Error("ABORT! Analytics configuration is missing or wrong \n" + defText(gray) + "Please locate the config file into the following folder\n"
                    + "<NetBeans Project>\n"
                    + "   |\n"
                    + "   +-config/\n"
                    + "      |\n"
                    + "      +-default.json\n"
                    + "and add the following section \"analytics\": {\"assignmentid\": XXX}\n"
                    + "where XXX is the integer number that follows the title of each assignment in LARVA Web page");
            abortSession();
        }
        return false;
    }

    protected String whoLarvaAgent() {
        return _analyticsAgent;
    }

    protected final String doQueryLARVAService(String service) {
        checkIdentityManager();
        ACLMessage msg = new ACLMessage();
        msg.setSender(getAID());
        msg.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        msg.setProtocol("ANALYTICS");
        msg.setContent(service.toUpperCase());
        msg.setPerformative(ACLMessage.QUERY_REF);
        Info("Retrieving the name of the corresponding LARVA agent for service <" + service + ">");
        this.send(msg);
        ACLMessage confirm = this.blockingReceive(2000);
        if (confirm == null) {
            Error("Agent " + _identitymanager + " does not answer");
            abortSession();
        }
        return confirm.getContent();
    }

    @Override
    public String defReportType() {
        return "behaviours";
    }

    @Override
    public String[] defReportableObjectList() {
        String res[] = new String[_myBehavioursList.size()];
        int i = 0;
        for (myBehaviour mb : this.getAllBehaviours()) {
            res[i++] = mb.getBehaviourName();
        }
        return res;
    }

    @Override
    public String reportObjectDate(String objectid) {
        return this.findMyBehaviourByName(objectid).getBehaviourLastRun();
    }

    @Override
    public String reportObjectStatus(String objectid) {
        return this.findMyBehaviourByName(objectid).getBehaviourStatus();
    }

    @Override
    public String reportObjectValue(String objectid) {
        return this.findMyBehaviourByName(objectid).getBehaviourCount() + "";
    }

    //
    // Custom behaviours
    //
    public abstract class myIntegratedBehaviour extends myBehaviour {

        protected boolean _showdetails = false;

        public myIntegratedBehaviour(Agent a, String name, boolean show) {
            super(a, name, true);
            _showdetails = show;

        }

        public myIntegratedBehaviour(Agent a, String name) {
            super(a, name);
            _showdetails = false;

        }

        @Override
        public void onStart() {
            if (_showdetails
                    || (this._count == 0 && this.getBehaviourName().equals("REGULAR") && _myCardID != null)) {
                System("Behaviour " + this.getBehaviourName() + " starts");
            }

        }

        @Override
        public void myBehaviourStart() {
            super.myBehaviourStart();

            if (_showdetails) {
                System("Behaviour " + this.getBehaviourName() + " awakes");
            }
        }

        @Override
        public int myBehaviourEnd() {
            super.myBehaviourEnd();
            if (_showdetails) {
                System("Behaviour " + this.getBehaviourName() + " sleeps");
                if (this.getBehaviourName().equals("MAIL")) {
                    System(_splitQueue.fancyStatus());
                }
            }
            return 0;
        }

        @Override
        public int onEnd() {
            if (_showdetails) {
                System("Behaviour " + this.getBehaviourName() + " ends");
            }
            return 0;
        }

        @Override
        public boolean done() {
//            if (_config.getBoolean("singlebehaviour", false)) {
//                return canExit();
//            } else {
//                return canShutDown();
//            }
            return canExit();
        }
    }

    public class plainIntegratedBehaviour extends myIntegratedBehaviour {

        public plainIntegratedBehaviour(Agent a, String name, boolean show) {
            super(a, name, show);

        }

        public void myBehaviourBody() {
            wrapperExecute();
        }
//        public void myBehaviourBody() {
//            if (!_exitRequested && !_shutdownRequested) {
//                plainExecute();
//            }
//            if (_exitRequested|| _shutdownRequested) {
//                doExit();
//            }
//
//        }

//        @Override
//        public boolean done() {
//            return canExit();
//        }
    }

    public class myPostOffice extends myIntegratedBehaviour {

        ArrayList<String> bannedAgents;

        public myPostOffice(Agent a, String name, boolean show) {
            super(a, name, show);
            bannedAgents = new ArrayList();
        }

        @Override
        public void myBehaviourBody() {
            ACLMessage inbox = receive();
            if (inbox != null) {
                String sender = inbox.getSender().getLocalName();
                if ((!_ignoreAMS || !inbox.getSender().getLocalName().equals("ams")) && !isbannedSenderAgent(sender)) {
                    System(new JsonObject().add("info", "acl_queue_" + (inbox.getProtocol() == null ? "REGULAR" : inbox.getProtocol())).add("value", toJsonACLM(inbox)));
                    _splitQueue.Push(inbox);
//                System("Mailing report: " + _splitQueue.fancyStatus());
                } else {
                    return;
                }
            } else {
                if (_splitQueue.isAllEmpty()) {
                    doBlock();
                } else {
                    this.forceDispatch();
                }
            }
        }

        @Override
        public boolean done() {
            return canShutDown();
        }

        public boolean isEmpty() {
            return _splitQueue.isAllEmpty();
        }

        public void forceDispatch() {
            myBehaviour mb;

            for (String protocol : _splitQueue.getKeySet()) {
                mb = _splitQueue.getService(protocol);
                if (mb != null) {
                    mb.forceaction();
                }
            }
        }

        public boolean isbannedSenderAgent(String name) {
            return bannedAgents.contains(name);
        }

        public void unbanSenderAgent(String name) {
            if (isbannedSenderAgent(name)) {
                bannedAgents.remove(name);
            }

        }

        public void banSenderAgent(String name, int milis) {
            if (!isbannedSenderAgent(name)) {
                bannedAgents.add(name);
                this.myAgent.addBehaviour(new WakerBehaviour(this.myAgent, milis) {
                    @Override
                    public void handleElapsedTimeout() {
                        unbanSenderAgent(name);
                    }
                });

            }
        }
    }

    public class myServiceHandler extends myIntegratedBehaviour {

        public myServiceHandler(Agent a, String name, boolean show) {
            super(a, name, show);
            _splitQueue.addList(name, this);
            if (_showdetails) {
                System("Adding listener to protocol " + name);
            }
            this.suspendBehaviour();
        }

        @Override
        public void myBehaviourBody() {
            ACLMessage inbox = pullACLM(this.getBehaviourName());
            if (inbox != null) {
                myServiceBody(inbox);
                suspendBehaviour();
            } else {
                doBlock();
            }

        }

        public void myServiceBody(ACLMessage msg) {
        }

        @Override
        public boolean done() {
            if (canExit() || canShutDown()) {
                doExit();
                return true;
            }
            return false;
        }

    }

    public class plainServiceHandler extends myServiceHandler {

        public plainServiceHandler(Agent a, boolean show) {
            super(a, "REGULAR", show);
            this.allowBehaviour();

        }

        @Override
        public void myBehaviourBody() {
            wrapperExecute();
//            suspendBehaviour();
            allowBehaviour();
        }
    }

    //
    // Behaviour's keeping
    //
    protected myBehaviour findMyBehaviourByName(String queuename) {
        plainBehaviour defaul = null;
        for (myBehaviour mb : this._myBehavioursList) {
            if (mb.getBehaviourName().equals(queuename)) {
                return mb;
            }
        }
        return defaul;
    }

    protected void removeMyBehaviour(String name) {
        myBehaviour mb = this.findMyBehaviourByName(name);
        if (mb != null) {
            System("Removing behaviour " + name);
            this.removeBehaviour(mb);
            this._myBehavioursList.remove(mb);
        }
    }

    //
    // Reporting tasks
    //
    public String reportPrettyPrint(JsonObject jsonreport) {
        String res = "";
        try {
            String type = jsonreport.getString("report", "NULL");
            if (!type.equals("NULL")) {
                res += "REPORTING " + type + "\n";
                for (JsonValue jsvitem : jsonreport.get(type).asArray()) {
                    JsonObject jsitem = jsvitem.asObject();
                    res += "" + defText(lightgreen) + jsitem.getString("objectid", "NULL") + defText(white) + "\n";
                    for (Member m : jsitem) {
                        if (!m.getName().equals("objectid")) {
                            res += "\t" + m.getName() + " = " + m.getValue().toString() + "\n";
                        }
                    }
                }
            } else {
                res = jsonreport.toString(WriterConfig.PRETTY_PRINT);
            }
        } catch (Exception ex) {
            Exception(ex);
            Error("Unable to pretty-print report " + jsonreport.toString());
        }
        return res;
    }

//    public JsonObject reportStatus() {
//        JsonObject res = new JsonObject().add("report", "agentstatus");
//        JsonArray list = new JsonArray();
//        return res.add("agentstatus", list);
//    }
//    protected JsonObject reportAllBehaviours() {
//        JsonObject res = new JsonObject();
//        JsonArray list = new JsonArray();
//        for (myBehaviour b : this.getAllBehaviours()) {
//            list.add(new JsonObject().
//                    add("objectid", b.getBehaviourName()).
//                    add("last", b.getBehaviourLastRun()).
//                    add("count", b.getBehaviourCount()));
//        }
//        return res.add("report", "behaviours").add("behaviours", list);
//    }
//
//    protected JsonObject reportAllMailQueues() {
//        return this._splitQueue.reportStatus();
//    }
//    protected JsonObject doReport(ReportableObject object) {
//        JsonObject res= new JsonObject();
//        JsonArray objectlist = new JsonArray();
//        
//        res.add("report",object.defReportType());
//        for (String id : object.defReportableObjectList()) {
//            objectlist.add(new JsonObject().add("objectid", id). 
//                    add("date", object.reportObjectDate(id)). 
//                    add("status", object.reportObjectStatus(id)));
//        }
//        res.add(object.defReportType(),objectlist);
//        return res;        
//    }
    protected void qualifyBehaviour() {
        if (_currentBehaviour != null && _config.getBoolean("showbehaviour", false)) {
            _dlogger.setOwnerQualifier(_currentBehaviour.getBehaviourName());
        }
//        else {
//            if (_config != null && _config.getBoolean("showbehaviour", false)) {
//                _dlogger.setOwnerQualifier("");
//            } else {
//                _dlogger.setOwnerQualifier("");
//            }
//        }
    }

}
