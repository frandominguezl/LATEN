/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminReport;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class Reports {

    protected ArrayList<ReportableObject> reportable;
    protected JsonObject report;

    public Reports() {
        reportable = new ArrayList<>();
    }

    public void clear() {
        reportable.clear();
    }
    
    public Reports addReportable(ReportableObject o) {
        reportable.add(o);
        return this;
    }

    public JsonObject getReport() {
        JsonObject res = new JsonObject();
        JsonArray reportset = new JsonArray();

        for (ReportableObject object : this.reportable) {
            JsonArray report = new JsonArray();
            for (String id : object.defReportableObjectList()) {
                report.add(new JsonObject().add("objectid", id).
                        add("date", object.reportObjectDate(id)).
                        add("status", object.reportObjectStatus(id)).
                        add("value", object.reportObjectValue(id)));
            }
            reportset.add(new JsonObject().add("report",object.defReportType()).add(object.defReportType(), report));
            
        }
        res.add("reportset",reportset);
        report = res;
        return res;
    }
    
    public static JsonArray getType(JsonObject report, String type) {
        JsonArray res= new JsonArray();
        for (JsonValue jsvr :report.get("reportset").asArray() ) {
            if (jsvr.asObject().getString("report","none").equals(type))
                    res = jsvr.asObject().get(type).asArray();
        }
        return res;
    }
/*
 {"report":"fullreport","agent":"AgentIdentityManager","date":"2020-10-12 13:37:36","reportset":
    [{"report":"mailqueues","mailqueues":[{"objectid":"REGULAR","date":"","status":"","value":"0"},
    {"objectid":"ANALYTICS","date":"2020-10-12 13:07:21","status":"{\"performative\":\"CANCEL\",\"sender\":\"inditex\",\"receiver\":\"AgentIdentityManager\",\"protocol\":\"ANALYTICS\",\"encoding\":\"\",\"conversation\":\"\",\"in-reply-to\":\"\",\"reply-with\":\"\",\"content\":null}","value":"0"},
    {"objectid":"ADMIN","date":"2020-10-12 13:37:36","status":"{\"performative\":\"QUERY-REF\",\"sender\":\"Kernel\",\"receiver\":\"AgentIdentityManager\",\"protocol\":\"ADMIN\",\"encoding\":\"215155191066024130179067060224067016066062196067018067044129147066042\",\"conversation\":\"\",\"in-reply-to\":\"\",\"reply-with\":\"\",\"content\":null}","value":"0"},
    {"objectid":"XUI","date":"","status":"","value":"0"}]},
    
    {"report":"behaviours","behaviours":
    [{"objectid":"MAIL","date":"2020-10-12 13:37:36","status":"{\"info\":\"acl_queue_ADMIN\",\"value\":{\"performative\":\"QUERY-REF\",\"sender\":\"Kernel\",\"receiver\":\"AgentIdentityManager\",\"protocol\":\"ADMIN\",\"encoding\":\"215155191066024130179067060224067016066062196067018067044129147066042\",\"conversation\":\"\",\"in-reply-to\":\"\",\"reply-with\":\"\",\"content\":null}}","value":"16"},
    {"objectid":"ADMIN","date":"2020-10-12 13:37:36","status":"{\"info\":\"acl_receive_ADMIN\",\"value\":{\"performative\":\"QUERY-REF\",\"sender\":\"Kernel\",\"receiver\":\"AgentIdentityManager\",\"protocol\":\"ADMIN\",\"encoding\":\"215155191066024130179067060224067016066062196067018067044129147066042\",\"conversation\":\"\",\"in-reply-to\":\"\",\"reply-with\":\"\",\"content\":null}}","value":"11"},
    {"objectid":"XUI","date":"2020-10-12 13:37:36","status":"","value":"11"},
    {"objectid":"ANALYTICS","date":"2020-10-12 13:37:36","status":"inditex checked-out in representation of user 19298 FRANCISCO JAVIER BOLIVAR EXPOSITO","value":"15"}]},
    
    {"report":"database","database":[
    {"objectid":"open","date":"","status":"true","value":""},{"objectid":"exceptions","date":"","status":"","value":""}]}]}    
    */
    public static JsonObject getObject(JsonObject report, String type, String objectid) {
        JsonObject res=new JsonObject();
        for (JsonValue jsvr : getType(report, type) ) {
            if (jsvr.asObject().getString("objectid","none").equals(objectid))
                    res = jsvr.asObject();
        }
        return res;
    }
}
