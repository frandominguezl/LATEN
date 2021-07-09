/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminKeys;

import AdminKeys.AdminCryptor;
import PublicKeys.KeyGen;
import PublicKeys.PublicCardID;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import java.util.Arrays;

/**
 *
 * @author lcv
 */
public class AdminCardID extends PublicCardID {

    protected String _version = "1.0";
    protected AdminCryptor _localcryptor;

    public AdminCardID(String key) {
        _localcryptor = new AdminCryptor();
        _localcryptor.setCryptoKey(key);
    }

    public JsonObject encodeCardID(int code, String jsonstring) {
        JsonObject res = null, card = new JsonObject();
        try {
            JsonObject toencode = Json.parse(jsonstring).asObject();
            res = encodeCardID(code, toencode);
        } catch (Exception Ex) {
            System.err.println("encodeCard()::" + Ex.toString());
        }
        return res;
    }

    public JsonObject encodeCardID(int code, JsonObject toencode) {
        JsonObject res = null, card = new JsonObject();
        try {
            JsonArray fields = new JsonArray();
            for (Member m : toencode) {
                fields.add(m.getName());
            }
            if (fields.size() > 1) {
                res = new JsonObject();
                card.add("version", _version);
                card.add("fields", fields.add("code"));
                card.add("data", toencode);
                String s = _localcryptor.enCrypt(card.toString());
                res.add("shortid", _localcryptor.keyPradoEncode(code)).add(
                        "cardid", s);
            }
        } catch (Exception Ex) {
            System.err.println("encodeCard()::" + Ex.toString());
        }
        _data = res;
        return res;
    }

    public JsonObject encodeCardIDNew(int code, String jsonstring) {
        JsonObject res = null, card = new JsonObject();
        try {
            JsonObject toencode = Json.parse(jsonstring).asObject();
            res = encodeCardID(code, toencode);
        } catch (Exception Ex) {
            System.err.println("encodeCard()::" + Ex.toString());
        }
        return res;
    }

    public JsonObject encodeCardIDNew(int code, JsonObject toencode) {
        JsonObject res = null, card = new JsonObject();
        try {
            JsonArray fields = new JsonArray();
            for (Member m : toencode) {
                fields.add(m.getName());
            }
            if (fields.size() > 1) {
                res = new JsonObject();
                card.add("version", _version);
                card.add("fields", fields.add("code"));
                card.add("data", toencode);
                String s = _localcryptor.enCryptNew(card.toString());
                res.add("shortid", _localcryptor.keyPradoEncode(code)).add(
                        "cardid", s); // finaljsa.toString());
            }
        } catch (Exception Ex) {
            System.err.println("encodeCard()::" + Ex.toString());
        }
        _data = res;
        return res;
    }

    public JsonObject decodeCardID() {
        JsonObject res = null, card = null;
        try {
            String s = _localcryptor.deCrypt(getCardID()),
                    c = "" + _localcryptor.keyPradoDecode(getShortID());
            res = new JsonObject();
            res.add("shortid", c);
            res.add("cardid", Json.parse(s).asObject());
        } catch (Exception Ex) {
            System.err.println("decodeCard()::" + Ex.toString());
        }
        this.setData(res);
        return res;
    }

    public JsonObject decodeCardIDNew() {
        JsonObject res = null, card = null;
        try {
            String s = _localcryptor.deCryptNew(this.getCardID()),
                    c = "" + _localcryptor.keyPradoDecode(getShortID());
            res = new JsonObject();
            res.add("shortid", Integer.parseInt(c));
            res.add("cardid", Json.parse(s).asObject());
        } catch (Exception Ex) {
            System.err.println("decodeCard()::" + Ex.toString());
        }
        this.setData(res);
        return res;
    }

    public String getName() {
        if (this._data.get("shortid").isString()) {
            this.decodeCardIDNew();
        }
        return _data.get("cardid").asObject().get("data").asObject().getString("name", "nonamed");
    }

    public String getAlias() {
        if (this._data.get("shortid").isString()) {
            this.decodeCardIDNew();
        }
        return _data.get("cardid").asObject().get("data").asObject().getString("alias", "nonamed");
    }

    public String getEmail() {
        if (this._data.get("shortid").isString()) {
            this.decodeCardIDNew();
        }
        return _data.get("cardid").asObject().get("data").asObject().getString("email", "nonamed");
    }

    public int getUserID() {
        if (this._data.get("shortid").isString()) {
            this.decodeCardIDNew();
        }
        return _data.get("cardid").asObject().get("data").asObject().getInt("userID", -1);
    }

    public boolean isTeacher() {
        if (this._data.get("shortid").isString()) {
            this.decodeCardIDNew();
        }
        return _data.get("cardid").asObject().get("data").asObject().getString("isTeacher", "0").equals("1");
    }

}

//    public JsonObject encodeCardIDNew(int code, String jsonstring) {
//        JsonObject res = null, card = new JsonObject();
//        try {
//            JsonObject toencode = Json.parse(jsonstring).asObject();
//            res = encodeCardID(code, toencode);
//        } catch (Exception Ex) {
//            System.err.println("encodeCard()::" + Ex.toString());
//        }
//        return res;
//    }
//
//    public JsonObject encodeCardIDNew(int code, JsonObject toencode) {
//        JsonObject res = null, card = new JsonObject();
//        try {
//            JsonArray fields = new JsonArray();
//            for (Member m : toencode) {
//                fields.add(m.getName());
//            }
//            if (fields.size() > 1) {
//                res = new JsonObject();
//                card.add("version", _version);
//                card.add("fields", fields.add("code"));
//                card.add("data", toencode);
//                String s = _localcryptor.enCrypt(card.toString());
//                byte []digits = s.getBytes();
//                String finals="";
//                JsonArray finaljsa = new JsonArray();
//                for (byte b : digits)  {
//                    finals+= String.format("%03d", b+(int)128);
//                    finaljsa.add(b);
//                }
//                res.add("shortid", _localcryptor.keyPradoEncode(code)).add(
//                        "cardid", finals); // finaljsa.toString());
//            }
//        } catch (Exception Ex) {
//            System.err.println("encodeCard()::" + Ex.toString());
//        }
//        _data = res;
//        return res;
//    }
//
//    public JsonObject decodeCardID() {
//        JsonObject res = null, card = null;
//        try {
//            String s = _localcryptor.deCrypt(getCardID()),
//                    c = ""+_localcryptor.keyPradoDecode(getShortID());
//            res = new JsonObject();
//            res.add("shortid", c);
//            res.add("cardid", Json.parse(s).asObject());
//        } catch (Exception Ex) {
//            System.err.println("decodeCard()::" + Ex.toString());
//        }
//        this.setData(res);
//        return res;
//    }
//
//    public JsonObject decodeCardIDNew() {
//        JsonObject res = null, card = null;
//        try {
//            String aux=this.getCardID();
//            byte thebytes[] = new byte[aux.length()/3];
//            int i=0;
//            while (aux.length()>0) {
//                String sbyte=aux.substring(0, 3);
//                thebytes[i++]=(byte)(Integer.parseInt(sbyte)-128);
//                aux = aux.substring(3);
//            }
//            String s = _localcryptor.deCrypt(new String(thebytes)),
//                    c = ""+_localcryptor.keyPradoDecode(getShortID());
//            res = new JsonObject();
//            res.add("shortid", Integer.parseInt(c));
//            res.add("cardid", Json.parse(s).asObject());
//        } catch (Exception Ex) {
//            System.err.println("decodeCard()::" + Ex.toString());
//        }
//        this.setData(res);
//        return res;
//    }

