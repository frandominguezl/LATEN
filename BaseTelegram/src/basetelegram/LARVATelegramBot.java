/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package basetelegram;

import TelegramStuff.Emojis;
import java.util.ArrayList;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 *
 * @author lcv
 */
public class LARVATelegramBot extends TelegramLongPollingBot {

    TelegramUpdates updateQueue;
    String name, token;

    public LARVATelegramBot(String name, String token, TelegramUpdates q) {
        super();
        updateQueue = q;
        this.name = name;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        final String messageTextReceived = update.getMessage().getText();
        final long chatId = update.getMessage().getChatId();
        updateQueue.pushUpdate(update);

    }

//    public void Answer(long chatID, String what) {
//        SendMessage message = new SendMessage().setChatId(chatID).setText(what);
//        try {
//            execute(message);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }
}
/*
BotFather
Done! Congratulations on your new bot. You will find it at t.me/LARVATelegramer_bot. You can now add a description, about section and profile picture for your bot, 
see /help for a list of commands. By the way, when you've finished creating your cool bot, ping our Bot Support if you want a better username for it. 
Just make sure the bot is fully operational before you do this.

Use this token to access the HTTP API:
1393186120:AAEnh1Pi00lG7oO1sd4okGoxNLenwoFaRUw
Keep your token secure and store it safely, it can be used by anyone to control your bot.

For a description of the Bot API, see this page: https://core.telegram.org/bots/api

 */

/*
        if (privateDBA) {
            return "1484821048:AAHAuoXdmcXFF21Gye_mpKlpEOd4fbrpFrs";
        } else {
            return "1393186120:AAEnh1Pi00lG7oO1sd4okGoxNLenwoFaRUw";
        }

*/