/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ConfigFile;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Luis Castillo
 */
public class ConfigFile {
   private String _filename;
   public JsonObject _settings;
   
   public ConfigFile()  {
      _filename="";
      _settings=null;
   }
    public ConfigFile(String file) {
        _filename = file;
    }
    public ConfigFile(String path, String file) {
        _filename = path+file;
    }
    
    public boolean isEmpty() {
        return _filename.equals("");
    }
    public String getConfigFileName()  {
        return _filename;
    }

    public boolean openConfig()  {
        File file;
        String str="";

        if (isEmpty())
            return true;
        file = new File(_filename);
        if (file != null)
            if (file.exists()){
                if (file.isFile())  {
                    try {
                        str= new Scanner(new File(_filename)).useDelimiter("\\Z").next();
                        //str = FileUtils.readFileToString(file, "utf-8");
                        _settings = Json.parse(str).asObject();
                    }  catch (Exception ex) {
                        return false;
                    }
                    //str = FileUtils.readFileToString(file, "utf-8");
                    return true;
                }
                else
                    return false;
            }
            else
                return false;
        else
            return false;
    }
   
    public boolean saveConfig() {
        PrintWriter outfile;
        try {
            outfile = new PrintWriter(new BufferedWriter(new 
                    FileWriter(_filename)));
        } catch (IOException ex) {
            return false;
        }
        String toRecord= _settings.toString(WriterConfig.PRETTY_PRINT); 
        outfile.println(toRecord);
        outfile.close();
        return true;
        
    }
}
