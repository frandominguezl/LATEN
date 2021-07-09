package laten;

import AppBoot.ConsoleBoot;

public class main {

    static ConsoleBoot _app;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        _app = new ConsoleBoot("LATEN", args);
        _app.selectConnection();
        _app.launchAgent(LATEN.class);
        _app.shutDown();
    }
    
}
