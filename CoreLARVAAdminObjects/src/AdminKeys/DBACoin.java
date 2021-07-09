/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminKeys;

/**
 *
 * @author lcv
 */
public class DBACoin extends charCode {

    public static final String COINLEX = "ABCDEFGHIJKLMNOPQRSTUVXYZ",         
            NUMERICON = "0123456789", 
            LEXICON = "ABCDEFGHIJKLMNOPQRSTUVWXYZ_";

    protected String session, coin;
    protected int serie, owner;
    protected charCode tocode = new charCode(COINLEX);
    protected boolean crc = false;
    public static final int KOWNER = 4, KSESSION = 5, KSERIE = 3, KCOIN=KOWNER+KSESSION+KSERIE;
    public static final int MAXSERIE = (int) (Math.pow(10, KSERIE)),
            MAXOWNER = (int) (Math.pow(10, KOWNER));

    public DBACoin() {
        super();
        coin = session = "";
        serie = -1;
    }

//    public DBACoin(String code) {
//        super(code);
//        coin = session = "";
//        serie = -1;
//    }

    public boolean isValid() {
        int owner = getOwner(), serie = getSerie();
        return (getSession().length() == KSESSION && 0 <= serie && serie < MAXSERIE
                && 0 <= owner && owner < DBACoin.MAXOWNER || canonical < 0);
    }

    public String getSession() {
        return session;
    }

    public DBACoin setSession(String s) {
        session = s.trim();
        return this;
    }

    public int getSerie() {
        return serie;
    }

    public DBACoin setSerie(int s) {
        serie = s % MAXSERIE;
        return this;
    }

    protected String getSSerie() {
        String sformat = "%0" + KSERIE + "d";
        return String.format(sformat, serie);
    }

    protected DBACoin setSerie(String s) {
//        s = s.replaceAll("^0*", " ").trim();
        try {
            serie = Integer.parseInt(s);
        } catch (Exception ex) {
            serie = -1;
        }
        return this;
    }

    protected String getSOwner() {
        String sformat = "%0" + KOWNER + "d";
        return String.format(sformat, owner);
    }

    protected DBACoin setOwner(String s) {
        s = s.replaceAll("^0*", " ").trim();
        try {
            owner = Integer.parseInt(s);
        } catch (Exception ex) {
            owner = -1;
        }
        return this;
    }

    public int getOwner() {
        return owner;
    }

    public DBACoin setOwner(int o) {
        owner = o;
        return this;
    }

    public String getCoin() {
        return coin;
    }

    public String doShuffle(String s, int n) {
        
        if (n<=0) {
            return s;
        }
        char res[] = s.toCharArray();
        
        char aux  = res[0];
        for (int i=0; i<res.length-1; i++)
            res[i] = res[i+1];
        res[res.length-1]=aux;
        return doShuffle(new String (res), n-1);
    }
    
    public String doUnShuffle(String s, int n) {
        
        if (n<=0) {
            return s;
        }
        char res[] = s.toCharArray();
        
        char aux  = res[res.length-1];
        for (int i=res.length-1; i>0; i--)
            res[i] = res[i-1];
        res[0]=aux;
        return doUnShuffle(new String (res), n-1);
    }
    
    public String encodeCoin() {
        String word1 = getSSerie() + getSOwner() + getSession();
        word = doShuffle(word1,1);
        this.encode(word, crc);
        coin = tocode.decode(this, KCOIN, false);
        return getCoin();
    }

    public DBACoin decodeCoin(String s) {
        try {
            if (s.contains("#")) {
                tocode.encode(s.split("#")[1].trim(),false);
            } else {
                tocode.encode(s,false);
            }
        } catch (Exception Ex) {
        }
        String res = this.decode(tocode, KCOIN, crc);
        while (res.length() < KOWNER + KSERIE + KSESSION) {
            res = charSet.charAt(0) + res;
        }
//        System.out.println(s+" "+tocode.toString()+" "+tocode.decode()+" "+res);
        res = doUnShuffle(res,1);

        this.setSerie(res.substring(0, this.KSERIE));
        this.setOwner(res.substring(this.KSERIE, this.KOWNER + this.KSERIE));
        this.setSession(res.substring(this.KOWNER + this.KSERIE));
        return this;
    }

    public String toString() {
        return getSOwner() + "::" + getSSerie() + "::" + getSession(); //+"::"+canonical+"::"+decode() ;
    }
}
