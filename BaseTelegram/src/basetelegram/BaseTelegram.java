/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package basetelegram;

import static ACLMessageTools.ACLMessageTools.isJsonACLM;
import TelegramStuff.Emojis;
import AdminAgent.AdminAgent;
import AdminKeys.AdminCardID;
import AdminKeys.AdminCryptor;
import AdminKeys.DBACoin;
import ConsoleAnsi.ConsoleAnsi;
import FileUtils.FileUtils;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.core.Profile;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.File;
import static java.lang.Math.abs;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

/**
 *
 * @author lcv
 * Solo reciben mensajes por el puerto REGULAR, tienen cerrado ADMIN y ANALYTICS
 */
public abstract class BaseTelegram extends AdminAgent {

    protected String parsemode = "Markdown", noparsemode = ""; // "Markdown" "HTML" "MarkdownV2"
    protected String status;
    protected int count = -1, maxcount = 20;
    protected LARVATelegramBot tele;
    protected TelegramUpdates telegramQueue;
    protected BotSession myTSession;
    protected ACLMessage inbox, outbox;
    protected ArrayList<ACLMessage> internalUpdates;
    protected String hello = "Hello, this is agent " + this.getBotName() + ". Type /help for more information ";
    protected HashMap<String, String> myPublicCommands;
    protected HashMap<Integer, Long> myGuests;
    protected AdminCardID thecard;
    protected boolean isPrivate, isDebug, allowRedirections = false;
    protected final long[] myTeacherCIDs = new long[]{324545938};
    protected final long LCastilloCID = myTeacherCIDs[0];

    @Override
    public void setup() {
        super.setup();
        this._exitRequested = false;

        telegramQueue = new TelegramUpdates();
        this.thecard = new AdminCardID(_enigma.getCryptoKey());
        for (String chatfile : FileUtils.listFiles("./downloads/", "cardID")) {
            long cid = 0;
            try {
                cid = Long.parseLong(chatfile.replace(".cardID", ""));
                this.loadCardID(cid);
                telegramQueue.initChatID(cid);
                Info("Found chatID " + cid + " with " + thecard.getName());
                this.setChatCredentials(cid);
            } catch (Exception ex) {
                Info("Found BAD chatID " + cid + " getting rid of it");
                this.deleteCardID(cid);
            }
        }
        internalUpdates = new ArrayList();
        myPublicCommands = new HashMap<>();
        myPublicCommands.put("/help", "\nShow this help");
        myPublicCommands.put("/link", "\nBind this chat to the previously uploadaed identity");
        myPublicCommands.put("/whoami", "\nChecks the last stored identity");
        myPublicCommands.put("/subscribe ", "\nActivate the reception of notifications about your progress in LARVA and those of your team mates");
        myPublicCommands.put("/cancel", "\nStop receiving notifications about milestones achieved by myself or my team");
        myPublicCommands.put("/query ", "\nSimple query about the status of LARVA");
        myPublicCommands.put("/services", "\nYellow Pages. Enumeration of  the agents who offer any service and the services they offer in LARVA");
        myPublicCommands.put("/coin", italic("coin") + "\n Check the fields of a given coin/label, show its owner and its validity in the session"
                + " they were created. A coin may be correctly encoded but it could also be deplated, due to a payment or due to its "
                + " use in the process");
        myPublicCommands.put("/exam", "\nAccess to exam template");
        myPublicCommands.put("/notifications", italic("ALL/MIN/NONE") + "\nEstablish the level of notifications you want to receive:\n"
                + "ALL: Receive all notification messages\n"
                + "MIN: Receive only the critical messages (errors and GOALs achievements)\n"
                + "NONE: You won't receive any notifications at all\n\n"
                + "By default this is equal to ALL. The usage of this command is, for example, /notifications ALL");
        myPublicCommands.put("/notificationstatus", "Check your current level of notifications");
        myPublicCommands.put("/myprogress", "Outputs your current progress for the whole subject");
        myPublicCommands.put("/problemprogress", "Outputs your current progress for a specific problem of an assignment");
        myPublicCommands.put("/assignmentprogress", "Outputs your current progress for all the problems of the specified assignment");
        _exitRequested = false;
        isDebug = this._config.getBoolean("isdebug", true);
        _enigma= new AdminCryptor("cardIDcryptor202");
    }

    @Override
    public void takeDown() {
        byeBye();
        super.takeDown();
    }

    @Override
    public final void plainExecute() {
        Update u; // --> Cola de mensajes de Telegram (Telegramchat)
        ACLMessage iu; // --> Cola de mensajes de LARVA blockingReceive()

        // Bot telegram tiene varios chats abiertos: uno por cada alumno identificado.
        
        // 1. Recibe de telegram un update y procesa un mensaje de Telegram por cada chat activo
        // Process one single update for every queue, to do a kind of leveled answer to all inbound requirements
        // Recibe de Telegram y envía a LARVA
        for (long cid : telegramQueue.getAllChatIDActive()) {
            u = telegramQueue.popUpdateChatID(cid);
            processTelegramUpdate(cid, u);
        }
        
        // 2. Recibe de LARVA updates de otros agentes (notificaciones)
        // recibe de LARVA --> envia a Telegram
        // *** WorldManager: progreso del alumno/grupo
        // IdentityManager
        // El ConversationID del ACLM lleva los CID de cada receptor separados por espacios
        this.processACLMUpdates();
        if (this.internalUpdates.size() > 0) {
            iu = this.internalUpdates.get(0);
            this.internalUpdates.remove(0);
            processInternalUpdate(iu);
        }
        if (telegramQueue.isAllEmpty() && this.internalUpdates.isEmpty()) {
            doBlock(500);
        }
    }

    //
    // Main options
    //
    protected void doRedirect(long cid) {
        allowRedirections = !allowRedirections;
        if (allowRedirections) {
            this.sendTelegram(cid, Emojis.GREENCIRCLE + " Redirections from PublicTelegram ON");
        } else {
            this.sendTelegram(cid, Emojis.BLACKCIRCLE + " Redirections from PublicTelegram OFF");
        }
    }

    protected void wakeUP() {
        String message = hello + newline() + Emojis.OK + " I am back to service.";
        if (true) {
            this.sendTelegram(LCastilloCID, message);
        } else {
            doBroadcast(message);
        }
    }

    protected void byeBye() {
        String message = Emojis.CANCEL + " This bot is going down for maintenance. Sorry for the inconvenience.";
        if (true) {
            this.sendTelegram(LCastilloCID, message);
        } else {
            doBroadcast(message);
        }
    }

    protected void doBroadcast(String message) {
        if (isDebug) {
            this.sendTelegram(LCastilloCID, message);
        } else {

            for (long mcid : telegramQueue.getAllChatIDRegistered()) { //.getAllChatIDActive()) {
                this.sendTelegram(mcid, Emojis.SPEAKER + " " + message);
            }
        }
    }

    protected void doLink(long cid) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        if (this.loadCardID(cid)) {
            this.DBsetUserChatID(thecard.getUserID(), cid);
            this.setChatCredentials(cid);
            this.sendTelegram(cid, "This chat has been safely authenticated in LARVA on behalf of " + thecard.getName());
        } else {
            this.sendTelegram(cid, "The provided identity is not valid");
        }
    }

    protected void doUnlink(long cid) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        if (this.loadCardID(cid)) {
            this.sendTelegram(cid, "This chat will not longer belong to " + tc.getUsername());
            // First removes subscriptions if any
            doCancel(cid);
            // Second delete de physical disk file cardID
            File todel = new File(this.getFilenameCardID(cid));
            todel.delete();
            // Remove credentials in RAM chats
            this.removeChatCredentials(cid);
            // Fixes database
            this.DBsetUserChatID(thecard.getUserID(), -1);
            _dataBase.updateDB("UPDATE Users SET chatID=-1 WHERE chatID=" + cid);
        } else {
            this.sendTelegram(cid, "The provided identity is not valid");
        }
    }

    protected void doSubscribe(long cid) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        try {
            _dataBase.updateDB("UPDATE Users SET invitedgroupID=" + tc.getGroupID() + " WHERE userID=" + tc.getUserID());
            this.sendTelegram(cid,
                    "Notifications [ON]" + newline() + "From now on, this chat will receive automatic"
                    + " updates of the evolution of students of group " + tc.getGroupname());

        } catch (Exception ex) {
            Exception("", ex);
        }
    }

    protected void doCancel(long cid) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        try {
            _dataBase.updateDB("UPDATE Users SET invitedgroupID=-1 WHERE userID=" + ROOTUSERID);
            this.sendTelegram(cid,
                    "Notifications [OFF]" + newline() + "From now on, this chat will receive automatic"
                    + " updates of the evolution of students of any ggroup");
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + "I could not  to this question due to an internal error. Please try later");
        }
    }

    protected void doExam(long cid, String msg) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        int userID;
        String docID;
        try {
            if (tc.isTeacher() && msg.trim().length()>0) {
                userID = Integer.parseInt(msg);
            } else {
                userID = tc.getUserID();
            }
            docID = this._dataBase.getColumnString("Exams", "docID", "userID", userID);
            if (docID.length()>0) {
                String oldpm= parsemode;
                parsemode="HTML";
               this.sendTelegram(cid, Emojis.LOGIN + "<a href=\"https://docs.google.com/presentation/d/"+docID+"\">Click to open the exam template</a>");              
               parsemode=oldpm;
            }
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + "I could not answer to this question due to an internal error. Please try later");
        }

    }

    protected void doQuery(long cid, String msg) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        ArrayList<String> agentnames = new ArrayList();
        try {
            if (tc.isTeacher()) {
                agentnames = this.DBgetAllServiceProviders("ADMIN");
            } else if (msg.length() > 1) { // query single agent
                agentnames.add(msg);
            } else {                    // query all admin agents
                agentnames = this.DBgetAllServiceProviders("Analytics group " + tc.getGroupname());
            }
            ACLMessage aclanswer;
            for (String name : agentnames) {
                if (name.contains("elegram")) {
                    continue;
                }
                aclanswer = this.queryLARVAAgent(cid, name, new JsonObject()
                        .add("performative", ACLMessage.getPerformative(ACLMessage.QUERY_REF))
                        .add("arguments", "status"));
                if (aclanswer != null) {
                    JsonObject jsanswer = Json.parse(aclanswer.getContent()).asObject();
                    String state = jsanswer.getString(jsanswer.getString("report", "none"), "");
                    this.sendTelegram(cid, "statusreport", Emojis.OK + mono(" " + name), parsemode);
                    //this.sendTelegram(cid, newline() + mono(aclanswer.getContent()));                
                } else {
                    this.sendTelegram(cid, Emojis.CANCEL + mono(" " + name));
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + "I could not  to this question due to an internal error. Please try later");
        }

    }

    protected void doServices(long cid, String msg) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        ArrayList<String> agentnames = new ArrayList();
        try {
            if (tc.isTeacher()) {
                agentnames = this.DBgetAllServiceProviders("ADMIN");
            } else if (msg.length() > 1) { // query single agent
                agentnames.add(msg);
            } else {                    // query all admin agents
                agentnames = this.DBgetAllServiceProviders("Analytics group " + tc.getGroupname());
            }
            ACLMessage aclanswer;
            for (String name : agentnames) {
                if (name.equals(this.getLocalName())) {
                    continue;
                }
                aclanswer = this.queryLARVAAgent(cid, name, new JsonObject()
                        .add("performative", ACLMessage.getPerformative(ACLMessage.QUERY_REF))
                        .add("arguments", "services"));
                if (aclanswer != null) {
                    JsonObject jsanswer = Json.parse(aclanswer.getContent()).asObject();
                    String state = "";
                    for (JsonValue s : jsanswer.get(jsanswer.getString("report", "")).asArray()) {
                        state += s.asString() + newline();
                    }
                    this.sendTelegram(cid, "statusreport", Emojis.HAMMERWRENCH + mono(name + newline() + state), parsemode);
                    //this.sendTelegram(cid, newline() + mono(aclanswer.getContent()));                
                } else {
                    this.sendTelegram(cid, Emojis.CANCEL + mono(" " + name));
                }
            }
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + "I could not  to this question due to an internal error. Please try later");
        }
    }
    
    protected void doSetNotifications(long cid, String type) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        
        try {
            _dataBase.updateDB("UPDATE Users SET notificationSettings='" + type + "' WHERE userID='" + tc.getUserID() + "'");
            this.sendTelegram(cid, Emojis.INFO + " Notifications set to " + type);
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + "I could not perform this task due to an internal error. Please try later");
        }
    }
    
    protected void doCheckNotificationsStatus(long cid) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        
        try {
            JsonObject usersNotifications = 
                        _dataBase.queryJsonDB("SELECT notificationSettings FROM Users WHERE chatID=" + cid).getRowByIndex(0);
            
            this.sendTelegram(cid, Emojis.INFO + " Your current notifications status is set to " + usersNotifications.get("notificationSettings"));
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + " I could not perform this task due to an internal error. Please try later");
        }
    }
    
    protected void doMyProgress(long cid) {
        
    }
    
    protected void doProblemProgress(long cid, int problemID) {
        try {
            TelegramChat tc = this.telegramQueue.getChatData(cid);
            
            // Gather the current milestone the user has of the problem
            JsonObject currentMilestones = 
                        _dataBase.queryJsonDB("SELECT milestones FROM AnalyticsReportAssignment WHERE whoID=" + tc.getUserID()
                                + " AND problemID=" + problemID).getRowByIndex(0);          
            
            // Count the current milestones
            int currentMilestonesCount = countMilestones(currentMilestones.get("milestones").asString());
            
            // Gather the problem's target milestones
            JsonObject targetMilestones = 
                        _dataBase.queryJsonDB("SELECT title, milestones FROM LATEN.Problems WHERE problemID=" + problemID).getRowByIndex(0);
            
            // Count the target milestones
            int targetMilestonesCount = countMilestones(targetMilestones.get("milestones").asString());
            
            String greenSquares = Emojis.GREENSQUARE.repeat(targetMilestonesCount);
            String blackSquares = Emojis.BLACKSQUARE.repeat(currentMilestonesCount);
            
            String message = "";
            
            if (currentMilestonesCount == targetMilestonesCount) {
                message = "Progress for problem " + targetMilestones.get("title") + ":\n\n" + greenSquares + "\n\n"
                        + "This problem is completed!";
            } else {
                blackSquares = Emojis.BLACKSQUARE.repeat(abs(targetMilestonesCount-currentMilestonesCount));
                message = "Progress for problem " + targetMilestones.get("title") + ":\n\n" + greenSquares + blackSquares + "\n\n"
                        + "You still have " + abs(targetMilestonesCount-currentMilestonesCount) + " more achievements to complete";
            }
            
            this.sendTelegram(cid, message);
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + " I could not perform this task due to an internal error. Please try later");
        }
    }
    
    protected void doAssignmentProgress(long cid, int assignmentID) {
        try {
            TelegramChat tc = this.telegramQueue.getChatData(cid);
            
            int whoID = -1;
            
            // We need to know if the assignment is individual or not
            JsonObject assignment = _dataBase.queryJsonDB("SELECT title, isIndividual FROM Assignments WHERE assignmentID=" + assignmentID).getRowByIndex(0);
            
            if (!assignment.get("isIndividual").asBoolean()) {
                whoID = tc.getGroupID();
            } else {
                whoID = tc.getUserID();
            }
            
            // Gather all the problems of the assignment
            JsonArray assignmentProblems = 
                        _dataBase.queryJsonDB("SELECT problemID FROM AnalyticsReportAssignment WHERE whoID=" + whoID
                                + " AND assignmentID=" + assignmentID).getAllRows();
            
            String message = "Report for " + assignment.get("title").asString() + "\n\n";
            
            for (JsonValue problem : assignmentProblems) {
                // Gather the current milestone the user has of the problem
                JsonObject currentMilestones = 
                        _dataBase.queryJsonDB("SELECT milestones FROM AnalyticsReportAssignment WHERE whoID=" + whoID
                                + " AND problemID=" + problem.asObject().get("problemID")).getRowByIndex(0);
                
                // Count the current milestones
                int currentMilestonesCount = countMilestones(currentMilestones.get("milestones").asString());
                
                // Gather the problem's target milestones
                JsonObject targetMilestones = 
                        _dataBase.queryJsonDB("SELECT title, milestones FROM LATEN.Problems WHERE problemID=" + problem.asObject().get("problemID")).getRowByIndex(0);
                
                // Count the target milestones
                int targetMilestonesCount = countMilestones(targetMilestones.get("milestones").asString());
                
                String greenSquares = Emojis.GREENSQUARE.repeat(targetMilestonesCount);
                String blackSquares = Emojis.BLACKSQUARE.repeat(currentMilestonesCount);
                
                if (currentMilestonesCount == targetMilestonesCount) {
                    message = message + "Problem: " + targetMilestones.get("title").asString().replace("\"", "") + " (" + currentMilestonesCount
                            + "/" + targetMilestonesCount + ")\n" + greenSquares;
                } else if (currentMilestonesCount == 0) {
                    blackSquares = Emojis.BLACKSQUARE.repeat(abs(targetMilestonesCount-currentMilestonesCount));
                    message = message + "Problem: " + targetMilestones.get("title").asString().replace("\"", "") + " (" + currentMilestonesCount
                            + "/" + targetMilestonesCount + ")\n" + blackSquares;
                } else {
                    blackSquares = Emojis.BLACKSQUARE.repeat(abs(targetMilestonesCount-currentMilestonesCount));
                    message = message + "Problem: " + targetMilestones.get("title").asString().replace("\"", "") + " (" + currentMilestonesCount
                            + "/" + targetMilestonesCount + ")\n" + greenSquares + blackSquares;
                }
                
                message += "\n\n";
            }
            
            this.sendTelegram(cid, message);
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + " I could not perform this task due to an internal error. Please try later");
        }
    }
    
    protected void doDueDate(long cid, int assignmentID) {
        TelegramChat tc = this.telegramQueue.getChatData(cid);
        
        try {
            JsonObject dueDate = _dataBase.queryJsonDB("SELECT title, dueDate FROM Assignments WHERE assignmentID=" + assignmentID).getRowByIndex(0);
            
            Calendar calendar = new GregorianCalendar();
            
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dueDate.get("dueDate").asString());
            
            calendar.setTime(date);
            
            // January is the month 0, so we have to do this...
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+1);
            
            this.sendTelegram(cid, Emojis.INFO + " The deadline for " + dueDate.get("title") + " is " + calendar.get(Calendar.DAY_OF_MONTH) + "/"
                    + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR));
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, Emojis.WARNING + " I could not perform this task due to an internal error. Please try later");
        }
    }
    
    //
    // ADMIN
    //

    //
    // Abstract methods to be instantiated in every bot
    //
    // Requests from Telegram users onto LARVA Agents
    protected abstract void processTelegramUpdate(long cid, Update u);

    // Request from LARVA Agents to Telegram users
    protected abstract void processInternalUpdate(ACLMessage aclu);

    // Botfather 
    protected abstract String getBotName();

    // Botfather
    protected abstract String getBotToken();

    //
    // 
    //
    protected void setChatCredentials(long cid) {
        TelegramChat tc = telegramQueue.getChatData(cid);

        if (tc == null) {
            return;
        }
        if (!tc.isValidChat()) {
            tc.setUserID(this.DBgetOwnerChatID(cid));
        }
        if (tc.isValidChat()) {
            JsonObject jsrq = _dataBase.queryJsonDB("SELECT * FROM Users WHERE userID=" + tc.getUserID()).getRowByIndex(0);
            tc.setUsername(jsrq.getString("name", "nonamed"));
            int invitedgroupID = jsrq.getInt("invitedgroupID", 0);
            tc.setTeacher(jsrq.getString("isTeacher", "0").equals("1"));
            if (tc.isTeacher()) {
                if (invitedgroupID > 0) {
                    tc.setSubscribed(true);
                    tc.setGroupID(invitedgroupID);
                    tc.setGroupname(this.DBgetGroupAlias(invitedgroupID));
                }
            } else {
                if (invitedgroupID > 0) {
                    tc.setSubscribed(true);
                }
                tc.setGroupID(this.DBgetGroup(tc.getUserID()));
                tc.setGroupname(this.DBgetGroupAliasUser(tc.getUserID()));
            }
        }
    }

    protected void removeChatCredentials(long cid) {
        TelegramChat tc = telegramQueue.getChatData(cid);
        tc.resetChat();
    }

    //
    // Utils
    //
    protected String getFilenameCardID(long cid) {
        return "./downloads/" + cid + ".cardID";
    }

    protected boolean loadCardID(long cid) {
        String filename = getFilenameCardID(cid);
        thecard.fromFile(filename);
        thecard.decodeCardIDNew();
        return true;
    }

    protected boolean storeCardID(long cid, Update u) {
        String fid = u.getMessage().getDocument().getFileId();
        GetFile gf = new GetFile().setFileId(fid);
        File fw = null;
        try {
            String filename = getFilenameCardID(cid);
            fw = new File(filename);
            String filepath = tele.execute(gf).getFilePath();
            File f = tele.downloadFile(filepath, fw);
            return loadCardID(cid);
        } catch (Exception ex) {
            Exception(ex);
            if (fw != null) {
                fw.delete();
            }
        }
        return false;
    }

    protected void deleteCardID(long cid) {
        File todel = new File(this.getFilenameCardID(cid));
        if (todel.exists()) {
            todel.delete();
        }
    }

    protected ACLMessage queryLARVAAgent(long cid, String agentname, JsonObject query) {
        ACLMessage out, res = null;
        try {
            if (this.telegramQueue.getChatData(cid).isValidChat()) {
                int userID = this.telegramQueue.getChatData(cid).getUserID();
                String conv = "" + userID, rw = query.toString();
                out = this.newAdminACLMessage("ADMIN");
                out.setSender(getAID());
                out.addReceiver(new AID(agentname, AID.ISLOCALNAME));
                String sperformative = query.getString("performative", ACLMessage.getPerformative(ACLMessage.NOT_UNDERSTOOD));
                out.setPerformative(ACLMessage.getInteger(sperformative));
                out.setContent(query.toString());
                out.setConversationId(conv);
                out.setReplyWith(rw);
                out.setLanguage("REGULAR");
                this.pushACLM(out);
                MessageTemplate t = MessageTemplate.and(MessageTemplate.MatchConversationId(conv), MessageTemplate.MatchInReplyTo(rw));
                res = blockingReceive(t, 3000);
            }
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(cid, "I could not answer to this question due to an internal error. Please try later");
        }
        return res;
    }

    //
    // Telegram API
    //
    protected void startTelegram() {
        ApiContextInitializer.init();

        // Se crea un nuevo Bot API
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        tele = new LARVATelegramBot(getBotName(), getBotToken(), telegramQueue);

        try {
            // Se registra el bot
            myTSession = telegramBotsApi.registerBot(tele);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    protected void endTelegram() {
        myTSession.stop();
    }

    protected void sendTelegram(long cid, String what) {
        this.sendTelegram(cid, "", what, parsemode);
    }

    protected void sendTelegram(long cid, String type, String what, String parsemode) {
        SendMessage message;
        try {
            if (parsemode.equals("MarkdownV2")) {
                what.replace(".", "\\.");
            }
            String pretext = "";
            if (isDebug) {
                pretext += Emojis.LOCKED;
            }
            if (isTeacher(cid)) {
                switch (type) {
                    case "analyticsreport":  // Messages from analytics
                        pretext += ""; //Emojis.BLUESQUARE + Emojis.BLUESQUARE + Emojis.BLUESQUARE;
                        break;
                    case "agentreport": // Popipng up agent report
                        pretext += "";
                        break;
                    case "shellreport": // Shell execution
                        pretext += ""; //Emojis.YELLOWSQUARE + Emojis.YELLOWSQUARE + Emojis.YELLOWSQUARE;
                        break;
                    case "statusreport": // Answer to questions
                    default:
                        pretext += ""; //Emojis.PURPLESQUARE + Emojis.PURPLESQUARE + Emojis.PURPLESQUARE;
                        break;
                }
            }
            if (parsemode.length() > 0) {
                message = new SendMessage().setChatId(cid).setParseMode(parsemode).setText(pretext + what);
            } else {
                message = new SendMessage().setChatId(cid).setText(pretext + what);
            }
            tele.execute(message);
        } catch (Exception Ex) {
            Exception(Ex);
        }
    }

//    protected void broadcastTelegram(String what) {
//        SendMessage message;
//        try {
//            if (this.parsemode.equals("MarkdownV2")) {
//                what.replace(".", "\\.");
//            }
//            for (long cid : this.telegramQueue.getAllChatIDRegistered()) {
//                message = new SendMessage().setChatId(cid).setParseMode(parsemode).setText(what);
//                tele.execute(message);
//            }
//        } catch (Exception Ex) {
//            Exception(Ex);
//        }
//    }
    protected void processACLMUpdates() {
        ACLMessage msg = null;
        boolean done = false;
        while (!done) {
            msg = this.blockingReceive(100);
            if (msg == null) {
                done = true;
            } else if (msg.getSender().getLocalName().equals("ams") /*|| !this.DBisAdminAgent(msg.getSender()) ||*/) {

            } else {
                Info("acl_receive_REGULAR " + ACLMessageTools.ACLMessageTools.toJsonACLM(msg).toString());
                this.internalUpdates.add(msg);
            }
        }
    }

    protected boolean isTeacher(long cid) {
        return (this.telegramQueue.getChatData(cid) != null && this.telegramQueue.getChatData(cid).isTeacher());
    }
    //
    // Common functions
    //

    protected void doCoin(long cid, String[] coins) {
        String res = "";
        DBACoin mc = new DBACoin();
        int i = 0;
        for (i = 1; i < coins.length; i++) {
            res = "";
            try {
                res = _enigma.deCryptNew(coins[i]);
                if (res.length() > 0) {
                    this.sendTelegram(cid, Emojis.GLOWINGSTAR + " " + res);
                } else {
                    mc.decodeCoin(coins[i]);
                    if (mc.isValid()) {
                        String agentName, agentOwner;
                        agentName = this.DBgetAgentName(mc.getOwner());
                        res += " The coin " + bold(coins[i]) + " has been generated #" + mc.getSerie() + " within the conversation SESSION#" + mc.getSession() + " for the agent " + agentName + "\n";
                        if (this.DBcheckProduct(mc.getSession(), coins[i])) {
                            res += Emojis.OK + " The coin is still valid in that session";
                        } else {
                            res += Emojis.WARNING + " The coin is no longer available in that session";
                        }
                        this.sendTelegram(cid, res);
                    } else {
                        this.sendTelegram(cid, Emojis.WARNING + " " + " The coin " + coins[i] + " seems to have been faked or corrupted");
                    }
                }

            } catch (Exception ex) {
//            Exception("", ex);
                this.sendTelegram(cid, Emojis.WARNING + " " + " The coin " + coins[i] + " seems to have been faked or corrupted");
            }
        }
    }

    // 
    // Help
    //
    protected String help(long cid) {
        String res = hello + newline();
        TelegramChat tc = this.telegramQueue.getChatData(cid);

        for (String command : myPublicCommands.keySet()) {
            res += command + "    " + myPublicCommands.get(command);
            res += newline();
        }

        return res;
    }

    //
    // Formatted output
    //
    protected String under(String text) {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = "<b>" + text + "</b>";
                break;
            default:
                res = "_" + text + "_";
        }
        return res;
    }

    protected String bold(String text) {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = "<b>" + text + "</b>";
                break;
            default:
                res = "*" + text + "*";
        }
        return res;
    }

    protected String italic(String text) {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = "<i>" + text + "</i>";
                break;
            default:
                res = "_" + text + "_";
        }
        return res;
    }

    protected String mono(String text) {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = "<tt>" + text + "</tt>";
                break;
            default:
                res = "`" + text + "`";
        }
        return res;
    }

    protected String newline() {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = "<p>";
                break;
            default:
                res = "\n";
        }
        return res;
    }

    protected String tab() {
        String res = "";
        switch (parsemode) {
            case "HTML":
                res = " &nbsp;  &nbsp;  &nbsp;  &nbsp;  &nbsp; ";
                break;
            default:
                res = "\t";
        }
        return res;
    }
}
