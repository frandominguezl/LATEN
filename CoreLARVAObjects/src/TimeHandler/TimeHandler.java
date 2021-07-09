/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TimeHandler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 *
 * @author lcv
 */
public class TimeHandler {

    public static final DateTimeFormatter inputdateformat = DateTimeFormatter.ofPattern("uuuu-MM-dd kk:mm:ss"),
            outputdateformat = inputdateformat, inputolddateformat = DateTimeFormatter.ofPattern("dd/MM/uuuu_kk:mm:ss"),
            outputolddateformat = inputdateformat;
    public static final TimeHandler _baseTime=new TimeHandler("2020-01-01 00:00:00");

    protected LocalDateTime _theTime;

    public TimeHandler() {
        _theTime = LocalDateTime.now();
    }

    public TimeHandler(long l) {
        _theTime =_baseTime._theTime.plusSeconds(l);
    }
    
    public TimeHandler(String stime) {
        try {
            _theTime = LocalDateTime.parse(stime, inputdateformat);
        } catch (DateTimeParseException ex) {
            try {
                _theTime = LocalDateTime.parse(stime, inputolddateformat);
            } catch (DateTimeParseException ex2) {
                _theTime = new TimeHandler()._theTime;
            }
        }

    }

    public boolean isAfterEq(TimeHandler t) {
        return _theTime.isAfter(t._theTime) || _theTime.isEqual(t._theTime);
    }

    public boolean isBeforeEq(TimeHandler t) {
        return _theTime.isBefore(t._theTime) || _theTime.isEqual(t._theTime);
    }

    public boolean isEqual(TimeHandler t) {
        return _theTime.isEqual(t._theTime);
    }
    
    public long elapsedTimeSecs(TimeHandler other) {
        Duration res = Duration.between(_theTime, other._theTime);
        
        return res.getSeconds();
    }

    public long elapsedTimeSecs() {
        Duration res = Duration.between(_baseTime._theTime, _theTime);
        
        return res.getSeconds();
    }

    @Override
    public String toString() {
        return outputdateformat.format(_theTime);
    }
}
