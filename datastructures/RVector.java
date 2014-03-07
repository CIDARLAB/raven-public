/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

/**
 *
 * @author evanappleton
 */
public class RVector {

    /** RVector constructor with explicit parameters **/
    public RVector(String lOverhang, String rOverhang, int level, String name, String uuid) {
        _uuid = uuid;
        _lOverhang = lOverhang;
        _rOverhang = rOverhang;
        _level = level;
        _resistance = new String();
        _name = name;
        _vectorID = _vectorCount;
        _vectorCount++;
    }
    
    /**************************************************************************
     * 
     * GETTER AND SETTER METHODS
     * 
     **************************************************************************/
    /** Get vectorID **/
    public int getVectorID() {
        return _vectorID;
    }

    /** Get Clotho UUID **/
    public String getUUID() {
        return _uuid;
    }

    /** Get left overhang **/
    public String getLOverhang() {
        return _lOverhang;
    }

    /** Get right overhang **/
    public String getROverhang() {
        return _rOverhang;
    }

    /** Get a vector's resistance **/
    public String getResistance() {
        return _resistance;
    }

    /** Get vector level **/
    public int getLevel() {
        return _level;
    }

    /** Get if this vector should be used for all nodes **/
    public boolean getUseForAll() {
        return _useForAll;
    }

    /** Get vector name **/
    public String getName() {
        return _name;
    }
    
    /** Get vector keys **/
    public String getVectorKey(String direction) {
        
        //Forward information
        String lOverhang = this._lOverhang;
        String rOverhang = this._rOverhang;
        String name = this._name;
        int stage = this._level;

        if (direction.equals("+")) {
            String aVecLOlevelRO = name + "|" + lOverhang + "|" + stage + "|" + rOverhang;
            return aVecLOlevelRO;
        } else {
            String lOverhangR = rOverhang;
            String rOverhangR = lOverhang;
            if (lOverhangR.contains("*")) {
                lOverhangR = lOverhangR.replace("*", "");
            } else {
                lOverhangR = lOverhangR + "*";
            }
            if (rOverhangR.contains("*")) {
                rOverhangR = rOverhangR.replace("*", "");
            } else {
                rOverhangR = rOverhangR + "*";
            }

            String aVecLOlevelROR = name + "|" + lOverhangR + "|" + stage + "|" + rOverhangR;
            return aVecLOlevelROR;
        }
    }

    /** Set Clotho UUID **/
    public void setUUID(String newuuid) {
        _uuid = newuuid;
    }

    /** Set right overhang **/
    public void setROverhang(String overhang) {
        _rOverhang = overhang;
    }

    /** Set left overhang **/
    public void setLOverhang(String overhang) {
        _lOverhang = overhang;
    }

    public void setStringResistance(String s) {
        _resistance = s;
    }

    /** Set vector level **/
    public void setLevel(int level) {
        _level = level;
    }

    /** Set this vector to be used by all intermediates of a graph **/
    public void setUseForAll(boolean useForAll) {
        _useForAll = useForAll;
    }

    /** Set vector name **/
    public void setName(String name) {
        _name = name;
    }
    
    //Fields
    private String _uuid;
    private String _lOverhang;
    private String _rOverhang;
    private String _resistance;
    private String _name;
    private int _level;
    private int _vectorID;
    private boolean _useForAll;
    private static int _vectorCount = 0;
}