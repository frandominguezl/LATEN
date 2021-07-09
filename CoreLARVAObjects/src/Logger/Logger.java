package Logger;

import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.defText;
import static ConsoleAnsi.ConsoleAnsi.green;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static ConsoleAnsi.ConsoleAnsi.lightred;
import static ConsoleAnsi.ConsoleAnsi.red;
import static ConsoleAnsi.ConsoleAnsi.white;
import TimeHandler.TimeHandler;
import com.eclipsesource.json.JsonObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A general-purpose class for logging messages, errors and exceptions, both on
 * screen and on disk. It can be used from a general program or from an agent's
 * body. It admits as record a String or a JsonObject and it produces the
 * following log - On screen: the string or the JsonObject.toString() in the
 * specified color - On disk: every record on a single line on disk, encoded in
 * JSON, preceded by a timestamp - {"date":"24/06/2020_11:52:23","record":.....}
 * - In order to homogeneize Strings and JsonObjects, Strings are also stored as
 * JsonObjects with the key "info"
 *
 */
public class Logger {

    protected String _filename, /// Name of the file  to store the log on disk
            _default = "./.nolog.log", /// Default filename    
            _owner, /// Name of the agent which owns the logger
            _qualifier; /// Label which extends the name of the owner
    protected boolean /// Parameters
            _validFile, /// The selected file is not available
            _echo, /// If true, it echoes everything on screen, otherwise is silent
            _tabular;               /// If true, the echo is arranged as a tabulated output
    protected int _maxLength, /// If the logged message exceeds this length, it is trimmed
            _textColor;             /// Color of the echoed texts
    protected PrintStream _outTo, ///  Default output stream for echoing messages. Std.out
            _errTo;                 /// Default output for errors. Std.err

    /**
     * Initializes the instance
     */
    public Logger() {
        _filename = _default;
        _validFile = true;
        _echo = true;
        _tabular = true;
        _owner = null;
        _outTo = System.out;
        _errTo = System.err;
        _textColor = white;
        _maxLength = 350;
        _owner = "";
        _qualifier = "";
    }

    /**
     * Returns the name of the selected file to record the log on file
     *
     * @return
     */
    public String getLoggerFileName() {
        return _filename;
    }

    public Logger setOwner(String name) {
        _owner = name;
        return this;
    }

    public Logger setOwnerQualifier(String s) {
        _qualifier = s;
        return this;
    }

    public Logger setLoggerFileName(String fname) {
        if (initRecord(fname)) {
            _filename = fname;
        }
        return this;
    }

    public Logger setOutputTo(PrintStream out) {
        _outTo = out;
        _errTo = out;
        return this;
    }

    public Logger setText(int color) {
        _textColor = color;
        return this;
    }

    public Logger enableTabular() {
        _tabular = true;
        return this;
    }

    public Logger disableTabular() {
        _tabular = false;
        return this;
    }

    public Logger enableEcho() {
        _echo = true;
        return this;
    }

    public Logger disableEcho() {
        _echo = false;
        return this;
    }

    protected boolean initRecord(String filename) {
        File file;

        file = new File(filename);
        if (file != null) {
            if (file.exists()) {
                if (file.isFile()) {
                    return (_validFile = true);    // Fichero existe
                } else {
                    return (_validFile = false);   // Es Directorio
                }
            } else {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    logException(ex);
                    return (_validFile = false);   // Fichero nuevo MAL
                }
                return (_validFile = true);        // Fichero nuevo OK
            }
        } else {
            return (_validFile = false);            // null
        }
    }

    public JsonObject addRecord(JsonObject o) {
        String timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
        JsonObject json = new JsonObject(), record = new JsonObject();
        if (!_owner.equals("")) {
            record.add("agent", _owner);
        }
        if (!this._qualifier.equals("")) {
            record.add("label", _qualifier);
        }
        record.merge(o);
        json.add("date", timeStamp).
                add("record", record);
        String toRecord = json.toString();
        if (_validFile) {
            PrintWriter outfile;
            try {
                outfile = new PrintWriter(new BufferedWriter(new FileWriter(_filename, true)));
                BufferedWriter out = new BufferedWriter(outfile);
                outfile.println(toRecord);
                outfile.close();
            } catch (IOException ex) {
                logException(ex);
            }

        }
        return json;
    }

    public JsonObject LogMessage(String message) {
        Output(message);
        return addRecord(new JsonObject().add("info", message));
    }

    public JsonObject LogMessage(JsonObject details) {
        Output(details.toString());
        return addRecord(details);
    }

    public JsonObject LogError(String message) {
        Error(message);
        return addRecord(new JsonObject().add("info", message));
    }

    public JsonObject LogError(JsonObject details) {
        Error(details.toString());
        return addRecord(details);

    }

    public void logException(Exception ex) {
        String classname = Thread.currentThread().getStackTrace()[2].getClassName(),
                methodname = Thread.currentThread().getStackTrace()[2].getMethodName(),
                filename = Thread.currentThread().getStackTrace()[2].getFileName();
//        LogError(new JsonObject().add("exception", ex.toString()).
//                add("infile", filename).
//                add("inclass", classname).
//                add("inmethod", methodname));
        ex.printStackTrace(this._outTo);
        LogError(new JsonObject().add("uncaught-exception", ex.toString()).
                add("info", ex.getLocalizedMessage()));
        
    }

    public String formatOutput(String s) {
        String res;
        if (this._maxLength > 0 && s.length() > _maxLength) {
            s = trimString(s, _maxLength);
//            s = s.substring(0, Math.min(_maxLength, s.length())) + " ... "
//                    + s.substring(s.length() - 5, s.length());
        }
        String heading;
        if (_qualifier.equals("")) {
            heading = String.format("%-10s", _owner);
        } else {
            heading = String.format("%-10s %-10s", _owner, _qualifier);
        }
        if (_tabular) {
            res = String.format("%-10s %-8s %s", heading,  new TimeHandler().toString().substring(11), s);
        } else {
            res = _owner + ": " + s;
        }
        if (res.contains("acl_send")) {
            res = defText(lightgreen)+res;
        }
        if (res.contains("acl_receive")) {
            res = defText(lightred)+res;
        }
        return res;
    }

    public void Output(String s) {
        if (_echo) {
            
            try {
                _outTo.printf(ConsoleAnsi.defText(_textColor) + formatOutput(s) + "\n");
            } catch (Exception Ex) {
                _outTo.printf(ConsoleAnsi.defText(_textColor) + s.substring(0, 10) + "\n");

            }
        }
    }

    public void Error(String s) {
            _outTo.printf(ConsoleAnsi.defText(lightred) + formatOutput(s) +defText(white)+"\n");
            _errTo.printf(ConsoleAnsi.defText(lightred) + formatOutput(s) +defText(white)+"\n");
    }


    public static String trimString(String s, int max) {
        if (s.length() > max) {
            return s.substring(0, Math.min(max, s.length())) + " ... "
                    + s.substring(s.length() - 5, s.length());
        } else {
            return s;
        }
    }
}
