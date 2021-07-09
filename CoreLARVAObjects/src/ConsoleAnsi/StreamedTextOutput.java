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
public class StreamedTextOutput {

    protected String stream;
    
    public static enum outputTo {
        ANSI, TELEGRAM, HTML
    };

    protected outputTo out;
    protected int BACKGR=black, FOREGR=gray;

    public StreamedTextOutput(outputTo output) {
        out = output;
        stream="";
    }

    public StreamedTextOutput(outputTo output, int bg, int fg) {
        out = output;
        BACKGR = bg;
        FOREGR = fg;
        stream="";
    }
    
    @Override
    public String toString(){
        String toreturn=stream;
        stream="";
        return toreturn;
    }

    public StreamedTextOutput under(String text) {
        
        switch (out) {
            case HTML:
                stream += "<b>" + text + "</b>";
                break;
            case TELEGRAM:
                stream += "_" + text + "_";
                break;
            case ANSI:
                stream += defText(yellow)+defBackground(black)+text+Normal();
                break;
        }
        return this;
    }

    public StreamedTextOutput bold(String text) {
        
        switch (out) {
            case HTML:
                stream += "<b>" + text + "</b>";
                break;
            case TELEGRAM:
                stream += "*" + text + "*";
                break;
            case ANSI:
                stream += defText(white)+defBackground(black)+text+Normal();
                break;
        }
        return this;
    }

    public StreamedTextOutput italic(String text) {
        
        switch (out) {
            case HTML:
                stream += "<i>" + text + "</i>";
                break;
            case TELEGRAM:
                stream += "_" + text + "_";
                break;
        }
        return this;
    }

    public StreamedTextOutput mono(String text) {
        
        switch (out) {
            case HTML:
                stream += "<tt>" + text + "</tt>";
                break;
            case TELEGRAM:
                stream += "`" + text + "`";
                break;
            case ANSI:
                stream += defText(black)+defBackground(green)+text+Normal();
        }
        return this;
    }

    public StreamedTextOutput newline() {
        
        switch (out) {
            case HTML:
                stream += "<p>";
                break;
            case TELEGRAM:
                stream += "\n";
                break;
            case ANSI:
                stream += "\n";
                break;
        }
        return this;
    }

    public StreamedTextOutput tab() {
        
        switch (out) {
            case HTML:
                stream += " &nbsp;  &nbsp;  &nbsp;  &nbsp;  &nbsp; ";
                break;
            case TELEGRAM:
                stream += "\t";
                break;
            case ANSI:
                stream += "\t";
                break;
        }
        return this;
    }
    
    public StreamedTextOutput OK() {
        
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                stream+= Emojis.OK;
                break;
            case ANSI:
                stream += defText(white)+defBackground(lightgreen)+Emojis.OK+Normal();
                break;
            
        }
        return this;
    }
    
    public StreamedTextOutput CANCEL() {
        
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                stream+= Emojis.CANCEL;
                break;
            case ANSI:
                stream += defText(white)+defBackground(lightred)+Emojis.CANCEL+Normal();
                break;
            
        }
        return this;
    }
    
    public StreamedTextOutput WARNING() {
        
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                stream+= Emojis.WARNING;
                break;
            case ANSI:
                stream += defText(black)+defBackground(yellow)+Emojis.WARNING+Normal();
                break;
            
        }
        return this;
    }
    
    public StreamedTextOutput INFO() {
        
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                stream+= Emojis.INFO;
                break;
            case ANSI:
                stream += defText(white)+defBackground(lightblue)+Emojis.INFO+Normal();
                break;
            
        }
        return this;
    }
    
    public StreamedTextOutput Normal() {
        switch (out) {
            case HTML:
                break;
            case TELEGRAM:
                break;
            case ANSI:
                stream += defText(FOREGR)+defBackground(BACKGR);
                break;
            
        }
        return this;
    }
    
    
  

}
