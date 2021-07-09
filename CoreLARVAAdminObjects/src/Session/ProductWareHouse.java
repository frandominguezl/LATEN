/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Session;

import AdminKeys.DBACoin;
import AdminReport.ReportableObject;
import TimeHandler.TimeHandler;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class ProductWareHouse {

    public HashMap<String, Product> ProductsAvailable, ProductsSold;

    public ProductWareHouse() {
        ProductsAvailable = new HashMap();
        ProductsSold = new HashMap();
    }
    
    public boolean available(String serial) {
        return ProductsAvailable.containsKey(serial);
    }

    public boolean sold(String serial) {
        return ProductsSold.containsKey(serial);
    }

    public Product getProductOwner(String serial) {
        if (sold(serial)) {
            return ProductsSold.get(serial);
        } else {
            return null;
        }
    }
}
