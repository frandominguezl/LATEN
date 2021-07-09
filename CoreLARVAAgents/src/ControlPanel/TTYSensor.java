package ControlPanel;

import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.blue;
import static ConsoleAnsi.ConsoleAnsi.brown;
import static ConsoleAnsi.ConsoleAnsi.defColor;
import static ConsoleAnsi.ConsoleAnsi.gray;
import static ConsoleAnsi.ConsoleAnsi.lightblue;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static ConsoleAnsi.ConsoleAnsi.lightred;
import static ConsoleAnsi.ConsoleAnsi.red;
import static ConsoleAnsi.ConsoleAnsi.white;
import Map2D.Palette;
import World.Compass;

/**
 *
 * @author lcv
 */
public class TTYSensor extends BaseSensor {

    protected int origx, origy, cellx, celly, width, height, maxhistory = 25, history = 0, maxreading, maxmemory;
    protected double ratio;
    protected boolean autoclose, frameshowed;
    double memory[][] = null;

//    protected int historyx[] = new int[maxhistory], historyy[] = new int[maxhistory];
    public Palette palette = new Palette().intoBW(256);

    public TTYSensor(String sensorname) { //, int w, int h) {
        super(sensorname);
        name = sensorname;
        origx = -1;
        origy = -1;
        autoclose = false;
        frameshowed = false;
//        width=w;
//        height=h;
        setZoom(1);
    }

    public void showFrame(ConsoleAnsi toConsole, int w, int h) {
        if (this.data == null && this.stream == null) {
            toConsole.doFrameTitle(name, origx, origy, 10, 3);
        } else if (!frameshowed) {
            toConsole.doFrameTitle(name, origx, origy, w, h);
            frameshowed = true;
        }
    }

    public final TTYSensor setZoom(int x) {
        celly = x;
        cellx = (int) (1.5 * celly) + 1;
        ratio = cellx * 1.0 / celly;
        return this;
    }

    public TTYSensor setMaxReading(int mxr) {
        maxreading = mxr;
        return this;
    }

    public TTYSensor setMemoryRange(int rangex, int rangey) {
        memory = new double[rangey][rangex];
        for (int y = 0; y < rangey; y++) {
            for (int x = 0; x < rangex; x++) {
                memory[y][x] = Integer.MIN_VALUE;
            }
        }
        return this;
    }

    public int getMemorySize() {
        if (memory == null) {
            return 0;
        } else {
            return memory.length * memory[0].length;
        }
    }

    public TTYSensor ShowBoolean(ConsoleAnsi toConsole, int px, int py) {
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        this.showFrame(toConsole, TTYControlPanel.LONGFIELD + 3, 3);
        if (origx >= 0) {
//            toConsole.doRectangleFrameTitle(name, origx, origy, TTYControlPanel.LONGFIELD + 3, 3);
            toConsole.setCursorXY(origx + 1, origy + 1);
        } else {
            toConsole.print(name + " ");
        }
        if (this.data == null || getInt() == TTYSensor.NOREADING) {
            toConsole.println("XXX");
            return this;
        }
        if (getBoolean()) {
            toConsole.setText(lightgreen);
            toConsole.setBackground(lightgreen);
            toConsole.print("TRUE ");
        } else {
            toConsole.setText(lightred);
            toConsole.setBackground(lightred);
            toConsole.print("FALSE");
        }
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowBooleanMini(ConsoleAnsi toConsole, int px, int py) {
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        toConsole.setCursorXY(origx + 1, origy + 1);
        toConsole.print(this.name.substring(0, 2));
        toConsole.setCursorXY(origx + 3, origy + 1);
        if (this.data == null || getInt() == TTYSensor.NOREADING) {
            toConsole.setBackground(black).setText(TTYControlPanel.BACKGR).println("X");
            return this;
        }
        if (getBoolean()) {
            toConsole.setBackground(lightgreen).println(" ");
        } else {
            toConsole.setBackground(lightred).println(" ");
        }
        toConsole.resetColors();
        return this;
    }

    protected String printLongInt(int number) {
        String format = "%" + TTYControlPanel.LONGFIELD + "d";
        if (number == TTYSensor.NOREADING) {
            return "XXX";
        } else {
            return String.format(format, number);
        }
    }

    protected String printShortInt(int number) {
        String format = "%d";
        if (number == TTYSensor.NOREADING) {
            return "XXX";
        } else {
            return String.format(format, number);
        }
    }

    protected String printDouble(double number) {
        String format = "%5.1f";
        if (number == TTYSensor.NOREADING) {
            return "XXX";
        } else {
            return String.format(format, number);
        }
    }

    protected int getX(int x) {
        return cellx * x + origx;
    }

    protected int getY(int y) {
        return celly * y + origy;
    }

    public TTYSensor ShowPlain(ConsoleAnsi toConsole) {
        setZoom(1);
        toConsole.resetColors();
        toConsole.println(name + " ");
        if (this.data == null) {
            toConsole.println("XXX");
            return this;
        }
        for (int y = 0; y < dimy; y++) {
            for (int x = 0; x < dimx; x++) {
                toConsole.print(printLongInt(getValue(x, y)));
            }
            toConsole.println("");
        }
        return this;
    }

    public TTYSensor ShowStream(ConsoleAnsi toConsole, int px, int py, int w, int h) {
        toConsole.resetColors();
        setZoom(1);
        origx = px;
        origy = py;
        this.showFrame(toConsole, cellx * w, celly * h + 2);
        toConsole.setBackground(black).setText(lightgreen);
        toConsole.doRectangle(origx + 1, origy + 1, cellx * w - 2, celly * h);
        int begin = (Math.max(0, this.stream.size() - h));
        for (int i = 0; i < (int) (Math.min(h, this.stream.size())); i++) {
//        for (int i = (int) (Math.max(0, this.stream.size() - h)); i < this.stream.size() - 1; i++) {
            toConsole.setCursorXY(px + 1, py + 1 + i);
            String s = this.stream.get(begin + i);
            toConsole.print(s.substring(0, (int) (Math.min(s.length(), cellx * w - 2))));
        }
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowStreamMini(ConsoleAnsi toConsole, int px, int py, int w, int h) {
        toConsole.resetColors();
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        toConsole.setCursorXY(origx + 1, origy + 1);
        toConsole.print(this.name.substring(0, 2));
        toConsole.setBackground(black).setText(lightgreen);
        if (this.stream != null && this.stream.size() == 0) {
            toConsole.doRectangle(origx + 3, origy + 1, w - 2, h);
            return this;
        }
        if (this.stream.size() == 1 && this.stream.get(0).length() == 0) {
            return this;
        }
        toConsole.doRectangle(origx + 3, origy + 1, w - 2, h);
        int begin = (Math.max(0, this.stream.size() - h));
        for (int i = 0; i < (int) (Math.min(h, this.stream.size())); i++) {
//        for (int i = (int) (Math.max(0, this.stream.size() - h)); i < this.stream.size() - 1; i++) {
            toConsole.setCursorXY(origx + 3, origy + 1 + i);
            String s = this.stream.get(begin + i);
            toConsole.print(s.substring(0, (int) (Math.min(s.length(), w - 2))));
        }
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowValues(ConsoleAnsi toConsole, int px, int py) {
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        this.showFrame(toConsole, dimx * TTYControlPanel.LONGFIELD + 1 + 1, dimy + 2);
        if (this.data == null) {
//            toConsole.doRectangleFrameTitle(name, origx, origy, 10, 3);
            toConsole.setCursorXY(origx + 1, origy + 1).print("XXX");
            return this;
        }
//        toConsole.doRectangleFrameTitle(name, origx, origy, dimx * TTYControlPanel.LONGFIELD + 1 + 1, dimy + 2);
        for (int y = 0; y < dimy; y++) {
            for (int x = 0; x < dimx; x++) {
                toConsole.setCursorXY(origx + x * TTYControlPanel.LONGFIELD + 1, origy + y + 1);
                toConsole.print(printLongInt(getValue(x, y)));
            }
        }
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowValuesMini(ConsoleAnsi toConsole, int px, int py) {
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        toConsole.setCursorXY(origx + 1, origy + 1);
        toConsole.print(this.name.substring(0, 2));
        if (this.data == null) {
            toConsole.setBackground(black).setText(TTYControlPanel.BACKGR).println("X");
            return this;
        }
        toConsole.setBackground(black).setText(white);
        for (int y = 0; y < dimy; y++) {
            for (int x = 0; x < dimx; x++) {
                toConsole.setCursorXY(3 + origx + x * 4, origy + y + 1);
                toConsole.print(String.format("%3.0f", getValue(x, y) * 1.0));
            }
        }
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowVisual(ConsoleAnsi toConsole, int px, int py) {
        ShowColor(toConsole, new Palette().intoBW(256), px, py);
        return this;
    }

    public TTYSensor ShowVisualMini(ConsoleAnsi toConsole, int px, int py) {
        ShowColorMini(toConsole, new Palette().intoBW(256), px, py);
        return this;
    }

    public TTYSensor ShowThermal(ConsoleAnsi toConsole, int px, int py, int sense) {
        sensitivity = sense;
        ShowColor(toConsole, new Palette().intoThermal((int) this.sensitivity + 1), px, py);
        return this;
    }

    public TTYSensor ShowThermalMini(ConsoleAnsi toConsole, int px, int py, int sense) {
        sensitivity = sense;
        ShowColorMini(toConsole, new Palette().intoThermal((int) this.sensitivity + 1), px, py);
        return this;
    }

    public TTYSensor ShowDepth(ConsoleAnsi toConsole, int px, int py) {
        ShowColor(toConsole, new Palette().intoBWInv(256), px, py);
        return this;
    }

    public TTYSensor ShowDepthMini(ConsoleAnsi toConsole, int px, int py) {
        ShowColorMini(toConsole, new Palette().intoBWInv(256), px, py);
        return this;
    }

    public TTYSensor ShowColor(ConsoleAnsi toConsole, Palette p, int px, int py) {
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        boolean scale = true;
        this.setZoom(2);
        palette = p;
        if (origx >= 0) {
            if (scale) {
                this.showFrame(toConsole, dimx * cellx + TTYControlPanel.LONGFIELD + cellx + 3, dimy * celly + 2);
//                toConsole.doRectangleFrameTitle(name, origx, origy, dimx * cellx + TTYControlPanel.LONGFIELD + cellx + 3, dimy * celly + 2);
            } else {
                this.showFrame(toConsole, dimx * cellx + 2, dimy * celly + 2);
//                toConsole.doRectangleFrameTitle(name, origx, origy, dimx * cellx + 2, dimy * celly + 2);
            }
            int midx = dimx / 2, midy = dimy / 2;
            for (int x = 0; x < dimx; x++) {
                for (int y = 0; y < dimy; y++) {
                    int level = getValue(x, y);
                    if (x == midx && y == midy) {
                        printCell(toConsole, origx + x * cellx + 1, origy + y * celly + 1, level, true, true);
                    } else {
                        printCell(toConsole, origx + x * cellx + 1, origy + y * celly + 1, level, false, true);
                    }
                }

            }
            if (scale) {
                toConsole.setText(white);
                for (int y = origy, yy = 0; yy < dimy * celly; yy++) {
                    int level = palette.getColor(yy * (int) this.sensitivity / (dimy * celly));
                    toConsole.setBackground(level);
                    toConsole.doRectangle(origx + cellx * dimx + 2, (y + yy) + 1, cellx, 1);
                    toConsole.setBackground(black);
                    toConsole.setCursorXY(origx + cellx * dimx + 2 + cellx, (y + yy) + 1);
                    toConsole.print(printLongInt(yy * this.sensitivity / (dimy * celly)));
                }
            }
            toConsole.resetColors();
        }
        return this;
    }

    public TTYSensor ShowColorMini(ConsoleAnsi toConsole, Palette p, int px, int py) {
        int bg, fg, level;
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px + 1;
        origy = py + 1;
        toConsole.resetColors();
        toConsole.setCursorXY(origx, origy);
        toConsole.print(this.name.substring(0, 2));
        palette = p;
        if (origx >= 0) {
            int mx = dimx / 2, my = dimy / 2;
            for (int x = 0; x < dimx; x++) {
                for (int y = 0; y < dimy; y++) {
                    level = getValue(x, y);
                    toConsole.setCursorXY(origx + 2*x + 2, origy + y);
                    if (level < 0) {//== BaseSensor.NOREADING) {
                        bg = red;
                    } else {
                        level = Math.min(sensitivity, level);
                        bg = palette.getColor(level);
                    }
                    toConsole.setBackground(bg).print("  ");
                }

            }
            toConsole.setCursorXY(origx +2* mx + 2, origy + my);
            level = getValue(mx, my);
            if (level < 0) {//== BaseSensor.NOREADING) {
                bg = red;
            } else {
                level = Math.min(sensitivity, level);
                bg = palette.getColor(level);
            }
            toConsole.setBackground(bg).setText(black/*(level>20?black:white)*/).print("X");
            toConsole.resetColors();
        }
        return this;
    }
    public TTYSensor ShowColorMicro(ConsoleAnsi toConsole, Palette p, int px, int py) {
        int bg, fg, level;
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px + 1;
        origy = py + 1;
        toConsole.resetColors();
        toConsole.setCursorXY(origx, origy);
        toConsole.print(this.name.substring(0, 2));
        palette = p;
        if (origx >= 0) {
            int mx = dimx / 2, my = dimy / 2;
            for (int x = 0; x < dimx; x++) {
                for (int y = 0; y < dimy; y++) {
                    level = getValue(x, y);
                    toConsole.setCursorXY(origx + x + 2, origy + y);
                    if (level < 0) {//== BaseSensor.NOREADING) {
                        bg = red;
                    } else {
                        level = Math.min(sensitivity, level);
                        bg = palette.getColor(level);
                    }
                    toConsole.setBackground(bg).print(" ");
                }

            }
            toConsole.setCursorXY(origx + mx + 2, origy + my);
            level = getValue(mx, my);
            if (level < 0) {//== BaseSensor.NOREADING) {
                bg = red;
            } else {
                level = Math.min(sensitivity, level);
                bg = palette.getColor(level);
            }
            toConsole.setBackground(bg).setText(black/*(level>20?black:white)*/).print("X");
            toConsole.resetColors();
        }
        return this;
    }

public TTYSensor ShowMemory(ConsoleAnsi toConsole, int gpsx, int gpsy) {
        int wsx = 3, wsy = 3;
        Palette p = new Palette().intoBW(256);
        int range = 7, bg, fg;
        this.setZoom(1);
        origx = wsx + gpsx; // + range / 2;
        origy = wsy + gpsy; // + range / 2;
        toConsole.resetColors();
        int midx = dimx / 2, midy = dimy / 2;
        for (int x = 0; x < dimx; x++) {
            for (int y = 0; y < dimy; y++) {
                int level = getValue(x, y);
                int mx = gpsx + x, my = gpsy + y;
                if (memory[my][mx] == Integer.MIN_VALUE) {
                    memory[my][mx] = level;
                    if (level < 0 || maxreading - level <= 0) {//== BaseSensor.NOREADING) {
                        bg = red;
                        fg = lightred;
                    } else {
                        level = Math.min(sensitivity, level);
                        bg = palette.getColor(level);
                        fg = ConsoleAnsi.negColor(bg);
                    }
                    toConsole.setCursorXY(origx + x, origy + y).setText(fg).setBackground(bg);
                    toConsole.print(" ");
                }
            }
        }
        toConsole.setCursorXY(origx + midx, origy + midy).setText(black).setBackground(lightgreen);
        toConsole.print(ConsoleAnsi.windowFrames[0].charAt(8) + "");
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowLocation(ConsoleAnsi toConsole, int gpsx, int gpsy) {
        int wsx = 3, wsy = 3;
        int range = 7, bg, fg;
        this.setZoom(1);
        origx = wsx + gpsx; // + range / 2;
        origy = wsy + gpsy; // + range / 2;
        toConsole.resetColors();
        int midx = dimx / 2, midy = dimy / 2;
        toConsole.setCursorXY(origx + midx, origy + midy).setText(black).setBackground(lightgreen);
        toConsole.print(ConsoleAnsi.windowFrames[0].charAt(8) + "");
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowHProgressBar(ConsoleAnsi toConsole, int px, int py, int width, int each, double max) {
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        boolean legend = true;
        if (!legend) {
//            toConsole.doRectangleFrameTitle(name + " " + printDouble(getDouble()), origx, origy, width, 3);
            toConsole.doProgressBar(origx + 1, origy + 1, width - TTYControlPanel.LONGFIELD - 5, getDouble(), max);
            this.showFrame(toConsole, width, 3);
        } else {
//            toConsole.doRectangleFrameTitle(name + " " + printDouble(getDouble()), origx, origy, width, 5);
            toConsole.doProgressBar(origx + 1, origy + 3, width - TTYControlPanel.LONGFIELD - 5, getDouble(), max);
            if (!this.frameshowed) {
                toConsole.printHRulerTop(origx + 1, origy + 1, width - TTYControlPanel.LONGFIELD - 5, (int) (max / 10), (int) max);
            }
            this.showFrame(toConsole, width, 5);
        }

        return this;
    }

    public TTYSensor ShowHProgressBarMini(ConsoleAnsi toConsole, int px, int py, int width, double max) {
        setZoom(1);
        origx = px + 1;
        origy = py + 1;
        toConsole.resetColors();
        toConsole.setCursorXY(origx, origy);
        toConsole.print(this.name.substring(0, 2));
        printHMinibar(toConsole, origx + 2, origy, max, max, width, gray, gray);
        printHMinibar(toConsole, origx + 2, origy, getDouble(), max, width, lightblue, gray);
        toConsole.setCursorXY(origx + width + 2, origy);
        toConsole.resetColors();
        toConsole.setBackground(black).setText(white);
        if (getInt() == NOREADING) {
            toConsole.println("X");
            return this;
        }
        if (max >= 1000) {
            toConsole.print(String.format("%4.0f", getDouble()));
        } else if (max >= 100) {
            toConsole.print(String.format("%3.0f", getDouble()));
        } else {
            toConsole.print(String.format("%2.0f", getDouble()));
        }
        return this;
    }

    public TTYSensor ShowAngular(ConsoleAnsi toConsole, int px, int py) {
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        int width = 13, radius = width / 2;
        int centerx = origx + width / 2 + 1, centery = origy + width / 2 + 1;
        toConsole.resetColors();
        this.showFrame(toConsole, width * cellx + 1, width * celly + 2);
//        toConsole.doRectangleFrameTitle(name, origx, origy, width * cellx + 1, width * celly + 2);
        printCircle(toConsole, centerx, centery, radius);
        toConsole.setCursorXY((int) (Math.round(centerx + 2 * radius / ratio)) - 3, centery);
        if (getInt() == NOREADING) {
            toConsole.println("X");
            return this;
        }
        toConsole.print(printDouble(this.getDouble()));
        toConsole.setBackground(lightred);
        printCircularDot(toConsole, centerx, centery, radius, getDouble(), "" + ConsoleAnsi.windowFrames[0].charAt(11));
        toConsole.resetColors();
        return this;
    }

    public TTYSensor ShowAngularMini(ConsoleAnsi toConsole, int px, int py) {
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px + 1;
        origy = py + 1;
        toConsole.resetColors();
        toConsole.setCursorXY(origx, origy);
        toConsole.print(this.name.substring(0, 2));
        origx += 2;
        toConsole.setText(white).setBackground(black).doRectangle(origx, origy, 3, 3);
        if (getInt() == NOREADING) {
            toConsole.setCursorXY(origx + 1, origy + 1).println("X");
            return this;
        }
        if (-22.5 <= getDouble() && getDouble() <= 22.5) { // N
            toConsole.setCursorXY(origx + 1, origy).println(ConsoleAnsi.windowFrames[0].charAt(5) + "");
            toConsole.setCursorXY(origx + 1, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(5) + "");
        } else if (22.5 <= getDouble() && getDouble() <= 67.5) { // NE
            toConsole.setCursorXY(origx + 2, origy).println("/");
            toConsole.setCursorXY(origx + 1, origy + 1).println("/");
        } else if (67.5 <= getDouble() && getDouble() <= 112.5) { // E
            toConsole.setCursorXY(origx + 2, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(4) + "");
            toConsole.setCursorXY(origx + 1, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(4) + "");
        } else if (112.5 <= getDouble() && getDouble() <= 157.5) { //SE
            toConsole.setCursorXY(origx + 2, origy + 2).println("\\");
            toConsole.setCursorXY(origx + 1, origy + 1).println("\\");
        } else if (157.5 <= (360 + (int) (1.0 * getDouble())) % 360 && (360 + (int) (1.0 * getDouble())) % 360 <= 203) { //S
            toConsole.setCursorXY(origx + 1, origy + 2).println(ConsoleAnsi.windowFrames[0].charAt(5) + "");
            toConsole.setCursorXY(origx + 1, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(5) + "");
        } else if (-112.5 >= getDouble() && getDouble() >= -157.5) { //SW
            toConsole.setCursorXY(origx, origy + 2).println("/");
            toConsole.setCursorXY(origx + 1, origy + 1).println("/");
        } else if (-67.5 >= getDouble() && getDouble() >= -112.5) { //W
            toConsole.setCursorXY(origx, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(4) + "");
            toConsole.setCursorXY(origx + 1, origy + 1).println(ConsoleAnsi.windowFrames[0].charAt(4) + "");
        } else if (-22.5 >= getDouble() && getDouble() >= -67.5) { // NW
            toConsole.setCursorXY(origx, origy).println("\\");
            toConsole.setCursorXY(origx + 1, origy + 1).println("\\");
        } else {
            toConsole.resetColors();
        }
        toConsole.resetColors();

        return this;
    }

    public TTYSensor ShowFlight(ConsoleAnsi toConsole, int px, int py, int width, int height) {
        int plane4 = defColor(0, 0.5, 1), plane3 = plane4, plane2 = defColor(153.0 / 255, 76.0 / 255, 0), plane1 = lightgreen;
        if (this.data == null) {
            ShowValues(toConsole, px, py);
            return this;
        }
        setZoom(1);
        origx = px;
        origy = py;
        toConsole.resetColors();
        this.showFrame(toConsole, width, height);
        int max = width - 2, base = (data.size() > max ? data.size() - max : 0);
        if (data.size() > 0) {
            if (base > 0) {
                for (int i = base; i < data.size(); i++) {
                    printMinibar(toConsole, origx + i - base + 1, origy + height - 2, 255, plane4, TTYControlPanel.BACKGR);
                    printMinibar(toConsole, origx + i - base + 1, origy + height - 2, (int) (Math.round(data.get(i).get(0))), plane3, plane4);
                    printMinibar(toConsole, origx + i - base + 1, origy + height - 2, (int) (Math.round(data.get(i).get(1))), plane1, plane3);
                    printMinibar(toConsole, origx + i - base + 1, origy + height - 2, (int) (Math.round(data.get(i).get(2))) - 5, plane2, plane1);

                }
            } else {
                printMinibar(toConsole, origx + data.size() - base, origy + height - 2, 255, plane4, TTYControlPanel.BACKGR);
                printMinibar(toConsole, origx + data.size() - base, origy + height - 2, (int) (Math.round(data.get(data.size() - 1).get(0))), plane3, plane4);
                printMinibar(toConsole, origx + data.size() - base, origy + height - 2, (int) (Math.round(data.get(data.size() - 1).get(1))), plane1, plane3);
//            printMinibar(toConsole, origx + data.size(), origy + height - 2, (int) (Math.round(data.get(data.size() - 1).get(1)))-5, plane3,plane1);
                printMinibar(toConsole, origx + data.size() - base, origy + height - 2, (int) (Math.round(data.get(data.size() - 1).get(2))) - 5, plane2, plane1);
            }
        }
        return this;
    }

    public void printMinibar(ConsoleAnsi toConsole, int x, int y, int level, int front, int back) {
        int bx = x, by = y;
        level = level / 5;
        while (level > 0) {
            toConsole.setCursorXY(bx, by);
            if (level >= 8) {
                toConsole.setText(back);
                toConsole.setBackground(front);
                toConsole.print(" ");
                by--;
            } else {
                toConsole.setText(front);
                toConsole.setBackground(back);
                toConsole.print("" + ConsoleAnsi.windowFrames[2].charAt(level));
            }
            level = level - 8;
        }
    }

    public void printHMinibar(ConsoleAnsi toConsole, int x, int y, double level, double maxlevel, int width, int front, int back) {
        int bx = x, by = y;
        int ilevel = (int) Math.ceil(Math.min(level, maxlevel) / maxlevel * width * 8);
        while (ilevel > 0) {
            toConsole.setCursorXY(bx, by);
            if (ilevel >= 8) {
                toConsole.setText(back);
                toConsole.setBackground(front);
                toConsole.print(" ");
                bx++;
            } else {
                toConsole.setText(front);
                toConsole.setBackground(back);
                toConsole.print("" + ConsoleAnsi.windowFrames[3].charAt(ilevel));
            }
            ilevel = ilevel - 8;
        }
    }

    public TTYSensor printCircularDot(ConsoleAnsi toConsole, int centerx, int centery, int radius, double degrees, String what) {
        double angle = -(((270 + degrees) % 360) / 180 * Math.PI);
        int cx = (int) Math.round(centerx + 2 * radius / ratio + ratio * radius * Math.cos(angle)),
                cy = (int) Math.round(centery - radius * Math.sin(angle));
        toConsole.setCursorXY(cx, cy);
        toConsole.print(what);
        return this;
    }

    public TTYSensor printCircle(ConsoleAnsi toConsole, int centerx, int centery, int radius) {
        for (int deg = 0; deg < 360; deg += 1) {
            printCircularDot(toConsole, centerx, centery, radius, deg, "" + ConsoleAnsi.windowFrames[0].charAt(11));
        }
        return this;
    }

    public void printCell(ConsoleAnsi toConsole, int x, int y, int level, boolean frame, boolean legend) {
        int bg, fg;
//        if (level < 0) {
//            bg = red;
//            fg = ConsoleAnsi.negColor(bg);
//        } else 
        if (level < 0) {//== BaseSensor.NOREADING) {
            bg = black;
            fg = lightred;
        } else {
            level = Math.min(sensitivity, level);
            bg = palette.getColor(level);
            fg = ConsoleAnsi.negColor(bg);
        }
        toConsole.setBackground(bg);
        toConsole.setText(fg);
        if (frame) {
            toConsole.doRectangleFrame(x, y, cellx, celly);
        } else {
            toConsole.doRectangle(x, y, cellx, celly);
        }
        if (legend) {
            toConsole.setCursorXY(x + 1, y + celly - 1);
            if (level < 0) { //== NOREADING) {
                toConsole.print("XXX");
            } else {
                toConsole.print(printShortInt(level));
            }
        }
    }

//    public void printCell(int x, int y, int level) {
//        if (level == NOREADING) {
//            toConsole.setBackground(defColor(0.5, 0, 0));
//        } else {
//            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
//        }
//        toConsole.setText(lightgreen);
//        toConsole.doRectangleFrame(x, y, cellx, celly);
//        toConsole.setCursorXY(x, y + 2);
//        if (level == Perceptor.NULLREAD) {
//            toConsole.print("XXX");
//        } else {
//            toConsole.print(String.format("%3d", level));
//        }
//    }
//
//    public void printTinyCell(int x, int y, int level, ConsoleAnsi c) {
//        if (level == Perceptor.NULLREAD) {
//            toConsole.setBackground(defColor(0.5, 0, 0));
//        } else {
//            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
//        }
//        toConsole.doRectangle(x, y, cellx, celly);
//        toConsole.setCursorXY(x, y + 2);
////        if (level == Perceptor.NULLREAD) {
////            toConsole.print("XXX");
////        } else {
////            toConsole.print(String.format("%3d", level));
////        }
//    }
//
//    public void printMiniCell(int x, int y, int level, ConsoleAnsi c) {
//        if (level == Perceptor.NULLREAD) {
//            toConsole.setBackground(defColor(0.5, 0, 0));
//        } else {
//            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
//        }
//        toConsole.doRectangleFrame(x, y, cellx - 1, celly - 1);
//        toConsole.setCursorXY(x, y + 1);
//        if (level == Perceptor.NULLREAD) {
//            toConsole.print("XXX");
//        } else {
//            toConsole.print(String.format("%3d", level));
//        }
//    }
}

///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package ControPalnel;
//
//import ConsoleAnsi.ConsoleAnsi;
//import static ConsoleAnsi.ConsoleAnsi.black;
//import static ConsoleAnsi.ConsoleAnsi.defColor;
//import static ConsoleAnsi.ConsoleAnsi.gray;
//import static ConsoleAnsi.ConsoleAnsi.lightgreen;
//import static ConsoleAnsi.ConsoleAnsi.lightred;
//import static ConsoleAnsi.ConsoleAnsi.negColor;
//import static ConsoleAnsi.ConsoleAnsi.red;
//import static ConsoleAnsi.ConsoleAnsi.white;
//import Map2D.Palette;
//import World.Perceptor;
//
///**
// *
// * @author lcv
// */
//public class TTYSensor extends BaseSensor {
//
//    public static final int BACKGR = ConsoleAnsi.defColor(0.3, 0.3, 0.3), FOREGR = ConsoleAnsi.defColor(1, 1, 1), TTYControlPanel.LONGFIELD = 4;
//    protected ConsoleAnsi toConsole;
//    protected int origx, origy, cellx, celly;
//    protected double ratio;
//    protected boolean autoclose;
//    public Palette palette = new Palette().intoBW(256);
//
//    public TTYSensor(String sensorname) {
//        super();
//        name = sensorname;
//        toConsole = new ConsoleAnsi("intern");
//        origx = -1;
//        origy = -1;
//        autoclose = false;
//        setZoom(1);
//    }
//
//    public TTYSensor(String sensorname, ConsoleAnsi c, int x, int y) {
//        super();
//        name=sensorname;
//        toConsole = c;
//        origx = x;
//        origy = y;
//        autoclose = false;
//        setZoom(1);
//    }
//
////    public TTYSensor(String n, int w, int h) {
////        super();
////        name = n;
////        toConsole = new ConsoleAnsi(name, w, h, 10);
////        toConsole.setText(FOREGR).setBackground(BACKGR).clearScreen();
////        origx = 1;
////        origy = 1;
////        autoclose = true;
////        setZoom(1);
////    }
//
//    public final TTYSensor setZoom(int x) {
//        celly = x;
//        cellx = (int) (1.5 * celly) + 1;
//        ratio=cellx*1.0/celly;
//        return this;
//    }
//
//    public TTYSensor ShowBoolean() {
//        toConsole.resetColors();
//        if (origx >= 0) {
//            toConsole.doRectangleFrameTitle(name, origx, origy, TTYControlPanel.LONGFIELD + 3, 3);
//            toConsole.setCursorXY(origx + 1, origy + 1);
//        } else {
//            toConsole.print(name + " ");
//        }
//        if (getBoolean()) {
//            toConsole.setText(lightgreen);
//            toConsole.setBackground(lightgreen);
//            toConsole.print("TRUE ");
//        } else {
//            toConsole.setText(lightred);
//            toConsole.setBackground(lightred);
//            toConsole.print("FALSE");
//        }
//        toConsole.resetColors();
//        return this;
//    }
//
//    protected String printLongInt(int number) {
//        String format = "%" + TTYControlPanel.LONGFIELD + "d";
//        return String.format(format, number);
//    }
//
//    protected String printShortInt(int number) {
//        String format = "%d";
//        return String.format(format, number);
//    }
//
//    protected String printDouble(double number) {
//        String format = "%5.1f";
//        return String.format(format, number);
//    }
//
//    protected int getX(int x) {
//        return cellx * x + origx;
//    }
//
//    protected int getY(int y) {
//        return celly * y + origy;
//    }
//
//    public TTYSensor ShowValues() {
//        toConsole.resetColors();
//        if (origx >= 0) {
//            toConsole.doRectangleFrameTitle(name, origx, origy, dimx * TTYControlPanel.LONGFIELD + 1 + 1, dimy + 2);
//        } else {
//            toConsole.println(name + " ");
//        }
//        for (int y = 0; y < dimy; y++) {
//            for (int x = 0; x < dimx; x++) {
//                if (origx >= 0) {
//                    toConsole.setCursorXY(origx + x * TTYControlPanel.LONGFIELD + 1, origy + y + 1);
//                }
//                toConsole.print(printLongInt(getValue(x, y)));
//            }
//            if (origx < 0) {
//                toConsole.println("");
//            }
//        }
//        toConsole.resetColors();
//        return this;
//    }
//
//    public TTYSensor ShowVisual(Palette which, boolean scale) {
//        toConsole.resetColors();
//        this.setZoom(2);
//        palette = which;
//        if (origx >= 0) {
//            if (scale) {
//                toConsole.doRectangleFrameTitle(name, origx, origy, dimx * cellx + TTYControlPanel.LONGFIELD + cellx + 3, dimy * celly + 2);
//            } else {
//                toConsole.doRectangleFrameTitle(name, origx, origy, dimx * cellx + 2, dimy * celly + 2);
//            }
//            int midx = dimx / 2, midy = dimy / 2;
//            for (int x = 0; x < dimx; x++) {
//                for (int y = 0; y < dimy; y++) {
//                    int level = getValue(x, y);
//                    if (x == midx && y == midy) {
//                        printCell(origx + x * cellx + 1, origy + y * celly + 1, level, true, true);
//                    } else {
//                        printCell(origx + x * cellx + 1, origy + y * celly + 1, level, false, true);
//                    }
//                }
//
//            }
//            if (scale) {
//                toConsole.setText(white);
//                for (int y = origy, yy = 0; yy < dimy * celly; yy++) {
//                    int level = palette.getColor(yy * 256 / (dimy * celly));
//                    toConsole.setBackground(level);
//                    toConsole.doRectangle(origx + cellx * dimx + 2, (y + yy) + 1, cellx, 1);
//                    toConsole.setBackground(black);
//                    toConsole.setCursorXY(origx + cellx * dimx + 2 + cellx, (y + yy) + 1);
//                    toConsole.print(printLongInt(yy * 256 / (dimy * celly)));
//                }
//            }
//            toConsole.resetColors();
//        }
//        return this;
//    }
//
//    public TTYSensor ShowHProgressBar(int width, double max, boolean legend) {
//        if (!legend) {
//            toConsole.doRectangleFrameTitle(name+" "+printDouble(getDouble()), origx, origy, width, 3);
//            toConsole.doProgressBar(origx + 1, origy + 1, width - TTYControlPanel.LONGFIELD - 3, getDouble(), max);
//        } else {
//            toConsole.doRectangleFrameTitle(name+" "+printDouble(getDouble()), origx, origy, width, 5);
//            toConsole.doProgressBar(origx + 1, origy + 1, width - TTYControlPanel.LONGFIELD - 3, getDouble(), max);
//            toConsole.printHRuler(origx + 1, origy + 2, width - TTYControlPanel.LONGFIELD - 3, (int) max);
//        }
//
//        return this;
//    }
//
//    public TTYSensor ShowAngular() {
//        int width = 13, radius = width / 2;
//        int centerx=origx + width / 2+1, centery=origy + width / 2+1;
//        toConsole.resetColors();
//        toConsole.doRectangleFrameTitle(name, origx, origy, width * cellx+1, width * celly+2);
//        printCircle(centerx, centery, radius);
//        toConsole.setCursorXY((int)(Math.round(centerx+2*radius/ratio))-3,centery);
//        toConsole.print(printDouble(this.getDouble()));
//        toConsole.setBackground(lightred);
//        printCircularDot(centerx,centery,radius,getDouble(),""+ConsoleAnsi.windowFrames[0].charAt(11));
//        toConsole.resetColors();
//        return this;
//    }
//
//    public TTYSensor printCircularDot(int centerx, int centery, int radius, double degrees, String what) {
//        double angle =-((360-degrees) / 180 * Math.PI);
//        int cx = (int) Math.round(centerx +2*radius/ratio+ ratio*radius * Math.cos(angle)),
//                cy = (int) Math.round(centery - radius * Math.sin(angle));
//        toConsole.setCursorXY(cx, cy);
//        toConsole.print(what);
//        return this;
//    }
//
//    public TTYSensor printCircle(int centerx, int centery, int radius) {
//        for (int deg = 0; deg < 360; deg += 1) {
//            printCircularDot(centerx, centery, radius, deg, "" + ConsoleAnsi.windowFrames[0].charAt(11));
//        }
//        return this;
//    }
//
//    public void printCell(int x, int y, int level, boolean frame, boolean legend) {
//        int bg, fg;
//        if (level < 0) {
//            bg = red;
//        } else {
//            bg = palette.getColor(level);
//        }
//        fg = ConsoleAnsi.negColor(bg);
//        toConsole.setBackground(bg);
//        toConsole.setText(fg);
//        if (frame) {
//            toConsole.doRectangleFrame(x, y, cellx, celly);
//        } else {
//            toConsole.doRectangle(x, y, cellx, celly);
//        }
//        if (legend) {
//            toConsole.setCursorXY(x + 1, y + celly - 1);
//            if (level == NOREADING) {
//                toConsole.print("XXX");
//            } else {
//                toConsole.print(printShortInt(level));
//            }
//        }
//    }
//
////    public void printCell(int x, int y, int level) {
////        if (level == NOREADING) {
////            toConsole.setBackground(defColor(0.5, 0, 0));
////        } else {
////            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
////        }
////        toConsole.setText(lightgreen);
////        toConsole.doRectangleFrame(x, y, cellx, celly);
////        toConsole.setCursorXY(x, y + 2);
////        if (level == Perceptor.NULLREAD) {
////            toConsole.print("XXX");
////        } else {
////            toConsole.print(String.format("%3d", level));
////        }
////    }
////
////    public void printTinyCell(int x, int y, int level, ConsoleAnsi c) {
////        if (level == Perceptor.NULLREAD) {
////            toConsole.setBackground(defColor(0.5, 0, 0));
////        } else {
////            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
////        }
////        toConsole.doRectangle(x, y, cellx, celly);
////        toConsole.setCursorXY(x, y + 2);
//////        if (level == Perceptor.NULLREAD) {
//////            toConsole.print("XXX");
//////        } else {
//////            toConsole.print(String.format("%3d", level));
//////        }
////    }
////
////    public void printMiniCell(int x, int y, int level, ConsoleAnsi c) {
////        if (level == Perceptor.NULLREAD) {
////            toConsole.setBackground(defColor(0.5, 0, 0));
////        } else {
////            toConsole.setBackground(defColor(level / 255.0, level / 255.0, level / 255.0));
////        }
////        toConsole.doRectangleFrame(x, y, cellx - 1, celly - 1);
////        toConsole.setCursorXY(x, y + 1);
////        if (level == Perceptor.NULLREAD) {
////            toConsole.print("XXX");
////        } else {
////            toConsole.print(String.format("%3d", level));
////        }
////    }
//    public void close() {
//        if (autoclose) {
//            toConsole.close();
//        }
//    }
//
//}
