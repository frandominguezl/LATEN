package AdminAgent;

import static ACLMessageTools.ACLMessageTools.respondTo;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import AdminKeys.AdminCardID;
import AdminKeys.AdminCryptor;
import AdminKeys.DBACoin;
import static AdminReport.Reports.getObject;
import Analytics.AnalyticsProject;
import ConsoleAnsi.EnhancedTextOutput;
import ConsoleAnsi.EnhancedTextOutput.outputTo;
import Database.AgentDataBase;
import static Database.AgentDataBase.BADRECORD;
import Database.JsonResult;
import static FileUtils.FileUtils.listFiles;
import IntegratedAgent.IntegratedAgent;
import Locker.Locker;
import static PublicKeys.KeyGen.getKey;
import Session.Session;
import TelegramStuff.Emojis;
import TimeHandler.TimeHandler;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.core.MicroRuntime;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.wrapper.AgentController;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author lcv
 */
public abstract class AdminAgent extends IntegratedAgent {

    public static final int ROOTUSERID = 7047;

    public static enum ROOTTELEGRAM {
        EXCEPTION, INFO, SETUP, TAKEDOWN, ERROR, HEARTBEAT
    };

    protected String _safeKey = "cardIDcryptor202", _safeName = "";
    protected AdminCardID _adminCardID;
    protected AdminCryptor _enigma;
    protected AgentDataBase _dataBase;
    protected AnalyticsProject _analytics;
    protected HashMap<Integer, Session> sessionListbyUser;
    protected HashMap<String, Session> sessionListbyKey;
    protected HashMap<String, Integer> whoRepresent;
    protected boolean _emergencymode;
    protected boolean _notifyback = false;
    protected boolean _stealthmode = false;
    protected HashMap<String, Locker> _locker;
    protected ArrayList<String> publicnotifierlist, teachertelegram;
    protected String lasttelegramwarning = "";
    protected int nlasttelegramwarning = 0;
    protected HashMap<String, Integer> lasterrors, nerrors;
    protected final int MAXCONSECUTIVEERRORS = 10;
    protected int myagentID;

    public AdminAgent() {
        super();
        _notifyback = false;
        _locker = new HashMap();

    }

    //
    // Agent lyfecycle
    //
    @Override
    public void setup() {
        // Inherits configuration
        super.setup();

        // Define crypto settings
        _enigma = new AdminCryptor(_safeKey);
        _safeName = _enigma.enCryptNew(getLocalName());
        if (_myCardID != null) {
            _adminCardID = new AdminCardID(_safeKey);
            _adminCardID.setData(_myCardID.getData());
            _adminCardID.decodeCardIDNew();
            System("Decoded ID: " + _adminCardID.toString());
        }

        // Configure database connection
        if (this._config.get("dbconnection") != null) {
            System("Found database connection");
            _dataBase = new AgentDataBase();
            try {
                String user = _config.get("dbconnection").asObject().get("user").asString();
                String password = _config.get("dbconnection").asObject().get("password").asString();
                int port = _config.get("dbconnection").asObject().get("port").asInt();
                String database = _config.get("dbconnection").asObject().get("database").asString();
                String host = _config.get("dbconnection").asObject().get("host").asString();
                _dataBase.openConnection(host, port, database, user, password);
                System("Connected to DataBase " + _dataBase.getURL());
                long autovalidate = 3600000;
                System("Adding periodic validation query every " + autovalidate / (60000) + " mins");
                addBehaviour(new TickerBehaviour(this, autovalidate) {
                    @Override
                    protected void onTick() {
                        doPeriodicCheck();
                    }
                });
            } catch (Exception ex) {
                Error("Unable to connect to DataBase " + _dataBase.getURL());
                _dataBase.reportException(ex);
            }
        }
        // Auto sets IdentityManager
        

        // Setup dedicated ACLM channels
        if (!this._singleBehaviour) {
            addBehaviour(new plainServiceHandler(this, true));

            addBehaviour(
                    new myServiceHandler(this, "ADMIN", _config.getBoolean("showbehaviour", false)) {
                @Override
                public void myServiceBody(ACLMessage msg) {
                    ServiceAdmin(msg);
                }
            }
            );
            _locker.put("ADMIN", new Locker());

            addBehaviour(
                    new myServiceHandler(this, "XUI", _config.getBoolean("showbehaviour", false)) {
                @Override
                public void myServiceBody(ACLMessage msg) {
                    ServiceXUI(msg);
                }
            }
            );
            _locker.put("XUI", new Locker());

            addBehaviour(
                    new myServiceHandler(this, "ANALYTICS", _config.getBoolean("showbehaviour", false)) {
                @Override
                public void myServiceBody(ACLMessage msg) {
                    ServiceAnalytics(msg);
                }
            }
            );
            _locker.put("ANALYTICS", new Locker());
        }

        // Register services
//        doServiceDF(DFOPERATION.DEREGISTER, "", "");
//        doServiceDF(DFOPERATION.REGISTER, "", "ADMIN");
        //this.DBCheckInAgent(getAID());
        //this.DBaddService(getAID(), "ADMIN");

        // Analytics, if any
        /*String hackathon = (this._config.get("analytics") != null
                ? this._config.get("analytics").asObject().getString("play", "") : ""),
                groupalias = (this._config.get("analytics") != null
                ? this._config.get("analytics").asObject().getString("group", "") : "");
        int assignmentID = (this._config.get("analytics") != null
                ? this._config.get("analytics").asObject().getInt("assignmentid", BADRECORD) : BADRECORD),
                groupID = (this._config.get("analytics") != null
                ? this._config.get("analytics").asObject().getInt("groupid", BADRECORD) : BADRECORD);
        if (!groupalias.equals("")) {
            groupID = this.DBgetGroup(groupalias);
        }*/

        /*if (assignmentID >= 0) {
            _analytics = new AnalyticsProject(_dataBase, assignmentID, groupID);
//            this.doServiceDF(DFOPERATION.REGISTER, "", "Analytics assignmentid " + assignmentID);
            this.DBaddService(getAID(), "Analytics assignmentid " + assignmentID);
            if (!this.DBisIndividual(assignmentID)) {
//                this.doServiceDF(DFOPERATION.REGISTER, "", "Analytics groupID " + groupID);
                if (groupID >= 0) {
                    this.DBaddService(getAID(), "Analytics groupid " + groupID);
                }
                if (!groupalias.equals("")) {
                    this.DBaddService(getAID(), "Analytics group " + groupalias);
                }
            } else {
                if (!hackathon.equals("")) {
                    this.DBaddService(getAID(), "Play " + hackathon);
                }

            }
        }*/
        // Set up listener to notifications
        publicnotifierlist = new ArrayList();
        /*ArrayList<String> notifaux = this.DBgetAllServiceProviders("NOTIFIER");
        for (String name : notifaux) {
            if (this.isConnected(name)) {
                publicnotifierlist.add(name);
            }
        }

        notifaux = this.DBgetAllServiceProviders("TEACHER TELEGRAM");
        teachertelegram = new ArrayList();
        for (String name : notifaux) {
            if (this.isConnected(name)) {
                teachertelegram.add(name);
            }
        }*/

        // Initializes Lab Sessions,
        sessionListbyUser = new HashMap<>();
        sessionListbyKey = new HashMap<>();
        whoRepresent = new HashMap();

        // Accounting of errors to ban an agent
        lasterrors = new HashMap();
        nerrors = new HashMap();

        // Control of exit
        _exitRequested = false;
        _emergencymode = false;
        this.fullreport.addReportable(this._dataBase);
        //myagentID = this.DBgetAgentID(getAID());
        //this.TeacherTelegramNotification(ROOTTELEGRAM.SETUP, "");
        //this.GeneralTelegramNotification(Emojis.GREENCIRCLE + " " + getLocalName() + " is up and running");
    }

    @Override
    protected void takeDown() {
        //this.TeacherTelegramNotification(ROOTTELEGRAM.TAKEDOWN, "");
        this.GeneralTelegramNotification(Emojis.REDCIRCLE + " " + getLocalName() + " is going down");
        super.takeDown();
    }

    @Override
    protected void Exception(Exception ex) {
        super.Exception(ex);
        try {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            this.TeacherTelegramNotification(ROOTTELEGRAM.EXCEPTION, "Unexpected exception\n" + ex.toString() + "\n" + errors.toString());
        } catch (Exception Ex) {
        }
    }

    protected void Exception(String info, Exception ex) {
        super.Exception(ex);
        try {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            if (info.equals("")) {
                this.TeacherTelegramNotification(ROOTTELEGRAM.EXCEPTION, "Unexpected exception\n" + ex.toString() + "\n" + errors.toString());
            } else {
                this.TeacherTelegramNotification(ROOTTELEGRAM.EXCEPTION, "Exception under control\nDue to " + info);
            }
        } catch (Exception Ex) {
        }
    }

    @Override
    protected void Error(String message) {
        super.Error(message);
        this.TeacherTelegramNotification(ROOTTELEGRAM.ERROR, message);
    }

    @Override
    protected void Error(JsonObject o) {
        super.Error(o);
        this.TeacherTelegramNotification(ROOTTELEGRAM.ERROR, o.toString());
    }

//    @Override
//    protected void doExit() {
//        System("Execution is over");
//        // Report status of behaviour list
//        System(reportAllBehaviours());
//        // Close DB connection
//        if (_dataBase != null) {
//            System("Closing connection to DataBase " + _dataBase.getURL());
//            _dataBase.closeConnection();
//        }
//        // Empty message queues
////        this.flushAllMessageQueues();
//        // Deregister services
//        doServiceDF(DFOPERATION.DEREGISTER, "", "");
////        doServiceDF(DFOPERATION.DEREGISTER, "", "XUI");
//
//        doDelete();
//    }
    @Override
    protected void doExit() {

        if (!_shuttingDown) {
            _shuttingDown = true;
            System("Execution is over");
            System("Coordinated shutting down");
            this.flushAllMessageQueues();
            // Close DB connection
            if (_dataBase != null) {
                System("Closing connection to DataBase " + _dataBase.getURL());
                _dataBase.closeConnection();
            }                 // Deregister services
//            doServiceDF(DFOPERATION.DEREGISTER, "", "");
            this.DBCheckOutAgent(getAID());
//            this.DBremoveService(getAID(), "");
            doDelete();
        }
    }

    public void doShutDown() {
        System("Shutdown requested");
        _exitRequested = true;
        doExit();
    }

    // Try to recover from an uncaught exception
    public void doEmergencyMode(Exception ex) {
        _emergencymode = true;
        Exception(ex);
        System("Going into emergency & recovery mode due to uncaught exception ");
    }
//    @Override
//    protected boolean canExit() {
//        return _exitRequested; // && _postOffice.isEmpty("ADMIN") && _postOffice.isEmpty("XUI");
//    }
    //
    // Messaging 
    //

    protected String notifyError(ACLMessage msg, int performative, String details) {
        return notifyError(msg, performative, new JsonObject().add("result", "error").
                add("details", details));
    }

    protected String notifyError(ACLMessage msg, int performative, JsonObject details) {
        ACLMessage outgoing = msg.createReply(); //respondTo(msg);
        Session s = getAnalyticsSession(msg);
        String receiver = msg.getSender().getLocalName();
        if (s != null) {
            outgoing.setConversationId(s.getKey());
            s.HACKreply = "REPLY#" + DBACoin.getRootKey(6);
            outgoing.setReplyWith(s.HACKreply);
        }
        outgoing.setPerformative(performative);
        String res = details.toString(); //details.add("aclmessage",fancyWriteACLM(msg)).toString();
        outgoing.setContent(res);
        if (this.lasterrors.get(receiver) == null) {
            this.lasterrors.put(receiver, performative);
            this.nerrors.put(receiver, 0);
        } else {
            if (lasterrors.get(receiver) == performative) {
                nerrors.put(receiver, nerrors.get(receiver) + 1);
            } else {
                lasterrors.put(receiver, performative);
                nerrors.put(receiver, 0);
            }
        }
        if (nerrors.get(receiver) >= this.MAXCONSECUTIVEERRORS) {
            this.doBanSenderAgent(receiver, 60000);
        } else {
            this.pushACLM(outgoing);
            Error(" " + ACLMessage.getPerformative(performative) + " " + res);
        }
        //this.DBUpdateAnalyticsNotification(s, Emojis.WARNING + "*|" + getLocalName() + "|" + s.whoName + "|" + s.getKey() + "|*\n" + res, false);
        return res;
    }

    protected String notifySuccess(ACLMessage msg, int performative, String details) {
        return notifySuccess(msg, performative, new JsonObject().add("result", "ok").
                add("details", details));
    }

    protected String notifySuccess(ACLMessage msg, int performative, JsonObject details) {
        ACLMessage outgoing = msg.createReply(); //respondTo(msg);
        Session s = getAnalyticsSession(msg);
        if (s != null) {
            outgoing.setConversationId(s.getKey());
            s.HACKreply = "REPLY#" + DBACoin.getRootKey(6);
            outgoing.setReplyWith(s.HACKreply);
        }
        outgoing.setPerformative(performative);
        outgoing.setContent(details.toString());
        this.pushACLM(outgoing);
//        System("Answer to " + ACLMessage.getPerformative(msg.getPerformative()) + " from " + msg.getSender().getLocalName() + "::" + details.toString());
        return details.toString();
    }

//    protected boolean DBvalidateMessage(ACLMessage msg) {
//        boolean res = true;
//        int userID;
//        String sender, reply;
//        Session currentSession;
//
//        // Check Sender
//        if (msg.getSender() == null) {
//            reply = "Missing sender in message " + msg.getContent();
////            DBnotifyGeneral(msg, reply);
//            return false;
//        }
//        sender = msg.getSender().getLocalName();
//        // Check user asociated to sender
//        userID = _dataBase.getColumnInt("Agents", "userID", "name", sender);
//        if (userID < 0 && msg.getPerformative() != ACLMessage.SUBSCRIBE) {
//            reply = "Agent " + sender + " is not associated to any user ";
////            DBnotifyGeneral(msg, reply);
//            return false;
//        }
//        if (_analytics == null) {
//            return true;
//        }
//        // Cheeck course-related issues
//        if (!DBisUserRegistered(userID, this._analytics._courseID)) {
//            reply = "User " + userID + " is not registered at course " + _analytics._courseID;
////            DBnotifyGeneral(msg, reply);
//            return false;
//        }
//        currentSession = sessionListbyUser.get(userID);
//        // Check checkin
//        if (currentSession == null && msg.getPerformative() != ACLMessage.SUBSCRIBE) {
//            reply = "Agent " + sender + " has not correctly checked in  ";
////            DBnotifyGeneral(msg, "Agent " + sender + " has not correctly checked in  ");
//            return false;
//        }
//        // Check general ACLM Fields
//        if (msg.getContent() == null) {
//            reply = "The content of the message from " + sender + " should not be empty ";
//            this.DBnotifyAnalytics(msg, "ERROR", reply);
//            return false;
//        }
//        if (msg.getContent().length() > 0 && msg.getContent().charAt(0) == '{' && !isJsonACLM(msg)) {
//            reply = "JSON corrupted in message from " + sender;
//            this.DBnotifyAnalytics(msg, "ERROR", reply);
//            return false;
//        }
//        return res;
//    }
    //
    // ADMIN
    //
    public JsonObject reportStatus() {
        JsonObject res = new JsonObject().add("report", "reportset");
        JsonArray list = new JsonArray();
        res.add("agent", getLocalName());
        res.add("date", new TimeHandler().toString());
        return res.merge(this.fullreport.getReport());
    }

    /*
 {"report":"fullreport","agent":"AgentIdentityManager","date":"2020-10-12 12:53:51",
    "reportset":[{"report":"mailqueues","mailqueues":[{"objectid":"REGULAR","date":"","status":"","value":"0"},{"objectid":"ANALYTICS","date":"","status":"","value":"0"},{"objectid":"ADMIN","date":"2020-10-12 12:53:51","status":"(QUERY-REF\n :sender  ( agent-identifier :name Kernel@CAJAR  :addresses (sequence http://numenor:7778/acc ))\n :receiver  (set ( agent-identifier :name AgentIdentityManager@CAJAR ) )\n :encoding  215155191066024130179067060224067016066062196067018067044129147066042  :protocol  ADMIN\n)","value":"0"},{"objectid":"XUI","date":"","status":"","value":"0"}]},{"report":"behaviours","behaviours":[{"objectid":"MAIL","date":"2020-10-12 12:53:51","status":"","value":"4"},{"objectid":"ADMIN","date":"2020-10-12 12:52:46","status":"","value":"3"},{"objectid":"XUI","date":"2020-10-12 12:53:51","status":"","value":"3"},{"objectid":"ANALYTICS","date":"2020-10-12 12:53:51","status":"","value":"3"}]},{"report":"database","database":[{"objectid":"topenxsession","date":"","status":"true","value":""},{"objectid":"exceptions","date":"","status":"","value":""}]}]}    
     */
    public String prettyPrintReport(JsonObject report, String level) {
        String res = "";
        JsonArray list;
        String lastmessage;
        res += "REPORT OF AGENT " + report.getString("agent", "unknown");
        res += "\nRECEIVED AT " + report.getString("date", "unknown") + "\n";
        switch (level.toUpperCase()) {
            case "ADMIN":
                // Status of behaviours
                JsonArray jsrepset = report.get("reportset").asArray(),
                 objects;
                for (JsonValue jsvr : jsrepset) {
                    JsonObject jsreport = jsvr.asObject();
                    res += jsreport.getString("report", "XXX") + " \n";
                    for (JsonValue jsvo : jsreport.get(jsreport.getString("report", "XXX")).asArray()) {
                        JsonObject jso = jsvo.asObject();
                        res += "   " + jso.getString("objectid", "XXX") + " " + jso.getString("status", "XXX") + "\n";
                    }

                }

//                res += "STATUS\n";
//                for (JsonValue jsvmb : Reports.getType(report, "behaviours")) {
//                    String oid = jsvmb.asObject().getString("objectid", "unknown behaviour");
//                    res += String.format("%10s:\t", oid);
//                    JsonObject obj = getObject(report, "behaviours", oid);
//                    res += obj.getString("date", "unknown date") + " - "
//                            + obj.getString("status", "unknown status").substring(0, Math.min(100, obj.getString("status", "").length())) + "\n";
//                }
//                res += "DATABASE\n";
//                res += String.format("%10s:\t%s - %s",
//                        "OPEN",
//                        getObject(report, "database", "open").getString("date", ""),
//                        getObject(report, "database", "open").getString("status", "")) + "\n";
//                res += String.format("%10s:\t%s - %s",
//                        "EXCEPT",
//                        getObject(report, "database", "exceptions").getString("date", ""),
//                        getObject(report, "database", "exceptions").getString("status", "")) + "\n";
//
//                res += "LAST MESSAGE\n";
//                for (JsonValue jsvsq : Reports.getType(report, "mailqueues")) {
//                    String oid = jsvsq.asObject().getString("objectid", "unknown behaviour");
//                    res += String.format("%10s:\t", oid);
//                    lastmessage = getObject(report, "mailqueues", oid).getString("status", "");
//                    if (!lastmessage.equals("")) {
//                        res += getObject(report, "mailqueues", oid).getString("date", "") + " ";
//                        try {
//                            JsonObject jsacl = Json.parse(lastmessage).asObject();
//                            res += "FROM: " + jsacl.getString("sender", "unknown")
//                                    + " CONTENT: " + jsacl.get("content").toString().substring(0, Math.min(100, jsacl.get("content").toString().length())) + "\n";
//                        } catch (Exception ex) {
//                            res += " not available\n";
//                        }
//                    } else {
//                        res += " not available\n";
//                    }
//                }
                break;
            case "PUBLIC":
                res += "STATUS: ";
                if (getObject(report, "behaviours", "REGULAR").size() > 0) {
                    res += getObject(report, "behaviours", "REGULAR").getString("status", "unknown status") + " at "
                            + getObject(report, "behaviours", "REGULAR").getString("date", "unknown date") + "\n";
                    res += "LAST MESSSAGE: ";
                    lastmessage = getObject(report, "mailqueues", "REGULAR").getString("status", "");
                    if (!lastmessage.equals("")) {
                        res += getObject(report, "mailqueues", "REGULAR").getString("date", "") + " ";
                        try {
                            JsonObject jsacl = Json.parse(lastmessage).asObject();
                            res += "FROM: " + jsacl.getString("sender", "unknown")
                                    + " CONTENT: " + jsacl.get("content").toString() + "\n";
                        } catch (Exception ex) {
                            res += " not available";
                        }
                    } else {
                        res += " not available";
                    }
                } else {
                    res += " apparently alive";
                }
                break;
        }
        res += "\n";
        return res;
    }

    protected void ServiceAdmin(ACLMessage adminmessage) {
        ACLMessage reply;
        String res;
        try {
            // Check authority
            if (this.isAdminMessage(adminmessage)) {
                reply = adminmessage.createReply();
                if (adminmessage.getLanguage() != null) {
                    reply.setProtocol(adminmessage.getLanguage());
                }
                int discriminator;
                switch (adminmessage.getPerformative()) {
                    case ACLMessage.QUERY_REF:
                        JsonObject jsscript = Json.parse(adminmessage.getContent()).asObject();
                        String arguments[] = jsscript.getString("arguments", "").split(" ");
                        switch (arguments[0]) {
                            case "status":
                                reply.setContent(new JsonObject()
                                        .add("report", "statusreport")
                                        .add("statusreport", this._state).toString());
                                break;
                            case "details":
                                reply.setContent(new JsonObject()
                                        .add("report", "detailedreport")
                                        .add("detailedreport", this.prettyPrintReport(
                                                this.reportStatus(), (arguments.length > 1 ? arguments[1] : "PUBLIC"))).toString());
                                break;
                            case "services":
                                JsonArray jsservices = new JsonArray();
                                for (String s : this.DBgetAllAgentServices(getAID())) {
                                    jsservices.add(s);
                                }
                                reply.setContent(new JsonObject()
                                        .add("report", "servicesreport")
                                        .add("servicesreport", jsservices).toString());
                                break;
//                            case "products":
//                                qrefProducts(adminmessage);
//                                break;
                        }
                        reply.setPerformative(ACLMessage.INFORM);
                        this.send(reply);
//                        this.pushACLM(reply);
                        break;
                    case ACLMessage.REQUEST:
                        if (adminmessage.getContent().toUpperCase().equals("SHUTDOWN")) {
                            doShutDown();
                        } else if (adminmessage.getContent().toUpperCase().contains("SHELL")) {
                            JsonObject jsreq = Json.parse(adminmessage.getContent()).asObject();
                            String sparse[] = jsreq.getString("arguments", "").replace("shell ", "").split(" "), allowed[] = listFiles("./scripts/", "sh"),
                                    script = sparse[0];
                            for (String s : allowed) {
                                if (s.toUpperCase().equals(script.toUpperCase())) {
                                    sparse[0] = "./scripts/" + sparse[0];
                                    Process ps = new ProcessBuilder(sparse).start();
                                    res = new Scanner(new File("./logs/" + this.getLocalName() + ".out")).useDelimiter("\\Z").next();
                                    reply.setContent(new JsonObject()
                                            .add("report", "shellreport")
                                            .add("shellreport", res).toString());
                                    this.send(reply);
//                                    this.pushACLM(reply);
                                    return;
                                }
                            }
                            res = "Bad script";
                            reply.setPerformative(ACLMessage.INFORM);
                            this.send(reply);
//                            this.pushACLM(reply);
                        } else {
                            Error("Bad Request from " + adminmessage.getSender().getLocalName() + " > " + adminmessage.getContent());
                        }
                        break;
                    case ACLMessage.INFORM:
                        if (_locker.get("ADMIN") != null && _locker.get("ADMIN").isActive()) {
                            _locker.get("ADMIN").addReceivedObject(adminmessage.getSender().getLocalName(), adminmessage);
                        }
                        break;
                    default:
                        Error("Bad or missing performative");
                        break;
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
        }
    }

    public void qrefProducts(ACLMessage msg) {
        notifySuccess(msg, ACLMessage.INFORM, new JsonObject().add("products", new JsonArray()));
        this.TeacherTelegramNotification(ROOTTELEGRAM.ERROR, "UNCODED CALL AT " + this.getMyStackTrace());
    }

    public ACLMessage newAdminACLMessage(String protocol) {
        ACLMessage res = new ACLMessage();
        res.setSender(getAID());
        res.setProtocol((protocol.equals("") ? "ADMIN" : protocol));
        res.setEncoding(_safeName);
        return res;
    }

    public boolean isAdminMessage(ACLMessage msg) {
        boolean res = false;
        String name, protocol, encodedname, inreplyto;
        // Clearance
        try {
//            protocol = msg.getProtocol();
//            inreplyto = msg.getInReplyTo();
            name = msg.getSender().getLocalName();
            encodedname = msg.getEncoding();
            if (name.equals(this._enigma.deCryptNew(encodedname))) {
                return true;
            }
        } catch (Exception Ex) {
            return false;
        }
        return false;
    }

    //
    // API DB
    //
    //
    // Marketplace
    //
    private int baseSerie(String name) {
        int base = 0;
        switch (name) {
            default:
                base += 10;
            case "alive":
                base += 10;
            case "ontarget":
                base += 10;
            case "cargo":
                base += 10;
            case "coins":
                base += 10;
            case "angular":
                base += 10;
            case "distance":
                base += 10;
            case "energy":
                base += 10;
            case "visual":
                base += 10;
            case "lidar":
                base += 10;
            case "elevation":
                base += 10;
            case "thermal":
                base += 10;
            case "angularHQ":
                base += 10;
            case "angularDELUX":
                base += 10;
            case "distanceHQ":
                base += 10;
            case "distanceDELUX":
                base += 10;
            case "thermalHQ":
                base += 10;
            case "thermalDELUX":
                base += 500;
            case "account":
                base = 0;
        }
        return base;
    }

    protected void DBaddProduct(String convID, String category, int ownerID, int serie, int price) {

        convID = convID.replaceAll("[A-Z]+#", "");
        DBACoin mc = new DBACoin();
        mc.setOwner(ownerID);
        mc.setSerie(serie + baseSerie(category));
        mc.setSession(convID);
        String reference = category.toUpperCase() + "#" + mc.encodeCoin();
        this.DBregisterProduct(convID, reference, serie, myagentID, price);
//        
//                System("Generating product "+reference+)
    }

    protected int DBgetMyProductPrice(String convID, String reference) {
        convID = convID.replaceAll("[A-Z]+#", "");
        JsonResult jsr = _dataBase.queryJsonDB("SELECT * FROM Products WHERE convID='" + convID + "' and reference='" + reference + "' AND ownerID=" + myagentID);
        if (jsr.size() > 0) {
            return jsr.getRowByIndex(0).getInt("price", -1);
        }
        return -1;

    }

    protected JsonArray DBgetAllHisProducts(String convID, int agentID) {
        JsonArray res = new JsonArray();
        convID = convID.replaceAll("[A-Z]+#", "");

        JsonResult jsr = _dataBase.queryJsonDB("SELECT * FROM Products WHERE convID='" + convID + "' AND ownerID=" + agentID);
        for (JsonValue jsvp : jsr.getAllRows()) {
            res.add(new JsonObject().
                    add("reference", jsvp.asObject().getString("reference", "none")).
                    add("serie", jsvp.asObject().getInt("serie", 0)).
                    add("price", jsvp.asObject().getInt("price", 0)));
        }
        return res;

    }

    protected String DBTransferProduct(String convID, String reference, int newownerID) {

        if (this.DBcheckProduct(convID, reference)) {
            try {
                DBACoin mv = new DBACoin();
                mv.decodeCoin(reference);
                if (!mv.isValid()) {
                    throw (new Exception("Bad coin reference"));
                }
                mv.setOwner(newownerID);
                mv.encodeCoin();
                if (!mv.isValid()) {
                    throw (new Exception("Bad coin reference"));
                }
                String newreference = reference.split("#")[0] + "#" + mv.getCoin();
                this.DBupdateProduct(convID, reference, newreference, mv.getOwner(), mv.getSerie());
                return newreference;
            } catch (Exception ex) {
                Exception("", ex);
                return "";
            }
        }
        return "";
    }

//    public String showMarket(Session s) {
//        if (s == null) {
//            return "";
//        }
//        String convid = s.getKey(), res = "MARKETPLACE OF SESSION " + convid;
//        convid = convid.replaceAll("^.*#", "");
//        JsonResult jsrowner = _dataBase.queryJsonDB("SELECT DISTINCT ownerID FROM burntProducts where convID='" + convid + "' "); //,jsmarket=_dataBase.queryJsonDB("SELECT * FROM burntProducts where convID='"+s.getKey()+"'");
//        for (int io = 0; io < jsrowner.size(); io++) {
//            JsonObject jso = jsrowner.getRowByIndex(io);
//            int agowner = jso.getInt("ownerID", -1);
//            String ownername = (this.DBgetAgentName(agowner).equals("") ? "UNKNOWN" : this.DBgetAgentName(jso.getInt("ownerID", -1)));
//            res += "\nOwned by " + ownername + ":";
//            int k = 0;
//            for (JsonValue jsvP : this.DBgetAllHisProducts(convid, agowner)) {
//                if (k % 3 == 0) {
//                    res += "\n";
//                }
//                String reference = jsvP.asObject().getString("reference", "");
//                int price = jsvP.asObject().getInt("price", -1);
//                res += "\t\t" + reference + " (" + price + " C)";
//                k++;
//            }
//        }
//        return res;
//    }
    public String showPublicMarket(String convid, outputTo o) {
        EnhancedTextOutput pw = new EnhancedTextOutput(o);
        String res = pw.Normal() + pw.INFO() + pw.bold(" MARKETPLACE OF SESSION " + convid) + pw.newline();
        convid = convid.replaceAll("^.*#", "");
        JsonResult jsrowner = _dataBase.queryJsonDB("SELECT DISTINCT ownerID FROM Products where convID='" + convid + "' "); //,jsmarket=_dataBase.queryJsonDB("SELECT * FROM burntProducts where convID='"+s.getKey()+"'");
        for (int io = 0; io < jsrowner.size(); io++) {
            JsonObject jso = jsrowner.getRowByIndex(io);
            int agowner = jso.getInt("ownerID", -1);
            String ownername = (this.DBgetAgentName(agowner).equals("") ? "UNKNOWN" : this.DBgetAgentName(jso.getInt("ownerID", -1)));
            res += pw.newline() + pw.under("Owned by " + ownername + ":") + pw.newline();
            int k = 0;
            for (JsonValue jsvP : this.DBgetAllHisProducts(convid, agowner)) {
                if (k % 4 == 0) {
                    res += pw.newline();
                }
                String reference = jsvP.asObject().getString("reference", "");
                int price = jsvP.asObject().getInt("price", -1), serie = jsvP.asObject().getInt("serie", -1);
                res += pw.mono(reference.split("#")[0] + "/" + serie + "/" + price + "/") + pw.tab();
                k++;
            }
        }
        return res;
    }

    public String showMyProducts(String convid, outputTo o) {
        EnhancedTextOutput pw = new EnhancedTextOutput(o);
        String res = pw.Normal() + pw.INFO();
        convid = convid.replaceAll("^.*#", "");
        int agowner = myagentID;
        String ownername = getLocalName();
        res += pw.newline() + pw.under("Offered by" + ownername + ":") + pw.newline();
        int k = 0;
        for (JsonValue jsvP : this.DBgetAllHisProducts(convid, agowner)) {
            if (k % 3 == 0) {
                res += pw.newline();
            }
            String reference = jsvP.asObject().getString("reference", "");
            res += pw.mono(reference) + pw.tab();
            k++;
        }
        return res;
    }

    protected JsonArray DBgetAllMyProducts(String convID) {

        return this.DBgetAllHisProducts(convID, myagentID);

    }

    protected void DBregisterProduct(String cID, String reference, int serie, int owid, int price) {
        cID = cID.replaceAll("[A-Z]+#", "");
        _dataBase.insertDB("INSERT INTO Products (convID, reference, serie, ownerID, price, agentID)"
                + " VALUES ('" + cID + "', '" + reference + "', " + serie + ", " + owid + ", " + price + ", " + this.DBgetAgentID(this.getAID()) + ")");
    }

    protected boolean DBcheckProduct(String convID, String reference) {
        return true;
//        convID = convID.replaceAll("[A-Z]+#", "");
//////        JsonResult jsrp = _dataBase.queryJsonDB("SELECT * FROM burntProducts WHERE convID='" + convID + "' AND reference='" + reference + "'");
////        JsonResult jsrp = _dataBase.queryJsonDB("SELECT * FROM Products WHERE reference='" + reference + "'");
////        boolean prodgood = (jsrp != null && jsrp.size() > 0);
////        if (prodgood) {
////            return true;
////        } else {
////            JsonResult jsrp2 = _dataBase.queryJsonDB("SELECT * FROM Products WHERE reference='" + reference + "'");
////            return false;
////        }
////        JsonResult jsrp = _dataBase.queryJsonDB("SELECT * FROM burntProducts WHERE convID='" + convID + "' AND reference='" + reference + "'");
////        int agentID = _dataBase.getColumnInt("Products", "agentID", "reference", reference);
//        int agentID = _dataBase.getAllRows("Products", "reference", reference).size();
//        boolean prodgood = (agentID > 0);
//        if (prodgood) {
//            return true;
//        } else {
//            int aux = _dataBase.getAllRows("Products", "reference", reference).size();
//            return false;
//        }
    }
//    protected boolean DBcheckProduct(String convID, String reference) {
//        Session mySession = this.sessionListbyKey.get(convID);
//
//        boolean badprod = mySession == null || mySession.burntProducts.contains(reference);
//        if (badprod) {
//            return false;
//        } else {
//            return true;
//        }
//
//    }

    protected void DBburnProduct(String convID, String reference) {
        Session mySession = this.sessionListbyKey.get(convID);
        convID = convID.replaceAll("[A-Z]+#", "");
//        _dataBase.updateDB("DELETE FROM burntProducts WHERE convID='" + convID + "' AND reference='" + reference + "'");
        _dataBase.updateDB("DELETE FROM Products WHERE reference='" + reference + "'");

    }

    protected void DBburnAllProducts(String convID) {
        convID = convID.replaceAll("[A-Z]+#", "");
        _dataBase.updateDB("DELETE FROM Products WHERE convID='" + convID + "'");
    }

    protected void DBupdateProduct(String convID, String reference, String newReference, int newowner, int newserie) {
        convID = convID.replaceAll("[A-Z]+#", "");
        _dataBase.updateDB("UPDATE Products SET reference='" + newReference + "', serie=" + newserie + ", ownerID=" + newowner
                + " WHERE convID='" + convID + "' AND reference='" + reference + "'");
    }

    //
    // Classical ORM
    //
    protected String DBgetIdentityManager() {
        ArrayList<String> ims = this.DBgetAllServiceProviders("Identity manager");
        if (ims.size() > 0) {
            return ims.get(0);
        } else {
            return "";
        }
    }

    public boolean DBisCheckedInAgent(ACLMessage msg) {
        return true;
//        boolean res = false;
//
//        String agentname, ip;
//        JsonObject cardID;
//        int userID;
//
//        agentname = msg.getSender().getLocalName();
//        int dbagentID = this.DBgetAgentID(msg.getSender());
//        if (dbagentID >= 0) {
//            ip = _dataBase.getColumnString("Agents", "ipCheckedIn", "agentID", dbagentID);
//            res = !ip.equals("OFFLINE");
//        }
//        return res;
    }

//    public void doDelayedCheckOutAgent(ACLMessage msg, int milis) {
//        this.addBehaviour(new WakerBehaviour(this, milis) {
//            @Override
//            protected void onWake() {
//                doCheckOutStudentAgent(msg);
//            }
//        });
//    }
    public void DBCheckOutAgent(AID who) {
        int agentID = this.DBgetAgentID(who);
        if (agentID < 0) {
            return;
        }
        _dataBase.updateDB("UPDATE Agents SET ipCheckedIn='OFFLINE', userID=" + BADRECORD
                + " WHERE agentID=" + agentID);

    }

    public void doCheckOutStudentAgent(ACLMessage msg) {
        String agentname;
        int userID, agentID;

        agentID = this.DBgetAgentID(msg.getSender());
        agentname = msg.getSender().getLocalName();
        userID = this.DBgetWhoRepresent(msg);
        if (userID >= 0 && agentID >= 0) {
            String userName = this.DBgetUserName(userID), requester = msg.getSender().getLocalName();
            System("Check-out request from agent " + agentname + " in representation of user " + userID + " " + userName);
            System("Checking-out agent " + agentname);
            DBnotifyAnalytics(msg, "disconnect", "Session closed");
            DBCheckOutAgent(msg.getSender());
            System(agentname + " checked-out in representation of user " + userID + " " + userName);
        }
    }

    public void DBCheckInAgent(AID who) {
        int dbagentID = this.DBgetAgentID(who);
        String agentname = who.getLocalName();
        if (dbagentID >= 0) {
            _dataBase.updateDB("UPDATE Agents SET messages='', lastping='" + new TimeHandler().toString() + "', ipCheckedIn='ADMIN'"
                    + " WHERE agentID=" + dbagentID);
        } else {
            _dataBase.updateDB("INSERT INTO Agents (name, messages, lastping, userID, ipCheckedIn) "
                    + " VALUES ('" + agentname + "', '', '" + new TimeHandler().toString() + "', -1, 'ADMIN' )");
        }
        this.DBremoveService(who, "");
    }

    public int doCheckInStudentAgent(ACLMessage msg) {
        String agentname = "", ip, userName = "";
        JsonObject cardID;
        int userID;

        // Checking in an agent means register it as the representer of a user, if credentials
        // are provided, otherwise it is just registered
        // Check credentials if any
        try {
            _dataBase.startCommit();
            agentname = msg.getSender().getLocalName();
            System("Checkin request from agent " + agentname);
            String aux = (msg.getEncoding() != null ? _enigma.deCryptNew(msg.getEncoding()) : "");
            cardID = Json.parse(aux).asObject().get("data").asObject();
            userID = cardID.get("pradoid").asInt();
            userName = this.DBgetUserName(userID);
//            ip = cardID.get("ip").asString();
            ip = "";
            System("Checkin request from agent " + agentname + " in representation of user " + userID + " " + userName);
            // Register the agent and its bound user into the DB
            int dbagentID = this.DBgetAgentID(msg.getSender());
            if (dbagentID >= 0) {
                _dataBase.updateDB("UPDATE Agents SET messages='', lastping='" + new TimeHandler().toString() + (userID >= 0 ? "', userid=" + userID + ", ipCheckedIn='" + ip + "'" : "'")
                        + " WHERE agentID=" + dbagentID);
            } else {
                _dataBase.updateDB("INSERT INTO Agents (name, messages, lastping" + (userID >= 0 ? ", userid, ipCheckedIn" : "") + ") "
                        + " VALUES ('" + agentname + "', '', '" + new TimeHandler().toString() + "' " + (userID >= 0 ? ", " + userID + ", '" + ip + "'" : "") + ")");
            }
            // If it represents a user, then topenxsession a new session
            if (userID >= 0) {
                _dataBase.updateDB("UPDATE Users SET agentID=" + dbagentID
                        + " WHERE userID=" + userID);
            }
            System(agentname + " checked-in in representation of user " + userID + " " + userName);
            _dataBase.endCommit();
            return userID;
        } catch (Exception ex) {
            _dataBase.rollBack();
            Exception("Unable to check-in agent " + agentname + " Please check cardID", ex);
            return BADRECORD;
        }
    }
//            cardID = null;
//            userID = -1;
//            ip = "";
//            System("Checking in agent " + agentname + " in representation of no user ");
//        }
//        try {
//            // Register the agent and its bound user into the DB
//            _dataBase.startCommit();
//            int dbagentID = this.DBgetAgentID(msg.getSender());
//            if (dbagentID >= 0) {
//                _dataBase.updateDB("UPDATE Agents SET messages='', lastping='" + new TimeHandler().toString() + (userID >= 0 ? "', userid=" + userID + ", ipCheckedIn='" + ip + "'" : "'")
//                        + " WHERE agentID=" + dbagentID);
//            } else {
//                _dataBase.updateDB("INSERT INTO Agents(name, messages, lastping" + (userID >= 0 ? ", userid, ipCheckedIn" : "") + ") "
//                        + " VALUES ('" + agentname + "', '', " + new TimeHandler().toString() + "' " + (userID >= 0 ? ", " + userID + ", '" + ip + "'" : "") + ")");
//            }
//            // If it represents a user, then topenxsession a new session
//            if (userID >= 0) {
//                _dataBase.updateDB("UPDATE Users SET agentID=" + dbagentID
//                        + " WHERE userID=" + userID);
//            }
//            System(agentname + " checked-in in representation of user "+ userID+" "+userName);
//            _dataBase.endCommit();
//            return userID;
//        } catch (Exception ex) {
//            Exception(ex);
//            _dataBase.rollBack();
//            return -1;
//
//        }
//    }

    //
    // Scenes and sessions keeping
    //
    protected void doCleanUpSessions() {
//        System("Cleaning stale sessions");
//        for (Session s : this.sessionListbyKey.values()) {
//            if (new TimeHandler(s.lastUpdated).elapsedTimeSecs(new TimeHandler()) > 180) {
//                removeAnalyticsSession(s);
//            }
//        }
    }

    protected String chooseOne(String names[]) {
        return names[(int) (Math.random() * names.length)];
    }

    protected boolean openScene(Session s, String AgentClassname) {
        String supers[][] = new String[3][];
        supers[0] = new String[]{"DroneGroceries", "DroneGourmet", "SuperDrone"};
        supers[1] = new String[]{"PCDrones", "FnacDrone", "ElDroneIngles"};
        supers[2] = new String[]{"AmazDrone", "MediaDrone", "DroneBay"};

        System("Opening scene " + s.getKey());
        // Generating bitcoins
        System("Scene " + s.getKey() + " generating " + s.COINSXSESSION + " coins");
        for (int i = 0; i < Session.COINSXSESSION; i++) {
            this.DBaddProduct(s.getKey(), "coin", myagentID, i, 1);
        }

        try {
            s.container = this.getContainerController();
            s.burocrats = new ArrayList();
            JsonObject auxconfig = Json.parse(_config.toString()).asObject();
            auxconfig.remove("analytics");
            String parameters[] = new String[1];
            JsonObject myconfig;
            // Seller        
            for (int i = 0; i < supers.length; i++) {
                auxconfig.add("conversationid", s.key);
                auxconfig.add("worldmanager", getLocalName());
                auxconfig.add("sellertype", "shop");
                auxconfig.add("margin", i - 1);
                parameters[0] = auxconfig.toString();
                MicroRuntime.startAgent(chooseOne(supers[i]) + "#" + getKey(4), AgentClassname, parameters);
//                AgentController a = s.container.createNewAgent(chooseOne(supers[i]) + "#" + getKey(4), AgentClassname, parameters);
//
//                a.start();
//                s.burocrats.add(a);
                //               System("Burocratic agent" + a.getName() + " has been created ");
            }
        } catch (Exception ex) {
            Exception(ex);
            closeScene(s);
            return false;
        }
        return true;
    }

    public boolean closeScene(Session s) {
        System("Closing scene " + s.getKey());
        try {
            for (AgentController a : s.burocrats) {
                a.kill();
            }
            this.DBburnAllProducts(s.getKey());
        } catch (Exception ex) {
            Exception(ex);
            return false;
        }
        return true;
    }

    protected void removeAnalyticsSession(Session s) {
        if (s == null) {
            return;
        }
        int userID = s.userID;
        String key = s.key;
        System("Closing the session " + key);
        this.sessionListbyUser.remove(userID);
        this.sessionListbyKey.remove(key);
        if (s.burocrats != null && !s.burocrats.isEmpty()) {
            closeScene(s);
        }
    }

    protected void closeAnalyticsSession(ACLMessage msg) {
        int userID = this.DBgetWhoRepresent(msg);
        String userName = this.DBgetUserName(userID), requester = msg.getSender().getLocalName();
        Session todel = this.sessionListbyUser.get(userID);
        System("Agent " + requester + " requests closing the session for user " + userID + " " + userName);
        removeAnalyticsSession(todel);
    }

    protected Session getAnalyticsSession(ACLMessage msg) {
        int userID = this.DBgetWhoRepresent(msg);
        String convID = msg.getConversationId();
//        String userName = this.DBgetUserName(userID),
//                requester = msg.getSender().getLocalName();
        Session mySession = null;
        if (userID >= 0 && this.sessionListbyUser.get(userID) != null) {
            mySession = this.sessionListbyUser.get(userID);
        } else if (convID != null && this.sessionListbyKey.get(convID) != null) {
            mySession = this.sessionListbyKey.get(convID);
        }

        if (mySession != null) {
            this._dlogger.setOwnerQualifier(mySession.key);
//            System("Retrieving session " + mySession);
//            mySession.lastUpdated = new TimeHandler().toString();
//            this.DBUpdateAnalyticsNotification(mySession, _state, _ignoreAMS);
            return mySession;
        }
        return null;
    }

    protected Session openAnalyticsSession(ACLMessage msg) {
        Session mysession = null;
        int userID = this.DBgetWhoRepresent(msg);
        if (userID >= 0) {
            String userName = this.DBgetUserName(userID), requester = msg.getSender().getLocalName();
            System("Agent " + requester + " requests opening a session for user " + userID + " " + userName);
            if (sessionListbyUser.get(userID) != null) {
                this.removeAnalyticsSession(sessionListbyUser.get(userID));
            }
            System("Opening a NEW session for user " + userID + " " + userName);
            mysession = new Session(this._analytics);
            mysession.userID = userID;
            mysession.username = userName;
            mysession.requester = requester;
            mysession.HACKreply = "";
            if (!mysession.isIndividual) {
                mysession.groupID = this.DBgetGroup(userID);
                mysession.groupname = this.DBgetGroupAlias(mysession.groupID);
                mysession.whoID = mysession.groupID;
            } else {
                mysession.whoID = userID;
            }
            mysession.usersInvolved = this.DBgetGroupMates(mysession.courseID, mysession.assignmentID, mysession.whoID);
            mysession.whoName = this.DBgetUserName(mysession.userID);
            mysession.error = false;
            if (this.DBgetAllServiceProviders("NOTIFIER").size() > 0) {
                mysession.donotify = true;
            }
            mysession.lastUpdated = new TimeHandler().toString();
            sessionListbyUser.put(userID, mysession);
            sessionListbyKey.put(mysession.getKey(), mysession);
        }
        return mysession;
    }

    // To be used as an auxiliary tool for other ADMIN agents to send telegrams
    protected Session openEmergencySession(ACLMessage msg) {
        int userID = this.DBgetWhoRepresent(msg);
        Session mysession = sessionListbyUser.get(userID);
        if (userID >= 0 && mysession == null) {
            String userName = this.DBgetUserName(userID), requester = msg.getSender().getLocalName();
            System("Agent " + requester + " requests opening a session for user " + userID + " " + userName);
//            if (sessionListbyUser.get(userID) == null) {
            System("Opening a NEW session for user " + userID + " " + userName);
            mysession = new Session();
            mysession.userID = userID;
            mysession.requester = requester;
            mysession.HACKreply = "";
            mysession.whoID = userID;
            mysession.telegrammessages = new ArrayList();
            mysession.usersInvolved = new ArrayList();
            mysession.usersInvolved.add(userID);
            mysession.whoName = this.DBgetUserName(mysession.userID);
            mysession.error = false;
            if (this.DBgetAllServiceProviders("NOTIFIER").size() > 0) {
                mysession.donotify = true;
            }
            if (userID >= 0) {
                sessionListbyUser.put(userID, mysession);
                sessionListbyKey.put(mysession.getKey(), mysession);
            }
        }
        return mysession;
    }

    protected int DBgetGrade(int courseID, int userID) {
        int problemID, points, tpoints = 0;
        String title;
        JsonResult problems = _dataBase.queryJsonDB("SELECT * FROM Problems WHERE courseID=" + courseID),
                competences = _dataBase.queryJsonDB("SELECT * FROM Competences WHERE courseID=" + courseID + " ORDER BY timing ASC");//_dataBase.getAllRowsJsonDB("Competences", new JsonObject().add("courseID", _course.get("courseID").asInt()));;
        JsonResult badges = _dataBase.queryJsonDB("SELECT * FROM Badges WHERE userID=" + userID);
        if (badges.size() > 0) {
            for (int i = 0; i < badges.size(); i++) {
                JsonObject row = badges.getRowByIndex(i);
                problemID = row.getInt("problemID", BADRECORD);
                String badge = row.get("badge").asString();
                if (problemID >= 0) {
                    title = problems.getRowByPK(problemID).getString("title", "unknown");
                    switch (badge) {
                        case "problem":
                            points = problems.getRowByPK(problemID).getInt("autoScore", 0);
                            break;
                        default:
                            points = 250;

                    }
                    if (points > 0) {
                    }
                } else {
                    title = row.getString("competenceID", "unknown");
                    points = competences.getRowByPK(title).getInt("autoScore", 0);
                }
                tpoints += points;
            }
        }
        return tpoints;
    }

    //
    // DataBase API
    //
    // General API
    protected JsonArray DBgetAssignment(int assignmentID) {
        JsonArray res = new JsonArray();
        if (_dataBase.isOpen() && !_dataBase.isError()
                && _dataBase.queryDB(String.format("SELECT * FROM FullAssignmentsView WHERE assignmentID = %d", assignmentID))) {
            res = _dataBase.getJsonResult().get("resultset").asArray();
        }
        return res;
    }

    protected boolean DBisIndividual(int assignmentID) {
        return DBgetAssignment(assignmentID).get(0).asObject().getBoolean("isIndividual", false);
    }

    protected boolean DBInsertUser(int userID, String name, String email, boolean isTeacher) {
        Info("Adding user " + name + " to the database as userID " + userID);
        _dataBase.updateDB("INSERT INTO Users SET userID=" + userID + ", name='" + name + "', email='" + email + "', isTeacher=" + isTeacher);
        return true;
    }

    protected boolean DBRegisterUser(int userID, int courseID) {
        try {
            Info("Registering userID " + userID + " in courseID " + courseID);
            _dataBase.startCommit();
            // Selecciona el Alias
            JsonResult jsraliases = _dataBase.queryJsonDB("SELECT * FROM userAliases");
            int ia = 0;
            while (_dataBase.queryJsonDB("SELECT * FROM Users WHERE alias='" + jsraliases.getRowByIndex(ia).asObject().getString("name", "LONE STAR") + "'").size() > 0 && ia < jsraliases.size()) {
                ia++;
            }
            Info("Aliasing userID " + userID + " as " + jsraliases.getRowByIndex(ia).asObject().getString("name", "LONE STAR"));
            _dataBase.updateDB("UPDATE Users SET alias='" + jsraliases.getRowByIndex(ia).asObject().getString("name", "LONE STAR") + "' WHERE userID=" + userID);
            // Add it to the course
            _dataBase.updateDB("INSERT INTO CourseRegistration SET courseID=" + courseID + ", userID=" + userID);
            // Add analytics row without affectinng the other students
            JsonArray competences = _dataBase.getAllRows("Competences", "courseID", courseID);
            String competenceID;
            for (JsonValue c : competences) {
                competenceID = c.asObject().get("competenceID").asString();
                _dataBase.updateDB("INSERT INTO AnalyticsReportStudent VALUES (" + userID + "," + courseID + ",'" + competenceID + "'," + "'', " + 0 + ")");
            }
            for (JsonValue jsvp : _dataBase.getAllRows("FullAssignmentsView", "courseID", courseID)) {
                int problemID = jsvp.asObject().getInt("problemID", BADRECORD), assignmentID = jsvp.asObject().getInt("assignmentID", BADRECORD);
                boolean isindividual = jsvp.asObject().getBoolean("isIndividual", false);
                if (isindividual) {
                    _dataBase.updateDB("INSERT INTO AnalyticsReportAssignment SET courseID=" + courseID + ", assignmentID="
                            + assignmentID + ", whoID=" + userID + ", problemID=" + problemID + ", milestones='', count=0, firstsolved=-1, latencysolved=-1, costsolved=-1, benefitsolved=-1, firstopen=-1");
                }
            }
            Info("Transaction OK");
            _dataBase.endCommit();
            return true;
        } catch (Exception ex) {
            Exception("Unable to register user " + userID + " in course " + courseID, ex);
            _dataBase.rollBack();
            return false;
        }
    }

    protected int DBgetAgentID(AID aid) {
        if (!_dataBase.canContinue()) {
            return BADRECORD;
        }
        String agname = aid.getLocalName();
        return _dataBase.getColumnInt("Agents", "agentID", "name", agname);
    }

    protected boolean DBisAdminAgent(AID aid) {
        if (!_dataBase.canContinue()) {
            return false;
        }
        String agname = aid.getLocalName();
        return _dataBase.getColumnString("Agents", "ipCheckedIn", "name", agname).equals("ADMIN");
    }

    protected String DBgetUserName(int userID) {
        if (!_dataBase.canContinue()) {
            return "";
        }
        JsonResult jsrq = _dataBase.queryJsonDB("SELECT * FROM Users WHERE userID=" + userID);
        if (jsrq.size() > 0) {
            return jsrq.getRowByIndex(0).getString("name", "");
        } else {
            return "";
        }
    }

    protected boolean DBIsTeacher(int userID) {
        if (!_dataBase.canContinue()) {
            return false;
        }
        JsonResult jsrq = _dataBase.queryJsonDB("SELECT * FROM Users WHERE userID=" + userID);
        if (jsrq.size() > 0) {
            return jsrq.getRowByIndex(0).getString("isTeacher", "0").equals("1");
        } else {
            return false;
        }
    }

    protected JsonObject DBgetUserProgress(int courseID, int userID) {
        JsonObject res = new JsonObject();
//        if (!_dataBase.canContinue() || userID <0) {
//            return res;
//        }
//        JsonObject jsuser = _dataBase.queryJsonDB("SELECT * FROM Users where userID="+userID).getRowByIndex(0),
//                competence,
//                studentrecord; 
//        JsonArray jsac = _dataBase.queryJsonDB("SELECT * FROM Competences").getAllRows();
//        for (JsonValue c : jsac) {
//            competence = c.asObject();
//        studentrecord = _dataBase.queryJsonDB("SELECT * FROM AnalyticsReportStudent "
//                        + "WHERE courseID=" + courseID + " AND userID=" + userID + " AND competenceID='" + competence.getString("competenceID", "none") + "'").getRowByIndex(0);
//        }
        return res;
    }

    protected int DBgetOwnerChatID(long cid) {
        try {
            JsonResult jsrq = _dataBase.queryJsonDB("SELECT * FROM Users WHERE chatID=" + cid);
            return jsrq.getRowByIndex(0).getInt("userID", BADRECORD);
        } catch (Exception ex) {
            Exception(ex);
            return BADRECORD;
        }
    }

    protected long DBgetUserChatID(int userID) {
        if (!_dataBase.canContinue()) {
            return BADRECORD;
        }
        return _dataBase.getColumnLong("Users", "chatID", "userID", userID);
    }

    protected void DBsetUserChatID(int userID, long cid) {
        if (_dataBase.canContinue()) {
            _dataBase.updateDB("UPDATE Users SET chatID=" + cid + " WHERE userID=" + userID);
        }
    }

    protected String DBgetGroupAliasUser(int userID) {
        return this.DBgetGroupAlias(this.DBgetGroup(userID));
    }

    protected String DBgetGroupAlias(int groupID) {
        if (!_dataBase.canContinue()) {
            return "";
        }
        try {
            JsonResult jsrq = _dataBase.queryJsonDB("SELECT * FROM FullCourseGroupRegistration WHERE groupID=" + groupID);
            return jsrq.getRowByIndex(0).getString("groupalias", "");
        } catch (Exception ex) {
            Exception("", ex);
            return "";
        }
    }

    protected String DBgetAgentName(int agentID) {
        if (!_dataBase.canContinue()) {
            return "";
        }
        return _dataBase.getColumnString("Agents", "name", "agentID", agentID);
    }

    protected boolean DBisUserRegistered(int userID, int courseID) {
        _dataBase.queryDB("SELECT * FROM CourseRegistration"
                + " WHERE courseID=" + courseID + " AND userID=" + userID);
        JsonArray r = _dataBase.getJsonResult().get("resultset").asArray();
        return r.size() > 0;
    }

    protected int DBgetGroup(int userID) {
        return _dataBase.getColumnInt("GroupMembers", "groupID", "userID", userID);
    }

    protected int DBgetGroup(String groupalias) {
        return _dataBase.getColumnInt(_config.get("dbconnection").asObject().get("database").asString() + ".Groups", "groupID", "alias", groupalias);
    }

    protected ArrayList<Integer> DBgetGroupMates(int courseID, int assignmentID, int whoID) {
        ArrayList<Integer> res = new ArrayList<>();
        if (this.DBisIndividual(assignmentID)) {
            res.add(whoID);
        } else {
            JsonArray group = _dataBase.getAllRows("GroupMembers", "groupID", whoID);
            for (JsonValue g : group) {
                res.add(g.asObject().get("userID").asInt());
            }
        }
        return res;
    }

    protected ArrayList<Integer> DBgetGroupMates(int groupID) {
        ArrayList<Integer> res = new ArrayList<>();
        JsonArray group = _dataBase.getAllRows("GroupMembers", "groupID", groupID);
        for (JsonValue g : group) {
            res.add(g.asObject().get("userID").asInt());
        }
        return res;
    }

    protected int DBgetGroup(int courseID, int userID) {
        int res = _dataBase.queryJsonDB("SELECT * FROM GroupMembers WHERE userID=" + userID).getRowByIndex(0).getInt("groupID", BADRECORD);
        return res;
    }

    protected int DBgetWhoRepresent(ACLMessage msg) {
        String agentname = msg.getSender().getLocalName();
        int userID;
//        if (this.whoRepresent.get(agentname) == null) {
        userID = _dataBase.getColumnInt("Agents", "userID", "name", agentname);
        this.whoRepresent.put(agentname, userID);
//        } else {
//            userID = this.whoRepresent.get(agentname);
//        }S

        return userID;
    }

    protected TimeHandler DBgetLastUpdate(String tablename) {
        JsonObject date = _dataBase.queryJsonDB("SELECT UPDATE_TIME FROM   information_schema.tables WHERE  "
                + "TABLE_SCHEMA = 'Agents'   AND TABLE_NAME = '" + tablename + "'").getRowByIndex(0);
        try {
            String update = date.getString("UPDATE_TIME", "");
            return new TimeHandler(update);
        } catch (Exception Ex) {
            return new TimeHandler();
        }
    }

    // Analytics
    protected JsonResult DBgetLastUpdatedRows(int courseID, String fromdate) {
        String date = new TimeHandler(fromdate).toString();
        JsonResult jq = _dataBase.queryJsonDB("SELECT * FROM LearningAnalytics WHERE courseID=" + courseID + " AND STRCMP(date, '" + date + "') > 0");
        return jq;
    }

    protected long DBlowestValue(int courseID, int assignmentID, int problemID, String field) {
        JsonResult res = _dataBase.queryJsonDB("SELECT * FROM AnalyticsReportAssignment WHERE problemID=" + problemID + " and " + field + " <> -1 ORDER BY " + field + " ASC");
        if (res.size() > 0) {
            return res.getRowByIndex(0).get(field).asLong();
        }
        return -1;
    }

    protected int DBhighestValue(int courseID, int assignmentID, int problemID, String field) {
        JsonResult res = _dataBase.queryJsonDB("SELECT * FROM AnalyticsReportAssignment WHERE problemID=" + problemID + " and " + field + " >= 0 ORDER BY " + field + " DESC");
        if (res.size() > 0) {
            return res.getRowByIndex(0).get(field).asInt();
        }
        return -1;
    }

    protected boolean DBhasBadge(int courseID, int assignmentID, int problemID, int whoID, String badge, String competenceID) {
        JsonResult jsq = _dataBase.queryJsonDB("SELECT * FROM Badges WHERE courseID=" + courseID + " AND assignmentID=" + assignmentID + " AND problemID=" + problemID + " AND userID="
                + whoID + " AND badge='" + badge + "'");
        return (jsq.size() > 0);

    }

    protected void DBaddBadge(int courseID, int assignmentID, int problemID, int whoID, String badge, String competenceID) {
        JsonArray res, users, group;
        users = new JsonArray();
        if (competenceID.equals("") || assignmentID < 0 || this.DBisIndividual(assignmentID)) {
            users.add(new JsonObject().add("userID", whoID));
        } else {
            group = _dataBase.getAllRows("GroupMembers", "groupID", whoID);
            for (JsonValue g : group) {
                users.add(new JsonObject().add("userID", g.asObject().get("userID").asInt()));
            }
        }
        for (JsonValue u : users) {
            int userID = u.asObject().get("userID").asInt();
            if (competenceID.equals("")) {
                Info("User " + userID + " gets badge " + (assignmentID >= 0 ? badge : competenceID) + " in problemID " + problemID);
                _dataBase.updateDB("REPLACE INTO Badges (courseID, assignmentID, problemID, userID, badge, competenceID, date) VALUES (" + courseID + ", " + assignmentID + ", " + problemID + ", " + userID + ", '" + badge + "', '', '" + new TimeHandler().toString() + "')");
            } else {
                _dataBase.updateDB("REPLACE INTO Badges (courseID, assignmentID, problemID, userID, badge, competenceID, date) VALUES (" + courseID + ", " + -1 + ", " + -1 + ", " + userID + ", '" + badge + "', '" + competenceID + "', '" + new TimeHandler().toString() + "')");
            }
        }

    }

    protected void DBcleanAnalyticsReport(int courseID) {
        // Truncate tables of reports
        try {
            _dataBase.startCommit();
            _dataBase.deleteDB("DELETE FROM AnalyticsReportStudent WHERE courseID = " + courseID);
            _dataBase.deleteDB("DELETE FROM AnalyticsReportAssignment WHERE courseID = " + courseID);
            _dataBase.deleteDB("DELETE FROM LearningAnalytics WHERE courseID = " + courseID);
            _dataBase.deleteDB("DELETE FROM Badges");
            _dataBase.endCommit();
        } catch (Exception ex) {
            _dataBase.rollBack();
            Exception("", ex);
        }
        JsonArray students, groups, assignments, problems, competences, milestones;
        int userID, groupID, assignmentID, problemID, count;
        String competenceID, milestoneID, milestone;
        boolean isIndividual;

        // Gather all data
        competences = _dataBase.getAllRows("Competences", "courseID", courseID);
        students = _dataBase.getAllRows("CourseRegistration", "courseID", courseID);
        groups = _dataBase.getAllRows(_config.get("dbconnection").asObject().get("database").asString() + ".Groups", "courseID", courseID);
        assignments = _dataBase.getAllRows("Assignments", "courseID", courseID);
        milestones = _dataBase.getAllRows("Milestones", "courseID", courseID);
        // Recalculate size of competences
        try {
            _dataBase.startCommit();
            for (JsonValue c : competences) {
                milestone = "";
                competenceID = c.asObject().get("competenceID").asString();
                for (JsonValue m : milestones) {
                    milestoneID = m.asObject().get("milestoneID").asString();
                    if (competenceID.equals(milestoneID.subSequence(0, 5))) {
                        milestone += milestoneID;
                    }
                }
                if (milestone.length() > 0) {
                    count = this.countMilestones(milestone);
                    _dataBase.updateDB("UPDATE Competences SET size=" + count + ", milestones='" + milestone + "' WHERE competenceID='" + competenceID + "'");
                }
            }
            // Insert default values in Students reports
            for (JsonValue s : students) {
                userID = s.asObject().get("userID").asInt();
                for (JsonValue c : competences) {
                    competenceID = c.asObject().get("competenceID").asString();
                    _dataBase.updateDB("INSERT INTO AnalyticsReportStudent VALUES (" + userID + "," + courseID + ",'" + competenceID + "'," + "'', " + 0 + ")");
                }
            }

            // Insert default values in assignment reports
            for (JsonValue a : assignments) {
                assignmentID = a.asObject().get("assignmentID").asInt();
                isIndividual = this.DBisIndividual(assignmentID);
                problems = _dataBase.getAllRows("AssignmentsProblems", "assignmentID", assignmentID);
                for (JsonValue p : problems) {
                    problemID = p.asObject().get("problemID").asInt();
                    if (isIndividual) {
                        for (JsonValue s : students) {
                            userID = s.asObject().get("userID").asInt();
                            _dataBase.updateDB("INSERT INTO AnalyticsReportAssignment VALUES (" + courseID + "," + assignmentID + "," + userID + "," + problemID + ", '', 0, -1, -1, -1, -1, -1)");
                        }
                    } else {
                        for (JsonValue g : groups) {
                            groupID = g.asObject().get("groupID").asInt();
                            _dataBase.updateDB("INSERT INTO AnalyticsReportAssignment VALUES (" + courseID + "," + assignmentID + "," + groupID + "," + problemID + ", '', 0, -1, -1, -1, -1,-1)");
                        }
                    }

                }
            }

            _dataBase.endCommit();
        } catch (Exception ex) {
            Exception("", ex);
            _dataBase.rollBack();
        }
    }

    protected void DBaddAnalyticsReport(Session mysession, ACLMessage reportmsg, JsonObject metrics) {
        if (!_analytics.isValidMilestoneReport(reportmsg, mysession)) {
            return;
        }
        String milestoneID = _analytics.getMilestoneID(reportmsg);
        DBaddAnalyticsReport(mysession, milestoneID, metrics);
    }

    protected void DBcleanAnalyticsNotification(Session mysession) {
        String milestoneID = "INFORMA";
        _dataBase.updateDB("DELETE FROM LearningAnalytics WHERE assignmentID=" + mysession.assignmentID
                + " AND problemID=" + mysession.problemID
                + " AND milestoneID='" + milestoneID + "'"
                + " AND whoID=" + mysession.whoID);

    }

    protected void DBaddAnalyticsNotification(Session mysession, JsonObject info) {
        JsonArray prev = new JsonArray();
        if (_analytics == null || mysession == null) {
            return;
        }
        String milestoneID = "INFORMA";
        try {
            _dataBase.startCommit();
            JsonResult q = _dataBase.queryJsonDB("SELECT * FROM LearningAnalytics WHERE assignmentID=" + mysession.assignmentID
                    + " AND problemID=" + mysession.problemID
                    + " AND milestoneID='" + milestoneID + "'"
                    + " AND whoID=" + mysession.whoID);
            if (q.size() == 0) {
                prev.add(info);
                _dataBase.updateDB("INSERT INTO LearningAnalytics (courseID, assignmentID, problemID, milestoneID, whoID, date, notification) "
                        + " VALUES (" + mysession.courseID + "," + mysession.assignmentID + ", " + mysession.problemID + ", '" + milestoneID + "' , "
                        + mysession.whoID + ", '" + new TimeHandler().toString() + "', '" + prev.toString() + "')");
            } else {
                prev = Json.parse(q.getRowByIndex(0).getString("notification", "")).asArray();
                prev.add(info);
                _dataBase.updateDB("UPDATE LearningAnalytics SET notification='" + prev.toString() + "', date='" + new TimeHandler().toString() + "' WHERE assignmentID=" + mysession.assignmentID
                        + " AND problemID=" + mysession.problemID
                        + " AND milestoneID='" + milestoneID + "'"
                        + " AND whoID=" + mysession.whoID);
            }
            _dataBase.endCommit();
        } catch (Exception ex) {
            _dataBase.rollBack();
            Exception("", ex);
        }

    }

    protected void DBaddAnalyticsReport(Session mysession, String milestoneID, JsonObject info) {
        long now;
        ArrayList<String> earnedGroupBadges = new ArrayList<>();
        String cmxcourse;

        // If not a milestone of the current problem, it is rejected
        if (!this.findMilestone(mysession.pmxcourse, milestoneID)) {
            Error("Alien milestone " + milestoneID + " does not belong to the problem ");
            return;
        }
        // Register the arrival of the milestone
        now = new TimeHandler().elapsedTimeSecs();

        try {
            _dataBase.startCommit();
            // The milestone is new in the current session
            boolean isnewsession, isnewgroup, isoldrepair, isoldnorepair, hasproblembadge, seemssolved;
            isnewsession = !this.findMilestone(mysession.mdxsession, milestoneID) || milestoneID.equals(("IGOAL04"));
            isnewgroup = !this.findMilestone(mysession.mdxcourse, milestoneID);
//            hasproblembadge = this.DBhasBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, mysession.whoID, milestoneID, "");
//            seemssolved = this.countMilestones(mysession.mdxcourse) == this.countMilestones(mysession.pmxcourse);
            // If it is not new and the problem is already solved, then repair, if needed
//            if (seemssolved && !hasproblembadge) {
//                isoldrepair = this.findMilestone(mysession.mdxcourse, "IGOAL04");
//            } else {
            isoldrepair = false;
//            }
            if (isnewsession || isoldrepair) {
                System("Analytics received"
                        + "   assignmentID: " + mysession.assignmentID
                        + "   problemID: " + mysession.problemID
                        + "   milestoneID: " + milestoneID
                        + "  who: " + mysession.whoID);
                // This is the first to arrive: opening the problem
                if (mysession.mdxsession.equals("")) {
                    mysession.topenxsession = now;
                    if (mysession.topenxcourse == -1) {
                        mysession.topenxcourse = mysession.topenxsession;
                    }
                }
                mysession.mdxsession = this.addMilestone(mysession.mdxsession, milestoneID);
                mysession.kmdxsession++;
                // It is a historically newly achieved milestone
                if (isnewgroup || isoldrepair) {
                    if (isnewgroup) {
                        // Notify students the new achievement
                        this.DBUpdateAnalyticsNotification(mysession, Emojis.MILESTONE + "*|" + getLocalName() + "|" + mysession.whoName + "|*" + mysession.getKey() + "|\n" + milestoneID + "@" + mysession.problemtitle + " ", true);

                        // Add the milestone to the already achieved
                        mysession.mdxcourse = this.addMilestone(mysession.mdxcourse, milestoneID);
                        mysession.kmdxcourse++;
                        // First save in the database
                        _dataBase.updateDB("INSERT INTO LearningAnalytics (courseID, assignmentID, problemID, milestoneID, whoID, date) "
                                + " VALUES (" + mysession.courseID + "," + mysession.assignmentID + ", " + mysession.problemID + ", '" + milestoneID + "' , "
                                + mysession.whoID + ", '" + new TimeHandler().toString() + "')");
                    }
                    // Check eventually completed competences afterwards
                    String competenceID = milestoneID.substring(0, 5);
                    cmxcourse = _dataBase.queryJsonDB("SELECT * FROM Competences WHERE competenceID='" + competenceID + "'").getRowByIndex(0).getString("milestones", "UNKNOWN");
                    // Propagate to individual reports
                    for (int iduser : mysession.usersInvolved) {
                        if (iduser == AdminAgent.ROOTUSERID) {
                            continue;
                        }
                        // Check new milestone regarding personal competences
                        _dataBase.queryDB("SELECT * FROM AnalyticsReportStudent WHERE courseID=" + mysession.courseID
                                + " AND competenceID='" + competenceID + "' AND userID=" + iduser);

                        JsonArray res = _dataBase.getJsonResult().get("resultset").asArray();
                        String mdxcomp = res.get(0).asObject().get("milestones").asString();
                        mdxcomp = addMilestone(mdxcomp, milestoneID);
                        int kmdxcomp = this.countMilestones(mdxcomp);
                        _dataBase.updateDB("UPDATE AnalyticsReportStudent SET milestones='" + mdxcomp + "', count=" + kmdxcomp
                                + " WHERE courseID=" + mysession.courseID
                                + " AND competenceID='" + competenceID + "' AND userID=" + iduser);
                        // Notify milestone
                        if (isItemSolved(cmxcourse, mdxcomp)) {
                            if (!this.DBhasBadge(-1, -1, -1, iduser, "competence", competenceID)) {
                                this.DBaddBadge(-1, -1, -1, iduser, "competence", competenceID);
                            }
                            // notify badge
                        }
                    }
                } else {
                    // Notify students the new achievement
                    this.DBUpdateAnalyticsNotification(mysession, "*|" + getLocalName() + "|" + mysession.whoName + "|*" + mysession.getKey() + "|" + "\n" + milestoneID + "@" + mysession.problemtitle + " ", true);
                }

                // Process group consequences
                // Process individual consequences
                // Problem solved
                if (isItemSolved(mysession.pmxcourse, mysession.mdxsession) && (milestoneID.equals("IGOAL04")
                        || milestoneID.equals("CGOAL06")
                        || milestoneID.equals("CGOAL07"))) { //&& !mysession.solved) {
                    if (!mysession.solved) {
                        mysession.tsolvedxsession = now;
                        mysession.solved = true;
//                    if (isItemSolved(mysession.pmxcourse, mysession.mdxcourse)) {
//                        earnedGroupBadges.add("problem");
//                    }
                        earnedGroupBadges.add("problem");
                        this.DBUpdateAnalyticsNotification(mysession, Emojis.PROBLEM + "*|" + getLocalName() + "|" + mysession.whoName + "|" + mysession.getKey() + "|" + "\nProblem " + mysession.problemtitle + " solved" + " ", true);
                        System(" Problem " + mysession.problemtitle + " is solved");

                        if (mysession.tsolvedxcourse == -1) {
                            mysession.tsolvedxcourse = mysession.tsolvedxsession;
                        }
                        mysession.latxsession = mysession.tsolvedxsession - mysession.topenxsession;
                        if (mysession.latxcourse == -1 || mysession.latxsession < mysession.latxcourse) {
                            mysession.latxcourse = mysession.latxsession;
                        }
                        if (mysession.costxcourse == -1 || mysession.costxcourse > mysession.costxsession) {
                            mysession.costxcourse = mysession.costxsession;
                        }
                    }
                    int totalrescued = mysession.benefitxsession / 1000, nowrescued = mysession.benefitxsession % 1000;
//                    if (mysession.benefitxcourse == -1 || mysession.benefitxcourse/1000 < totalrescued) {
                    mysession.benefitxcourse = totalrescued * 1000 + nowrescued;
//                    }
                    _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + mysession.mdxcourse + "', count=" + mysession.kmdxcourse
                            + ", firstopen=" + mysession.topenxcourse + ", firstsolved=" + mysession.tsolvedxcourse + ", latencysolved=" + mysession.latxcourse
                            + ", benefitsolved=" + mysession.benefitxcourse + ", costsolved=" + mysession.costxcourse
                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
                    _dataBase.updateDB("UPDATE LearningAnalytics SET date  = '" + new TimeHandler().toString() + "' "
                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);

                    if (mysession.tsolvedxbest < 0 || mysession.tsolvedxsession < mysession.tsolvedxbest) {
                        mysession.tsolvedxbest = mysession.tsolvedxcourse;
                        earnedGroupBadges.add("earlybird");
                    }
                    if (mysession.latxbest < 0 || mysession.latxsession < mysession.latxbest) {
                        mysession.latxbest = mysession.latxsession;
                        earnedGroupBadges.add("quickest");
                    }
                    if (mysession.costxbest < 0 || mysession.costxsession < mysession.costxbest) {
                        mysession.costxbest = mysession.costxsession;
                        earnedGroupBadges.add("shortest");
                    }
                    if (mysession.benefitxbest < 0 || mysession.benefitxsession > mysession.benefitxbest) {
                        mysession.benefitxbest = mysession.benefitxsession;
                        earnedGroupBadges.add("striker");
                    }
//            if (benefitsolved >= 0 && benefitsolved == bestbenefitsolved) {
//                earnedBadges.add("striker");
//            }
                } else { // New milestone but does not complete a  problem yet
                    _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + mysession.mdxcourse + "', count=" + mysession.kmdxcourse
                            + ", firstopen=" + mysession.topenxcourse + ", firstsolved=" + mysession.tsolvedxcourse + ", latencysolved=" + mysession.latxcourse
                            + ", benefitsolved=" + mysession.benefitxcourse + ", costsolved=" + mysession.costxcourse
                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
                    _dataBase.updateDB("UPDATE LearningAnalytics SET date  = '" + new TimeHandler().toString() + "' "
                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);

//                    _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + mysession.mdxcourse + "', count=" + mysession.kmdxcourse
//                            + ", firstopen=" + mysession.topenxcourse + ", firstsolved=" + mysession.tsolvedxcourse + ", latencysolved=" + mysession.latxcourse
//                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//                    _dataBase.updateDB("UPDATE LearningAnalytics SET date  = '" + new TimeHandler().toString() + "' "
//                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
                }
                _dataBase.endCommit();

                // Propagate to individual reports
                for (int iduser : mysession.usersInvolved) {
                    // Check assignment earned badges
                    for (String b : earnedGroupBadges) {
                        if (!this.DBhasBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, iduser, b, "")) {
                            this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, iduser, b, "");
                        }
                        // HERE Notify badges
                    }
                }
                // Propagate to group badges
                if (!this.DBisIndividual(mysession.assignmentID)) {
                    for (String b : earnedGroupBadges) {
                        if (!this.DBhasBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, mysession.whoID, b, "")) {
                            this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, mysession.whoID, b, "");
                        }
                        // notify
                    }
                }
            } else {
                // Notify students the new achievement

//                Info("Milestone " + milestoneID + "@" + mysession.problemtitle + " was formerly achieved this session. Ignore.");
            }
        } catch (Exception ex) {
            _dataBase.rollBack();
            Exception("", ex);
        }

    }

    //
    // LARVA Notifications
    //
    protected boolean checkPublicTelegram() {

        // Check known notifiers
        int notifiers = 0;
        for (String name : publicnotifierlist) {
            if (this.isConnected(name)) {
                notifiers++;
            }
        }
        // If no known notifier is connected, check for new notifiers registered
        if (notifiers == 0) {
            publicnotifierlist = new ArrayList();
            notifiers = 0;
            ArrayList<String> notifaux = this.DBgetAllServiceProviders("NOTIFIER");
            for (String name : notifaux) {
                if (this.isConnected(name)) {
                    publicnotifierlist.add(name);
                }
            }
        }
        // If none exist then return false
        return !publicnotifierlist.isEmpty();
    }

    protected boolean checkTeacherTelegram() {
        // Check known notifiers
        int notifiers = 0;
        for (String name : this.teachertelegram) {
            if (this.isConnected(name)) {
                notifiers++;
            }
        }
        // If no known notifier is connected, check for new notifiers registered
        if (notifiers == 0) {
            this.teachertelegram = new ArrayList();
            notifiers = 0;
            ArrayList<String> notifaux = this.DBgetAllServiceProviders("NOTIFIER");
            for (String name : notifaux) {
                if (this.isConnected(name)) {
                    this.teachertelegram.add(name);
                }
            }
        }
        // If none exist then return false
        return !teachertelegram.isEmpty();
    }

    protected void TeacherTelegramNotification(ROOTTELEGRAM type, String what) {
        String sep = "&@";
//        if (this.lasttelegramwarning.equals(type.toString() + sep + what)) {
//            this.nlasttelegramwarning++;
//            return;
//        }
//        if (this.nlasttelegramwarning > 0) {
//            TeacherTelegramNotification(type, "Last message repeated " + this.nlasttelegramwarning + " times");
//        }
        if (!this.checkTeacherTelegram()) {
            System("Teacher telegram is down");
            return;
        }
        ACLMessage notifier = new ACLMessage();
        teachertelegram.forEach(name -> {
            notifier.addReceiver(new AID(name, AID.ISLOCALNAME));
        });
        notifier.setSender(getAID());
        notifier.setPerformative(ACLMessage.REQUEST);
        notifier.setProtocol("REGULAR");
        notifier.setConversationId("" + ROOTUSERID);
        String date = new TimeHandler().toString();
        JsonObject jsreport = new JsonObject();
        jsreport.add("report", "agentreport").
                add("agentreport", new JsonObject().
                        add("date", date).
                        add("type", type.toString().toLowerCase()).
                        add("agent", this.getName()).
                        add("what", what));
        notifier.setContent(jsreport.toString());
        this.send(notifier);
//        this.pushACLM(notifier);
        this.lasttelegramwarning = type.toString() + sep + what;
        this.nlasttelegramwarning = 0;
    }

    protected void GeneralTelegramNotification(String what) {
        ACLMessage notifier;
        /*if (!this.checkPublicTelegram()) {
            System("Public telegram is down");
//            this.TeacherTelegramNotification(ROOTTELEGRAM.ERROR, "Public telegram seems to be down.\n" + Emojis.UPRIGHTARROW + " Redirecting\n"+what);
            return;
        }*/
        notifier = new ACLMessage();
        publicnotifierlist.forEach(name -> {
            notifier.addReceiver(new AID(name, AID.ISLOCALNAME));
        });
        notifier.setSender(getAID());
        notifier.setPerformative(ACLMessage.REQUEST);
        notifier.setProtocol("REGULAR");
        ArrayList<Integer> usersinvolved = new ArrayList();
        if (this._analytics != null) {
            usersinvolved = this.DBgetGroupMates(_analytics._groupID);
        }
        String convid = "";
        for (int uid : usersinvolved) {
            convid += uid + " ";
        }
        notifier.setConversationId(convid);
        String date = new TimeHandler().toString();
        JsonObject jsreport = new JsonObject();
        jsreport.add("report", "broadcast")
                .add("what", what);
        notifier.setContent(jsreport.toString());
        this.send(notifier);
//        this.pushACLM(notifier);

    }

//    protected void EmergencyTelegramNotification(ACLMessage msg, String what, boolean nodupes) {
//        ACLMessage notifier;
//        String convid = "";
//        Session mysession = this.openEmergencySession(msg);
//        notifier = new ACLMessage();
//
////        if (mysession != null) {
////            if (nodupes && mysession.telegrammessages.contains(what)) {
////                return;
////            }
////            if (this.nlasttelegramwarning > 0) {
////                what = "The previous message repeated x" + this.nlasttelegramwarning + " times\n" + what;
////            }
////            mysession.telegrammessages.add(what);
////            JsonArray ui = new JsonArray();
////            for (int uid : mysession.usersInvolved) {
////                ui.add(uid);
////                convid += uid + " ";
////            }
////        } else {
////            convid = "-1"; // Broadcast
////        }
////        publicnotifierlist.forEach(name -> {
////            notifier.addReceiver(new AID(name, AID.ISLOCALNAME));
////        });
////        notifier.addReceiver(msg.getSender());
////        notifier.setSender(getAID());
////        notifier.setPerformative(ACLMessage.REQUEST);
////        notifier.setProtocol("REGULAR");
////        notifier.setConversationId(convid);
////        String date = new TimeHandler().toString();
////        JsonObject jsreport = new JsonObject();
////        jsreport.add("report", "analyticsreport").
////                add("analyticsreport", new JsonObject().
////                        add("date", date).
////                        add("what", what));
////        notifier.setContent(jsreport.toString());
////        this.pushACLM(notifier);
//    }
    protected void DBUpdateAnalyticsNotification(Session mysession, String what, boolean nodupes) {
        ACLMessage notifier;
        notifier = new ACLMessage();
        if (nodupes && mysession.telegrammessages.contains(what)) {
            return;
        }
        if (!this.checkPublicTelegram()) {
            System("Public telegram is down");
//            this.TeacherTelegramNotification(ROOTTELEGRAM.ERROR, "Public telegram seems to be down.\n" + Emojis.UPRIGHTARROW + " Redirecting\n"+what);
            return;
        }
        mysession.telegrammessages.add(what);
        publicnotifierlist.forEach(name -> {
            notifier.addReceiver(new AID(name, AID.ISLOCALNAME));
        });
        String convid = "";
        JsonArray ui = new JsonArray();
        for (int uid : mysession.usersInvolved) {
            ui.add(uid);
            convid += uid + " ";
        }
        notifier.setSender(getAID());
        notifier.setPerformative(ACLMessage.REQUEST);
        notifier.setProtocol("REGULAR");
        notifier.setConversationId(convid);
        String date = new TimeHandler().toString();
        JsonObject jsreport = new JsonObject();
        jsreport.add("report", "analyticsreport").
                add("analyticsreport", new JsonObject().
                        add("date", date).
                        add("courseID", mysession.courseID).
                        add("assignmentID", mysession.assignmentID).
                        add("isIndividual", mysession.isIndividual).
                        add("problemID", mysession.problemID).
                        add("problemtitle", mysession.problemtitle).
                        add("whoID", mysession.whoID).
                        add("usersInvolved", ui).
                        add("what", what));
        notifier.setContent(jsreport.toString());
        this.send(notifier);
//        this.pushACLM(notifier);

    }

//    protected void DBregenerateAnalyticsReports(int courseID) {
//        JsonArray analytics = _dataBase.getAllRows("LearningAnalytics", "courseID", courseID);
//        boolean isIndividual;
//        for (JsonValue record : analytics) {
//            this.DBprocessAnalyticsReport(courseID, record.asObject().get("assignmentID").asInt(),
//                    record.asObject().get("problemID").asInt(),
//                    record.asObject().get("milestoneID").asString(),
//                    record.asObject().get("whoID").asInt());
//        }
//    }
    protected void DBnotifyAnalytics(ACLMessage msg, String type, String cause) {
        int userID;
        Session currentSession;

        userID = _dataBase.getColumnInt("Agents", "userID", "name", msg.getSender().getLocalName());
        if (userID < 0) { //|| !DBisUserRegistered(userID, this._analytics.courseID)) {
            return;
        }
        type = type.toLowerCase();
        currentSession = sessionListbyUser.get(userID);
        if (currentSession == null || currentSession.skipanalytics) {
            return;
        }
        if (type.toUpperCase().equals("ERROR")) {
            Error(cause);
            if (_notifyback) {
                notifyError(msg, ACLMessage.FAILURE, cause);
            }
        }
        this.DBaddAnalyticsNotification(currentSession,
                new JsonObject().add(type, new TimeHandler().toString() + " " + cause));
    }

    protected void DBnotifyGeneral(ACLMessage msg, String cause) {
        Error(cause);
        if (_notifyback) {
            notifyError(msg, ACLMessage.FAILURE, cause);
        }
        _dataBase.updateDB("INSERT INTO GeneralNotifications SET date=" + new TimeHandler().elapsedTimeSecs()
                + ", notification='" + cause + "'");

    }

    protected void DBcleanGeneralNotifications() {
        long now = new TimeHandler().elapsedTimeSecs() - 48 * 3600;
        _dataBase.updateDB("DELETE FROM GeneralNotifications WHERE date < " + now);

    }

    protected long DBgetLastOpenSession(int assignmentID, int problemID, int whoID) {
        long res = -1;
        JsonResult jq = _dataBase.queryJsonDB("SELECT * FROM LearningAnalytics WHERE assignmentID="
                + assignmentID + " AND problemID=" + problemID + " AND whoID=" + whoID + " AND milestoneID='INFORMA'");
        if (jq.size() > 0) {
            JsonObject row = jq.getRowByIndex(0);
            JsonArray notifs = Json.parse(row.get("notification").asString()).asArray();
            for (JsonValue n : notifs) {
                if (n.asObject().get("connect") != null) {
                    res = new TimeHandler(n.asObject().get("connect").asString()).elapsedTimeSecs();
                }
            }
        }
        return res;
    }

//    protected boolean DBaddAnalyticsRecord(int assignmentID, int problemID, String milestoneID, int userID) {
//        int groupID= DBgetGroup(userID);
//        
//        _dataBase.queryDB("SELECT * FROM LearninAnalytics WHERE assignmentID="+assignmentID+ 
//                " AND problemID="+problemID+
//                " AND milestoneID='"+milestoneID+ 
//                " AND userID="+userID);
//        if (_dataBase.getJsonResult().get("resultset").asArray().size()==0) {
//            _dataBase.updateDB("INSERT INTO LearningAnalytics (assignmentID, problemID, milestoneID, userID, date) "+
//                    " VALUES ("+assignmentID+", "+problemID+", "+"'"+milestoneID+"' ."+", "+userID+", '"+ new TimeHandler().toString()+"')");
//            return true;
//        }
//        return false;
//    }
    //
    // SERVICES
    //
    protected boolean DBaddExclusiveService(AID who, String service) {
        boolean res = true;

        System("Request to register agent " + who.getLocalName() + " as an EXCLUSIVE service provider for " + service);
        int agentID = this.DBgetAgentID(who);
        if (agentID < 0) {
            Error("Request to register agent " + who.getLocalName() + " as service provider for " + service + " FAILED");
            return false;
        }
        try {
            System("Removing all former service providers of " + service);
            _dataBase.updateDB("DELETE FROM AgentServices WHERE service='" + service + "'");
            return this.DBaddService(who, service);
        } catch (Exception ex) {
            Exception("", ex);
            return false;
        }
    }

    protected boolean DBaddService(AID who, String service) {
        boolean res = true;

//
        System("Request to register agent " + who.getLocalName() + " as service provider for " + service);
        int agentID = this.DBgetAgentID(who);
        if (agentID < 0) {
            Error("Request to register agent " + who.getLocalName() + " as service provider for " + service + " FAILED");
            return false;
        }
        try {
            _dataBase.updateDB("INSERT INTO AgentServices SET agentID=" + agentID + ", service='" + service + "'");
        } catch (Exception ex) {
            Exception("", ex);
            return false;
        }
        return true;
    }

    protected boolean DBremoveService(AID who, String service) {
        boolean res = true;

        System("Request to unregister agent " + who.getLocalName() + " as service provider for " + service);
        int agentID = this.DBgetAgentID(who);
        if (agentID < 0) {
            Error("Request to unregister agent " + who.getLocalName() + " as service provider for " + service + " FAILED");
            return false;
        }
        ArrayList<String> myservices = this.DBgetAllAgentServices(who);
        try {
            if (service.equals("")) {
                _dataBase.updateDB("DELETE FROM AgentServices WHERE agentID=" + agentID);
            } else {
                if (!myservices.contains(service)) {
                    return false;
                }
                _dataBase.updateDB("DELETE FROM AgentServices WHERE agentID=" + agentID + " AND service='" + service + "'");
            }
        } catch (Exception ex) {
            Exception("", ex);
            return false;
        }
        return true;
    }

    protected ArrayList<String> DBgetAllAgentServices(AID who) {
        ArrayList<String> res = new ArrayList<>();
        try {
            int agentID = this.DBgetAgentID(who);
            if (agentID < 0) {
                return res;
            }
            JsonResult jsrservices = _dataBase.queryJsonDB("SELECT * FROM AgentServices WHERE agentID=" + agentID);
            for (int i = 0; i < jsrservices.size(); i++) {
                res.add(jsrservices.getRowByIndex(i).getString("service", "unknown"));
            }
        } catch (Exception ex) {
            Exception("", ex);
            res.clear();
        }
        return res;
    }

    protected ArrayList<String> DBgetAllServiceProviders(String service) {
        ArrayList<String> res = new ArrayList<>();
        try {
            JsonResult jsrservices = _dataBase.queryJsonDB("SELECT * FROM AgentServices WHERE service='" + service + "'");
            for (int i = 0; i < jsrservices.size(); i++) {
                int agentID = jsrservices.getRowByIndex(i).getInt("agentID", BADRECORD);
                String agentName = this.DBgetAgentName(agentID);
                if (this.isConnected(agentName)) {
                    res.add(agentName);
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
            res.clear();
        }
        return res;

    }

    protected ArrayList<String> DBgetSimilarServiceProviders(String service) {
        ArrayList<String> res = new ArrayList<>();
        try {
            JsonResult jsrproviders = _dataBase.queryJsonDB("SELECT * FROM AgentServices");
            for (int i = 0; i < jsrproviders.size(); i++) {
                int agentID = jsrproviders.getRowByIndex(i).getInt("agentID", BADRECORD);
                String agentName = this.DBgetAgentName(agentID);
                if (this.isConnected(agentName)) {
                    for (String service2 : this.DBgetAllAgentServices(new AID(agentName, AID.ISLOCALNAME))) {
                        if (service2.matches("^.*" + service + ".*$")) {
                            res.add(agentName);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
            res.clear();
        }
        return res;
    }

    protected void DBcleanUpServiceProviders() {
        ArrayList<String> res = new ArrayList<>();
        try {
            JsonResult jsrservices = _dataBase.queryJsonDB("SELECT * FROM AgentServices");
            System("Purge flawed service providers ");
            int i = 0;
            for (; i < jsrservices.size();) {
                int agentID = jsrservices.getRowByIndex(i).getInt("agentID", BADRECORD);
                String agentName = this.DBgetAgentName(agentID);
                if (agentName.equals("") || !this.isConnected(agentName)) {
                    System("Purge stale service provider " + agentID + " " + (agentName.equals("") ? "" : agentName));
                    _dataBase.updateDB("DELETE FROM AgentServices where agentID=" + agentID);
                    i = 0;
                    res.add(agentName);
                    jsrservices = _dataBase.queryJsonDB("SELECT * FROM AgentServices");
                } else {
                    i++;
                }
            }
            System("Purge stale agents");
            jsrservices = _dataBase.queryJsonDB("SELECT * FROM Agents");
            i = 0;
            for (; i < jsrservices.size();) {
                int agentID = jsrservices.getRowByIndex(i).getInt("agentID", BADRECORD);
                String agentName = this.DBgetAgentName(agentID);
                if (agentName.equals("") || !this.isConnected(agentName)) {
                    System("Purge stale agent " + agentID + " " + (agentName.equals("") ? "" : agentName));
                    _dataBase.updateDB("DELETE FROM Agents where agentID=" + agentID);
                    i = 0;
                    res.add(agentName);
                    jsrservices = _dataBase.queryJsonDB("SELECT * FROM Agents");
                } else {
                    i++;
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
        }

    }

    //
    // Periodic events
    //
    protected void doPeriodicCheck() {
        doValidateDB();
        this.TeacherTelegramNotification(ROOTTELEGRAM.HEARTBEAT, "Alive!");
    }

    private void doValidateDB() {
        System("DataBase validation query");
        if (!_dataBase.canContinue()) {
            System("Flushing the following errors: " + _dataBase.whichError());
            _dataBase.flushError();
        }
        _dataBase.validationQuery();

    }
    //
    // Agent registration, deregistration
    //
    //
    // Upper level
    //
//    public static enum DFOPERATION {
//        REGISTER, DEREGISTER, QUERY
//    };

    public boolean isConnected(String agentName) {
        return getAllActiveAgents().contains(agentName);
    }

    public ArrayList<String> getAllActiveAgents() {
        ArrayList<String> res = new ArrayList<>();
        AMSAgentDescription list[];
        list = this.doAMSQuery("");
        for (AMSAgentDescription list1 : list) {
            res.add(list1.getName().getLocalName());
        }
        return res;
    }

//    public boolean hasService(String agentName, String service) {
//        return getAllServiceAgents(service).contains(agentName);
//    }
//
//    public ArrayList<String> getAllServicesProvided(String agentName) {
//        ArrayList<String> res = new ArrayList<>();
//        DFAgentDescription list[];
//        list = this.doServiceDF(DFOPERATION.QUERY, agentName, "");
//        if (list != null && list.length > 0) {
//            Iterator sdi = list[0].getAllServices();
//            while (sdi.hasNext()) {
//                ServiceDescription sd = (ServiceDescription) sdi.next();
//                res.add(sd.getType());
//            }
//        }
//        return res;
//    }
//    public ArrayList<String> getAllServiceAgents(String service) {
//        ArrayList<String> res = new ArrayList<>();
//        DFAgentDescription list[];
//        list = this.doServiceDF(DFOPERATION.QUERY, "", service);
//        for (DFAgentDescription list1 : list) {
//            res.add(list1.getName().getLocalName());
//        }
//        return res;
//    }
    //
    // Lower level
    //
//    protected DFAgentDescription[] doServiceDF(DFOPERATION op, String agentname, String type) {
//        DFAgentDescription res[] = null;
//
//        DFAgentDescriptionDFAgentDescription dfd = new DFAgentDescription(), olddfd[];
//        ServiceDescription sd, oldsd;
//
//        try {
//            switch (op) {
//                case REGISTER:
//                    String services = "";
//                    if (agentname.equals("")) {
//                        agentname = getAID().getLocalName();
//                    }
//                    dfd.setName(getAID()); //new AID(agentname, AID.ISLOCALNAME));
//                    olddfd = doServiceDF(DFOPERATION.QUERY, agentname, "");
//                    if (olddfd != null && olddfd.length > 0) {
//                        doServiceDF(DFOPERATION.DEREGISTER, "", "");
//                        Iterator proti = olddfd[0].getAllProtocols();
//                        while (proti.hasNext()) {
//                            String aux = (String) proti.next();
//                            dfd.addProtocols(aux);
//                        }
//                        Iterator sdi = olddfd[0].getAllServices();
//                        while (sdi.hasNext()) {
//                            ServiceDescription aux = (ServiceDescription) sdi.next();
//                            dfd.addServices(aux);
//                            services += aux.getType() + ", ";
//                        }
//                    }
//                    sd = new ServiceDescription();
//                    sd.setName(agentname);
//                    sd.setType(type);
//                    services += type;
//                    dfd.addServices(sd);
//                    DFService.register(this, dfd);
//                    String dfnames = "",
//                     amsnames = "";
//                    for (DFAgentDescription i : doServiceDF(DFOPERATION.QUERY, "", "")) {
//                        dfnames += i.getName().getLocalName() + ", ";
//                    }
//                    for (AMSAgentDescription i : doAMSQuery("")) {
//                        amsnames += i.getName().getLocalName() + ", ";
//                    }
//                    System("Agent " + agentname + " registered for services " + services);
////                    System("Known agents: " + amsnames);
////                    System("Registered agents: " + dfnames);
//                    break;
//                case DEREGISTER:
//                    olddfd = doServiceDF(DFOPERATION.QUERY, agentname, "");
//                    if (agentname.equals("")) {
//                        agentname = getAID().getLocalName();
//                    }
//                    olddfd = doServiceDF(DFOPERATION.QUERY, agentname, "");
//                    if (olddfd.length < 1)
//                        return new DFAgentDescription [0];
//                    dfd.setName(new AID(agentname, AID.ISLOCALNAME));
//                    if (type.equals("")) {
//                        System("Agent " + agentname + " deregistered for all services ");
//                    } else {
//                        sd = new ServiceDescription();
//                        sd.setName(agentname);
//                        sd.setType(type);
//                        dfd.addServices(sd);
//                        System("Agent " + agentname + " deregistered for service " + type);
//                    }
//                    DFService.deregister(this, dfd);
//                    break;
//                case QUERY:
//                    SearchConstraints c = new SearchConstraints();
//                    c.setMaxResults((long) -1);
//                    if (!agentname.equals("")) {
//                        dfd.setName(new AID(agentname, AID.ISLOCALNAME));
//                    }
//                    sd = new ServiceDescription();
//                    if (!type.equals("")) {
//                        sd.setType(type);
//                    }
//                    dfd.addServices(sd);
//                    res = DFService.search(this, dfd, c);
//                    break;
//            }
//        } catch (Exception ex) {
//            this.Exception(ex);
//        }
//
//        return res;
//    }
    protected AMSAgentDescription[] doAMSQuery(String agentname) {
        AMSAgentDescription amsd = new AMSAgentDescription(), amsdlist[] = null;
        if (!agentname.equals("")) {
            amsd.setName(new AID(agentname, AID.ISLOCALNAME));
        }
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long) -1);
        try {
            amsdlist = AMSService.search(this, amsd, sc);
        } catch (FIPAException ex) {
        }
        return amsdlist;
    }

    //
    // XUI
    //
    protected void ServiceXUI(ACLMessage xuimessage) {

    }

    //
    // ANALYTICS
    //
    protected void ServiceAnalytics(ACLMessage analyticsmessage) {

    }

    protected String addMilestone(String milestones, String milestone) {
        if (!findMilestone(milestones, milestone)) {
            milestones += milestone;
        }
        return milestones;
    }

    protected boolean findMilestone(String milestones, String milestone) {
        return milestones.contains(milestone);
    }

    protected int countMilestones(String milestones) {
        return milestones.length() / 7;
    }

    protected String getMilestone(String milestones, int index) {
        String res = "";
        if (0 <= index && index < countMilestones(milestones)) {
            res = milestones.substring(7 * index, Math.min(7 * (index + 1), milestones.length()));
        }
        return res;
    }

    protected boolean isItemSolved(String milestonestodo, String mymilestones) {
        boolean res = true;
        if (findMilestone(mymilestones, "CGOAL06")) {
            return res;
        }
        for (int i = 0; i < this.countMilestones(milestonestodo) && res; i++) {
            res = res && this.findMilestone(mymilestones, this.getMilestone(milestonestodo, i));
        }
        return res;
    }

}

//    protected void DBaddAnalyticsReport(Session mysession, String milestoneID, JsonObject info) {
//        JsonArray prev = new JsonArray();
////        if (_analytics == null || mysession.skipanalytics) {
////            return;
////        }
//        if (!_analytics.isValidMilestoneReport(milestoneID, mysession)) {
//            return;
//        }
//        System("Analytics received"
//                + "   assignmentID: " + mysession.assignmentID
//                + "   problemID: " + mysession.problemID
//                + "   milestoneID: " + milestoneID
//                + "  who: " + mysession.whoID);
//
//        JsonResult jq = _dataBase.queryJsonDB("SELECT * FROM LearningAnalytics WHERE assignmentID=" + mysession.assignmentID
//                + " AND problemID=" + mysession.problemID
//                + " AND milestoneID='" + milestoneID + "'"
//                + " AND whoID=" + mysession.whoID);
//        // Adds only the first occurrence
//        try {
//            _dataBase.startCommit();
//            if (jq.size() == 0) {
//                _dataBase.updateDB("INSERT INTO LearningAnalytics (courseID, assignmentID, problemID, milestoneID, whoID, date) "
//                        + " VALUES (" + mysession.courseID + "," + mysession.assignmentID + ", " + mysession.problemID + ", '" + milestoneID + "' , " + mysession.whoID + ", '" + new TimeHandler().toString() + "')");
//            }
//            // Propagetes the changes
//            this.DBprocessAnalyticsReport(mysession, milestoneID);
//            _dataBase.endCommit();
//        } catch (Exception ex) {
//            _dataBase.rollBack();
//            Exception(ex);
//        }
//
//    }
//
//    // Propagate the update to the report tables reportAssignment, reportStudent & Badges
//    protected void DBprocessAnalyticsReport(Session mysession, String milestoneID) {
//        JsonArray res, users, group;
//        String milestonesdone, competencemilestones = "";
//        int countmilestonesdone = 0;
//        long firstsolved, latencysolved, lastopen = 0, lastsolved, firstlatencysolved, costsolved, benefitsolved, firstopen, aux, now;
//        long bestfirstsolved, bestlatencysolved, bestcostsolved, bestbenefitsolved;
//        ArrayList<String> earnedBadges = new ArrayList<>();
//        JsonResult jqcompetences;
//        boolean twice = false;
//
//        // Get all previous information to update        
//        _dataBase.queryDB("SELECT * FROM AnalyticsReportAssignment WHERE assignmentID="
//                + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//        res = _dataBase.getJsonResult().get("resultset").asArray();
//        if (res.size() > 0) {
//            milestonesdone = res.get(0).asObject().getString("milestones", "");
//            firstopen = res.get(0).asObject().get("firstopen").asInt();
//            firstsolved = res.get(0).asObject().get("firstsolved").asInt();
//            firstlatencysolved = res.get(0).asObject().get("latencysolved").asInt();
//            costsolved = res.get(0).asObject().get("costsolved").asInt();
//            benefitsolved = res.get(0).asObject().get("benefitsolved").asInt();
//        } else {
//            milestonesdone = "";
//            firstopen = -1;
//            firstsolved = -1;
//            firstlatencysolved = -1;
//            costsolved = -1;
//            benefitsolved = -1;
//        }
//        // Register the time of the last connection
//        now = new TimeHandler().elapsedTimeSecs();
//        if (milestoneID.equals(this.getMilestone(mysession.pmxcourse, 0))) {
////            topenxsession = this.DBgetLastOpenSession(mysession.assignmentID, mysession.problemID, mysession.whoID);
////            aux = topenxsession;
//            mysession.topenxsession = now;
//            if (firstopen == -1) {
//                firstopen = mysession.topenxsession;
//            }
//        }
//        String competenceID = milestoneID.substring(0, 5);
//        competencemilestones = _dataBase.queryJsonDB("SELECT * FROM Competences WHERE competenceID='" + competenceID + "'").getRowByIndex(0).getString("milestones", "UNKNOWN");
//
//        if (!this.findMilestone(milestonesdone, milestoneID)) {
//            milestonesdone = this.addMilestone(milestonesdone, milestoneID);
//            countmilestonesdone = this.countMilestones(milestonesdone);
//
//            // Update new values in assignment report
//            _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + milestonesdone + "', count=" + countmilestonesdone
//                    + ", firstopen=" + firstopen + ", firstsolved=" + firstsolved + ", latencysolved=" + firstlatencysolved
//                    + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//            this.DBUpdateAnalyticsNotification(mysession, Emojis.MILESTONE + " " + milestoneID, true);
//        }
//        if (!this.findMilestone(mysession.mdxsession, milestoneID)) {
//            mysession.mdxsession = this.addMilestone(mysession.mdxsession, milestoneID);
//            mysession.kmdxsession = this.countMilestones(mysession.mdxsession);
//        }
//
////Check badges 
//        // If the problem is solved, then calculates statistics and badges, either individually or in group
//        // First regarding the problem being solved
//        if (isItemSolved(mysession.pmxcourse, mysession.mdxsession) && !mysession.solved) {
//            this.DBUpdateAnalyticsNotification(mysession, Emojis.PROBLEM + " Problem " + mysession.problemtitle + " is solved", true);
//            mysession.solved = true;
//            mysession.tsolvedxsession = now;
//            earnedBadges.add("problem");
//            if (firstsolved == -1) {
//                firstsolved = mysession.tsolvedxsession;
//
//            }
//            latencysolved = mysession.tsolvedxsession - mysession.topenxsession;
//            if (firstlatencysolved == -1 || latencysolved < firstlatencysolved) {
//                firstlatencysolved = latencysolved;
//            }
//            if (costsolved == -1 || costsolved > mysession.costxsession) {
//                costsolved = mysession.costxsession;
//            }
//            _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + milestonesdone + "', count=" + countmilestonesdone
//                    + ", firstopen=" + firstopen + ", firstsolved=" + firstsolved + ", latencysolved=" + firstlatencysolved + ", costsolved=" + costsolved
//                    + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//            bestfirstsolved = DBlowestValue(mysession.courseID, mysession.assignmentID, mysession.problemID, "firstsolved");
//            bestlatencysolved = DBlowestValue(mysession.courseID, mysession.assignmentID, mysession.problemID, "latencysolved");
//            bestcostsolved = DBlowestValue(mysession.courseID, mysession.assignmentID, mysession.problemID, "costsolved");
//            bestbenefitsolved = DBhighestValue(mysession.courseID, mysession.assignmentID, mysession.problemID, "benefitsolved");
//
//            if (firstsolved >= 0 && firstsolved == bestfirstsolved) {
//                earnedBadges.add("earlybird");
//            }
//            if (latencysolved >= 0 && latencysolved == bestlatencysolved) {
//                earnedBadges.add("quickest");
//
//            }
//            if (costsolved >= 0 && costsolved == bestcostsolved) {
//                earnedBadges.add("shortest");
//            }
////            if (benefitsolved >= 0 && benefitsolved == bestbenefitsolved) {
////                earnedBadges.add("striker");
////            }
//        }
//        // Propagate to individual reports
//        for (int iduser : mysession.usersInvolved) {
//
//            // Check new milestone regarding personal competences
//            _dataBase.queryDB("SELECT * FROM AnalyticsReportStudent WHERE courseID=" + mysession.courseID
//                    + " AND competenceID='" + competenceID + "' AND userID=" + iduser);
//
//            res = _dataBase.getJsonResult().get("resultset").asArray();
//            milestonesdone = res.get(0).asObject().get("milestones").asString();
//            milestonesdone = addMilestone(milestonesdone, milestoneID);
//            countmilestonesdone = this.countMilestones(milestonesdone);
//            _dataBase.updateDB("UPDATE AnalyticsReportStudent SET milestones='" + milestonesdone + "', count=" + countmilestonesdone
//                    + " WHERE courseID=" + mysession.courseID
//                    + " AND competenceID='" + competenceID + "' AND userID=" + iduser);
//            // Notify milestone
//            if (isItemSolved(competencemilestones, milestonesdone)) {
//                this.DBaddBadge(-1, -1, -1, iduser, "competence", competenceID);
//                // notify badge
//            }
//
//            // Check assignment earned badges
//            for (String b : earnedBadges) {
//                this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, iduser, b, "");
//                // HERE Notify badges
//            }
//        }
////        // Propagate to group badges
//        if (!this.DBisIndividual(mysession.assignmentID)) {
//            for (String b : earnedBadges) {
//                this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, mysession.whoID, b, "");
//                // notify
//            }
//        }
//    }
//    protected void DBaddAnalyticsReport(Session mysession, String milestoneID, JsonObject info) {
//        long now;
//        ArrayList<String> earnedBadges = new ArrayList<>();
//        String cmxcourse;
//
//        // If not a milestone of the current problem, it is rejected
//        if (!this.findMilestone(mysession.pmxcourse, milestoneID)) {
//            return;
//        }
//        // Register the arrival of the milestone
//        now = new TimeHandler().elapsedTimeSecs();
//        System("Analytics received"
//                + "   assignmentID: " + mysession.assignmentID
//                + "   problemID: " + mysession.problemID
//                + "   milestoneID: " + milestoneID
//                + "  who: " + mysession.whoID);
//
//        // Register the milestone in the current session
//        if (!this.findMilestone(mysession.mdxsession, milestoneID)) {
//            mysession.mdxsession = this.addMilestone(mysession.mdxsession, milestoneID);
//            mysession.kmdxsession++;
//
////                if ((mysession.pmxcourse.length()==mysession.mdxsession.length() || isItemSolved(mysession.pmxcourse, mysession.mdxsession))
//
//        }
//        
//        // It is a newly achieved milestone
//        if (!this.findMilestone(mysession.mdxcourse, milestoneID)) {
//            // Add the milestone to the already achieved
//            mysession.mdxcourse = this.addMilestone(mysession.mdxcourse, milestoneID);
//            mysession.kmdxcourse++;
//            // Notify students the new achievement
//            this.DBUpdateAnalyticsNotification(mysession, Emojis.MILESTONE + " " + milestoneID, true);
//
//            // Check potentially done competences afterwards
//            String competenceID = milestoneID.substring(0, 5);
//            cmxcourse = _dataBase.queryJsonDB("SELECT * FROM Competences WHERE competenceID='" + competenceID + "'").getRowByIndex(0).getString("milestones", "UNKNOWN");
//
//            // This is the first to arrive: opening the problem
//            if (mysession.mdxcourse.equals("")) {
//                mysession.topenxsession = now;
//                if (mysession.topenxcourse == -1) {
//                    mysession.topenxcourse = mysession.topenxsession;
//                }
//            }
//
//            try {
//                _dataBase.startCommit();
//                // Process group consequences
//                // First save in the database
//                _dataBase.updateDB("INSERT INTO LearningAnalytics (courseID, assignmentID, problemID, milestoneID, whoID, date) "
//                        + " VALUES (" + mysession.courseID + "," + mysession.assignmentID + ", " + mysession.problemID + ", '" + milestoneID + "' , " + mysession.whoID + ", '" + new TimeHandler().toString() + "')");
//
//                // Process individual consequences
//                // Problem solved
//                if (isItemSolved(mysession.pmxcourse, mysession.mdxsession)  && !mysession.solved) {
//                    this.DBUpdateAnalyticsNotification(mysession, Emojis.PROBLEM + " Problem " + mysession.problemtitle + " is solved", true);
//                    mysession.solved = true;
//                    mysession.tsolvedxsession = now;
//                    earnedBadges.add("problem");
//                    if (mysession.tsolvedxcourse == -1) {
//                        mysession.tsolvedxcourse = mysession.tsolvedxsession;
//
//                    }
//                    mysession.latxsession = mysession.tsolvedxsession - mysession.topenxsession;
//                    if (mysession.latxcourse == -1 || mysession.latxsession < mysession.latxcourse) {
//                        mysession.latxcourse = mysession.latxsession;
//                    }
//                    if (mysession.costxcourse == -1 || mysession.costxcourse > mysession.costxsession) {
//                        mysession.costxcourse = mysession.costxsession;
//                    }
//                    _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + mysession.mdxcourse + "', count=" + mysession.kmdxcourse
//                            + ", firstopen=" + mysession.topenxcourse + ", firstsolved=" + mysession.tsolvedxcourse + ", latencysolved=" + mysession.latxcourse + ", costsolved=" + mysession.costxsession
//                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//
//                    if (mysession.tsolvedxbest < 0 || mysession.tsolvedxsession < mysession.tsolvedxbest) {
//                        mysession.tsolvedxbest = mysession.tsolvedxcourse;
//                        earnedBadges.add("earlybird");
//                    }
//                    if (mysession.latxbest < 0 || mysession.latxsession < mysession.latxbest) {
//                        mysession.latxbest = mysession.latxsession;
//                        earnedBadges.add("quickest");
//                    }
//                    if (mysession.costxbest < 0 && mysession.costxsession < mysession.costxbest) {
//                        mysession.costxbest = mysession.costxsession;
//                        earnedBadges.add("shortest");
//                    }
////            if (benefitsolved >= 0 && benefitsolved == bestbenefitsolved) {
////                earnedBadges.add("striker");
////            }
//                } else { // New milestone but does not complete a  problem yet
//                    _dataBase.updateDB("UPDATE AnalyticsReportAssignment SET milestones='" + mysession.mdxcourse + "', count=" + mysession.kmdxcourse
//                            + ", firstopen=" + mysession.topenxcourse + ", firstsolved=" + mysession.tsolvedxcourse + ", latencysolved=" + mysession.latxcourse
//                            + " WHERE assignmentID=" + mysession.assignmentID + " AND problemID=" + mysession.problemID + " AND whoID=" + mysession.whoID);
//
//                }
//                _dataBase.endCommit();
//            } catch (Exception ex) {
//                _dataBase.rollBack();
//                Exception(ex);
//            }
//            // Propagate to individual reports
//            for (int iduser : mysession.usersInvolved) {
//
//                // Check new milestone regarding personal competences
//                _dataBase.queryDB("SELECT * FROM AnalyticsReportStudent WHERE courseID=" + mysession.courseID
//                        + " AND competenceID='" + competenceID + "' AND userID=" + iduser);
//
//                JsonArray res = _dataBase.getJsonResult().get("resultset").asArray();
//                String mdxcomp = res.get(0).asObject().get("milestones").asString();
//                mdxcomp = addMilestone(mdxcomp, milestoneID);
//                int kmdxcomp = this.countMilestones(mdxcomp);
//                _dataBase.updateDB("UPDATE AnalyticsReportStudent SET milestones='" + mdxcomp + "', count=" + kmdxcomp
//                        + " WHERE courseID=" + mysession.courseID
//                        + " AND competenceID='" + competenceID + "' AND userID=" + iduser);
//                // Notify milestone
//                if (isItemSolved(cmxcourse, mdxcomp)) {
//                    this.DBaddBadge(-1, -1, -1, iduser, "competence", competenceID);
//                    // notify badge
//                }
//
//                // Check assignment earned badges
//                for (String b : earnedBadges) {
//                    this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, iduser, b, "");
//                    // HERE Notify badges
//                }
//            }
////        // Propagate to group badges
//            if (!this.DBisIndividual(mysession.assignmentID)) {
//                for (String b : earnedBadges) {
//                    this.DBaddBadge(mysession.courseID, mysession.assignmentID, mysession.problemID, mysession.whoID, b, "");
//                    // notify
//                }
//            }
//
//        }
//    }
////
