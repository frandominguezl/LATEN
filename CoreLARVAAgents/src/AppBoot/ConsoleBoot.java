/**
 *
 * @author lcv
 */
package AppBoot;

import ConfigFile.ConfigFile;
import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.gray;
import static ConsoleAnsi.ConsoleAnsi.lightblue;
import static ConsoleAnsi.ConsoleAnsi.white;
import Logger.Logger;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.MicroRuntime;
import jade.wrapper.AgentController;

/**
 * Main class to launch Jade Applications, keeping a commonly used main() function,
 * abstraction of the connection procedure, regardless it is possible to define
 * a full-p2p or a restricted connection, and allow for a safe ending of agents and containers
 * and provides a safe emergency stop thnaks to a lock file, which, if deleted, produces
 * the death of all agents and the closing of the container in a safe way.
 * @author lcv
 */
public class ConsoleBoot extends AppBoot {

    protected String _config = "default";
    protected ConfigFile _mainConfig = null;
    protected JsonObject _params, _connection;
    protected Logger _disklogger;

    protected String _boot, _agname;
    protected ConsoleAnsi _console, _cprogress;
    protected boolean _silent = false,
            _system = false,
            _progress = false,
            _newconsole = false,
            _logtodisk = false,
            _analytics = false,
            _showbehaviour = false;

    /**
     * Main constructor of the app launcher
     * @param title The name of the instance
     * @param args The arguments given in the command line
     */
    public ConsoleBoot(String title, String[] args) {
        super(args);
        _title = title;
        _disklogger = new Logger().setOwner("bootstrap");
        _tasks.add("LOADCONF");
        _tasks.add("STARTLOG");
    }

    @Override
    protected ConsoleBoot Progress() {
        if (!_progress) {
            return this;
        }
        if (_cprogress == null) {
            super.Progress();
        } else {
            double progress = _achieved.size() * 1.0 / _tasks.size();
            _cprogress.setBackground(lightblue).doProgressBar(2, 2, 40, _achieved.size(), _tasks.size());
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                _disklogger.logException(ex);
            }
            if (progress >= 1) {
                _cprogress.close();
            }
        }
        return this;
    }

    @Override
    protected ConsoleBoot processArguments() {
        _params = new JsonObject();
        boolean isHelp = false;
        try {
            int i = 0;
            while (i < _args.length) {
                switch (_args[i]) {
                    case "-config": // load config file
                        _config = _args[i + 1];
                        i++;
                        break;
                    case "-silent": // Complete mute
                        _silent = true;
                        _params.set("silent", true);
                        break;
                    case "-progress": // Show messages onscreen only
                        _progress = true;
                        break;
                    case "-logtodisk":
                        _logtodisk = true;
                        _params.set("log", true);
                        break;
                    case "-newconsole": // open external console
                        _newconsole = true;
                        break;
                    case "-behaviour": // Label output with the behaviour that requested ouput
                        _showbehaviour = true;
                        _params.set("showbehaviour", true);
                        break;
                    case "-system":
                        _system = true;
                        _params.set("system", true);
                        break;
                    case "-full":   // Full verbosity
                        _silent = false;
                        _progress = _logtodisk = _newconsole = _system = _showbehaviour = true;
                        _params.set("silent", false);
                        _params.set("log", true);
                        _params.set("system", true);
                        _params.set("showbehaviour", true);
                        break;
                    case "-help":
                        isHelp = true;
                        break;
                    default:
                        this.Abort("Error in commandline arguments");
                }
                i++;
            }
            if (isHelp) {
                showHelp();
                System.exit(0);
            }
            if (_newconsole) {
                _console = new ConsoleAnsi(_title, 150, 25, 10).
                        setText(white).setBackground(black).
                        clearScreen().captureStdInOut().setCursorOff();
            } else {
                _title = "internal";
                _console = new ConsoleAnsi(_title).clearScreen();
            }
            if (_progress) {
                _cprogress = new ConsoleAnsi(_title + "-loading", 60, 3).
                        setBackground(gray).setText(black).
                        setCursorOff().clearScreen();
            } else {
                _cprogress = null;
            }
            if (_silent) {
                _disklogger.disableEcho();
            } else {
                _disklogger.setText(white).enableEcho().enableTabular().setOutputTo(_console.out());
            }
            Progress();
            Message("Arguments ok");
        } catch (Exception ex) {
            Abort(ex.toString());
        }
        doCompleted("ARGUMENTS");
        return this;
    }

    protected void showHelp() {
        System.out.println("\nThis is LARVA Jade application launcher platform. This application and its integrated agents show several types of messages: INFORMATION, SYSTEM, ERROR/EXCEPTION. INFORMATION is due to general purposes, SYSTEM shows information  about the internal steps os the app launcher and agents, and ERROR/EXCEPTION shows information about the internal errors found.\n\n"
                + "-silent\nDon't show any type message, only errors. It also provides an abstraction layer to connect to an existin Jade Agent's Platform,either local or remote and the shutdown of the related containers\n\n"
                + "-system\n Show SYSTEM messages\n\n"
                + "-logtodisk\nLog all messages on disk, in the folder ./log/ either it is displayed or not.\n\n"
                + "-newconsole\nOpens the output of the app launcher and the agents in an external terminal. Usually, the execution inside a IDE does not support text colouring. if your agent displays"
                + "text with colors, this option must be active, otherwise text colours would not show up inside the IDE\n\n"
        //                + "-progress\nShows the progress of the app launcher until agents are executed."
        );

    }

    @Override
    protected ConsoleBoot Configure() {
        if (!isCompleted("ARGUMENTS")) {
            processArguments();
        }
        String logfile;
        _mainConfig = new ConfigFile("./config/" + _config + ".json");
        Message("Loading config file " + _mainConfig.getConfigFileName());
        if (!_mainConfig.openConfig()) {
            Error("No configuration [" + _config + "] has been found");
        } else {
            _params = _mainConfig._settings.merge(_params);
        }
        doCompleted("LOADCONF");
        if (_params.get("name") != null) {
            _agname = _params.get("name").asString();
            logfile = "./logs/" + _agname + "_log.json";
        } else {
            _agname = "unknown" + Math.random();
            logfile = "./logs/boot_log.json";
        }

        int ctext, cbackg;
        JsonArray ct = new JsonArray().add(1).add(1).add(1),
                cb = new JsonArray().add(0).add(0).add(0);
        ctext = ConsoleAnsi.defColor(ct.get(0).asDouble(), ct.get(1).asDouble(), ct.get(2).asDouble());
        cbackg = ConsoleAnsi.defColor(cb.get(0).asDouble(), cb.get(1).asDouble(), cb.get(2).asDouble());
        if (_params.getBoolean("log", false)) {
            Message("Starting log file... ." + logfile);
            _disklogger.setLoggerFileName(logfile);
        }
        _disklogger.setText(ctext);
        doCompleted("STARTLOG");

        _boot = _params.getString("boot", "jade");
        if (_boot.equals("magentix")) {
            _connection = _params.get("magentixconnection").asObject();
            _host = _connection.getString("host", "");
            _port = _connection.getInt("port", -1);
            _virtualhost = _connection.getString("virtualhost", "");
            _username = _connection.getString("username", "");
            _password = _connection.getString("password", "");
        } else if (_boot.equals("jade")) {
            JsonValue connect = _params.get("jadeconnection");
            _connection = (connect == null ? new JsonObject() : connect.asObject());
            _host = _connection.getString("host", "");
            _port = _connection.getInt("port", -1);
            _containerName = _connection.getString("container", "");
            _platformId = _connection.getString("platformid", "");

        } else {
            Error("Unknown platform");
        }
        doCompleted("CONFIGURE");
        return this;
    }

    public String tty() {
        return _console.tty();
    }

    public JsonObject getConfig() {
        return this._mainConfig._settings;
    }

    public Logger getLogger() {
        return this._disklogger;
    }

    public String getConfigName() {
        return this._config;
    }

    /**
     * General selecton of the connection with the local or remote platform. Please use ONLY when 
     * the connection data has already been stored at the json-config file. If it can use full P2P jade.Boot it does, 
     * but if it is not possible, the it goes jade.Microboot. In any case, we don't have to worry about the inet 
     * infrastructure defined in our computer
     * @return A reference to the same instance
     */
    public ConsoleBoot selectConnection() {

        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        _platformType = PLATFORM.JADE;
        if (isJade()) {
            return (ConsoleBoot) selectConnection(_host, _port);

        } else {
            Abort("Unknown platform");
        }
        return this;
    }

    /**
     * Given that the name of the agent might be specified in the config file, this launch method
     * uses it to name the agent. If the name is not present in the config file, it uses an alias
     * @param c The class of the agent to be reated and executed
     * @return A reference to the instance
     */
    public ConsoleBoot launchAgent(Class c) {

        return this.launchAgent(_agname, c, new String[]{_params.toString()});
    }

    /**
     * This is the most common way of launching an agent, that is, giving its name since the beginning
     * @param name  The name of the agent that is to be created
     * @param c The class of the agent
     * @return A reference to the same instance
     */
    public ConsoleBoot launchAgent(String name, Class c) {
        return this.launchAgent(name, c, new String[]{_params.toString()});
    }

     /**
     * This is a method to launch an Agent and pass it the command line arguments. Its use has been abandoned and kept only
     * for compatibility with old agents. Pleas use ConsoleBoot.launchagent instead
     * @param name  The name that will be given to the agent
     * @param c     The class name wihch it belongs to
     * @param arguments Command line from the Linux shell
     * @return A reference to the same instance
     * @deprecated
     */
    @Override
    @Deprecated
    public ConsoleBoot launchAgent(String name, Class c, String[] arguments) {
        if (!isCompleted("CONNECT")) {
            this.selectConnection();
        }
        AgentController ag;

        _agentNames.add(name);
        if (isMicroBoot()) {
            try {
                MicroRuntime.startAgent(name, c.getName(), arguments);
                ag = MicroRuntime.getAgent(name);
                _controllers.put(name, ag);
            } catch (Exception ex) {
                Error("jade.MicroBooot ERROR CREATING AGENT " + name + " " + ex.toString());
            }
        } else {
            try {
                ag = _container.createNewAgent(name, c.getName(), arguments);
                ag.start();
                _controllers.put(name, ag);
            } catch (Exception e) {
                Error("jade.Booot Error creating Agent " + e.toString());
                ag = null;
            }
        }

        doCompleted("LAUNCH");
        return this;
    }

    @Override
    protected void Message(String s) {
        if (_system) {
            _disklogger.LogMessage(s);
        }
    }

    @Override
    protected void Error(String s) {
        _disklogger.LogError(s);
    }

    @Override
    protected void Abort(String s) {
        Error(s);
        Exit();
    }

    @Override
    protected ConsoleBoot close() {
        _console.waitToClose();
        return this;
    }
}
