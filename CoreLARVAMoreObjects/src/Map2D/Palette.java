/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Map2D;

import static ConsoleAnsi.ConsoleAnsi.defColor;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class Palette {

    protected HashMap<Integer, Integer> palette, histogram;

    public Palette() {
        palette = new HashMap<>();
    }

    public Palette intoTerrain(int nlevels) {
        palette.clear();
        for (int i = 0; i < nlevels; i++) {
            palette.put(i, getTerrainColor(i, 0, nlevels));
        }
        return this;
    }

    public Palette intoBW(int nlevels) {
        palette.clear();
        for (int i = 0; i < nlevels; i++) {
            palette.put(i, getBWColor(nlevels-i+1, 0, nlevels));
        }
        return this;
    }

    public Palette intoBWInv(int nlevels) {
        palette.clear();
        for (int i = 0; i < nlevels; i++) {
            palette.put(i, getBWColor(i, 0, nlevels));
        }
        return this;
    }

    public int getColor(int level) {
        return palette.get(level);
    }

    public Palette intoThermal(int nlevels) {
        palette.clear();
        for (int i = 0; i < nlevels; i++) {
            palette.put(i, getThermalColor(nlevels-i-1, 0, nlevels));
        }
        return this;
    }

    public int getBWColor(int value, int minvalue, int maxvalue) { // Value [0,1], getcolor ANSICOLOR
        double r = 0, g = 0, b = 0;
        double scale = 1-Math.min(1, Math.max((value - minvalue) * 1.0 / (maxvalue - minvalue), 0));
        r = g = b = scale;
        return defColor(r, g, b);

    }

 public int getThermalColor(int value, int minvalue, int maxvalue) { // Value [0,1], getcolor ANSICOLOR
        double r = 0, g = 0, b = 0;
        double scale = 1-Math.min(1, Math.max((value - minvalue) * 1.0 / (maxvalue - minvalue), 0)); // 1 si es minima, 0 si es máxima
        scale = Math.pow(scale, 0.75);
        if (0.75 <= scale && scale <= 1) {
            b = (1-scale) / 0.25;
        } else if (0.5 <= scale && scale <= 0.75) {
            g = (0.75-scale) / 0.25;
            b = (scale-0.5) / 0.25;
        } else if (0.25 <= scale && scale <= 0.5) {
            r = (0.5-scale) / 0.25;
            g = (scale-0.25) / 0.25;
        } else {//if (0.75 <= scale && scale <=1) {
            r = 1;
            g = (0.25-scale) / 0.25;
            b = (0.25-scale) / 0.25;
        }
        return defColor(r, g, b);
    }

    public int getTerrainColor(int value, int minvalue, int maxvalue) { // Value [0,1], getcolor ANSICOLOR
        double r = 0, g = 0, b = 0;
        double scale = 1-Math.min(1, Math.max((value - minvalue) * 1.0 / (maxvalue - minvalue), 0)); // 1 si es minima, 0 si es máxima
        if (0.75 <= scale && scale <= 1) {
            g = (1-scale) / 0.25;
            r = (1-scale) / 1;
        } else if (0.5 <= scale && scale <= 0.75) {
            r = (0.75-scale) / 0.75;
            g = (scale-0.5) / 0.25;
            //r = (scale-0.5) / 0.25;
        } else if (0.25 <= scale && scale <= 0.5) {
            r = (0.5-scale) / 0.25;
            g = (0.5- scale) / 0.25;
        } else {//if (0.75 <= scale && scale <=1) {
            r = (0.25-scale) / 0.25;
            g = (0.25-scale) / 0.25;
            b = (0.25-scale) / 0.25;
        }
        return defColor(r, g, b);
    }

}
