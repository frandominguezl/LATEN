/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Database;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import AdminReport.ReportableObject;

/**
 *
 * @author lcv
 */
public class AgentDataBase implements ReportableObject {
    public static final int BADRECORD=-1;
    protected Connection _DBconnection;
    protected boolean _isError;
    protected String _whichError;
    protected String _url;
    protected PreparedStatement st;
    protected ResultSet rs;
    protected String q;
    protected int count;
    protected boolean stealthmode=false;

    public AgentDataBase() {
        _DBconnection = null;
        closeConnection();
    }

    public void activateStealthMode() {
        stealthmode = true;
    }
    
    public void deactivateStealthMode() {
        stealthmode = false;
    }
    public boolean isOpen() {
        return _DBconnection != null;
    }

    public boolean isError() {
        return _isError;
    }

    public boolean canContinue() {
        boolean can = isOpen() && !isError();
        if (!can) {
            System.err.println(whichError());
        }
        return can;
    }

    public String whichError() {
        return _whichError;
    }

    public void flushError() {
        _isError = false;
        _whichError = "";
    }

    public JsonObject reportStatus() {
        JsonObject res = new JsonObject();

        res.add("report", "database");
        return res;
    }

    public void reportException(Exception Ex) {
        _isError = Ex != null;
        if (isError()) {
            _whichError += "DataBase::" + Ex.toString() + "\n";
            System.err.println(whichError());
        }
    }

    public void validationQuery() {
        if (canContinue()) {
            this.queryDB("SELECT 1");
        }
    }

    public void startCommit() throws SQLException {
        this._DBconnection.setAutoCommit(false);
    }

    public void endCommit() throws SQLException {
        this._DBconnection.commit();
        this._DBconnection.setAutoCommit(true);
    }

    public void rollBack() {
        try {
            System.err.append("Database rolling back");
            this._DBconnection.rollback();
            this.flushError();
        } catch (SQLException ex) {
            reportException(ex);
        }
    }

    public boolean openConnection(String host, int port, String database, String user, String password) {
        _url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?serverTimezone=CET";
        try {
//            Class.forName("com.mysql.jdbc.Driver"); 
            Class.forName("com.mysql.cj.jdbc.Driver");
            _DBconnection = DriverManager.getConnection(_url, user, password);
            flushError();
        } catch (Exception ex) {
            reportException(ex);
            return false;
        }
        return true;
    }

    public void closeConnection() {
        if (isOpen())
            try {
            _DBconnection.close();
        } catch (Exception ex) {
            reportException(ex);
        }
        _isError = true;
        _whichError = "DB Not open yet";
    }

    public String getURL() {
        return _url;
    }

    public void closeTransaction() {
        try {
            if (st != null) {//&& !_st.isClosed()) {
                st.close();
            }
            if (rs != null) //&& !_rs.isClosed())
            {
                rs.close();
            }
//            if (st != null) {//&& !_st.isClosed()) {
//                if (rs != null) //&& !_rs.isClosed())
//                {
//                    rs.close();
//                }
//                st.close();
//            }
        } catch (SQLException ex) {
            reportException(ex);
        }
    }

    public boolean queryDB(String sentence) {
        closeTransaction();
        if (!canContinue()) {
            return false;
        }
        try {
            st = _DBconnection.prepareStatement(sentence);
            rs = st.executeQuery();
        } catch (SQLException ex) {
            reportException(ex);
        }
        return !isEmpty();
    }

    public boolean updateDB(String sentence) {
        closeTransaction();
        if (!canContinue()) {
            return false;
        }
        try {
            st = _DBconnection.prepareStatement(sentence);
            count = st.executeUpdate();
            rs = null;
        } catch (SQLException ex) {
            reportException(ex);
        }
        return count > 0;
    }

    public boolean insertDB(String sentence) {
        closeTransaction();
        if (!canContinue()) {
            return false;
        }
        try {
            st = _DBconnection.prepareStatement(sentence);
            count = st.executeUpdate();
            rs = null;
        } catch (SQLException ex) {
            reportException(ex);
        }
        return count > 0;
    }

    public boolean deleteDB(String sentence) {
        closeTransaction();
        if (!canContinue()) {
            return false;
        }
        try {
            st = _DBconnection.prepareStatement(sentence);
            count = st.executeUpdate();
            rs = null;
        } catch (SQLException ex) {
            reportException(ex);
        }
        return count > 0;
    }

    public ArrayList<String> getTableList() {
        ArrayList<String> res = new ArrayList<>();

        this.queryDB("Show tables");
        try {
            ResultSet r = this.getResult();
            while (r.next()) {
                res.add(r.getString(1));
            }
            this.closeTransaction();
        } catch (Exception Ex) {
            reportException(Ex);
        }
        return res;
    }

    public boolean isEmpty() {
//        try {
        if (rs != null) { //&& rs.next()) {
            return false;
        }
//        } catch (SQLException ex) {
//            reportException(ex);
//        }
        return true;
    }

    public ResultSet getResult() {
        return rs;
    }

    public JsonObject getJsonResult() {
        JsonObject res = new JsonObject(), aux;
        JsonArray reslist = new JsonArray();
        res.add("resultset", reslist);
        if (this.isEmpty()) {
            return res;
        }
        try {
            // get column names
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCnt = rsMeta.getColumnCount();
            String name;
            while (rs.next()) {
//            do{
                aux = new JsonObject();
                for (int i = 1; i <= columnCnt; i++) {
                    name = rsMeta.getColumnName(i);
                    switch (rsMeta.getColumnType(i)) {
                        case java.sql.Types.INTEGER:
                            aux.add(name, rs.getInt(name));
                            break;
                        case java.sql.Types.BIGINT:
                            aux.add(name, rs.getLong(name));
                            break;
                        case -7:
                            aux.add(name, rs.getBoolean(name));
                            break;
                        case java.sql.Types.VARCHAR:
                        case java.sql.Types.LONGVARCHAR:
                        default:
                            aux.add(name, rs.getString(name));
                            break;
                    }
                }
                reslist.add(aux);
//                if (!rs.next()) {
//                    this.closeTransaction();
//                    rs = null;
//                }
            }; // while (rs.next());
        } catch (Exception ex) {
            this.reportException(ex);
            return res;
        }
        res.set("resultset", reslist);

        return res;

    }

    public int getUpdatedRows() {
        return count;
    }

    public int getInsertedKey() {
        return count;
    }

    //
    // Abstract API
    //
    public JsonArray getAllRows(String table) {
        JsonArray res = new JsonArray();
        try {
            if (this.queryDB(String.format("SELECT * FROM %s ",
                    table))) {
                res = getJsonResult().get("resultset").asArray();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public JsonArray getAllRows(String table, String column, int value) {
        JsonArray res = new JsonArray();
        try {
            if (this.queryDB(String.format("SELECT * FROM %s WHERE %s = %d",
                    table, column, value))) {
                res = getJsonResult().get("resultset").asArray();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public JsonArray getAllRows(String table, String column, String value) {
        JsonArray res = new JsonArray();
        try {
            if (this.queryDB(String.format("SELECT * FROM %s WHERE %s = '%s'",
                    table, column, value))) {
                res = getJsonResult().get("resultset").asArray();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public int getColumnInt(String table, String intColumn, String column, int value) {
        int res = -1;
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(intColumn).asInt();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public int getColumnInt(String table, String intColumn, String column, String value) {
        int res = -1;
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(intColumn).asInt();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public long getColumnLong(String table, String longColumn, String column, int value) {
        long res = -1;
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(longColumn).asLong();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public long getColumnLong(String table, String longColumn, String column, String value) {
        long res = -1;
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(longColumn).asLong();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public String getColumnString(String table, String sColumn, String column, String value) {
        String res = "";
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(sColumn).asString();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public String getColumnString(String table, String sColumn, String column, int value) {
        String res = "";
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(sColumn).asString();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    public boolean getColumBoolean(String table, String intColumn, String column, int value) {
        boolean res = false;
        JsonArray aux = new JsonArray();
        try {
            aux = getAllRows(table, column, value);
            if (aux.size() > 0) {
                res = aux.get(0).asObject().get(intColumn).asBoolean();
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;
    }

    // Abstract JSON API
    // Assumes PK is always on column 1
    public JsonResult queryJsonDB(String query) {
        JsonResult res = null;
        try {
            if (this.queryDB(query)) {
                res = new JsonResult(getResult());
            }
        } catch (Exception ex) {
            this.reportException(ex);
        }
        return res;

    }

    public JsonResult getAllRowsJsonDB(String table) {
        String query = String.format("SELECT * FROM %s ", table);
        return queryJsonDB(query);
    }

    public JsonResult getAllRowsJsonDB(String table, JsonObject where) {
        String query = String.format("SELECT * FROM %s ", table);
        double dv;
        if (where != null) {
            query += " WHERE ";
            int count = 0;
            for (Member v : where) {
                if (count > 0) {
                    query += " AND ";
                }
                if (v.getValue().isNumber()) {
                    dv = v.getValue().asDouble();
                    if ((int) dv == dv) {
                        query += v.getName() + "=" + v.getValue().asInt();
                    } else {
                        query += v.getName() + "=" + v.getValue().asDouble();
                    }
                } else if (v.getValue().isString()) {
                    query += v.getName() + "='" + v.getValue().asString() + "'";
                } else {
                    query += v.getName() + "=" + (v.getValue().asBoolean() ? "true" : "false");
                }
                count++;
            }
        }
        return queryJsonDB(query);
    }

    @Override
    public String defReportType() {
        return "database";
    }

    @Override
    public String[] defReportableObjectList() {
        return new String[]{"open", "exceptions"};
    }

    @Override
    public String reportObjectDate(String objectid) {
        return "";
    }

    @Override
    public String reportObjectStatus(String objectid) {
        switch (objectid) {
            case "open":
                return "" + isOpen();
            case "exceptions":
                return whichError();
            default:
                return "unreported";

        }
    }

    @Override
    public String reportObjectValue(String objectid) {
        return "";
    }

}
