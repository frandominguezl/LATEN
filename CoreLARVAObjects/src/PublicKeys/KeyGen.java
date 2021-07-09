/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PublicKeys;


/**
 *
 * @author lcv
 */
public class KeyGen {

    protected static final String _miniatoms="01234567890ABCDEF";
    protected static final String _atoms="abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    //protected static final String _atoms="0123456789ABCDEF";

    public static String getKey()  {
        return getKey(8);
    }    

    public static String getMiniKey()  {
        return getMiniKey(8);
    }    

    public static String getKey(int length)  {
        String newkey="";
        final int len=length;
        for (int i=0; i<len; i++)  {
            newkey=newkey+_atoms.charAt((int)(Math.random()*_atoms.length()));
        }
        return newkey;
    }    
    public static String getMiniKey(int length)  {
        String newkey="";
        final int len=length;
        for (int i=0; i<len; i++)  {
            newkey=newkey+_miniatoms.charAt((int)(Math.random()*_miniatoms.length()));
        }
        return newkey;
    }    

}
