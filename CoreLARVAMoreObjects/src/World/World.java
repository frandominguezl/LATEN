/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package World;

import ConfigFile.ConfigFile;
import Geometry.Point;
import Geometry.Vector;
import Map2D.Map2DGrayscale;
import Ontology.Ontology;
import World.Perceptor.ATTACH;
import World.Perceptor.OPERATION;
import World.Perceptor.SELECTION;
import World.Thing.PROPERTY;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author lcv
 */
public class World {

    protected Thing _environment;
    protected HashMap<String, Thing> _population;
    protected HashMap<PROPERTY, ArrayList<Thing>> _visibility;
    protected Ontology _ontology;
    protected String _name;
    protected boolean _godmode = false, debug = false;
    protected double _rotx[] = new double[]{-0.5, -0.25, 0.0, 0.25, -0.5, -1.3, -1, -0.7},
            _roty[] = new double[]{-1.0, -0.75, -0.5, -.25, 0, -.25, -0.5, -0.75},
            _incr[] = new double[]{0, 0.45, 0, -.54, 0, .55, 0, -0.55};
    protected JsonObject _config;

    public World(String name) {
        _name = name;
        _population = new HashMap();
        _visibility = new HashMap<>();
        _ontology = new Ontology().add("THING", Ontology.ROOT).add("ENVIRONMENT", "THING").add("OBJECT", "THING");
    }

    public String getName() {
        return this._name;
    }

    public boolean loadConfig(String worldconfigfilename) {
        ConfigFile cfg = new ConfigFile("./worlds/" + worldconfigfilename + ".worldconf.json");
        if (cfg.openConfig()) {
            try {
                _config = cfg._settings;;
                JsonObject aux = _config;
                this._name = aux.getString("name", "unknownw");
                for (Member o : aux.get("types").asObject()) {
                    this.getOntology().add(o.getName(), o.getValue().asString());
                }
                aux = aux.get("world").asObject();
                Thing e = setEnvironment(aux.getString("name", "nonamed")).getEnvironment();
                e.setPosition(new Point(0, 0, 0));
                e.setOrientation(Compass.NORTH);
                Map2DGrayscale terrain = new Map2DGrayscale(10, 10, 0);
                String filenameraw = "./worlds/" + aux.getString("surface", "none"), filenamefixed = filenameraw.replace(".png", "_fixed.png");
//                if (new File(filenamefixed).exists()) {
//                    terrain.loadMap(filenamefixed);
//                } else {
//                    terrain.loadAndFixMap(filenameraw);                    
//                }
                terrain.loadMap(filenameraw);
                e.setSurface(terrain);
                e.setSize(new Point(this._environment._surface.getWidth(),
                        this._environment._surface.getHeight(), 0));

                for (JsonValue jsthing : aux.get("things").asArray()) {
                    JsonObject jsting = jsthing.asObject();
                    JsonArray jsproperties = jsting.get("properties").asArray();
                    PROPERTY[] props = new PROPERTY[jsproperties.size()];
                    for (int i = 0; i < jsproperties.size(); i++) {
                        props[i] = PROPERTY.valueOf(jsproperties.get(i).asString().toUpperCase());
                    }
                    e = new Thing(jsting.getString("name", "nonamed"), this);
                    e.setType(jsting.getString("type", ""));
                    this.addThing(e, props);
                    JsonArray auxp = jsting.get("surface-location").asArray();
                    if (auxp.get(0).asInt() < 0 || auxp.get(1).asInt() < 0) {
                        int rx, ry;
                        boolean valid = true;
                        do{
                            rx = (int) (Math.random() * terrain.getWidth());
                            ry = (int) (Math.random() * terrain.getHeight());
                            for(Thing myt : this._population.values()) {
                                if (!myt.getName().equals(this.getName())&& myt.getType().equals("PEOPLE") && myt.getPosition().to2D().fastDistanceXYTo(new Point(rx,ry))<5) {
                                    valid = false;
                                }
                            }
                        } while (!valid);
                        e.placeAtSurface(new Point(rx, ry));
                    } else {
                        e.placeAtSurface(new Point(auxp.get(0).asDouble(), auxp.get(1).asDouble(), 0));
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex.toString());
                return false;
            }
            return true;
        }
        return false;
    }

    public JsonObject getConfig() {
        return _config;
    }

    public World setOntology(Ontology o) {
        _ontology = o;
        return this;
    }

    public Ontology getOntology() {
        return _ontology;
    }

    public World setEnvironment(String name) {
        _environment = addThing(name, "ENVIRONMENT",
                new PROPERTY[]{PROPERTY.SURFACE, PROPERTY.POSITION, PROPERTY.ORIENTATION});
        _environment.setSize(new Point(1, 1, 0));
        _environment.setOrientation(Compass.NORTH);
        return this;
    }

    public Thing getEnvironment() {
        return _environment;
    }

    public Thing addThing(Thing i, PROPERTY[] visible) {
        if (i.getPosition() == null) {
            if (!i.getType().equals("ENVIRONMENT")) {
                i.setPosition(getEnvironment().getPosition());
                i.setOrientation(getEnvironment().getOrientation());
            }
        }
        if (i.getSize() == null) {
            i.setSize(new Point(1, 1, 0));
        }
        _population.put(i.getId(), i);
        for (int ch = 0; ch < visible.length; ch++) {
            addVisible(visible[ch], i);
        }
        return i;
    }

    public void removeThing(Thing i) {
        _population.remove(i.getId());
        for (PROPERTY p : PROPERTY.values()) {
            if (this._visibility.get(p) != null
                    && this._visibility.get(p).contains(i)) {
                this._visibility.get(p).remove(i);
            }
        }
    }

    public Thing addThing(String name, PROPERTY[] visible) {
        return addThing(name, _ontology.getRootType(), visible);
    }

    public Thing addThing(String name, String type, PROPERTY[] visible) {
        Thing i = new Thing(name, this);
        i.setType(type);
        return addThing(i, visible);
    }

    public Thing getThing(String id) {
        return _population.get(id);
    }

    public Thing getThingByName(String name) {
        Thing res = null;
        for (Thing t : _population.values()) {
            if (t.getName().equals(name)) {
                res = t;
            }
        }
        return res;
    }

    public boolean findThing(String id) {
        return _population.containsKey(id);
    }

    public Set<String> listThings() {
        return _population.keySet();
    }

    public ArrayList<String> listThings(String type) {
        ArrayList<String> list = new ArrayList<>();
        for (String s : _population.keySet()) {
            Thing t = this.getThing(s);
            if (this.getOntology().isSubTypeOf(t.getType(), type)) {
                list.add(s);
            }
        }
        return list;
    }

    public World addVisible(PROPERTY c, Thing t) {
        if (_visibility.get(c) == null) {
            _visibility.put(c, new ArrayList<>());
        }
        _visibility.get(c).add(t);
        return this;
    }

    public ArrayList<Thing> getDetectableList(Perceptor p) {
        PROPERTY property = p.getProperty();
        String type = p.getType();
        Thing who = p.getOwner();
        ArrayList<Thing> detectable = new ArrayList<>();

        // Intern sensors only perceive oneself
        if (p.getSelection() == SELECTION.INTERN) {
            detectable.add(who);
            return detectable;
        }
        // An Thing is detectable if the propoerty of the sensor is
        // visible on it and is the detected type
        for (Thing t : _visibility.get(property)) {
            if (!t.equals(who) && _ontology.matchTypes(t.getType(), type)) {
                detectable.add(t);
            }
        }
        // Sometimes only the closest Thing is detected
        if (p.getSelection() == SELECTION.CLOSEST && detectable.size() > 1) {
            Point mypos = p.getOwner().getPosition(), yourpos;
            double shortest = Double.MAX_VALUE, distance;
            Thing best = null, ti;
            for (int i = 0; i < detectable.size(); i++) {
                ti = detectable.get(i);
                yourpos = ti.getPosition();
                distance = mypos.fastDistanceXYTo(yourpos);
                if (distance < shortest) {
                    shortest = distance;
                    best = ti;
                }
            }
            detectable.clear();
            detectable.add(best);
        }
        return detectable;
    }

    public JsonObject getPerception(Perceptor p) {
        JsonObject res = new JsonObject(), owned, detected, partialres = null;
        JsonArray allreadings = new JsonArray(), xyreading, rowreading, coordinates = new JsonArray();
        Thing who = p.getOwner();
        PROPERTY property = p.getProperty();
        OPERATION operation = p.getOperation();
        ATTACH attachment = p.getAttachment();
        ArrayList<Thing> detectable = getDetectableList(p);
        Point point, pini, pend;
        Vector vectororientation;
        point = who.getPosition();

        vectororientation = who.getVector();
        int range = p.getRange(), orientation = who.getOrientation();
        Point prange, observable;
        double x1, y1, x2, y2, incrx;
        if (range == 1) { // single rangle
            x1 = point.getX();
            y1 = point.getY();
            incrx = 0;
        } else {        // multiple range
            if (p.getAttachment() == ATTACH.ZENITAL) {
                x1 = point.getX() - range / 2;
                y1 = point.getY() - range / 2;
                incrx = 0;
            } else {
                x1 = point.getX() + (range - 1) * _rotx[orientation];
                y1 = point.getY() + (range - 1) * _roty[orientation];
//                x1 = point.getX() + (range - 1) * interpolate(_rotx,alpha);
//                y1 = point.getY() + (range - 1) * interpolate(_roty,alpha);
                incrx = _incr[orientation];
            }
        }
        // Scans the world within the selected range (1x1 | nxn) and detects readings
        for (double sy = y1; sy < y1 + range; sy++) {
            rowreading = new JsonArray();
            for (double sx = x1; sx < x1 + range; sx++) {
                xyreading = new JsonArray();
                // For every position xy check all the potentially detected objects
                for (Thing t : detectable) {
                    // Start reading properties
                    observable = new Point(sx, sy, this.getEnvironment().getSurface().getLevel(sx, sy));
//                    if (property == PROPERTY.SURFACE || property==PROPERTY.PRESENCE) {
//                        observable = new Point(sx, sy, t.getSurface().getLevel(sx, sy));
//                    } else {
//                        observable = new Point(sx, sy);
//                    }
                    if (observable.fastDistanceXYTo(t.getPosition()) <= p.getSensitivity()) {
                        if (operation == OPERATION.QUERY) {
                            if (property == PROPERTY.ENERGY) {
                                partialres = new JsonObject().add("value", t.getEnergy());
                            }
                            if (property == PROPERTY.STATUS) {
                                partialres = new JsonObject().add("value", t.getAlive());
                            }
                            if (property == PROPERTY.ONTARGET) {
                                partialres = new JsonObject().add("value", t.getOnTarget());
                            }
                            if (property == PROPERTY.PAYLOAD) {
                                partialres = new JsonObject().add("value", t.getPayload());
                            }
                            if (property == PROPERTY.POSITION) {
                                partialres = new JsonObject().add("value", t.getPosition().toJson());
                            }
                            if (property == PROPERTY.ORIENTATION) {
                                partialres = new JsonObject().add("value", t.getVector().toJson());
                            }
                            if (property == PROPERTY.PRESENCE) {
                                partialres = new JsonObject().add("value", (t.contains(observable) ? 100 : 0));

                            }
                            if (property == PROPERTY.SURFACE) {
                                int value = t.getSurface().getLevel(observable.getX(), observable.getY());
                                if (value != -1) {
                                    partialres = new JsonObject().add("value", value);
                                } else {
                                    partialres = new JsonObject().add("value", Perceptor.NULLREAD);
                                }
                            }
                        }
                        if (operation == OPERATION.DISTANCE) {
                            if (property == PROPERTY.POSITION) {
                                partialres = new JsonObject().add("value", observable.realDistanceTo(t.getPosition().to2D()));
                            }
                            if (property == PROPERTY.SURFACE) {
                                int value = t.getSurface().getLevel(observable.getX(), observable.getY());
                                if (value >= 0) { //(value != -1) {
                                    partialres = new JsonObject().add("value", who.getPosition().getZ() - value);
                                } else {
                                    partialres = new JsonObject().add("value", Perceptor.NULLREAD);
                                }
                            }
                        }
                        if (operation == OPERATION.ANGLE) {
                            if (property == PROPERTY.POSITION) {
                                if (point.to2D().isEqualTo(t.getPosition().to2D())) {
                                    partialres = new JsonObject().add("value", 0);
                                } else if (attachment == ATTACH.FRONTAL) {
                                    partialres = new JsonObject().add("value", vectororientation.angleXYTo(t.getPosition()));
                                } else {
                                    partialres = new JsonObject().add("value", Compass.VECTOR[Compass.NORTH].angleXYTo(new Vector(observable, t.getPosition())));
                                }
                            }
                            if (property == PROPERTY.ORIENTATION) {
                                partialres = new JsonObject().add("value", Compass.VECTOR[Compass.NORTH].angleXYTo(who.getVector()));
                            }

                        }
                        if (_godmode || p.getName().contains("_GOD_")) {
                            xyreading.add(partialres.merge(new JsonObject().add("name", t.getName())));
                        } else {
                            xyreading.add(partialres.get("value"));
                        }
                    } else {
                        if (p.getName().toUpperCase().startsWith("THERMAL")) {
                            xyreading.add(p.getSensitivity());
                        } else {
                            xyreading.add(Perceptor.NULLREAD);
                        }
                    }
                }
                if (xyreading.size() == 0) {
                    xyreading.add(Perceptor.NULLREAD);
                }
                if (xyreading.size() == 1) {
                    rowreading.add(xyreading.get(0));
                } else {
                    if (xyreading.get(0).isNumber()) {
//                        int max = xyreading.get(0).asInt();
//                        for (JsonValue v : xyreading) {
//                            if (v.asInt() > max) {
//                                max = v.asInt();
//                            }
//                        }
//                        rowreading.add(max);
                        double min = xyreading.get(0).asDouble();
                        for (JsonValue v : xyreading) {
                            if (v.asDouble() < min) {
                                min = v.asDouble();
                            }
                        }
                        rowreading.add(min);
                    } else {
                        rowreading.add(xyreading);
                    }
                    //coordinates.add(new JsonObject().add("xy", new Point(sx, sy).toJson()));
                }
            }
            if (rowreading.size() == 1) {
                allreadings.add(rowreading.get(0));
            } else {
                allreadings.add(rowreading);
                coordinates.add(new Point(Math.round(x1), Math.round(sy)).toJson());
            }
            x1 += incrx;
        }
        if (p.getRange() == 1) {
            return res.add("sensor", p.getName().replace("delux", "").replace("hq","")).add("data", allreadings);
        } else {
            return res.add("sensor", p.getName().replace("delux", "").replace("hq","")).add("data", allreadings);
        }
    }
}
//    public JsonObject oldgetPerception(Perceptor p) {
//        JsonObject res = new JsonObject(), owned, detected, partialres = null;
//        JsonArray allreadings = new JsonArray(), xyreading;
//        Thing who = p.getOwner();
//        PROPERTY property = p.getProperty();
//        OPERATION operation = p.getOperation();
//        ATTACH attachment = p.getAttachment();
//        ArrayList<Thing> detectable = getDetectableList(p);
//        Point point, pini, pend;
//        Vector orientation;
//        point = who.getPosition();
//        orientation = who.getVector();
//        int range = p.getRange();
//        double x1, y1, x2, y2;
//        if (range == 1) { // single rangle
//            x1 = x2 = point.getX();
//            y1 = y2 = point.getY();
//        } else {        // multiple range
//            Vector vp1, vp1p2;
//            if (p.getAttachment() == ATTACH.FRONTAL) {
//                vp1 = Compass.VECTOR[Entity.rotateLeft(Entity.rotateLeft(who.getOrientation()))].clone().scalar(range / 2);
//                vp1p2 = Compass.VECTOR[Entity.rotateRight(who.getOrientation())].clone().scalar(range - 1);
//            } else if (p.getAttachment() == ATTACH.LEFT) {
//                vp1 = Compass.VECTOR[who.getOrientation()].clone().scalar(range / 2);
//                vp1p2 = Compass.VECTOR[Entity.rotateLeft(Entity.rotateLeft(Entity.rotateLeft(who.getOrientation())))].clone().scalar(range - 1);
//            } else if (p.getAttachment() == ATTACH.RIGHT) {
//                vp1 = Compass.VECTOR[Entity.Opposite(who.getOrientation())].clone().scalar(range / 2);
//                vp1p2 = Compass.VECTOR[who.getOrientation()].clone().scalar(range - 1);
//            } else { //if (p.getAttachment() == ATTACH.ZENITAL) {
//                vp1 = Compass.VECTOR[Compass.NORTHWEST].clone().scalar(range / 2);
//                vp1p2 = Compass.VECTOR[Compass.SOUTHEAST].clone().scalar(range - 1);
//            }
//            pini = point.clone().plus(vp1);
//            pend = pini.clone().plus(vp1p2);
//            x1 = (int) (Math.round(Math.min(pini.getX(), pend.getX())));
//            y1 = (int) (Math.round(Math.min(pini.getY(), pend.getY())));
//            x2 = (int) (Math.round(Math.max(pini.getX(), pend.getX())));
//            y2 = (int) (Math.round(Math.max(pini.getY(), pend.getY())));
//        }
//        for (double sy = y1; sy <= y2; sy++) {
//            for (double sx = x1; sx <= x2; sx++) {
//                Point observable = new Point(sx, sy);
//                xyreading = new JsonArray();
//                for (Thing t : detectable) {
//                    if (observable.fastDistanceTo(t.getPosition()) < p.getSensitivity()) {
//                        if (operation == OPERATION.QUERY) {
//                            if (property == PROPERTY.POSITION) {
//                                partialres = new JsonObject().add("value", t.getPosition().toJson());
//                            }
//                            if (property == PROPERTY.ORIENTATION) {
//                                partialres = new JsonObject().add("value", t.getVector().toJson());
//                            }
//                            if (property == PROPERTY.PRESENCE) {
//                                partialres = new JsonObject().add("value", t.contains(observable));
//
//                            }
//                            if (property == PROPERTY.SURFACE) {
//                                int value = t.getSurface().getLevel((int) observable.getX(), (int) observable.getY());
//                                if (value != -1) {
//                                    partialres = new JsonObject().add("value", value);
//                                } else {
//                                    partialres = new JsonObject().add("value", Perceptor.NULLREAD);
//                                }
//                            }
//                        }
//                        if (operation == OPERATION.DISTANCE) {
//                            if (property == PROPERTY.POSITION) {
//                                partialres = new JsonObject().add("value", observable.realDistanceTo(t.getPosition()));
//                            }
//                            if (property == PROPERTY.SURFACE) {
//                                int value = t.getSurface().getLevel((int) observable.getX(), (int) observable.getY());
//                                if (value != -1) {
//                                    partialres = new JsonObject().add("value", who.getPosition().getZ() - value);
//                                } else {
//                                    partialres = new JsonObject().add("value", Perceptor.NULLREAD);
//                                }
//                            }
//                        }
//                        if (operation == OPERATION.ANGLE) {
//                            if (property == PROPERTY.POSITION) {
//                                if (point.to2D().isEqualTo(t.getPosition().to2D())) {
//                                    partialres = new JsonObject().add("value", 0);
//                                } else if (attachment == ATTACH.FRONTAL) {
//                                    partialres = new JsonObject().add("value", orientation.angleXYTo(t.getPosition()));
//                                } else {
//                                    partialres = new JsonObject().add("value", Compass.VECTOR[Compass.NORTH].angleXYTo(new Vector(observable, t.getPosition())));
//                                }
//                            }
//                            if (property == PROPERTY.ORIENTATION) {
//                                partialres = new JsonObject().add("value", Compass.VECTOR[Compass.NORTH].angleXYTo(who.getVector()));
//                            }
//
//                        }
//                        if (_godmode) {
//                            xyreading.add(partialres.merge(new JsonObject().add("name", t.getName())));
//                        } else {
//                            xyreading.add(partialres.get("value"));
//                        }
//                    }
//                }
//                if (xyreading.size() == 1) {
//                    allreadings.add(xyreading.get(0));
//                } else {
//                    allreadings = xyreading;
//                }
//            }
//        }
//        if (p.getRange() == 1) {
//            return res.add("sensor", p.getName()).add("data", allreadings);
//        } else {
//            return res.add("sensor", p.getName()).add("data", allreadings).add("range_from", new Point(x1, y1).toJson()).add("range_to", new Point(x2, y2).toJson());
//        }
//    }
//    // Handling rotations
//    public static double interpolate(double values[], double angle) {
//        double res = 0;
//        int prev = (int) Math.floor(angle / 45), next = (int) Math.round(angle / 45);
//        prev = prev % 8;
//        next = next % 8;
//        return (values[prev] + values[next]) / 2;
//    }
