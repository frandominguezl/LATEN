/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ConsoleAnsi;

import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.defBackground;
import static ConsoleAnsi.ConsoleAnsi.defText;
import static ConsoleAnsi.ConsoleAnsi.gray;
import static ConsoleAnsi.ConsoleAnsi.green;
import static ConsoleAnsi.ConsoleAnsi.lightblue;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static ConsoleAnsi.ConsoleAnsi.lightred;
import static ConsoleAnsi.ConsoleAnsi.white;
import static ConsoleAnsi.ConsoleAnsi.yellow;
import TelegramStuff.Emojis;

/**
 *
 * @author lcv
 */
public class EnhancedTextOutput {

    public static enum outputTo {
        ANSI, TELEGRAM, HTML
    };

    protected outputTo out;
    protected int BACKGR=black, FOREGR=gray;

    public EnhancedTextOutput(outputTo output) {
        out = output;
    }

    public EnhancedTextOutput(outputTo output, int bg, int fg) {
        out = output;
        BACKGR = bg;
        FOREGR = fg;
    }

    public String under(String text) {
        String res = "";
        switch (out) {
            case HTML:
                res = "<b>" + text + "</b>";
                break;
            case TELEGRAM:
                res = "_" + text + "_";
                break;
            case ANSI:
                res += defText(yellow)+defBackground(black)+text+Normal();
                break;
        }
        return res;
    }

    public String bold(String text) {
        String res = "";
        switch (out) {
            case HTML:
                res = "<b>" + text + "</b>";
                break;
            case TELEGRAM:
                res = "*" + text + "*";
                break;
            case ANSI:
                res += defText(white)+defBackground(black)+text+Normal();
                break;
        }
        return res;
    }

    public String italic(String text) {
        String res = "";
        switch (out) {
            case HTML:
                res = "<i>" + text + "</i>";
                break;
            case TELEGRAM:
                res = "_" + text + "_";
                break;
        }
        return res;
    }

    public String mono(String text) {
        String res = "";
        switch (out) {
            case HTML:
                res = "<tt>" + text + "</tt>";
                break;
            case TELEGRAM:
                res = "`" + text + "`";
                break;
            case ANSI:
                res += defText(green)+defBackground(black)+text+Normal();
        }
        return res;
    }

    public String newline() {
        String res = "";
        switch (out) {
            case HTML:
                res = "<p>";
                break;
            case TELEGRAM:
                res = "\n";
                break;
            case ANSI:
                res = "\n";
                break;
        }
        return res;
    }

    public String tab() {
        String res = "";
        switch (out) {
            case HTML:
                res = " &nbsp;  &nbsp;  &nbsp;  &nbsp;  &nbsp; ";
                break;
            case TELEGRAM:
                res = "\t";
                break;
            case ANSI:
                res = "\t";
                break;
        }
        return res;
    }
    
    public String OK() {
        String res="";
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                res+= Emojis.OK;
                break;
            case ANSI:
                res += defText(white)+defBackground(lightgreen)+Emojis.OK+Normal();
                break;
            
        }
        return res;
    }
    
    public String CANCEL() {
        String res="";
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                res+= Emojis.CANCEL;
                break;
            case ANSI:
                res += defText(white)+defBackground(lightred)+Emojis.CANCEL+Normal();
                break;
            
        }
        return res;
    }
    
    public String WARNING() {
        String res="";
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                res+= Emojis.WARNING;
                break;
            case ANSI:
                res += defText(black)+defBackground(yellow)+Emojis.WARNING+Normal();
                break;
            
        }
        return res;
    }
    
    public String INFO() {
        String res="";
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                res+= Emojis.INFO;
                break;
            case ANSI:
                res += defText(white)+defBackground(lightblue)+Emojis.INFO+Normal();
                break;
            
        }
        return res;
    }
    
    public String Normal() {
        return Normal("");
    }
    
    public String Normal(String text) {
        String res="";
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                break;
            case ANSI:
                res += defText(FOREGR)+defBackground(BACKGR)+text;
                break;
            
        }
        return res;
    }
    
    
  

}
