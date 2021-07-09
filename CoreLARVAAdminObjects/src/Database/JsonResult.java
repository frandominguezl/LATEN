/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Database;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 *
 * @author lcv
 */
public class JsonResult {

    protected JsonObject _resultset;

//    public JsonResult() {
//        _resultset = new JsonObject();
//    }
//    public JsonResult(JsonObject copy) {
//        _resultset = copy;
//    }
//    
//    
    public JsonResult(ResultSet rs) {
        JsonObject aux;
        _resultset = new JsonObject();
        String key;

        try {
            // get column names
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCnt = rsMeta.getColumnCount();
            String name;
            while (rs.next()) {
                aux = new JsonObject();
                key = "";
                for (int i = 1; i <= columnCnt; i++) {
                    name = rsMeta.getColumnName(i);
                    switch (rsMeta.getColumnType(i)) {
                        case java.sql.Types.INTEGER:
                            aux.add(name, rs.getInt(name));
                            if (i == 1) {
                                key = "" + rs.getInt(name);
                            }
                            break;
                        case -7:
                            aux.add(name, rs.getBoolean(name));
                            break;
                        case java.sql.Types.VARCHAR:
                        case java.sql.Types.LONGVARCHAR:
                        default:
                            aux.add(name, rs.getString(name));
                            if (i == 1) {
                                key = "" + rs.getString(name);
                            }
                            break;
                    }
                }
                _resultset.add(key, aux);
            }
        } catch (Exception ex) {
            System.err.println("JSONRESULT:: "+ex.toString());
        }

    }

    public int size() {
        return _resultset.size();
    }
    
    public JsonObject getRowByIndex(int index) {
        JsonObject res=new JsonObject();
        if (index < 0 || index >= size())
            return res;
        for (Member m : _resultset)  {
            if (index == 0)
                return m.getValue().asObject();
            index --;
        }
           
        return res;
    }
    
    public JsonObject getRowByPK(String pk) {
        JsonObject bad=new JsonObject(), good =_resultset.get(pk).asObject();
        if (good != null)
            return good;
        return bad;
    }
    
    public JsonObject getRowByPK(int pk) {
        return getRowByPK(""+pk);
    }
    
    public JsonArray getAllRows() {
        JsonArray res = new JsonArray();
        for (int i=0; i<size(); i++) 
            res.add(getRowByIndex(i));
        return res;
    }
}
