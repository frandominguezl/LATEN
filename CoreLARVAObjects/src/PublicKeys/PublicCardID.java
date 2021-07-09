/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PublicKeys;

import ConfigFile.ConfigFile;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 *
 * @author lcv
 */
public class PublicCardID {
    protected JsonObject _data;
    
    public PublicCardID() {
        _data=null;
    }
    
    public boolean setData(JsonObject data) {
        boolean res=false;
        if (data!=null && data.get("shortid")!=null && data.get("cardid")!=null) {
            _data = data;
            res = true;
        } else
            _data = null;
        return res;
    }
    public boolean isValid() {
        return _data!=null && !_data.getString("shortid", "NO").equals("NO");
    }
    
    public JsonObject getData() {
        if (!isValid())
            return null;
        return _data; //Json.parse(_data.toString()).asObject();
    }
    
    public String getShortID() {
        if (!isValid()) return "";
        return _data.get("shortid").asString();
    }
    
    public String getCardID() {
        if (!isValid()) return "";
        return _data.get("cardid").asString();
    }
    
    public boolean toFile(String filename) {
        if (!isValid()) return false;
        ConfigFile filecard=new ConfigFile(filename);
        filecard._settings=_data;
        return filecard.saveConfig();
    }
    
    public boolean fromFile(String filename) {
        boolean res=false;
        ConfigFile filecard=new ConfigFile(filename);
        if (filecard.openConfig()) {
            res = setData(filecard._settings);
        }
        return res;
    }
    
    public String toString() {
        if (!isValid()) return "";
        return _data.toString(); //getShortID()+"/"+getCardID();
    }
}
