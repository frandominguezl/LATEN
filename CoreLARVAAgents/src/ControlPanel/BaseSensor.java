/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ControlPanel;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class BaseSensor {

    public static final double NOREADING = Integer.MIN_VALUE;
//    static final Point NOPOINT = new Point(NOREADING);
    protected int dimx, dimy;
    protected int sensitivity;
    ArrayList<ArrayList<Double>> data;
    ArrayList<String> stream;
    String name;

    public BaseSensor() {
    }

    public BaseSensor(String sensorname) {
        name = sensorname;
        stream = new ArrayList();
    }

    public BaseSensor(int range) {
        init(range, range);
    }

    public void init(int rows, int columns) {
        dimx = columns;
        dimy = rows;
        data = new ArrayList<>();
        for (int i = 0; i < dimy; i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < dimx; j++) {
                row.add(-1.0);
            }
            data.add(row);
        }
        sensitivity = 256;
    }

    public void setSentivity(int s) {
        sensitivity = s;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public int size() {
        return dimx * dimy;
    }

    public double get(int x, int y) {
        if (0 <= x && x < dimx && 0 <= y && y < dimy) {
            return data.get(y).get(x);
        }
        return NOREADING;
    }

    public int getValue(int x, int y) {
        return (int) Math.round(get(x, y));
    }

    public int getInt() {
        return getValue(0, 0);
    }

    public Boolean getBoolean() {
        return getValue(0, 0) != 0;
    }

    public Double getDouble() {
        return get(0, 0);
    }

    public ArrayList<String> getStream() {
        return this.stream;
    }

    public boolean fromJson(JsonObject reading) {
        boolean res = false;
        JsonArray readdata;

        try {
            name = reading.getString("sensor", "unknown");
            readdata = reading.get("data").asArray();
            int many = readdata.size();
            if (many == 0) {
                return true;
            }
            if (many == 1 && readdata.get(0).isNumber()) {

                init(1, 1);
                data.get(0).set(0, readdata.get(0).asDouble());
            } else if (readdata.get(0).isString()) {
                this.stream.clear();
                for (int i = 0; i < readdata.size(); i++) {
                    stream.add(readdata.get(i).asString());
                }
            } else {
                if (many == 1) {
                    many = readdata.get(0).asArray().size();
                    init(1, many);
                    for (int y = 0; y < dimy; y++) {
                        for (int x = 0; x < dimx; x++) {
                            data.get(y).set(x, readdata.get(y).asArray().get(x).asDouble());
                        }
                    }

                } else {
                    init(many, many);
                    for (int y = 0; y < dimy; y++) {
                        for (int x = 0; x < dimx; x++) {
                            data.get(y).set(x, readdata.get(y).asArray().get(x).asDouble());
                        }
                    }

                }
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
        return res;
    }

    public void addRow(ArrayList<Double> tuple) {
        this.data.add(tuple);
    }

    public void addRow() {
        if (this.data == null) {
            data = new ArrayList<>();
        }
        ArrayList<Double> row = new ArrayList();
        row.add(-1.0);
        row.add(-1.0);
        row.add(-1.0);
        row.add(-1.0);
        row.add(-1.0);
        this.data.add(row);
    }

    public void addToRow(int row, int column, double value) {
        this.data.get(row).set(column, value);
    }

    public void addToLastRow(int column, double value) {
        this.data.get(data.size() - 1).set(column, value);
    }

//    public boolean fromJson(JsonObject reading) {
//        boolean res = false;
//        JsonArray readdata;
//
//        try {
//            name = reading.getString("sensor", "unknown");
//            readdata = reading.get("data").asArray();
//            int many = readdata.size();
//            if (many == 1) {
//                init(1, 1);
//                data.get(0).set(0, readdata.get(0).asDouble());
//            } else if (many == 3 && readdata.get(0).isNumber()) {
//                init(1, 3);
//                data.get(0).set(0, readdata.get(0).asDouble());
//                data.get(0).set(1, readdata.get(1).asDouble());
//                data.get(0).set(2, readdata.get(2).asDouble());
//            } else {
//                init(many, many);
//                for (int y = 0; y < dimy; y++) {
//                    for (int x = 0; x < dimx; x++) {
//                        data.get(y).set(x, readdata.get(y).asArray().get(x).asDouble());
//                    }
//                }
//            }
//        } catch (Exception ex) {
//        }
//        return res;
//    }
//    public boolean fromJson(JsonObject reading) {
//        boolean res = false;
//        JsonArray readdata;
//
//        try {
//            name = reading.getString("sensor", "unknown");
//            readdata = reading.get("data").asArray();
//            int many = readdata.size();
//            if (many == 1) {
//                init(1, 1);
//                data.get(0).set(0, readdata.get(0).asDouble());
//            } else if (many == 3 && readdata.get(0).isNumber()) {
//                init(1, 3);
//                data.get(0).set(0, readdata.get(0).asDouble());
//                data.get(0).set(1, readdata.get(1).asDouble());
//                data.get(0).set(2, readdata.get(2).asDouble());
//            } else {
//                init(many, many);
//                for (int y = 0; y < dimy; y++) {
//                    for (int x = 0; x < dimx; x++) {
//                        data.get(y).set(x, readdata.get(y).asArray().get(x).asDouble());
//                    }
//                }
//            }
//        } catch (Exception ex) {
//        }
//        return res;
//    }
    /* Perceptions(Zulu): {"name":"Zulu","perceptions":[
    {"sensor":"COMPASS","data":[90]},
    {"sensor":"gps","data":[10,10,237]},
    {"sensor":"radar","data":[[22,5,2,7,18],[27,13,2,9,6],[27,11,0,11,9],[19,9,3,14,13],[9,10,17,36,39]],"synchro":[{"x":8,"y":8},{"x":8,"y":9},{"x":8,"y":10},{"x":8,"y":11},{"x":8,"y":12}]},
    {"sensor":"visual","data":[[215,232,235,230,219],[210,224,235,228,231],[210,226,237,226,228],[218,228,234,223,224],[228,227,220,201,198]],"synchro":[{"x":8,"y":8},{"x":8,"y":9},{"x":8,"y":10},{"x":8,"y":11},{"x":8,"y":12}]},
    {"sensor":"thermal","data":[[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,100]],"synchro":[{"x":5,"y":5},{"x":5,"y":6},{"x":5,"y":7},{"x":5,"y":8},{"x":5,"y":9},{"x":5,"y":10},{"x":5,"y":11},{"x":5,"y":12},{"x":5,"y":13},{"x":5,"y":14},{"x":5,"y":15}]}]}   
     */
}
