/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Session;

import AdminKeys.DBACoin;
import com.eclipsesource.json.JsonObject;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class Product {

    public static final int UNOWNED = 001;
    public String name, serial, owner, date, session;
    int ownerAgentID, price, serie;
    public ArrayList<String> deposits = new ArrayList();

    public Product(String n, int sserie, String ssession) {
        name = n;
        serie = sserie;
        session = ssession;
        ownerAgentID = UNOWNED;
        reCode();
        deposits = new ArrayList();
        price = 1;
    }

    public void reCode() {
        DBACoin mc = new DBACoin();
        mc.setOwner(ownerAgentID);
        mc.setSerie(baseSerie(name) + serie);
        mc.setSession(session.replace("SESSION#",""));
        serial = name.toUpperCase() + "#"+ mc.encodeCoin();
    }

    public JsonObject toJson() {
        JsonObject res = new JsonObject();
        res.add("reference", serial).add("price", price);

        return res;
    }

    public final int baseSerie(String name) {
        int base = 0;
        switch (name) {
            default:
                base += 10;
            case "alive":
                base += 10;
            case "ontarget":
                base += 10;
            case "cargo":
                base += 10;
            case "coins":
                base += 10;
            case "angular":
                base += 10;
            case "distance":
                base += 10;
            case "energy":
                base += 10;
            case "visual":
                base += 10;
            case "lidar":
                base += 10;
            case "elevation":
                base += 10;
            case "thermal":
                base += 10;
            case "angularHQ":
                base += 10;
            case "angularDELUX":
                base += 10;
            case "distanceHQ":
                base += 10;
            case "distanceDELUX":
                base += 10;
            case "thermalHQ":
                base += 10;
            case "thermalDELUX":
                base += 500;
            case "account":
                base = 0;
        }
        return base;
    }
}
