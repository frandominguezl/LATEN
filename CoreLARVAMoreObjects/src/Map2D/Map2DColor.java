/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Map2D;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author lcv
 */
public class Map2DColor {

    private BufferedImage _map;
    private int _lmax, _lmin;
    
    public Map2DColor() {
        _map = null;
        _lmax = _lmin = -1;
    }

    public Map2DColor(int width, int height) {
        _lmax = _lmin = -1;
        _map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public Map2DColor(int width, int height, int level) {
        _lmax = _lmin = -1;
        _map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                setPixel(x, y, new Color(level, level, level));
            }
        }
    }

    public Map2DColor Clone() {
        Map2DColor aux = new Map2DColor(this.getWidth(), this.getHeight());
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                aux.setPixel(x, y, this.getPixel(x, y));
            }
        }
        return aux;
    }

    public Map2DColor loadMap(String filename) throws IOException {
        File f;

        _map = null;
        f = new File(filename);
        _map = ImageIO.read(f);
        this.getExtremeHeights();
        return this;
    }

    public Map2DColor saveMap(String filename) throws IOException {
        File f;

        if (this.hasMap()) {
            f = new File(filename);
            ImageIO.write(_map, "PNG", f);
        }
        return this;
    }

    public int getWidth() {
        if (this.hasMap()) {
            return _map.getWidth();
        } else {
            return -1;
        }
    }

    public int getHeight() {
        if (this.hasMap()) {
            return _map.getHeight();
        } else {
            return -1;
        }
    }

    public int getMinHeight() {
        if (this.hasMap()) {
            return _lmin;
        } else {
            return -1;
        }
    }

    public int getMaxHeight() {
        if (this.hasMap()) {
            return _lmax;
        } else {
            return -1;
        }
    }

    public Color getPixel(int x, int y) {
        if (this.hasMap() && 0 <= x && x < this.getWidth() && 0 <= y && y < this.getHeight()) {
            return new Color(_map.getRGB(x, y));
        } else {
            return Color.BLACK;
        }
    }

    public int getLevel(int x, int y) {
        if (this.hasMap() && 0 <= x && x < this.getWidth() && 0 <= y && y < this.getHeight()) {
            return getPixel(x, y).getBlue();
        } else {
            return -1;
        }
    }

    public int getLevel(double x, double y) {
        return getLevel((int)Math.round(x),(int)Math.round(y));
    }
 
    public Map2DColor setPixel(int x, int y, Color c) {
        if (this.hasMap() && 0 <= x && x < this.getWidth() && 0 <= y && y < this.getHeight()) {
            _map.setRGB(x, y, c.getRGB());
            int level = c.getBlue();
            if (level > _lmax) {
                _lmax = level;
            }
            if (level < _lmin) {
                _lmin = level;
            }
        }
        return this;
    }

    public Map2DColor setLevel(int x, int y, int level) {
        return setPixel(x, y, new Color(level, level, level));

    }

    public boolean hasMap() {
        return (_map != null);
    }

    private void getExtremeHeights() {
        if (this.hasMap()) {
            _lmin = 256;
            _lmax = -1;
            for (int x = 0; x < getWidth(); x++) {
                for (int y = 0; y < getHeight(); y++) {
                    int level = this.getPixel(x, y).getBlue();
                    if (level > _lmax) {
                        _lmax = level;
                    }
                    if (level < _lmin) {
                        _lmin = level;
                    }
                }
            }

        }
    }

}