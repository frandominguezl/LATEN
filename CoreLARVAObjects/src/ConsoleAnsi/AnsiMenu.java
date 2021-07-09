/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ConsoleAnsi;

import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.defBackground;
import static ConsoleAnsi.ConsoleAnsi.defText;
import static ConsoleAnsi.ConsoleAnsi.gray;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author lcv
 */
public class AnsiMenu {

    private ConsoleAnsi _parent;
    private ArrayList<String> _options;
    String _title;
    int _x, _y, _w, _h, _cw, _ch;

    public AnsiMenu() {
        _parent = new ConsoleAnsi("internal");
        Init();
    }

    public AnsiMenu(ConsoleAnsi owner) {
        _parent = owner;
        Init();
    }

    public void Init() {
        _options = new ArrayList<>();
        _title = "";
        _cw = _parent._width;
        _ch = _parent._height;
        _x = _cw / 2;
        _y = _ch / 2;
        _w = 1;
        _h = 2;
        addOption("Exit");
    }

    public int size() {
        return _options.size();
    }

    public AnsiMenu addOption(String option) {
        _options.add(option);
        _w = Math.max(_w, option.length() + 7);
        _x = (_cw - _w) / 2;
        _h++;
        _y = (_ch - _h) / 2;
        return this;
    }

    public AnsiMenu addTitle(String t) {
        _title = t;
        _w = Math.max(_w, t.length() + 7);
        _x = (_cw - _w) / 2;
        return this;
    }

    public int chooseOption() {
        showFancyMenu();
        int option = 0;
        do {
            try {
                option = _parent.readInt();
            } catch (Exception ex) {
            }
        } while (option < 0 || option > size() + 1);
        return option;

    }

    public void showMenu() {
        int i, ct = _parent.getText(), cb = _parent.getBackground();

        _parent.setCursorXY(1, 1).setText(cb).setBackground(ct).print(" = ");
        for (i = 0; i < size(); i++) {
//            _parent.print("["+(i+1)+"]   "+_options.get(i));
            _parent.print(String.format("%s%s%d%s%s%s", _parent.defText(ct), _parent.defBackground(cb),
                    i + 1, _parent, _parent.defText(ct), _parent.defBackground(ct), _options.get(i)));
        }
        _parent.print(String.format("%s%s%d%s%s%s", _parent.defText(ct), _parent.defBackground(cb),
                0, _parent, _parent.defText(ct), _parent.defBackground(ct), "Exit "));

    }

    public void showFancyMenu() {
        int i, ct = _parent.getText(), cb = _parent.getBackground();
        _parent.setBackground(ct);
        _parent.setText(cb);
        _parent.doRectangleFrame(_x, _y, _w, _h);
        _parent.setCursorXY(_x + 2, _y);
        _parent.print(_title);
        for (i = 0; i < size(); i++) {
            _parent.setCursorXY(_x + 2, _y + i + 1);
            _parent.print("" + i + ". " + _options.get(i));
        }
        _parent.setCursorXY(_x + 2, _y + _h - 1);
        _parent.print("Choose option ");
        _parent.setBackground(cb);
        _parent.setText(ct);
    }
}
