package laten;

import TelegramStuff.Emojis;

import AdminAgent.AdminAgent;

import com.eclipsesource.json.*;
import jade.core.AID;
import java.io.IOException;
import jade.lang.acl.ACLMessage;
import java.io.File;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

import java.sql.ResultSet;

public class LATEN extends AdminAgent {
    
    ArrayList<String> goalMilestones = new ArrayList<>();
    ArrayList<String> achievedNormalMilestones = new ArrayList<>();
    
    ArrayList<JsonObject> groupMembers = new ArrayList<>();
    
    String conversationIDs = "";
    
    String sessionFile = "Test_log.json";
    String P2Log = "CaixaBank_P2log.json";
    String P3Log = "CaixaBank_P3log.json";
    String testLog = "Test_log.json";
    
    String group;
    
    @Override
    public void setup() {
        super.setup();
        
        goalMilestones.add("IGOAL01");
        goalMilestones.add("IGOAL02");
        goalMilestones.add("IGOAL03");
        goalMilestones.add("IGOAL04");
        
        _exitRequested = false;
    }
    
    @Override
    public void plainExecute() {
        try {
            File ficheroSesiones = new File(sessionFile);
            Scanner myReader = new Scanner(ficheroSesiones);
            
            // Consider different type of notifications (all, just the minimum, only ACL Messages, none)
            while(this.blockingReceive() != null) {
                String linea = this.blockingReceive().getContent();
                
                Info(linea);
                
                if (this.groupMembers.isEmpty()) {
                    this.fillGroupMembers(linea);
                    
                    // Set the conversations IDs
                    this.setConvIDs();
                }
                
                JsonObject jsonLine;
                
                try {
                    jsonLine = Json.parse(linea).asObject();
                } catch (Exception e) {
                    Info(e.toString());
                    continue;
                }
                
                String info;
                
                if (linea.contains("acl_receive_REGULAR") && linea.contains("\"command\":\"login\"")) {
                    // This is a notification marked as ALL
                    String newConvIDs = this.buildNotificationString("ALL");                
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.ENVELOPE + " Sending login request...", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Login request from")) {
                    info = jsonLine.get("record").asObject().get("value").asObject().get("content").asObject().get("what").asString();
                    
                    // This is a notification marked as ALL
                    String newConvIDs = this.buildNotificationString("ALL");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", info, newConvIDs);
                    
                } else if (linea.contains("Analytics received")) {
                    info = jsonLine.get("record").asObject().get("info").asString();
                    
                    String[] split = info.trim().split("\\s+");
                    String milestoneID = split[7];
                    String message;
                    
                    String newConvIDs = "";
                    
                    // If we already have achieved this one, don't send the message at all
                    if (achievedNormalMilestones.contains(milestoneID)) {
                        continue;
                    }
                    
                    // Send it if it's a GOAL or an unachieved normal one
                    if (goalMilestones.contains(milestoneID)) {
                        message = Emojis.CHEQFLAG + " Goal " + milestoneID + " achieved!";
                        
                        // Should be notified always
                        newConvIDs = this.buildNotificationString("ALL MIN");
                    } else {
                        achievedNormalMilestones.add(milestoneID);
                                              
                        message = Emojis.OK + " Achieved milestone " + milestoneID;
                        
                        // Should be notified if ALL or MIN
                        newConvIDs = this.buildNotificationString("ALL");
                    }
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", message, newConvIDs);
                    
                } else if (linea.contains("BAD CardID")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Bad CardID: No CardID found", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && !linea.contains("error") && linea.contains("Unable to load world")) {
                    info = jsonLine.get("record").asObject().get("value").asObject().get("content").asObject().get("what").asString();  

                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", info, newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Unable to load world") && linea.contains("error")) {
                    info = jsonLine.get("record").asObject().get("value").asObject().get("content").asObject().get("details").asString();
                    
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + info, newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Bad ConversationId")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Bad ConversationID", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Crash onto the ground")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Crash onto the ground", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Crash onto world's boundaries")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Crash onto world's boundaries", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Energy exhausted")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Energy exhausted", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Agent is dead")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Agent is dead", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Flying too high")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Flying too high", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("Action  not within its capabilities")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " Action not within its capabilities", newConvIDs);
                    
                } else if (linea.contains("acl_send_REGULAR") && linea.contains("The key is absent or wrong")) {
                    // This is an error, should always notify 
                    String newConvIDs = this.buildNotificationString("ALL MIN");
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", Emojis.WARNING + " The key is absent or wrong", newConvIDs);
                    
                } else if (linea.contains("acl_receive_REGULAR")) {
                    JsonObject ACLObject = jsonLine.get("record").asObject().get("value").asObject();
                    
                    String agentName = ACLObject.get("sender").asString();
                    
                    int agentID = this.DBgetAgentID(new AID(agentName, AID.ISLOCALNAME));
                    
                    JsonObject usersNotifications = 
                        _dataBase.queryJsonDB("SELECT userID, notificationSettings FROM Users WHERE agentID=" + agentID).getRowByIndex(0);
                    
                    // If the owner of the agent doesn't have the ACL settings, just continue
                    if (!usersNotifications.get("notificationSettings").asString().equals("ACL"))
                        continue;
                    
                    String performative = ACLObject.get("performative").asString();
                    String action, message = "";
                    
                    if (linea.contains("\"command\":\"execute\"")) {
                        action = ACLObject.get("content").asObject().get("action").asString();
                        message = Emojis.WARNING + " Received ACL message with performative "
                            + performative + " and action " + action;
                    } else if (linea.contains("\"command\":\"read\"")) {
                        message = Emojis.WARNING + " Reading sensors...";
                    }                    
                    
                    // This will notify the owner of the agent if he has the ACL notification mode
                    String newConvIDs = Integer.toString(usersNotifications.get("userID").asInt());
                    
                    sendMessage("PTelegram", ACLMessage.AGREE, "REGULAR", message, newConvIDs);
                }
                
                // Wait some time before sending the next one, we don't want to collapse the bot
                this.blockingReceive(200);
            }
            
            this._exitRequested = true;
            
            myReader.close();
        }   catch (IOException ex) {
            Logger.getLogger(LATEN.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Sends a message
     * @param receiver Message's receiver
     * @param performative The performative to use
     * @param protocol Message's protocol
     * @param content Message's content
     * @param conversationID The conversation ID to send the message to
     */
    protected void sendMessage(String receiver, int performative, String protocol, String content, String conversationID) {        
        ACLMessage out = new ACLMessage();
        out.setSender(this.getAID());
        out.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        out.setPerformative(performative);
        out.setProtocol(protocol);
        out.setContent(content);

        if(conversationID != "") {
            out.setConversationId(conversationID);
        }

        this.send(out);
    }
    
    /**
     * Identifies the group this communication belongs to
     * @param firstLine The first line of the communication to evaluate
     * @return The group
     */
    protected String identifyGroup(String firstLine) {
        JsonObject jsonLine = Json.parse(firstLine).asObject();
        
        return jsonLine.get("record").asObject().get("agent").asString();
    }
    
    /**
     * Sets the conversation IDs according to the DB query and the notifications settings
     */
    protected void setConvIDs() {
        for (JsonObject groupMember : this.groupMembers) {
            this.conversationIDs = this.conversationIDs + " " + groupMember.get("userID");
        }
    }
    
    /**
     * Returns an updated list of conversation IDs to send the notifications
     * @param notification range
     * @return String containing the conversation IDs
     */
    protected String buildNotificationString(String notification) {
        String convIDs = "";
        
        for (JsonObject groupMember : this.groupMembers) {
            for (String type : notification.trim().split(" ")) {
                if (groupMember.get("notificationSettings").toString().replaceAll("\"", "").equals(type)) {
                    convIDs = convIDs + " " + groupMember.get("chatID").toString();
                }
            }
        }
        
        return convIDs;
    }
    
    /**
     * Fills the object of the group members
     * @param message The first message received
     */
    protected void fillGroupMembers(String message) {
        // Identify which group does this communication belong to
        this.group = this.identifyGroup(message);

        // Verify which members do we have to send the message to
        // First, obtain the groupID
        JsonObject groupID = _dataBase.queryJsonDB("SELECT groupID FROM Agents.Groups WHERE alias='" + this.group + "'").getRowByIndex(0);

        // Members belonging to that group
        JsonArray usersID = _dataBase.queryJsonDB("SELECT userID FROM GroupMembers WHERE groupID=" + groupID.get("groupID")).getAllRows();
        //ArrayList<Integer> usersID = this.DBgetGroupMates(groupID.get("groupID"));

        for(int i = 0; i < usersID.size(); i++) {
            this.groupMembers.add(usersID.get(i).asObject());
        }

        // Adds the notifications settings for each user
        for(JsonObject groupMember : this.groupMembers) {
            JsonObject usersNotifications = 
                    _dataBase.queryJsonDB("SELECT chatID, notificationSettings FROM Users WHERE userID=" + groupMember.get("userID")).getRowByIndex(0);

            groupMember.add("notificationSettings", usersNotifications.get("notificationSettings"));
            groupMember.add("chatID", usersNotifications.get("chatID"));
        }
    }
}
