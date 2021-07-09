/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminReport;

/**
 * Interface to report the internal state of objects across the platform. It reports a set of object (objeclist) and, for each, a date, ans status and value
 * @author lcv
 */
public interface ReportableObject {

    /**
     * An ID of the type of report
     * @return 
     */
    public abstract String defReportType();
    
    /**
     * List of object's IDs reported
     * @return 
     */
    public abstract String [] defReportableObjectList();
    
    /**
     * Report the date associated to the object
     * @param objectid
     * @return 
     */
    public abstract String reportObjectDate(String objectid);
    
    /**
     * Reports and String decribing the internal state of the object
     * @param objectid
     * @return 
     */
    public abstract String reportObjectStatus(String objectid);
    /**
     * Report a value indicating a qualification of the state of the object
     * @param objectid
     * @return 
     */
    public abstract String reportObjectValue(String objectid);

}
