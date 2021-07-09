/**
 *
 * @author lcv
 */
package AppBoot;

import InetTools.InetTools;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
* A basic application launcher that abstracts the launch of Jade and the stop of the associated container
 */

public class AppBoot {

    protected boolean _connected = false, _echo = false;
    protected String _title, _subtitile, _version = "1.0", _args[];
    protected ArrayList<String> _tasks, _achieved;
    protected jade.core.Runtime _runtime;
    protected MicroRuntime _uruntime;
    protected ContainerController _container;
    protected Profile _profile;
    protected HashMap<String, AgentController> _controllers;
    protected ArrayList<String> _agentNames;
    protected String _host, _virtualhost, _containerName, _platformId, _username, _password;
    protected final String _lockfilename = ".DeleteThisToReset.lock";
    protected FileWriter _lockfile;
    protected int _port;
    protected double _progress;

    protected enum PLATFORM {
        MAGENTIX, JADE, MICROJADE
    }
    PLATFORM _platformType;

    /**
     * The constructor receives a copy of the arguments passed in the command line
     * @param args An array of Strings that contains the arguments of the command line
     */
    public AppBoot(String[] args) {
        _args = args;
        _tasks = new ArrayList<>();
        _tasks.add("ARGUMENTS");
        _tasks.add("CONFIGURE");
        _tasks.add("CONNECT");
        _tasks.add("LAUNCH");
        _achieved = new ArrayList<>();
        _containerName = "";

    }

    protected AppBoot Progress() {
        _progress = _achieved.size() * 1.0 / _tasks.size();
        Message("AppBoot v" + _version + "  " + (int) (_progress * 100) + "%% Completed");
        return this;
    }

    protected AppBoot doCompleted(String task) {
        if (_tasks.contains(task) && !isCompleted(task)) {
            _achieved.add(task);
            Progress();
        }
        if (task.equals("CONNECT")) {
            this.activateLock();
        }
        return this;
    }

    protected boolean isCompleted(String task) {
        return _achieved.contains(task);
    }

    protected AppBoot processArguments() {
        Message("Processing arguments:");
        Message("...Ignored");
        doCompleted("ARGUMENTS");
        return this;
    }

    protected AppBoot Configure() {
        if (!isCompleted("ARGUMENTS")) {
            processArguments();
        }
        Message("Configuring boot:");
        Message("...Ignored");
        doCompleted("CONFIGURE");
        return this;
    }

    /**
     * Inner method to set a full-p2p Jade connection
     * @param host Host that contains the main container
     * @param port Port
     * @return A reference to the same instance
     */
    protected AppBoot setupJadeConnection(String host, int port) {
        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        System.out.println("Trying to connecto to Jade (Boot) @" + host + ":" + port);
        _platformType = PLATFORM.JADE;
        _host = host;
        _port = port;
        _controllers = new HashMap<>();
        _agentNames = new ArrayList<>();

        try {
            Message("jade.Boot Host " + _host + "["
                    + _port + "] <"
                    + _platformId + ">");
            _runtime = jade.core.Runtime.instance();
            _profile = new ProfileImpl();
            if (!_host.equals("")) {
                _profile.setParameter(Profile.MAIN_HOST, _host);
            }
            if (_port != -1) {
                _profile.setParameter(Profile.MAIN_PORT, "" + _port);
            }
            _container = _runtime.createAgentContainer(_profile);
            _runtime.setCloseVM(true);
            Message("Connected to Jade");
            _connected = true;
            if (_containerName.equals("")) {
                _containerName = _container.getContainerName();
            }
        } catch (Exception ex) {
            Abort("Unable to connect:" + ex.toString());
        }

        doCompleted("CONNECT");
        return this;
    }

    /**
     * Inner method to set a restricted-p2p Jade connection
     * @param host Host that contains the main container
     * @param port Port
     * @return A reference to the same instance
     */
    public AppBoot setupMicroJadeConnection(String host, int port) {
        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        System.out.println("Trying to connecto to Jade (MicroBoot) @" + host + ":" + port);
        _platformType = PLATFORM.MICROJADE;
        _host = host;
        _port = port;
        _controllers = new HashMap<>();
        _agentNames = new ArrayList<>();

        Message("jade.MicroBoot Host: " + _host + "["
                + _port + "] <"
                + _platformId + ">");
        jade.util.leap.Properties pr = new jade.util.leap.Properties();
        if (!_host.equals("")) {
            pr.setProperty(Profile.MAIN_HOST, _host);
        }
        if (_port != -1) {
            pr.setProperty(Profile.MAIN_PORT, "" + _port);
        }

        MicroRuntime.startJADE(pr, null);
        _containerName = MicroRuntime.getContainerName();
        doCompleted("CONNECT");
        return this;
    }

    /**
     * Analyzes the inet connection and sets upa a jadeBoot or jade.Microboot conection, the most appropriate one
     * @param host  The target host
     * @param port The target port
     * @return A reference to the same instance
     */
    public AppBoot selectConnection(String host, int port) {

        if (!isCompleted("CONFIGURE")) {
            Configure();
        }
        _host = host;
        _port = port;
        if (isBehindRouter()) {
            return setupMicroJadeConnection(host, port);
        } else {
            return setupJadeConnection(host, port);
        }
    }

    /**
     * This is a method to launch an Agent and pass it the command line arguments. Its use has been abandoned and kept only
     * for compatibility with old agents. Pleas use ConsoleBoot.launchagent instead
     * @param name  The name that will be given to the agent
     * @param c     The class name wihch it belongs to
     * @param arguments Command line from the Linux shell
     * @return A reference to the same instance
     */
    public AppBoot launchAgent(String name, Class c, String[] arguments) { 

        if (!isCompleted("CONNECT")) {
            Abort("Please configure the connection first");
        }
        AgentController ag;
        _agentNames.add(name);
        if (isMicroBoot()) {
            try {
                MicroRuntime.startAgent(name, c.getName(), arguments);
                ag = MicroRuntime.getAgent(name);
                _controllers.put(name, ag);
            } catch (Exception ex) {
                Error("ERROR CREATING AGENT " + name + " " + ex.toString());
            }
        } else {
            try {
                ag = _container.createNewAgent(name, c.getName(), arguments);
                ag.start();
                _controllers.put(name, ag);
            } catch (Exception e) {
                Error("Error creating Agent " + e.toString());
                ag = null;
            }
        }

        doCompleted("LAUNCH");
        return this;
    }

    public AppBoot shutDown() {

        boolean somealive;
        String alive;
        do {
            try {
                Thread.sleep(2500);
            } catch (Exception e) {
            }
            alive = "";
            somealive = false;
            for (String name : _agentNames) {
                try {
                    if (isMicroBoot()) {
                        somealive = MicroRuntime.size() > 0;
                    } else {
                        _container.getAgent(name);
                        somealive = true;
                    }
                    alive += name + ". ";
                } catch (Exception ex) {
                    _controllers.remove(name);
                }
            }
        } while (somealive && this.checkLock());
        Message("Shutting down container " + _containerName);
        this.deactivateLock();
        close();
        try {
            if (isMicroBoot()) {
                MicroRuntime.stopJADE();
            } else {
                try {
                    _container.kill();
                } catch (Exception ex) {
                    Error(ex.toString());
                }
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
        Message("Container " + _containerName + " shut down");
        System.exit(0);
        return this;
    }


    protected boolean isJade() {
        return (_platformType == PLATFORM.JADE
                || _platformType == PLATFORM.MICROJADE);
    }

    protected boolean isMicroBoot() {
        return _platformType == PLATFORM.MICROJADE;
    }

    protected void Message(String s) {
        if (_echo) {
            System.out.println(s);
        }
    }

    protected void Error(String s) {
        if (_echo) {
            System.err.println(s);
        }
    }

    protected void Abort(String s) {
        if (_echo) {
            Error(s);
        }
        Exit();
    }

    protected AppBoot close() {
        Message("AppBoot closing");
        return this;
    }

    protected void Exit() {
        close();
        Message("AppBoot exiting");
        System.exit(0);
    }

    protected boolean isBehindRouter() {
        return !_host.equals("localhost")
                && !InetTools.getExtIPAddress().equals(InetTools.getLocalIPAddress());
    }

    public boolean activateLock() {
        try {
            _lockfile = new FileWriter(new File(_lockfilename));
            _lockfile.close();
            return true;
        } catch (IOException ex) {
            System.err.println(ex.toString());
            return false;
        }
    }

    public boolean checkLock() {
        return new File(_lockfilename).exists();
    }

    public boolean deactivateLock() {
        if (!checkLock()) {
            return true;
        } else {
            new File(_lockfilename).delete();
            return true;
        }
    }
}
