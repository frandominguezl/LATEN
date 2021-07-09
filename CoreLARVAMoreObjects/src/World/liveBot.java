/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package World;

import Geometry.Point;
import World.Thing;
import World.World;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.awt.Color;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class liveBot extends Thing {

    public String role, groupname;
    public int energylevel, burnmovement, burnsensor, compass, altitude, order;
    public double distance, angle;
    public Point origin;
    public int minAllowedLevel, maxAllowedLevel, range, alive, ontarget;
    public ArrayList<String> capabilities, coins;
    public JsonObject lastPerceptions;
    public String statusinfo;
    public ArrayList<Thing> payload;
    public Color colorcode;
    public String lastEvent, relpywith;

    public liveBot(String name) {
        super(name);
        capabilities = new ArrayList<>();
        payload = new ArrayList<>();
        statusinfo = "Fresh new";
        lastEvent=statusinfo;
    }

    public liveBot(String name, World w) {
        super(name, w);
        capabilities = new ArrayList<>();
        payload = new ArrayList<>();
        statusinfo = "Fresh new";
        lastEvent=statusinfo;
    }
    
    public boolean isAtBase() {
        return getPosition().isEqualTo(origin);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsdrone = new JsonObject();
        jsdrone.set("name", getName());
        jsdrone.set("team", groupname);
        jsdrone.set("last", lastEvent);
        jsdrone.set("ontarget", ontarget);
        jsdrone.set("alive", alive);
        jsdrone.set("x", getPosition().getX());
        jsdrone.set("y", getPosition().getY());
        jsdrone.set("z", getPosition().getZ());
        jsdrone.set("energy", energylevel);
        jsdrone.set("altitude", altitude);
        jsdrone.set("distance", distance);
        jsdrone.set("angle", angle);
        jsdrone.set("compass", this.compass);
        jsdrone.set("payload", getFullPayload());
        return jsdrone;
    }

    public void fromJson(JsonObject update) {
        _position = new Point(update.getInt("x", -1),
                update.getInt("y", -1), update.getInt("z", -1));
        energylevel = update.getInt("energy", -1);
        altitude = update.getInt("altitude", -1);
        angle = update.getDouble("angle", 0);
        distance = update.getDouble("distance", 0);
        _name = update.getString("name", "unknown");
        groupname = update.getString("team", "unknown");
        this.lastEvent = update.getString("last", "---");
        ontarget = update.getInt("ontarget", -1);
        alive = update.getInt("alive", -1);
        payload = new ArrayList();
        for (JsonValue jsv : update.get("payload").asArray()) {
            payload.add(new Thing(jsv.asString()));
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override

    public int getEnergy() {
        return energylevel;
    }

    @Override
    public int getOnTarget() {
        return ontarget;
    }

    @Override
    public int getAlive() {
        return alive;
    }

    @Override
    public int getPayload() {
        return payload.size();
    }
    
    public String getStatus(){
        return this.statusinfo;
    }

    public void setStatus(String s){
        statusinfo = s;
    }

    public JsonArray getFullPayload() {
        JsonArray jspl = new JsonArray();
        for (Thing t : payload) {
            jspl.add(t.getName());
        }
        return jspl;

    }

    @Override
    public Point getPosition() {
        return this._position;
    }

}
