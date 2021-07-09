/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package basetelegram;

import static Database.AgentDataBase.BADRECORD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 *
 * @author lcv
 */
public class TelegramUpdates {

    HashMap<Long, TelegramChat> updateQueue;

    public TelegramUpdates() {
        updateQueue = new HashMap<>();
    }

    public void initChatID(long cid) {
        if (updateQueue.get(cid) == null)
            updateQueue.put(cid, new TelegramChat());
    }
    
    public Set<Long> getAllChatIDRegistered() {
        return updateQueue.keySet();
    }

    protected synchronized boolean isEmptyChatID(long chatID) {
        return (getChatIDPendingUpdates(chatID) == null || getChatIDPendingUpdates(chatID).isEmpty());
    }

    public synchronized boolean isEmptyUserID(int userID) {
        return (getUserIDPendingUpdates(userID) == null || getUserIDPendingUpdates(userID).isEmpty());
    }

    public synchronized boolean isAllEmpty() {
        boolean res = true;
        for (long cid : updateQueue.keySet()) {
            res = res && getChatIDPendingUpdates(cid).isEmpty();
        }
        return res;
    }

    public long getChatIDUser(int userID) {
        for (long cid : updateQueue.keySet()) {
            if (this.updateQueue.get(cid).getUserID() == userID) {
                return cid;
            }
        }
        return BADRECORD;
    }

    public List<Long> getAllChatIDActive() {
        List<Long> res = new ArrayList<Long>();
        for (long cid : updateQueue.keySet()) {
            if (!this.isEmptyChatID(cid)) {
                res.add(cid);
            }
        }
        return res;
    }

    public List<Long> getAllChatIDGroup(int groupID) {
        List<Long> res = new ArrayList<Long>();
        for (long cid : updateQueue.keySet()) {
            if (updateQueue.get(cid).getGroupID()==groupID) {
                res.add(cid);
            }
        }
        return res;
    }

    protected ArrayList<Update> getChatIDPendingUpdates(long chatID) {
        if (updateQueue.get(chatID) == null) {
            return null;
        }
        return updateQueue.get(chatID).getPendingUpdates();
    }

    protected ArrayList<Update> getUserIDPendingUpdates(int userID) {
        return this.getChatIDPendingUpdates(this.getChatIDUser(userID));
    }

    public synchronized void pushUpdate(Update update) {
        Long chatID = update.getMessage().getChatId();
        if (getChatIDPendingUpdates(chatID) == null) {
            updateQueue.put(chatID, new TelegramChat());
        }
        getChatIDPendingUpdates(chatID).add(update);
        //System.err.println("CHAT: " + chatID + " (" + this.getChatIDPendingUpdates(chatID).size() + ") ++ " + update.getMessage().getText());
    }

    public synchronized Update popUpdateChatID(long chatID) {
        Update res = null;
        if (!isEmptyChatID(chatID)) {
            res = getChatIDPendingUpdates(chatID).get(0);
            getChatIDPendingUpdates(chatID).remove(0);
//            System.err.println("CHAT: " + chatID + " (" + this.getChatIDPendingUpdates(chatID).size() + ") -- " + res.getMessage().getText());
        }
        return res;
    }

    public synchronized Update popUpdateUserID(int userID) {
        return this.popUpdateChatID(this.getChatIDUser(userID));

    }
    
    public TelegramChat getChatData(long cid) {
        return this.updateQueue.get(cid);
    }
}
