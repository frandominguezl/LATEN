/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and topenxsession the template in the editor.
 */
package Analytics;

import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;
import AdminKeys.AdminCryptor;
import Database.AgentDataBase;
import Database.JsonResult;
import Session.Session;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author lcv
 */
public class AnalyticsProject {

    AgentDataBase _dataBase;
    public int _assignmentID, _courseID, _whoID, _userID, _groupID;
    public String _atitle, _ctitle;
    public HashMap<Integer, ArrayList<String>> _mTitleByPID;
    public HashMap<String, String> _mTitleByMID;
    public boolean _isValid = false, _isIndividual;
    AdminCryptor _enigma = new AdminCryptor();

    public AnalyticsProject(AgentDataBase db, int assignmentID) {
        Init(db, assignmentID, -1);
    }

    public AnalyticsProject(AgentDataBase db, int assignmentID, int groupID) {
        Init(db, assignmentID, groupID);
    }

    public final void Init(AgentDataBase db, int assignmentID, int groupID) {
        int problemID = -1;
        JsonObject rowresult;
        JsonArray rowlistresult;
        String ptitle;
        String milestones, competenceID = "", milestoneTitle, milestoneID;

        _assignmentID = assignmentID;
        _groupID = groupID;
        _dataBase = db;
        rowlistresult = _dataBase.getAllRows("FullAssignmentsView", "assignmentID", _assignmentID);
        _mTitleByPID = new HashMap<>();
        _mTitleByMID = new HashMap<>();
        for (int i = 0; i < rowlistresult.size(); i++) {
            rowresult = rowlistresult.get(i).asObject();
            if (i == 0) {
                _atitle = rowresult.getString("atitle", "");
                _isIndividual = rowresult.getBoolean("isIndividual", false);
                _courseID = _dataBase.getColumnInt("Assignments", "courseID", "assignmentID", _assignmentID);
                _ctitle = _dataBase.getColumnString("Courses", "title", "courseID", _courseID);
//                System.out.println("Found assignment " + atitle);
            }
            if (rowresult.getInt("problemID", -1) != problemID) {
                problemID = rowresult.getInt("problemID", -1);
                ptitle = rowresult.getString("title", "");
//                System.out.println("   Found problem " + problemID
//                        + ": " + title);
                _mTitleByPID.put(problemID, new ArrayList<>());
            }
            milestones = rowresult.getString("milestones", "");
            while (milestones.length() > 0) {
                milestoneID = milestones.substring(0, 7);
                if (!milestoneID.substring(0, 5).equals(competenceID)) {
                    competenceID = milestoneID.substring(0, 5);
//                    System.out.println();
//                    System.out.print("      Competence " + competenceID + ": ");
                }
//                System.out.print(milestoneID + "(");
                JsonArray auxlist = _dataBase.getAllRows("Milestones", "milestoneID", milestoneID);
                milestoneTitle = auxlist.get(0).asObject().getString("title", "");
                _mTitleByPID.get(problemID).add(milestoneTitle);
                _mTitleByMID.put(milestoneID, milestoneTitle);
//                System.out.print(milestoneTitle + "), ");
                milestones = milestones.substring(7);
            }
//            System.out.println();
        }

        _isValid = true;
    }
    
    public boolean isEligibleUser(int userID) {
        return true;
//        JsonResult jsuser = _dataBase.queryJsonDB("SELECT * FROM FullCourseGroupRegistration WHERE userID="+userID+" AND courseID="+this._courseID+" AND groupID="+this._groupID);
//        return jsuser.size()>0;
    }

    public boolean isValidMilestoneReport(ACLMessage msg, Session s) {
//        return true;
        JsonObject report = null;
        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
            // Check registration into course/assignment
            // Check agentID
            String mtitle = getMilestoneTitle(msg);

            if (_dataBase.isOpen() && _dataBase.canContinue()) {
                if (s.assignmentID == getAssignmentID(msg)
                        && _mTitleByPID.get(s.problemID) != null
                        && _mTitleByPID.get(s.problemID).contains(mtitle)) {
                    s.mtitle = mtitle;
                    return true;
                }
            }

        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    public boolean isValidMilestoneReport(String milestoneID, Session s) {
        // Check registration into course/assignment
        // Check agentID
//        return true;
        try {
            String mtitle = getMilestoneTitle(milestoneID);
            if (_dataBase.isOpen() && _dataBase.canContinue()) {
                if (_mTitleByPID.get(s.problemID) != null
                        && _mTitleByPID.get(s.problemID).contains(mtitle)) {
                    s.mtitle = mtitle;
                    return true;
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

//    protected boolean addAnalyticsRecord(int assignmentID, int problemID, String milestoneID, int whoID) {
//
//        _dataBase.queryDB("SELECT * FROM LearningAnalytics WHERE assignmentID=" + assignmentID
//                + " AND problemID=" + problemID
//                + " AND milestoneID='" + milestoneID + "'"
//                + " AND whoID=" + whoID);
//        if (_dataBase.getJsonResult().get("resultset").asArray().size() == 0) {
//            _dataBase.updateDB("INSERT INTO LearningAnalytics (assignmentID, problemID, milestoneID, whoID, date) "
//                    + " VALUES (" + assignmentID + ", " + problemID + ", " + "'" + milestoneID + "' , " + whoID + ", '" +  new TimeHandler().toString() + "')");
//            return true;
//        }
//        return false;
//    }
    // {"report":"milestone","milestone":[{"behaviourname":"UNKNOWN","assignmentid":0,"problemid":0,"userid":"efkybkyYfoE8B7EB","date":"31/07/2020_21:45:12","objectid":"Take down"}]}}}}
    public int getAssignmentID(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return -1;
        }
        int res = -1;
        try {
            res = report.get("assignmentid").asInt();
        } catch (Exception ex) {
            res = -1;
        }
        return res;
    }

    public int getProblemID(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return -1;
        }
        int res = -1;
        try {
            res = report.get("problemid").asInt();
        } catch (Exception ex) {
            res = -1;
        }
        return res;
    }

    public String getMilestoneTitle(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return "";
        }
        String res = "";
        try {
            res = report.get("objectid").asString();
        } catch (Exception ex) {
            res = "";
        }
        return res;
    }

    public String getMilestoneTitle(String milestoneID) {
        String res = _mTitleByMID.get(milestoneID);
        return res;
    }

    public String getMilestoneReceiver(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return "";
        }
        String res = "";
        try {
            res = report.get("json").asObject().get("receiver").asString();
        } catch (Exception ex) {
            res = "";
        }
        return res;
    }

    public String getMilestoneSender(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return "";
        }
        String res = "";
        try {
            res = report.get("json").asObject().get("sender").asString();
        } catch (Exception ex) {
            res = "";
        }
        return res;
    }

    public String getMilestoneID(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return "";
        }
        String res = "", title = getMilestoneTitle(msg);

        res = _dataBase.getColumnString("Milestones", "milestoneID", "title", title);
        return res;
    }

    public int getUserID(ACLMessage msg) {
        int res = -1;
        try {
            String pradocode = msg.getEncoding();
            res = _enigma.keyPradoDecode(pradocode);
        } catch (Exception ex) {
            res = -1;
        }
        return res;

    }

    public int getGroupID(ACLMessage msg) {
        JsonObject report = null;

        // Check the format
        try {
            JsonObject body = getJsonContentACLM(msg);
            report = body.get(body.get("report").asString()).asArray().get(0).asObject();
        } catch (Exception ex) {
            return -1;
        }
        int res = -1, userID = getUserID(msg);
        res = _dataBase.getColumnInt("GroupMembers", "groupID", "userID", userID);
        return res;
    }

}
//
//public AnalyticsProject(AgentDataBase db, int assignmentID) {
//        String milestones, atitle = "", title = "", competenceID = "", milestoneTitle, milestoneID;
//        int problemID = -1;
//        JsonObject rowresult;
//        JsonArray rowlistresult;
//
//        assignmentID = assignmentID;
//        _dataBase = db;
//        if (_dataBase.isOpen() && !_dataBase.isError()) {
//            if (_dataBase.queryDB(String.format("SELECT * FROM FullAssignmentsView WHERE assignmentID = %d", assignmentID))) {
//                _mTitleByPID = new HashMap<>();
//                rowlistresult = _dataBase.getJsonResult().get("resultset").asArray();
//                rowresult = rowlistresult.get(0).asObject();
//                rowresult = _dataBase.getAllRows("FullAssignmentsView", "assignmentID", assignmentID);
//
//                for (int i = 0; i < rowlistresult.size(); i++) {
//                    rowresult = rowlistresult.get(i).asObject();
//                    if (i == 0) {
//                        atitle = rowresult.getString("atitle", "");
//                        isIndividual = rowresult.getBoolean("isIndividual", false);
////                        System.out.println("Found assignment " + atitle);
//                    }
//                    if (rowresult.getInt("problemID", -1) != problemID) {
//                        problemID = rowresult.getInt("problemID", -1);
//                        title = rowresult.getString("title", "");
////                        System.out.println("   Found problem " + problemID
////                                + ": " + title);
//                        _mTitleByPID.put(problemID, new ArrayList<>());
//                    }
//                    milestones = rowresult.getString("milestones", "");
//                    while (milestones.length() > 0) {
//                        milestoneID = milestones.substring(0, 7);
//                        if (!milestoneID.substring(0, 5).equals(competenceID)) {
//                            competenceID = milestoneID.substring(0, 5);
////                            System.out.println();
////                            System.out.print("      Competence " + competenceID + ": ");
//                        }
////                        System.out.print(milestoneID + "(");
//                        _dataBase.queryDB(String.format(
//                                "SELECT * FROM Milestones WHERE milestoneID = '%s'", milestoneID));
//                        JsonArray listaux = _dataBase.getJsonResult().get("resultset").asArray();
//                        milestoneTitle = listaux.get(0).asObject().getString("title", "");
//                        _mTitleByPID.get(problemID).add(milestoneTitle);
////                        System.out.print(milestoneTitle + "), ");
//                        milestones = milestones.substring(7);
//                    }
////                    System.out.println();
//                }
////                    for (int e : _mTitleByPID.keySet()) {
////                        milestones = _mTitleByPID.get(e).get(1);
////                        String milsID;
////                        while (milestones.length() > 0) {
////                            milsID = milestones.substring(0, 7);
////                            _dataBase.queryDB(String.format(
////                                    "SELECT * FROM Milestones WHERE milestoneD = '%s'", milsID));
////                            JsonObject jsonres = _dataBase.getJsonResult();
////                            System.out.println("      Found milestone " + milsID);
////                            _mTitleByPID.get(e).add()
////                            milestones = milestones.substring(7);
////                        }
////                    }
//            } else {
//                System.err.println("Empty query");
//            }
//            _isValid = true;
//        }
//    }
