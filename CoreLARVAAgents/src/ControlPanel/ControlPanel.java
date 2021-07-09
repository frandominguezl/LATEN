/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ControlPanel;

import ConsoleAnsi.ConsoleAnsi;
import static Glossary.Sensors.sensorList;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.lang.acl.ACLMessage;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class ControlPanel {
    
    protected JsonArray sensorsReadings;
    protected HashMap<String, BaseSensor> sensorsBody;
    protected int gpsx, gpsy, width, height;
    

    public ControlPanel() {
        sensorsBody = new HashMap<>();
        for (String s : sensorList) {
            sensorsBody.put(s, new BaseSensor(s));

        }
    }

    public ControlPanel feedData(ACLMessage fromserver) {
        try {
            JsonArray body = Json.parse(fromserver.getContent()).asObject().
                    get("details").asObject().get("perceptions").asArray();
            for (JsonValue jsvsensor : body) {
                JsonObject jssensor = jsvsensor.asObject();
                sensorsBody.get(jssensor.getString("sensor", "unknown")).fromJson(jssensor);
            }
        } catch (Exception ex) {

        }
        return this;
    }
    
    public boolean getBoolean(String sensorname) {
        return sensorsBody.get(sensorname).getBoolean();
    }
    
    public int getInt(String sensorname) {
        return (int) (sensorsBody.get(sensorname).getInt());
    }
    
    public double getDouble(String sensorname) {
        return sensorsBody.get(sensorname).getDouble();
    }
    
    // (0,0) en el medio del sensor
    public int getCenteredInt(String sensorname, int x, int y) {
        return sensorsBody.get(sensorname).getValue(x+sensorsBody.get(sensorname).dimx/2,y+sensorsBody.get(sensorname).dimy/2);
    }
}
