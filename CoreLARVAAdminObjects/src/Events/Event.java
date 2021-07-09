/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Events;

/**
 *
 * @author lcv
 */
public class Event {
    public static final String eINFO="info", eSYSTEM="system",eACTION="agentaction",eERROR="error", 
            eWARNING="warning", eEXCEPTION="exception", eMILESTONE="milestone", ePROBLEM="problem",
            eBADGE="badge", eACLM="aclmessage";
    public static final String eeMOVE="move";
    public String date, type, subtype, description;
}
