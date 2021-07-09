/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package YellowPages;

import static PublicKeys.KeyGen.getKey;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class ProductTable {
    protected ArrayList <Product> Store;
    
    public ProductTable() {
        Store = new ArrayList();
    }
 
    public void fromJSON(JsonObject jstable) {
        
    }
    
    public JsonObject toJSON() {
        JsonObject jsS = new JsonObject();
        JsonArray jsas = new JsonArray();
        for (Product jso : Store) {
            jsas.add(jso.toJSON());
        }        
        return jsS.set("products",jsas);
    }
    
    public void fromJSON(){
       
    }
}

class Product {
    String name, serial;
    int coins;
    
    public Product(){
        serial = getKey();
    }
    
    public Product(JsonObject prod){
        name = prod.getString("name", "noname");
        serial = prod.getString("serial", "noserial");
        coins = prod.getInt("coins", -1);
    }
    
    public JsonObject toJSON(){
        return new JsonObject().
                add("name", name).
                add("serial", serial).
                add("coins", coins);
    }
}

