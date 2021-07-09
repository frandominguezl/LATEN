/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DashBoard;

import com.eclipsesource.json.JsonObject;


public class DashBoard {
    private JsonObject _dashb;
    public DashBoard() {
        _dashb=new JsonObject();
    }
    public DashBoard addKey(String key) {
        return setFalse(key);
    }
    
    public DashBoard addKey(String key, String value) {
        _dashb.set(key,value);
        return this;
    }
    
    public DashBoard setTrue(String key) {
        _dashb.set(key, true);
        return this;
    }
    public DashBoard setFalse(String key) {
        _dashb.set(key, false);
        return this;
    }
    
    public boolean isTrue(String key) {
        return _dashb.getBoolean(key, false);
    }
    
    public String getValue(String key) {
        return _dashb.getString(key,"");
    }
    
    public DashBoard setValue(String key, String value) {
        _dashb.set(key, value);
        return this;
    }
    
    public boolean isValue(String key, String value) {
        return _dashb.getString(key, "").equals(value);
    }
    public String getReport() {
        String res="|";
        for (JsonObject.Member m : _dashb) {
            res += m.getName()+"="+m.getValue().toString()+"|";
        }
        return res;
    }
//    public synchronized String getColoredReport(int color) {
//        String res, header, values;
//        res=setText(color)+"|";
//        header=setText(color)+"+";
//        values=setText(color)+"|";
//        for (JsonObject.Member m : _dashb) {
//            res +=setText(black)+
//            String.format("%10s", m.getName())+setText(color)+"|";
//            header+="----------+";
//            values+=setText(black)+
//            String.format("%10s", m.getValue().toString())+setText(color)+"|";
//        }
//        return header+"\n"+res+"\n"+header+"\n"+values+"\n"+header+"\n";
//    }
}
