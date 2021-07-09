/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ControlPanel;

import ConfigFile.ConfigFile;
import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static ConsoleAnsi.ConsoleAnsi.lightred;
import static ConsoleAnsi.ConsoleAnsi.red;
import static ConsoleAnsi.ConsoleAnsi.white;
import static ConsoleAnsi.ConsoleAnsi.yellow;
import Geometry.Point;
import Glossary.Sensors;
import static Glossary.Sensors.sensorList;
import Map2D.Map2DGrayscale;
import Map2D.Palette;
import World.Thing;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class TTYControlPanel extends ControlPanel {

    protected ConsoleAnsi sensors, world;
    protected AID owner;
    public static final int BACKGR = ConsoleAnsi.defColor(0.3, 0.3, 0.3), FOREGR = ConsoleAnsi.defColor(1, 1, 1), LONGFIELD = 4, offset = 7;
    public int wshiftx = 6, wshifty = 3, lastx, lasty;
    public int maxflight;
    protected Map2DGrayscale map;
    protected JsonArray things;

    public TTYControlPanel(AID o) {
        owner = o;
        sensorsBody = new HashMap<>();
        for (String s : sensorList) {
            sensorsBody.put(s, new TTYSensor(s));

        }
    }

    public TTYControlPanel(AID o, ConsoleAnsi to) {
        owner = o;
        sensorsBody = new HashMap<>();
        for (String s : sensorList) {
            sensorsBody.put(s, new TTYSensor(s));
        }
        sensors = to;
    }

    public boolean loadWorld(String worldfile) {
        boolean res = false;
        try {
            String filenameraw = "./worlds/" + worldfile;
            System.out.println("Loading map "+worldfile);
            map = new Map2DGrayscale(10, 10).loadMap(filenameraw);
            res = true;
        } catch (Exception Ex) {
            System.err.println(Ex.toString());
        }
        return res;
    }

    public boolean loadFullWorld(String worldconfigfilename) {
        boolean res = false;
        try {
            ConfigFile cfg = new ConfigFile("./worlds/" + worldconfigfilename + ".worldconf.json");
            System.out.println("Loading full world "+worldconfigfilename);
            if (cfg.openConfig()) {
                if (!loadWorld(cfg._settings.get("world").asObject().getString("surface", "none"))) {
                    return false;
                }
                things = cfg._settings.get("world").asObject().get("things").asArray();
            }
            width = this.map.getWidth();
            height = this.map.getHeight();
            res = true;
        } catch (Exception Ex) {
            System.err.println(Ex.toString());
        }
        return res;
    }

    public void showFullWorld() {
        Palette palette = new Palette().intoBW(256);
        int wsx = 6, wsy = 6;
        int range = 7, bg, fg;
        if (this.map == null) {
            return;
        }
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                int level = map.getLevel(x, y);
                if (level < 0 || this.maxflight - level <= 0) {
                    bg = red;
                    fg = lightred;
                } else {
                    bg = palette.getColor(level);
                    fg = ConsoleAnsi.negColor(bg);
                }
                this.world.setCursorXY(wsx + x, wsy + y).setText(fg).setBackground(bg);
                this.world.print(" ");
            }
        }
        // Print ludwig
        if(this.things == null)
            return;
        for (JsonValue jsthing : things) {
            JsonObject jsting = jsthing.asObject();
            JsonArray auxp = jsting.get("surface-location").asArray();
            if (auxp.get(0).asInt() < 0 || auxp.get(1).asInt() < 0) {
            } else {
                int x = (int) auxp.get(0).asDouble(), y = (int) auxp.get(1).asDouble();
                this.world.setCursorXY(wsx + x, wsy + y).setText(lightred).setBackground(lightred);
                this.world.print(" ");
            }
        }
    }

    public Map2DGrayscale getMap() {
        return this.map;
    }
    
    public final TTYControlPanel open() {
        if (world == null) {
            world = new ConsoleAnsi("WORLD", width + 10, height + 10, -(300 / width));
            world.setCursorOff().setBackground(TTYControlPanel.BACKGR).setText(white).clearScreen();
            world.setBackground(black);
            world.doRectangle(6, 6, width, height);
            world.printWRuler(6, 6, width, height, 20, width, height);
            if (map != null) {
                this.showFullWorld();
            }
        }
        if (sensors == null) {
            sensors = new ConsoleAnsi("SENSORS", 104, 83, 8).
                    setBackground(BACKGR).
                    setText(FOREGR).
                    clearScreen().setCursorOff();
        }
        return this;
    }

    public final TTYControlPanel openMicro() {
        if (sensors == null) {
            sensors = new ConsoleAnsi("SENSORS", 104, 60, 3).
                    setBackground(BACKGR).
                    setText(FOREGR).
                    clearScreen().setCursorOff();
        }
        return this;
    }

    public final TTYControlPanel openMini() {
        if (world == null) {
            world = new ConsoleAnsi("WORLD", width + 10, height + 10, -(300 / width));
            world.setCursorOff().setBackground(TTYControlPanel.BACKGR).setText(white).clearScreen();
            world.setBackground(black);
            world.doRectangle(6, 6, width, height);
            world.printWRuler(6, 6, width, height, 20, width, height);
            if (map != null) {
                this.showFullWorld();
            }
        }
        if (sensors == null) {
            sensors = new ConsoleAnsi("SENSORS", 65, 14, -7).
                    setBackground(BACKGR).
                    setText(FOREGR).
                    clearScreen().setCursorOff();
        }
        return this;
    }

    public final TTYControlPanel openMiniExt() {
        if (sensors == null) {
            sensors = new ConsoleAnsi("SENSORS", 65, 21, -7).
                    setBackground(BACKGR).
                    setText(FOREGR).
                    clearScreen().setCursorOff();
        }
        return this;
    }

    public void reset() {
        if (world != null) {
            world.clearScreen();
        }
        if (sensors != null) {
            sensors.clearScreen();
            this.sensorsBody.clear();
        }

    }

    public final TTYControlPanel close() {
        if (world != null) {
            world.waitToClose();
        } else if (sensors != null) {
            sensors.waitToClose();
        }

        if (sensors != null) {
            sensors.close();
        }
        return this;
    }

    protected TTYSensor getTTYSensor(String name) {
        return (TTYSensor) this.sensorsBody.get(name);
    }

    public ConsoleAnsi getSensors() {
        return sensors;
    }

    public ConsoleAnsi getWorld() {
        return world;
    }

    @Override
    public TTYControlPanel feedData(ACLMessage fromserver) {
//        try {
//            JsonArray body = Json.parse(fromserver.getContent()).asObject().
//                    get("details").asObject().get("perceptions").asArray();
//            for (JsonValue jsvsensor : body) {
//                JsonObject jssensor = jsvsensor.asObject();
//                getTTYSensor(jssensor.getString("sensor", "unknown")).fromJson(jssensor);
//            }
//        } catch (Exception ex) {
//
//        }
        return this;
    }

    public TTYControlPanel feedData(ACLMessage fromserver, int worldwidth, int worldheight, int maxflight) {
        try {
            JsonArray body = Json.parse(fromserver.getContent()).asObject().
                    get("details").asObject().get("perceptions").asArray();
            getTTYSensor(Sensors.TERRAIN).addRow();
            getTTYSensor(Sensors.TERRAIN).addToLastRow(0, maxflight);
            for (JsonValue jsvsensor : body) {
                JsonObject jssensor = jsvsensor.asObject();
                getTTYSensor(jssensor.getString("sensor", "unknown")).fromJson(jssensor);
                switch (jssensor.getString("sensor", "unknown")) {
                    case Sensors.GPS:
                        getTTYSensor(Sensors.TERRAIN).addToLastRow(1, getTTYSensor(Sensors.GPS).get(2, 0));
                        getTTYSensor(Sensors.TERRAIN).addToLastRow(3, getTTYSensor(Sensors.GPS).get(0, 0));
                        getTTYSensor(Sensors.TERRAIN).addToLastRow(4, getTTYSensor(Sensors.GPS).get(1, 0));
                        break;
                    case Sensors.VISUAL:
                        getTTYSensor(Sensors.TERRAIN).addToLastRow(2, getTTYSensor(Sensors.VISUAL).get(3, 3));
                        break;
                }
            }
            if (getTTYSensor("gps").get(0, 0) != BaseSensor.NOREADING) {
                gpsx = (int) getTTYSensor("gps").get(0, 0);
                gpsy = (int) getTTYSensor("gps").get(1, 0);
                width = worldwidth;
                height = worldheight;
                this.maxflight = maxflight;
//                if (world == null) {
//                    world = new ConsoleAnsi("WORLD", width + 10, height + 10, -(300 / width));
//                    world.setCursorOff().setBackground(TTYControlPanel.BACKGR).setText(white).clearScreen();
//                    world.setBackground(black);
//                    world.doRectangle(6, 6, width, height);
//                    world.printWRuler(6, 6, width, height, 20, width, height);
//
//                }
            }
            if (getTTYSensor(Glossary.Sensors.VISUAL).getMemorySize() == 0) {
                getTTYSensor(Glossary.Sensors.VISUAL).setMaxReading(maxflight);
                getTTYSensor(Glossary.Sensors.VISUAL).setMemoryRange(worldwidth + 7, worldheight + 7);
            }

        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
        return this;
    }

    public TTYControlPanel plainShow() {
        ConsoleAnsi plain = new ConsoleAnsi("inter");
        plain.println("AGENT: " + owner.getLocalName());
        for (String s : sensorList) {
            getTTYSensor(s).ShowPlain(plain);
        }
        return this;
    }

    public TTYControlPanel fancyShow() {
        int maxdistance = width + height, maxthermal = (int) (width * 0.75), maxlevel = 256, maxenergy = 1000;
        if (sensors == null) {
            open();
            sensors.doFrameTitle("AGENT: " + owner.getLocalName(), 1, 1, 104, 100);
        }
        int xorigen = 2, yorigen = 2, xbase = xorigen, ybase = yorigen;
        getTTYSensor(Glossary.Sensors.ALIVE).ShowBoolean(sensors, xbase, ybase);
        getTTYSensor(Glossary.Sensors.ONTARGET).ShowBoolean(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.PAYLOAD).ShowValues(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.GPS).ShowValues(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.ENERGY).ShowHProgressBar(sensors, xbase += 16, ybase, 54, 100, maxenergy);
        getTTYSensor(Glossary.Sensors.STATUS).ShowStream(sensors, xbase = xorigen, ybase += 3, 21, 1);
        getTTYSensor(Glossary.Sensors.DISTANCE).ShowHProgressBar(sensors, xbase = xorigen, ybase += 3, 100, (int) (maxdistance / 20), maxdistance);
        getTTYSensor(Glossary.Sensors.ALTITUDE).ShowHProgressBar(sensors, xbase, ybase += 5, 100, (int) (maxlevel / 10), maxlevel);
        getTTYSensor(Glossary.Sensors.COMPASS).ShowAngular(sensors, xbase, ybase += 5);
        getTTYSensor(Glossary.Sensors.ANGULAR).ShowAngular(sensors, xbase += 30, ybase);
        getTTYSensor(Glossary.Sensors.VISUAL).ShowVisual(sensors, xbase += 30, ybase);
        if (world != null) {
            if (map == null) {
                getTTYSensor(Glossary.Sensors.VISUAL).ShowMemory(world, gpsx, gpsy);
            } else {
                getTTYSensor(Glossary.Sensors.VISUAL).ShowLocation(world, gpsx, gpsy);
            }
        }
        getTTYSensor(Glossary.Sensors.LIDAR).ShowDepth(sensors, xbase = xorigen, ybase += 16);
        getTTYSensor(Glossary.Sensors.TERRAIN).ShowFlight(sensors, xbase +=40, ybase , 41, 16);
        getTTYSensor(Glossary.Sensors.TRACE).ShowStream(sensors, xbase += 41, ybase, 10, 14);
        getTTYSensor(Glossary.Sensors.THERMAL).ShowThermalMini(sensors, xbase = xorigen, ybase+=16, maxthermal);
        getTTYSensor(Glossary.Sensors.CARGO).ShowStreamMini(sensors, xbase += 65, ybase, 15, 10);
        
        return this;
    }

    public TTYControlPanel fancyShowMini() {
        int maxdistance = width + height, maxthermal = (int) (width * 0.75), maxlevel = 256, maxenergy = 1000;
        if (sensors == null) {
            openMini();
            sensors.setCursorXY(1, 1).print("AGENT: " + owner.getLocalName());
        }
        int xorigen = 1, yorigen = 2, xbase = xorigen, ybase = yorigen;
        getTTYSensor(Glossary.Sensors.STATUS).ShowStreamMini(sensors, xbase = xorigen, ybase, 28, 1);
        getTTYSensor(Glossary.Sensors.CARGO).ShowStreamMini(sensors, xbase += 30, ybase, 10, 10);
        getTTYSensor(Glossary.Sensors.ALIVE).ShowBooleanMini(sensors, xbase = xorigen, ybase += 2);
        getTTYSensor(Glossary.Sensors.ONTARGET).ShowBooleanMini(sensors, xbase += 8, ybase);
        getTTYSensor(Glossary.Sensors.GPS).ShowValuesMini(sensors, xbase += 8, ybase);
        getTTYSensor(Glossary.Sensors.ENERGY).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 10, maxenergy);
        getTTYSensor(Glossary.Sensors.COMPASS).ShowAngularMini(sensors, xbase += 18, ybase);
        getTTYSensor(Glossary.Sensors.ANGULAR).ShowAngularMini(sensors, xbase += 6, ybase);
        getTTYSensor(Glossary.Sensors.DISTANCE).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 10, maxdistance);
        getTTYSensor(Glossary.Sensors.ALTITUDE).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 10, maxlevel);
        if (world != null) {
            if (map == null) {
                getTTYSensor(Glossary.Sensors.VISUAL).ShowMemory(world, gpsx, gpsy);
            } else {
                getTTYSensor(Glossary.Sensors.VISUAL).ShowLocation(world, gpsx, gpsy);
            }
        }
        return this;
    }

    public TTYControlPanel fancyShowMiniExt() {
        int maxdistance = width + height, maxthermal = (int) (width * 0.75), maxlevel = 256, maxenergy = 1000;
        if (sensors == null) {
            openMini();
            sensors.setCursorXY(1, 1).print("AGENT: " + owner.getLocalName());
        }
        int xorigen = 1, yorigen = 2, xbase = xorigen, ybase = yorigen;
        getTTYSensor(Glossary.Sensors.STATUS).ShowStreamMini(sensors, xbase = xorigen, ybase, 28, 1);
        getTTYSensor(Glossary.Sensors.TRACE).ShowStreamMini(sensors, xbase += 30, ybase, 15, 10);
        getTTYSensor(Glossary.Sensors.CARGO).ShowStreamMini(sensors, xbase += 17, ybase, 10, 10);
        getTTYSensor(Glossary.Sensors.ALIVE).ShowBooleanMini(sensors, xbase = xorigen, ybase += 2);
        getTTYSensor(Glossary.Sensors.ONTARGET).ShowBooleanMini(sensors, xbase += 8, ybase);
        getTTYSensor(Glossary.Sensors.GPS).ShowValuesMini(sensors, xbase += 8, ybase);
        getTTYSensor(Glossary.Sensors.ENERGY).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 10, maxenergy);
        getTTYSensor(Glossary.Sensors.COMPASS).ShowAngularMini(sensors, xbase += 18, ybase);
        getTTYSensor(Glossary.Sensors.ANGULAR).ShowAngularMini(sensors, xbase += 6, ybase);
        getTTYSensor(Glossary.Sensors.DISTANCE).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 11, maxdistance);
        getTTYSensor(Glossary.Sensors.ALTITUDE).ShowHProgressBarMini(sensors, xbase = xorigen, ybase += 2, 11, maxlevel);
        getTTYSensor(Glossary.Sensors.VISUAL).ShowVisualMini(sensors, xbase = xorigen, ybase += 2);
        getTTYSensor(Glossary.Sensors.LIDAR).ShowDepthMini(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.THERMAL).ShowThermalMini(sensors, xbase += 10, ybase, maxthermal);
        return this;
    }

    public TTYControlPanel fancyShowMicro() {
        int maxdistance = width + height, maxthermal = (int) (width * 0.75), maxlevel = 256, maxenergy = 1000;
        if (sensors == null) {
            openMicro();
            sensors.doFrameTitle("AGENT: " + owner.getLocalName(), 1, 1, 104, 100);
        }
        int xorigen = 2, yorigen = 2, xbase = xorigen, ybase = yorigen;
        getTTYSensor(Glossary.Sensors.ALIVE).ShowBoolean(sensors, xbase, ybase);
        getTTYSensor(Glossary.Sensors.ONTARGET).ShowBoolean(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.PAYLOAD).ShowValues(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.GPS).ShowValues(sensors, xbase += 10, ybase);
        getTTYSensor(Glossary.Sensors.ENERGY).ShowHProgressBar(sensors, xbase += 16, ybase, 54, 100, maxenergy);
        getTTYSensor(Glossary.Sensors.STATUS).ShowStream(sensors, xbase = xorigen, ybase += 3, 21, 1);
        getTTYSensor(Glossary.Sensors.DISTANCE).ShowHProgressBar(sensors, xbase = xorigen, ybase += 3, 100, (int) (maxdistance / 20), maxdistance);
        getTTYSensor(Glossary.Sensors.ALTITUDE).ShowHProgressBar(sensors, xbase, ybase += 5, 100, (int) (maxlevel / 10), maxlevel);
        getTTYSensor(Glossary.Sensors.COMPASS).ShowAngular(sensors, xbase, ybase += 5);
        getTTYSensor(Glossary.Sensors.ANGULAR).ShowAngular(sensors, xbase += 30, ybase);
        getTTYSensor(Glossary.Sensors.VISUAL).ShowVisual(sensors, xbase += 30, ybase);
        getTTYSensor(Glossary.Sensors.LIDAR).ShowDepth(sensors, xbase = xorigen, ybase += 16);
        getTTYSensor(Glossary.Sensors.THERMAL).ShowThermal(sensors, xbase += 40, ybase, maxthermal);
        getTTYSensor(Glossary.Sensors.TRACE).ShowStream(sensors, xbase += 40, ybase, 10, 14);
        getTTYSensor(Glossary.Sensors.TERRAIN).ShowFlight(sensors, xbase = xorigen, ybase += 16, 100, 9);
        return this;
    }

}
