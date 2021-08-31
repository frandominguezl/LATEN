/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package publictelegram;

import AppBoot.ConsoleBoot;
import java.util.Scanner;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

/**
 *
 * @author lcv
 */
public class main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        myTelegramBot tele;
//        ApiContextInitializer.init();
//
//        // Se crea un nuevo Bot API
//        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
//        BotSession mySession;
//        tele = new myTelegramBot(new TelegramUpdates());
//
//        try {
//            // Se registra el bot
//            mySession = telegramBotsApi.registerBot(tele);
//            System.out.println("Press to close");
//            String line = new Scanner(System.in).nextLine();
//            System.out.println("Closing");
//            mySession.stop();
//            System.out.println("Closed");
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
        ConsoleBoot app = new ConsoleBoot("AgentTelegram", args);
        app.selectConnection();
        app.launchAgent(PublicTelegram.class);
        app.shutDown();
    }
}
