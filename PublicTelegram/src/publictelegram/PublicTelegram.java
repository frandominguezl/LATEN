/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package publictelegram;

import AdminKeys.AdminCardID;
import FileUtils.FileUtils;
import TelegramStuff.Emojis;
import basetelegram.BaseTelegram;
import basetelegram.TelegramChat;
import basetelegram.TelegramUpdates;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.File;
import java.util.ArrayList;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 *
 * @author lcv
 */
public class PublicTelegram extends BaseTelegram {

    @Override
    public void setup() {
        super.setup();
        System.out.println("This is agent " + getLocalName() + " DEBUG:" + isDebug);
        telegramQueue = new TelegramUpdates();
        this.thecard = new AdminCardID(_enigma.getCryptoKey());
        if (!isDebug) {
            for (String chatfile : FileUtils.listFiles("./downloads/", "cardID")) {
                long cid = 0;
                try {
                    cid = Long.parseLong(chatfile.replace(".cardID", ""));
                    if (!this.loadCardID(cid)) {
                        throw new Exception("Bad cardID");
                    }
                    telegramQueue.initChatID(cid);
                    Info("Found chatID " + cid + " with " + thecard.getName());
                    TelegramChat tc = telegramQueue.getChatData(cid);
                    this.setChatCredentials(cid);
                } catch (Exception ex) {
                    Exception(ex);
                    this.deleteCardID(cid);
                }
            }
        }

        this._exitRequested = false;
        hello = " Hello, this is agent " + this.getBotName() + ". Type /help for more information ";
        startTelegram();
        _exitRequested = false;
        this.DBaddService(getAID(), "NOTIFIER");
        wakeUP();
    }

    @Override
    protected String getBotName() {
        return "LATEN";
    }

    @Override
    protected String getBotToken() {
        return "1731008990:AAFq_ffMDFSKM5yJw1867BDHgLuCG2yyC2o";
    }

    @Override
    protected void processTelegramUpdate(long cid, Update u) {
        if (!isDebug || cid == this.LCastilloCID) {
            TelegramChat tc = this.telegramQueue.getChatData(cid);
            File cardfile;
            if (u.getMessage().hasText()) {
                String line = u.getMessage().getText(), parameters[] = line.split(" ");
                switch (parameters[0]) {
                    case "/whoami":
                        this.sendTelegram(cid, "This chat currently belongs to " + tc.getUsername());
                        break;
                    case "/subscribe":
                        this.doSubscribe(cid);
                        break;
                    case "/link":
                        this.doLink(cid);
                        break;
                    case "/unlink":
                        this.doUnlink(cid);
                        break;
                    case "/cancel":
                        doCancel(cid);

                        break;
                    case "/query":
                        doQuery(cid, line.replace(parameters[0], "").trim());
                        break;
                    case "/services":

                        doServices(cid, line.replace(parameters[0], "").trim());

                        break;
                    case "/coin":
                        doCoin(cid, parameters);
                        break;
                    case "/exam":
                        if (tc.isTeacher()) {
                            doExam(cid, line.replace(parameters[0], "").trim());
                        } else {
                            doExam(cid, "");
                        }
                        break;
                    case "/redirect":
                        if (tc.isTeacher() || this.isTeacher(cid)) {
                            this.doRedirect(cid);
                        }
                        break;
                    case "/broadcast":
                        if (tc.isTeacher()) {
                            this.doBroadcast(u.getMessage().getText().replace("/broadcast", ""));
                        } else {
                            this.sendTelegram(cid, Emojis.NOENTRY + " Sorry, only teachers are allowed to use this command");
                        }
                        break;
                    case "/help":
                        this.sendTelegram(cid, help(cid));
                        break;
                    default:
                        this.sendTelegram(cid, hello);

                }
            } else if (u.getMessage().hasDocument()) {
                if (storeCardID(cid, u)) {
                    this.sendTelegram(cid, "This cardID belongs to " + newline() + mono("Name: " + thecard.getName() + newline()
                            + (thecard.getAlias().length() > 1 ? "alias: " + thecard.getAlias() + newline() : "")
                            + "email: " + thecard.getEmail()));
                    this.sendTelegram(cid, "Click /link to link this identity to this chat");
                } else {
                    this.sendTelegram(cid, "Unrecognizable cardID. Please use the cardID provided by the teacher");
                }
            } else {
                this.sendTelegram(cid, hello);
            }
        } else {
            this.sendTelegram(cid, " Sorry, bot under maintenance operations. I will inform you later when I am back to service.");
        }
    }

    //
    // Main options
    //
    //
    // ADMIN
    //
    @Override
    protected void processInternalUpdate(ACLMessage msg) {
        ArrayList<Long> usersinvolved = new ArrayList();
        String what, convid = "";
        JsonObject jsreport;
//        if (this.internalUpdates.isEmpty()) {
//            return;
//        }
//        ACLMessage msg = internalUpdates.get(0);
//        internalUpdates.remove(0);
        try {
            if (msg.getConversationId() == null) {
                return;
            }
            if (msg.getConversationId().length() > 0) {
                for (String ui : msg.getConversationId().trim().split(" ")) {
                    usersinvolved.add(telegramQueue.getChatIDUser(Integer.parseInt(ui)));
                }
            }
            switch (msg.getPerformative()) {
                case ACLMessage.REQUEST: // push to inform about the state of a problem or to inform the teacher
                    what = "";
                    jsreport = Json.parse(msg.getContent()).asObject();
                    if (jsreport.getString("report", "").equals("analyticsreport")) { // Inform students about their evolution
                        jsreport = jsreport.get(jsreport.getString("report", "")).asObject();
                        int assignmentID = jsreport.getInt("assignmentID", -1), whoID = jsreport.getInt("whoID", -1), userID, groupID;
                        String problemTitle = jsreport.getString("problemtitle", "");
                        what = jsreport.getString("what", "");
                        Boolean isIndividual = jsreport.getBoolean("isIndividual", false);
                        // Extract chatID of affected persons
                        if (whoID < 0) {
                            return;
                        }
                        // If rediirect, include also the teacher
                        if (allowRedirections) {
                            usersinvolved.add(LCastilloCID);
                        }
                        // if subscribed, then send update;
                        for (long cid : usersinvolved) {
                            if (telegramQueue.getChatData(cid) != null && (telegramQueue.getChatData(cid).isSubscribed() || telegramQueue.getChatData(cid).isTeacher())) {
                                this.sendTelegram(cid, "analyticesreport", what, parsemode);
                            }
                        }
                    }
                    if (jsreport.getString("report", "").equals("broadcast")) { // Boradcast required
                        what = jsreport.getString("what", "");
                        if (!usersinvolved.isEmpty()) {// Class Broadcast 
                            // Group broadcast
                            if (allowRedirections) {
                                usersinvolved.add(LCastilloCID);
                            }
                            for (long cid : usersinvolved) {
                                if (telegramQueue.getChatData(cid) != null && (telegramQueue.getChatData(cid).isSubscribed() || telegramQueue.getChatData(cid).isTeacher())) {
                                    this.sendTelegram(cid, "analyticesreport", what, parsemode);
                                }
                            }
                        }
                    }
                    break;
                case ACLMessage.INFORM:
                try {
                    JsonObject jsanswer = Json.parse(msg.getContent()).asObject();
                    String state = jsanswer.getString(jsanswer.getString("report", "none"), "");
                    if (allowRedirections) {
                        usersinvolved.add(LCastilloCID);
                    }
                    for (long cid : usersinvolved) {
                        this.sendTelegram(cid, "statusreport", Emojis.ALIVE + mono(" " + msg.getSender().getLocalName()), parsemode);
                    }
                } catch (Exception ex) {
                    Exception("", ex);
                }
                break;
            }
        } catch (Exception ex) {
            Exception("", ex);
            this.sendTelegram(this.LCastilloCID, Emojis.WARNING + " " + getLocalName() + newline() + "I had an unexpected Exception"
                    + newline() + ex.toString());
        }
    }
}
