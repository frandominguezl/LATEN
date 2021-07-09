package publictelegram;

import AppBoot.ConsoleBoot;


public class main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("AgentTelegram", args);
        app.selectConnection();
        app.launchAgent(PublicTelegram.class);
        app.shutDown();
    }
}
