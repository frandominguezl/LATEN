/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and topenxsession the template in the editor.
 */
package Session;

import AdminKeys.DBACoin;
import World.liveBot;
import Analytics.AnalyticsProject;
import ConfigFile.ConfigFile;
import Geometry.Point;
import Glossary.Sensors;
import static Glossary.Sensors.ALIVE;
import static Glossary.Sensors.ALTITUDE;
import static Glossary.Sensors.ANGULARDLX;
import static Glossary.Sensors.COMPASS;
import static Glossary.Sensors.DISTANCEDLX;
import static Glossary.Sensors.ENERGY;
import static Glossary.Sensors.GPS;
import static Glossary.Sensors.LIDAR;
import static Glossary.Sensors.ONTARGET;
import static Glossary.Sensors.THERMAL;
import static Glossary.Sensors.THERMALDLX;
import static Glossary.Sensors.VISUAL;
import static Glossary.Sensors.sensorList;
import Map2D.Map2DGrayscale;
import PublicKeys.KeyGen;
import TimeHandler.TimeHandler;
import World.Perceptor;
import World.Thing;
import World.Thing.PROPERTY;
import World.World;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lcv
 */
public final class Session {

    public String key, requester; //, inputfolder, outputfolder;
    public boolean error, debug, admin, solved;
    public int status;
    public int userID = -1, groupID = -1, whoID = -1, courseID = -1, assignmentID = -1, problemID = -1;
    public String problemtitle = "", username, groupname;
    public String lastAction = "", lastUpdated = "";
    public boolean isIndividual = false, skipanalytics = false, donotify = false;
    public String milestoneID = "", mtitle = "", whoName = "";
    public World world = null;
    private final int MAXAGENTS = 10, _mapOffset = 50, MAXDRONES = 4;
    // Evolution of the session
    public String mdxsession = "", mdxcourse = "", pmxcourse = "";
    public int kmdxsession = 0, kmdxcourse = 0, costxcourse, benefitxsession = 0, costxsession = 0, costxbest, benefitxbest, benefitxcourse;
    public long tsolvedxsession = -1, topenxsession = -1, tsolvedxcourse = 0, topenxcourse = 0, aux, now,
            latxsession, latxcourse, topenxbest, tsolvedxbest, latxbest;
    public ArrayList<Integer> usersInvolved = null;
    public HashMap<String, liveBot> droneRecord;
    public HashMap<String, DroneRole> agentRoles;
    public ArrayList<String> droneNames, listeners, germans;
    public double distancexproblem = -1, distancexsession = -1;

    // States of the session
    public static final int WAIT_TO_LOG = 0, LOGIN_IN = 1, LOGGED_IN = 2, LOGOUT = 3;

    // Society of supporting agents
    public ContainerController container;
    // Shopping centers and the like
    public ArrayList<AgentController> burocrats;

    //            ContainerController agc = this.getContainerController();
//            AgentController a = agc.createNewAgent("Bank",Banker.class.getName(), null);
//            a.start();
//            a.kill();
    // Colors
    public final Color _cGOAL = new Color(255, 255, 0), _cTRACEOK = new Color(0, 200, 0),
            _cCRASH = new Color(255, 0, 0), _cFORB = new Color(128, 0, 0),
            _cUNK = new Color(128, 128, 128), _cBLI = new Color(0, 0, 0),
            _cTRACES[] = {new Color(125, 0, 0), new Color(0, 125, 0), new Color(0, 0, 125),
                new Color(125, 125, 0), new Color(0, 125, 125), new Color(125, 0, 125),
                new Color(0, 250, 0), new Color(0, 0, 250),
                new Color(250, 250, 0), new Color(0, 250, 250), new Color(250, 0, 250)};
    // Roles of the agent
    public static final int rNOROLE = -1, rFLY = 0, rBIRD = 1, rHAWK = 2, rRESCUE = 3;

    // Status Hackathon
    public static final int _HACKClosed = 0, _HACKHello = 1, _HACKPasswd = 2, _HACKCounterPasswd = 3;

    public enum HackStates {
        OUT, IN
    };
    public HackStates HACKState = HackStates.OUT;
    public int HACKnumberGuessing;
    public String HACKwordGuessing, HACKreply;
    public StringBuilder HackwordState;
    private final String[] HACKwords = {"Android", "Beerus", "Buu", "Frieza", "Ginyu", "Gohan", "Goku", "Gotenks", "Hit", "Krillin", "Nappa", "PerfectCell", "Piccolo", "Tenshinhan", "Trunks", "Vegeta", "Yamcha"};

    // Others
    public ConfigFile _config;
    public Map2DGrayscale _map; //, _mapresult, _mapstudents;
    //public int _worldMinLevel, _worldMaxLevel, _worldGoals;
    public int _worldEnergy = 100000;

    public ArrayList<String> trace, telegrammessages = new ArrayList(), radio, safeGermans, allGermans;

    // P3 y 4
    public static final int COINSXSESSION = 180, COINSXDRONE = 20,
            BSENSORSXSESSION = 4, HQSENSORSXSESSION = 4,
            DLSENSORSXSESSION = 4, CHARGEXSESSION = 10, CHARGEMAX = 1000;
    public Boolean eyeOfGod = false, freeMatch = false;

    public static enum DroneRole {
        LISTENER, SEEKER, RESCUER
    };
    int ndrones = 0;

    public ACLMessage myCastRadio;

    public Session() {
        initSession();
    }
    public ArrayList<String> burntProducts;

    public Session(AnalyticsProject analytics) {
        initSession();
        if (analytics != null) {
            bindSession(analytics);
        }
        this.key = getKey();
        trace = new ArrayList();
        radio = new ArrayList();
        myCastRadio = new ACLMessage();
        myCastRadio.setSender(new AID("RadioOperator", AID.ISLOCALNAME));
        myCastRadio.setPerformative(ACLMessage.INFORM);
        myCastRadio.setProtocol("BROADCAST");
        myCastRadio.setConversationId(getKey());
    }

    //
    // ----- TOP LOOP
    //
    public final void initSession() {
        error = true;
        debug = false;
        admin = true;
        solved = false;
        status = 0;
        key = "SESSION#" + DBACoin.getRootKey(DBACoin.KSESSION); //KeyGen.getKey();
        _config = null;

        status = WAIT_TO_LOG;
        costxsession = 0;
        benefitxsession = 0;
        agentRoles = new HashMap<>();
        droneRecord = new HashMap<>();
        droneNames = new ArrayList<>();
        listeners = new ArrayList<>();
        safeGermans = new ArrayList();
        allGermans = new ArrayList();
        burntProducts = new ArrayList();
        this.burocrats = new ArrayList<>();
    }

    public void addTrace(String record) {
//        String tow = new TimeHandler().toString();
        this.lastAction = record;
//        this.lastUpdated = tow;
//        trace.add(record);
    }

    public final void bindSession(AnalyticsProject analytics) {
        courseID = analytics._courseID;
        assignmentID = analytics._assignmentID;
        groupID = analytics._groupID;
        isIndividual = analytics._isIndividual;
    }

    public boolean openSession(String worldname) {
        if (this.assignmentID != 3) {
            status = this.LOGIN_IN;
            world = new World(worldname);
            if (!world.loadConfig(worldname)) {
                return false;
            }
            error = false;
            status = this.LOGGED_IN;
            _map = world.getEnvironment().getSurface();
            for (String st : world.listThings("PEOPLE")) {
                if (world.getThing(st).getType().toUpperCase().equals("MALE")
                        || world.getThing(st).getType().toUpperCase().equals("FEMALE")) {
                    this.allGermans.add(world.getThing(st).getName());
                }
            }
        }
        return true;
    }

    public void activateSession() {
//        lastPerceptions = new JsonObject();
//        doPerceptions();
//        plotTrace();        
    }

    public boolean closeSession() {
        boolean result = true;

        return result;
    }

    public boolean subscribeAgent(String name, DroneRole role) {
        int res;
        if (role == DroneRole.LISTENER) {
            // No more listeners can register
            if (listeners.size() == MAXAGENTS) {
                return false;
            }
            // Already registered?
            res = listeners.indexOf(name);
            if (res >= 0) {
                return true;
            }
            // New listener registered
            res = listeners.size();
            listeners.add(name);
            agentRoles.put(name, role);
            this.myCastRadio.addReceiver(new AID(name, AID.ISLOCALNAME));
            return true;
        } else {
            // No more agents can register
            if (droneNames.size() == MAXDRONES) {
                return false;
            }
            // Already registered?
            res = droneNames.indexOf(name);
            if (res >= 0) {
                return true;
            }
            // New Agent registered
            res = droneNames.size();
            droneNames.add(name);
            agentRoles.put(name, role);
            return true;
        }
    }

    public void configureDrone(String name, String sensors[], int width, int origx, int origy) {
        int res;
        liveBot agent;
        // Already registered?
        res = droneNames.indexOf(name);
        if (res < 0) {
            return;
        }
        // New Agent registered
        agent = new liveBot(name, world);
        DroneRole role = agentRoles.get(name);
        agent.groupname = this.groupname;
        agent.order = droneRecord.size();
        agent.role = role.toString();
        agent.capabilities = new ArrayList<>();
        //
        // CAPABILITIES
        //

        // EXECUTE
        agent.capabilities.add(Glossary.Capabilities.MOVEFORWARD);
        agent.capabilities.add(Glossary.Capabilities.ROTATELEFT);
        agent.capabilities.add(Glossary.Capabilities.ROTATERIGHT);
        agent.capabilities.add(Glossary.Capabilities.MOVEUP);
        agent.capabilities.add(Glossary.Capabilities.MOVEDOWN);
        agent.capabilities.add(Glossary.Capabilities.TOUCHDOWN);
        agent.capabilities.add(Glossary.Capabilities.RECHARGE);
        if (role == DroneRole.RESCUER) {
            agent.capabilities.add(Glossary.Capabilities.RESCUE);
        }
        agent.capabilities.add(Glossary.Sensors.STATUS);
        agent.capabilities.add(Glossary.Sensors.TRACE);

        // PERCEIVE
        List<String> mysensorlist = Arrays.asList(Glossary.Sensors.sensorList);
        ArrayList<String> hiddensensors = new ArrayList();
        // Capabilities
        for (String mysens : sensors) {
            if (mysensorlist.contains(mysens)) {
                agent.capabilities.add(mysens);
                hiddensensors.add(mysens);
            }
        }
        // Hidden set of sensors for debug mode
        if (debug) {
            for (String hiddensens : new String[]{ALIVE, ONTARGET, GPS, COMPASS, DISTANCEDLX, ANGULARDLX, ALTITUDE, VISUAL, LIDAR, THERMAL, ENERGY}) {
                boolean toadd = true;
                for (String mysens : sensors) {
                    if (mysens.substring(0, 3).equals(hiddensens.subSequence(0, 3))) {
                        toadd = false;
                    }
                }
                if (toadd) {
                    hiddensensors.add(hiddensens);
                }
            }
        }

        Perceptor p;
        for (String mysens : hiddensensors) {
            if (mysensorlist.contains(mysens)) {
                p = new Perceptor(mysens, agent);
                switch (mysens) {
                    case Glossary.Sensors.COMPASS:
//                        p = new Perceptor(Glossary.Sensors.COMPASS, agent);
                        p.setWhatPerceives(Thing.PROPERTY.ORIENTATION, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.GPS:
//                        p = new Perceptor(Glossary.Sensors.GPS, agent);
                        p.setWhatPerceives(Thing.PROPERTY.POSITION, "", Perceptor.SELECTION.INTERN);
                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ALIVE:
//                        p = new Perceptor(Glossary.Sensors.ALIVE, agent);
                        p.setWhatPerceives(Thing.PROPERTY.STATUS, "", Perceptor.SELECTION.INTERN);
                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ONTARGET:
//                        p = new Perceptor(Glossary.Sensors.ONTARGET, agent);
                        p.setWhatPerceives(Thing.PROPERTY.ONTARGET, "", Perceptor.SELECTION.INTERN);
                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ENERGY:
//                        p = new Perceptor(Glossary.Sensors.ENERGY, agent);
                        p.setWhatPerceives(Thing.PROPERTY.ENERGY, "", Perceptor.SELECTION.INTERN);
                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.LIDAR:
//                        p = new Perceptor(Glossary.Sensors.LIDAR, agent);
                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 7);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.VISUAL:
//                        p = new Perceptor(Glossary.Sensors.VISUAL, agent);
                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 7);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.THERMAL:
//                        p = new Perceptor(Glossary.Sensors.THERMAL, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.ALL);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.ALL);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 7);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(width * 0.75);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.THERMALHQ:
//                        p = new Perceptor(Glossary.Sensors.THERMAL, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.ALL);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.ALL);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 21);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(width * 0.75);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.THERMALDLX:
//                        p = new Perceptor(Glossary.Sensors.THERMAL, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.ALL);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.ALL);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 31);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(width * 0.75);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ALTITUDE:
//                        p = new Perceptor(Glossary.Sensors.ALTITUDE, agent);
                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
                        p.setRange(1);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.DISTANCE:
//                        p = new Perceptor(Glossary.Sensors.DISTANCE, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
                        p.setSensitivity(50);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.DISTANCEHQ:
//                        p = new Perceptor(Glossary.Sensors.DISTANCE, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
                        p.setSensitivity(75);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.DISTANCEDLX:
//                        p = new Perceptor(Glossary.Sensors.DISTANCEDLX, agent);
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
                        p.setSensitivity(150);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ANGULAR:
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(50);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ANGULARHQ:
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(75);
                        agent.addSensor(p);
                        break;
                    case Glossary.Sensors.ANGULARDLX:
                        if (eyeOfGod) {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
                        } else {
                            p.setWhatPerceives(Thing.PROPERTY.POSITION, "MALE", Perceptor.SELECTION.CLOSEST);
                        }
                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
                        p.setSensitivity(150);
                        agent.addSensor(p);
                        break;
                }
            }
        }
        JsonObject jsdrones = world.getConfig().get("drones").asObject();
        agent.maxAllowedLevel = 256;
        agent.minAllowedLevel = 0;
        agent.energylevel = 10;
        agent.burnmovement = (role == DroneRole.SEEKER) ? 1 : 4;
        agent.colorcode = _cTRACES[res];
        int x, y, w = world.getEnvironment().getSurface().getWidth(), h = world.getEnvironment().getSurface().getHeight();

        if (0 <= origx && origx < _map.getWidth() && 0 <= origy && origy < _map.getHeight()) {
            agent.setPosition(new Point(origx, origy, _map.getLevel(origx, origy)));
        } else {
            agent.setPosition(new Point(0, 0, _map.getLevel(0, 0)));
        }
        droneRecord.put(name, agent);
        agent.setType("AGENT");
        world.addThing(agent, new PROPERTY[]{PROPERTY.POSITION, PROPERTY.PRESENCE});
        agent.placeAtSurface(new Point(origx, origy, 0));
        agent.altitude = 0;
        agent.compass = 90;
        checkStatus(agent);
    }

    protected int getBurnRatio(String what) {
        int res = 0;
        // Both roles ans sensors
        switch (what) {
            case Glossary.Sensors.COMPASS:
            case Glossary.Sensors.GPS:
            case Glossary.Sensors.ALIVE:
            case Glossary.Sensors.ONTARGET:
            case Glossary.Sensors.ENERGY:
            case Glossary.Sensors.LIDAR:
            case Glossary.Sensors.VISUAL:
            case Glossary.Sensors.THERMAL:
            case Glossary.Sensors.ALTITUDE:
            case Glossary.Sensors.DISTANCE:
            case Glossary.Sensors.ANGULAR:
            case "SEEKER":
                res = 1;
                break;
            case Glossary.Sensors.THERMALHQ:
            case Glossary.Sensors.ANGULARHQ:
            case Glossary.Sensors.DISTANCEHQ:
            case "RESCUER":
                res = 4;
                break;
            case Glossary.Sensors.THERMALDLX:
            case Glossary.Sensors.DISTANCEDLX:
            case Glossary.Sensors.ANGULARDLX:
                res = 8;
                break;
            default:
                res = 0;
                break;
        }
//        return res;
        if (this.freeMatch) {
            return 1;
        } else {
            return res;
        }
    }

    public int registerAgent(String name, String sensors[], int width) {
        return 0;

//        int res;
//        liveBot agent;
//
//        // Only for P2
//        System.exit(1);
//
//        // No more agents can register
//        if (droneNames.size() == MAXAGENTS) {
//            return -1;
//        }
//        // Already registered?
//        res = droneNames.indexOf(name);
//        if (res >= 0) {
//            return res;
//        }
//        // New Agent registered
//        agent = new liveBot(name, world);
//        res = droneNames.size();
//        droneNames.add(name);
//        agent.capabilities = new ArrayList<>();
//        //
//        // CAPABILITIES
//        //
//
//        // EXECUTE
//        agent.capabilities.add(Glossary.Capabilities.MOVEFORWARD);
//        agent.capabilities.add(Glossary.Capabilities.ROTATELEFT);
//        agent.capabilities.add(Glossary.Capabilities.ROTATERIGHT);
//        agent.capabilities.add(Glossary.Capabilities.MOVEUP);
//        agent.capabilities.add(Glossary.Capabilities.MOVEDOWN);
//        agent.capabilities.add(Glossary.Capabilities.TOUCHDOWN);
//        agent.capabilities.add(Glossary.Capabilities.RECHARGE);
//        agent.capabilities.add(Glossary.Capabilities.RESCUE);
//        agent.capabilities.add(Glossary.Sensors.STATUS);
//        agent.capabilities.add(Glossary.Sensors.TRACE);
//
//        // PERCEIVE
//        List<String> mysensorlist = Arrays.asList(Glossary.Sensors.sensorList);
//        Perceptor p;
//        for (String mysens : sensors) {
//            if (mysensorlist.contains(mysens)) {
//                agent.capabilities.add(mysens);
//                switch (mysens) {
//                    case Glossary.Sensors.COMPASS:
//                        p = new Perceptor(Glossary.Sensors.COMPASS, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.ORIENTATION, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.GPS:
//                        p = new Perceptor(Glossary.Sensors.GPS, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.POSITION, "", Perceptor.SELECTION.INTERN);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.ALIVE:
//                        p = new Perceptor(Glossary.Sensors.ALIVE, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.STATUS, "", Perceptor.SELECTION.INTERN);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.ONTARGET:
//                        p = new Perceptor(Glossary.Sensors.ONTARGET, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.ONTARGET, "", Perceptor.SELECTION.INTERN);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.ENERGY:
//                        p = new Perceptor(Glossary.Sensors.ENERGY, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.ENERGY, "", Perceptor.SELECTION.INTERN);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.PAYLOAD:
//                        p = new Perceptor(Glossary.Sensors.PAYLOAD, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.PAYLOAD, "", Perceptor.SELECTION.INTERN);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.LIDAR:
//                        p = new Perceptor(Glossary.Sensors.LIDAR, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 7);
//                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.VISUAL:
//                        p = new Perceptor(Glossary.Sensors.VISUAL, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.QUERY, 7);
//                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.THERMAL:
//                        p = new Perceptor(Glossary.Sensors.THERMAL, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.POSITION, "ANIMAL", Perceptor.SELECTION.ALL);
//                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 7);
//                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
//                        p.setSensitivity(width * 0.75);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.ALTITUDE:
//                        p = new Perceptor(Glossary.Sensors.ALTITUDE, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.SURFACE, "ENVIRONMENT", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
//                        p.setRange(1);
//                        agent.addSensor(p);
//                        break;
//                    case Glossary.Sensors.DISTANCE:
//                        p = new Perceptor(Glossary.Sensors.DISTANCE, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.DISTANCE, 1);
//                        agent.addSensor(p);
//                        p.setSensitivity(width + width);
//                        break;
//                    case Glossary.Sensors.ANGULAR:
//                        p = new Perceptor(Glossary.Sensors.ANGULAR, agent);
//                        p.setWhatPerceives(Thing.PROPERTY.POSITION, "PEOPLE", Perceptor.SELECTION.CLOSEST);
//                        p.setHowPerceives(Perceptor.OPERATION.ANGLE, 1);
//                        p.setAttacment(Perceptor.ATTACH.ZENITAL);
//                        agent.addSensor(p);
//                        break;
//                }
//            }
//        }
//        JsonObject jsdrones = world.getConfig().get("drones").asObject();
//        agent.maxAllowedLevel = jsdrones.getInt("maxflight", 256);
//        agent.minAllowedLevel = 0;
//        agent.energylevel = CHARGEMAX;
//        agent.burnmovement = 1;
//        agent.burnsensor = 1;
//        agent.colorcode = _cTRACES[res];
//        agent.range = 7;
//        int x, y, w = world.getEnvironment().getSurface().getWidth(), h = world.getEnvironment().getSurface().getHeight();
//        switch (jsdrones.getString("origin", "random")) {
//            case "choice":
//                x = jsdrones.get("surface-location").asArray().get(0).asInt();
//                y = jsdrones.get("surface-location").asArray().get(1).asInt();
//                break;
//            case "middle":
//                x = w / 2;
//                y = h / 2;
//                break;
//            case "N":
//                x = w / 2;
//                y = 0;
//                break;
//            case "S":
//                x = w / 2;
//                y = h - 1;
//                break;
//            case "E":
//                x = w - 1;
//                y = h / 2;
//                break;
//            case "W":
//                x = 0;
//                y = h / 2;
//                break;
//            case "NE":
//                x = w - 1;
//                y = 0;
//                break;
//            case "SE":
//                x = w - 1;
//                y = h - 1;
//                break;
//            case "SW":
//                x = w - 1;
//                y = h - 1;
//                break;
//            case "NW":
//                x = 0;
//                y = 0;
//                break;
//            default:
//                x = (int) (Math.random() * w);
//                y = (int) (Math.random() * h);
//        }
//        if (0 <= x && 0 < _map.getWidth() && 0 <= y && y < _map.getHeight()) {
//            agent.setPosition(new Point(x, y, _map.getLevel(x, y)));
//        } else {
//            agent.setPosition(new Point(0, 0, _map.getLevel(0, 0)));
//        }
//        droneRecord.put(name, agent);
//        agent.setType("AGENT");
//        world.addThing(agent, new PROPERTY[]{PROPERTY.POSITION, PROPERTY.PRESENCE});
//        agent.placeAtSurface(new Point(x, y, 0));
//        agent.coins = new ArrayList();
//        for (int i = 0; i < 10; i++) {
//            agent.coins.add(""); //new DBACoin().setSession(key).setSerie(this.droneRecord.size()*10+i).encode());
//        }
//        checkStatus(agent);
//        return res;
    }

    public liveBot findAgent(String name) {
        return droneRecord.get(name);
    }

    public boolean execAgent(liveBot agent, String action) {
        boolean res;
        if (agent == null) {
            return false;
        }
        if (debug || (agent.capabilities.indexOf(action) >= 0 && agent.getAlive() == 1)) {
            lastAction = action;
            lastUpdated = new TimeHandler().toString();
            agent.statusinfo = "";
            switch (action) {
                case Glossary.Capabilities.MOVEFORWARD:
                    agent.moveForward(1);
                    agent.energylevel -= this.getBurnRatio(agent.role);
                    costxsession++;
                    res = true;
                    break;
                case Glossary.Capabilities.ROTATELEFT:
                    agent.rotateLeft();
                    agent.energylevel -= this.getBurnRatio(agent.role);
                    costxsession++;
                    res = true;
                    break;
                case Glossary.Capabilities.ROTATERIGHT:
                    agent.rotateRight();
                    agent.energylevel -= this.getBurnRatio(agent.role);
                    costxsession++;
                    res = true;
                    break;
                case Glossary.Capabilities.MOVEDOWN:
                    agent.moveDown(5);
                    agent.energylevel -= this.getBurnRatio(agent.role) * 5;
                    costxsession++;
                    res = true;
                    break;
                case Glossary.Capabilities.TOUCHDOWN:
                    double x = agent.getPosition().getX(),
                     y = agent.getPosition().getY(),
                     z = agent.getPosition().getZ(),
                     terrainz = world.getEnvironment().getSurface().getLevel(x, y);
                    costxsession++;
                    if (z - terrainz <= 5) {
                        agent.moveDown((int) (z - terrainz));
                        agent.energylevel -= this.getBurnRatio(agent.role) * (int) (z - terrainz);
                        res = true;
                        break;
                    } else {
                        agent.statusinfo += "Too high to perform touchdown";
                        res = true;
                        break;
                    }
                case Glossary.Capabilities.MOVEUP:
//                    agent.moveUp(5);
                    int nups;
//                    nups = Integer.min(5, agent.maxAllowedLevel-(int) agent.getPosition().getZ());
                    nups = 5;
                    agent.moveUp(nups);
                    agent.energylevel -= this.getBurnRatio(agent.role) * nups;
                    costxsession++;
                    res = true;
                    break;
                case Glossary.Capabilities.RECHARGE:
                    if (agent.getPosition().getZ() == _map.getLevel(agent.getPosition().getX(),
                            agent.getPosition().getY())) {
//                        int refuel = (int) Math.min(_worldEnergy, 1000 - agent.energylevel);
//                        _worldEnergy -= refuel;
//                        agent.energylevel += refuel;
                        agent.energylevel = CHARGEMAX;
                        res = true;
                        break;
                    } else {
                        agent.statusinfo += "Recharge  is only possible at ground level";
                        res = true;
                        break;
                    }
                case Glossary.Capabilities.RESCUE:
                    String whatname = isGoal(agent);
                    if (!isGoal(agent).equals((""))) {
                        Thing what = world.getThing(whatname);
                        agent.payload.add(what);
                        world.removeThing(what);
                        res = true;
                        break;
                    } else {
                        agent.statusinfo += "Nothing to rescue here";
                        res = true;
                        break;
                    }
                default:
                    break;
            }
            agent.lastEvent = action;
            getPerceptions(agent);
            this.checkStatus(agent);
            agent.altitude = (int) (agent.getPosition().getZ()) - _map.getLevel(agent.getPosition().getX(), agent.getPosition().getY());

            if (!agent.statusinfo.equals("")) {
                agent.lastEvent = agent.statusinfo;
            }
            return true;
        } else {
            if (agent.capabilities.indexOf(action) < 0) {
                agent.statusinfo = "Action " + action + " not within its capabilities";
            }
            if (agent.getAlive() != 1) {
                agent.statusinfo = "Agent is dead";
            }
            return false;
        }
    }

    //
    // ----- HIGH LEVEL PERCEPTIONS
    //
    public void checkStatus(liveBot agent) {
        boolean single = false, multiple = true, crashtotoher,
                crashtoground, crashtomaxlevel, crashtoenergy, crashtoborder;
        if (agent == null) {
            return;
        }
        agent.energylevel = (int) Math.max(agent.energylevel, 0);
        crashtoenergy = agent.energylevel < 1;
        if (crashtoenergy) {
            agent.statusinfo += "Energy exhausted. ";
        }
        crashtoground = agent.getPosition().getZ() < _map.getLevel(agent.getPosition().getX(), agent.getPosition().getY());
        if (crashtoground) {
            agent.statusinfo += "Crash onto the ground. ";
        }
        crashtoborder = agent.getPosition().getX() < 0 || agent.getPosition().getX() >= world.getEnvironment().getSurface().getWidth()
                || agent.getPosition().getY() < 0 || agent.getPosition().getY() >= world.getEnvironment().getSurface().getHeight();
        if (crashtoborder) {
            agent.statusinfo += "Crash onto world's boundaries. ";
        }
        crashtomaxlevel = agent.getPosition().getZ() >= agent.maxAllowedLevel;
        if (crashtomaxlevel) {
            agent.statusinfo += "Flying too high. ";
        }
        single = (!(crashtoenergy
                || agent.getPosition().getZ() < agent.minAllowedLevel
                || crashtomaxlevel
                || crashtoborder
                //             || _map.getLevel(agent.getPosition().getx, agent.getPosition().gety) == Sensor.
                || crashtoground));
        for (Map.Entry<String, liveBot> r : droneRecord.entrySet()) {
            if (!agent.getName().equals(r.getValue().getName())) {
                crashtotoher = agent.getPosition().isEqualTo(r.getValue().getPosition());
                multiple = multiple && !crashtotoher;
                if (crashtotoher) {
                    String other = r.getValue().getName();
                    liveBot otheragent = this.findAgent(other);
                    agent.statusinfo += "Crashed with " + other;
                    agent.alive = 0;
                    agent.placeAtSurface(agent.getPosition().to2D());
                    otheragent.statusinfo = "Crashed with " + agent.getName();
                    otheragent.alive = 0;
                    otheragent.placeAtSurface(agent.getPosition().to2D());
                }
            }
        }
        agent.alive = (single && multiple ? 1 : 0);
        agent.ontarget = (isGoal(agent).equals("") ? 0 : 1);
//        agent.lastPerceptions = agent.readPerceptions();
        if (debug) {
            agent.alive = 1;
        }
    }

    public String isGoal(liveBot agent) {
        String res = "";
        if (agent == null) {
            return res;
        }
        for (String p : this.world.listThings("PEOPLE")) {
            Thing t = world.getThing(p);
            if (agent.getPosition().isEqualTo(t.getPosition())) {
                res = p;
                agent.lastEvent = "RESCUED " + res;
                break;
            }
        }
        return res;
    }

    //
    // ----- LOW LEVEL PERCEPTIONS
    //
    public JsonObject readPerceptions(liveBot agent) {
        JsonObject res = new JsonObject();

        res = getPerceptions(agent);
        int burnt = 0;
        for (String sensor : sensorList) {
            if (!debug) {
                if (agent.capabilities.indexOf(sensor) >= 0) {
                    burnt += this.getBurnRatio(sensor);
                }
            } else {
                if (agent.capabilities.indexOf(sensor) >= 0) {
                    if (!freeMatch) {
                        burnt += this.getBurnRatio(sensor);
                    }
                }
            }
        }
        System.out.println("Burnt " + burnt + " units in perceptions");
        agent.energylevel -= burnt;
        this.checkStatus(agent);
        return res;
    }

    public JsonObject getPerceptions(liveBot agent) {
        JsonObject res = new JsonObject();

        res = agent.readPerceptions();
        for (String sensor : sensorList) {
            if (!debug) {
                if (agent.capabilities.indexOf(sensor) < 0) {
                    res.remove(sensor);
                }
            }
        }
        JsonArray perceptions = res.get("perceptions").asArray();
        for (JsonValue jsvsensor : perceptions) {
            if (jsvsensor.asObject().getString("sensor", "none").startsWith(Glossary.Sensors.DISTANCE)) {
                distancexsession = jsvsensor.asObject().get("data").asArray().get(0).asDouble();
                agent.distance = distancexsession;
            }
            if (jsvsensor.asObject().getString("sensor", "none").startsWith(Glossary.Sensors.ANGULAR)) {
                agent.angle = jsvsensor.asObject().get("data").asArray().get(0).asDouble();

            }
        }
        if (distancexproblem == -1) {
            distancexproblem = distancexsession;
        }
        JsonArray jsaaux = new JsonArray();
        for (String act : trace) {
            jsaaux.add(act);
        }
        perceptions.add(new JsonObject().add("sensor", Sensors.TRACE).add("data", jsaaux));
        jsaaux = new JsonArray();
        for (Thing t : agent.payload) {
            jsaaux.add(t.getName());
        }
        perceptions.add(new JsonObject().add("sensor", Sensors.CARGO).add("data", jsaaux));
        perceptions.add(new JsonObject().add("sensor", Sensors.STATUS).add("data", new JsonArray().add(agent.statusinfo)));
        res.set("perceptions", perceptions);
        return res;
    }

    public int getMaxVisibility(int rol) {
        switch (rol) {
            case rFLY:
                return 20;
            case rBIRD:
                return 50;
            case rHAWK:
                return 255;
            case rRESCUE:
                return 0;
            default:
                return 0;
        }
    }

    public int getRange(int rol) {
        switch (rol) {
            case rFLY:
                return 5;
            case rBIRD:
                return 11;
            case rHAWK:
                return 41;
            case rRESCUE:
                return 0;
            default:
                return 5;
        }
    }

    public int getMaxLevel(int rol) {
        switch (rol) {
            case rFLY:
                return 255;
            case rBIRD:
                return 255;
            case rHAWK:
                return 240;
            case rRESCUE:
                return 255;
            default:
                return 255;
        }
    }

    public double getFuelRate(int rol) {
        switch (rol) {
            case rFLY:
                return 0.1;
            case rBIRD:
                return 0.5;
            case rHAWK:
                return 4;
            case rRESCUE:
                return 0.5;
            default:
                return 0.1;
        }
    }

    //
    // ----- Key handling
    // 
    public String getKey() {
        if (key.length() > 0) {
            return key;
        } else {
            return "NOKEY";
        }
    }

    final public void guessNumber() {
        HACKnumberGuessing = 1 + (int) (Math.random() * 100);
    }

    final public void guessWord() {
        HACKwordGuessing = new String(HACKwords[(int) (Math.random() * HACKwords.length)]).toUpperCase();
    }

}

class myProduct {

    String reference;
    int ownerID, price, serie;
}
