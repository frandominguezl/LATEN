/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminKeys;

import static PublicKeys.KeyGen.getKey;
import PublicKeys.PublicCryptor;
import com.eclipsesource.json.JsonArray;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author lcv
 */
public class AdminCryptor extends PublicCryptor {

    public AdminCryptor() {
        super("cardIDcryptor202"); /// 128 bits

    }

    public AdminCryptor(String k) {
        super(k);
    }

    public String keyPradoEncode(int pradocode) {
        String newkey = getKey(16), key = "";
        int len = 0, alen = _atoms.length(), letter;
        while (pradocode > 0) {
            if (pradocode % 100 < alen && pradocode % 100 >= 10) {
                letter = pradocode % 100;
                pradocode = pradocode / 100;
            } else {
                letter = pradocode % 10;
                pradocode = pradocode / 10;
            }
            key = _atoms.charAt(letter) + key;
            key = newkey.charAt(key.length()) + key;
//            newkey = newkey.substring(0, len)+_atoms.charAt(letter)+newkey.substring(len+1,newkey.length());
        }
        len = key.length();
        newkey = "" + _atoms.charAt(len) + key + newkey.substring(0, 15 - len);
        return newkey;
    }

    public int keyPradoDecode(String pradocode) {
        String result = "";
        int len = _atoms.indexOf((int) pradocode.charAt(0));
        for (int i = 0; i < len; i += 2) {
            result = result + _atoms.indexOf(pradocode.charAt(2 + i));
        }
        if (result.equals("")) {
            return 0;
        } else {
            int ret;
            try {
                ret = Integer.parseInt(result);
            } catch (Exception ex) {
                ret = 0;
            }
            return ret;
        }
    }

    public String enCryptNew(String text) {
        String res = "";
        Key aesKey;
        Cipher cipher;

        try {
            aesKey = new SecretKeySpec(_cryptoKey.getBytes(_charset), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(text.getBytes(_charset));
            res = new String(encrypted, _charset);
            byte[] digits = res.getBytes();
            String finals = "";
            for (byte b : digits) {
                finals += String.format("%03d", b + (int) 128);
            }
            res = finals;
        } catch (Exception ex) {
            System.err.println("DBA.encrypt " + ex.toString());
        }
        return res;
    }

    public String deCryptNew(String text) {
        String res = "";
        Key aesKey;
        Cipher cipher;

        try {

            byte thebytes[] = new byte[text.length() / 3];
            int i = 0;
            while (text.length() > 0) {
                String sbyte = text.substring(0, 3);
                thebytes[i++] = (byte) (Integer.parseInt(sbyte) - 128);
                text = text.substring(3);
            }
            text = new String(thebytes);
            aesKey = new SecretKeySpec(_cryptoKey.getBytes(_charset), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] encrypted = text.getBytes(_charset);
            res = new String(cipher.doFinal(encrypted), _charset);
        } catch (Exception ex) {
            System.err.println("DBA.decrypt: " + ex.toString());
        }
        return res;
    }

}
