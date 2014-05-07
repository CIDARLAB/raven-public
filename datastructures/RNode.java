/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author jenhantao, evanappleton
 */
public class RNode {

    /** SDSNode constructor, no neighbors, parent or children or meta-data specified **/
    public RNode() {
        _recommended = false;
        _discouraged = false;
        _efficiency = 0;
        _successCnt = 0;
        _failureCnt = 0;
        _neighbors = new ArrayList<RNode>();
        _composition = new ArrayList<String>();
        _direction = new ArrayList<String>();
        _scars = new ArrayList<String>();
        _uuid = null;
        _type = new ArrayList<String>();
        _lOverhang = "";
        _rOverhang = "";
        _name = "";
        _stage = 0;
        _nodeID = _nodeCount;
        _nodeCount++;
    }

    /** SDSNode constructor for intermediates with meta-data, neighbors and composition, but no part**/
    public RNode(boolean recommended, boolean discouraged, ArrayList<String> composition, ArrayList<String> direction, ArrayList<String> type, ArrayList<String> scars, String lOverhang, String rOverhang, int successCnt, int failureCnt, RVector vector) {
        _uuid = null;
        _recommended = recommended;
        _discouraged = discouraged;
        _efficiency = 0;
        _successCnt = successCnt;
        _failureCnt = failureCnt;
        _neighbors = new ArrayList<RNode>();
        _scars = scars;
        _composition = composition;
        _direction = direction;
        _type = type;
        _lOverhang = lOverhang;
        _rOverhang = rOverhang;
        _vector = vector;
        _name = "";
        _nodeID = _nodeCount;
        _nodeCount++;
    }
    
    /** Clone nodes of a graph by traversing and copying nodes **/
    @Override
    public RNode clone() {
        
        RNode clone = new RNode();
        clone._recommended = this._recommended;
        clone._discouraged = this._discouraged;
        clone._uuid = this._uuid;
        clone._type = this._type;
        clone._lOverhang = this._lOverhang;
        clone._rOverhang = this._rOverhang;
        clone._composition = this._composition;
        clone._direction = this._direction;
        clone._scars = this._scars;
        clone._name = this._name;
        clone._stage = this._stage;
        clone._vector = this._vector;
        clone._efficiency = this._efficiency;
        clone._successCnt = this._successCnt;
        clone._failureCnt = this._failureCnt;
        ArrayList<RNode> neighbors = this._neighbors;
        cloneHelper(clone, this, neighbors);
        
        return clone;
    }
    
    private void cloneHelper(RNode parentClone, RNode parent, ArrayList<RNode> children) {
        
        for (int i = 0; i < children.size(); i++) {
            
            RNode child = children.get(i);
            
            RNode childClone = new RNode();
            childClone._recommended = child._recommended;
            childClone._discouraged = child._discouraged;
            childClone._uuid = child._uuid;
            childClone._type = child._type;
            childClone._lOverhang = child._lOverhang;
            childClone._rOverhang = child._rOverhang;
            childClone._composition = child._composition;
            childClone._direction = child._direction;
            childClone._scars = child._scars;
            childClone._name = child._name;
            childClone._stage = child._stage;
            childClone._vector = child._vector;
            childClone._efficiency = child._efficiency;
            childClone._successCnt = child._successCnt;
            childClone._failureCnt = child._failureCnt;
            
            parentClone.addNeighbor(childClone);
            childClone.addNeighbor(parentClone);
            
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                cloneHelper(childClone, child, grandChildren);
            }
        }
    }
    
    /** Merge nodes with part sequences that are too small with the current node **/
    public RNode mergeNodes(RNode smallNode, RNode parent, String thisSeq) {
        
        //Make new merged node
        RNode mergedNode = this.clone();
        mergedNode._lOverhang = smallNode._lOverhang;
        mergedNode._name = smallNode._name + "_" + this._name;
        
        ArrayList<String> mergedType = new ArrayList<String>();
        mergedType.addAll(smallNode._type);
        mergedType.addAll(this._type);
        mergedNode._type = mergedType;
        
        ArrayList<String> mergedComp = new ArrayList<String>();
        mergedComp.addAll(smallNode._composition);
        mergedComp.addAll(this._composition);
        mergedNode._composition = mergedComp;
        
        ArrayList<String> mergedDir = new ArrayList<String>();
        mergedDir.addAll(smallNode._direction);
        mergedDir.addAll(this._direction);
        mergedNode._direction = mergedDir;
        
        mergedNode._specialSeq = smallNode._specialSeq + thisSeq;
        mergedNode._PCRSeq = thisSeq;

        //Adjust neighbor nodes
        parent.removeNeighbor(smallNode);
        parent.replaceNeighbor(this, mergedNode);
        mergedNode._neighbors.clear();
        mergedNode._neighbors.add(parent);

        return mergedNode;
    }
    
    /**************************************************************************
     * 
     * GETTER AND SETTER METHODS
     * 
    **************************************************************************/
    
    public int getNodeID() {
        return _nodeID;
    }
    
    /** Determine if part at node is recommended **/
    public boolean getRecommended() {
        return _recommended;
    }

    /** Determine if part at node is recommended **/
    public boolean getDiscouraged() {
        return _discouraged;
    }
    
    /** Get Clotho UUID **/
    public String getUUID() {
        return _uuid;
    }

    /** Get part feature type **/
    public ArrayList<String> getType() {
        return _type;
    }
    
    /** Get left overhang **/
    public String getLOverhang() {
        return _lOverhang;
    }
    
    /** Get right overhang **/
    public String getROverhang() {
        return _rOverhang;
    }
    
    /** Get node neighbors **/
    public ArrayList<RNode> getNeighbors() {
        return _neighbors;
    }

    /** Get node composition **/
    public ArrayList<String> getComposition() {
        return _composition;
    }
    
    /** Get vector **/
    public RVector getVector() {
        return _vector;
    }
    
    /** Get name **/
    public String getName() {
        return _name;
    }
    
    /** Get stage **/
    public int getStage() {
        return _stage;
    }
    
    /** Get efficiency **/
    public double getEfficiency() {
        return _efficiency;
    }
    
    /** Get modularity **/
    public double getModularity() {
        return _modularity;
    }
    
    /** Return success count - for debugging **/
    public int getSuccessCnt() {
        return _successCnt;
    }
    
    /** Return failure count - for debugging **/
    public int getFailureCnt() {
        return _failureCnt;
    }
    
    /** Get the direction of the node's composition **/
    public ArrayList<String> getDirection() {
        return _direction;
    }
    
    /** Get the scars of a part **/
    public ArrayList<String> getScars() {
        return _scars;
    }
    
    /** Get the special sequence - only the case for merged nodes **/
    public String getSpecialSeq() {
        return _specialSeq;
    }
    
    /** Get the special PCR sequence - only the case for merged nodes **/
    public String getPCRSeq() {
        return _PCRSeq;
    }
    
    /** Get the special left flanking sequence - only the case for merged nodes **/
    public String getLeftSeq() {
        return _lSeq;
    }
    
    /** Get the special right flanking sequence - only the case for merged nodes **/
    public String getRightSeq() {
        return _rSeq;
    }
    
    /** Get node keys for either forward or reverse direction **/
    public String getNodeKey(String dir) {
        
        //Forward key information
        ArrayList<String> composition = this._composition;
        ArrayList<String> directions = this._direction;
        ArrayList<String> scars = this._scars;
        String leftOverhang = this._lOverhang;
        String rightOverhang = this._rOverhang;
        
        if (dir.equals("+")) {           
            String aPartLOcompRO = composition + "|" + directions + "|" + scars + "|" + leftOverhang + "|" + rightOverhang;
            return aPartLOcompRO;
        } else {
            
            //Backward key information
            ArrayList<String> revComp = (ArrayList<String>) composition.clone();
            Collections.reverse(revComp);
            
            ArrayList<String> invertedDirections = new ArrayList();

            for(String d: directions) {
                if(d.equals("+")) {
                    invertedDirections.add(0,"-");
                } else {
                    invertedDirections.add(0,"+");
                }
            }
            
            ArrayList<String> invertedScars = new ArrayList();
            for (String scar: scars) {
                if (scar.contains("*")) {
                    scar = scar.replace("*", "");
                    invertedScars.add(0,scar);
                } else {
                    scar = scar + "*";
                    invertedScars.add(0,scar);
                }
            }

            String invertedLeftOverhang = rightOverhang;
            String invertedRightOverhang = leftOverhang;
            if (invertedLeftOverhang.contains("*")) {
                invertedLeftOverhang = invertedLeftOverhang.replace("*", "");
            } else {
                invertedLeftOverhang = invertedLeftOverhang + "*";
            }
            if (invertedRightOverhang.contains("*")) {
                invertedRightOverhang = invertedRightOverhang.replace("*", "");
            } else {
                invertedRightOverhang = invertedRightOverhang + "*";
            }
            
            String aPartCompDirScarLOROR = revComp + "|" + invertedDirections + "|" + invertedScars + "|" + invertedLeftOverhang + "|" + invertedRightOverhang;
            return aPartCompDirScarLOROR;
        }
    }
    
    /** Set part as recommended or not required **/
    public void setRecommended(boolean recommended) {
        _recommended = recommended;
    }
    
    /** Set part as discouraged or not required **/
    public void setDiscouraged(boolean discouraged) {
        _discouraged = discouraged;
    }
    
    /** Set Clotho UUID **/
    public void setUUID(String newuuid) {
        _uuid = newuuid;
    }

    /** Set part feature type **/
    public void setType(ArrayList<String> type) {
        _type = type;
    }
    
    /** Set left overhang **/
    public void setLOverhang(String overhang) {
        _lOverhang = overhang;
    }
    
    /** Set right overhang **/
    public void setROverhang(String overhang) {
        _rOverhang = overhang;
    }
    
    /** Add neighbor node to end of the list **/
    public void addNeighbor(RNode newNeighbor) {
        _neighbors.add(newNeighbor);
    }
    
    /** Remove a node's neighbor **/
    public void removeNeighbor(RNode neighbor) {
        _neighbors.remove(neighbor);
    }

    
    /** Replace a neighbor with the same composition at an exact point in the list to conserve order **/
    public void replaceNeighbor(RNode oldNode, RNode newNode) {
        int indexOf = _neighbors.indexOf(oldNode);
        _neighbors.remove(indexOf);
        _neighbors.add(indexOf, newNode);
    }
    
    /** Set node composition **/
    public void setComposition(ArrayList<String> comp) {
        _composition = comp;
    }
    
    /** Set vector **/
    public void setVector(RVector vector) {
        _vector = vector;
    }
    
    /** Set name **/
    public void setName(String name) {
        _name = name;
    }
    
    /** Set stage of the node **/
    public void setStage(int stage) {
        _stage = stage;
    }
    
    /** Set the efficiency of a node **/
    public void setEfficiency(double eff) {
        _efficiency = eff;
    }
    
    /** Set the modularity of a node **/
    public void setModularity(double mod) {
        _modularity = mod;
    }
    
    /** Set success count **/
    public void setSuccessCnt(int success) {
        _successCnt = success;
    }
    
    /** Set failure count **/
    public void setFailureCnt(int failure) {
        _failureCnt = failure;
    }
    
    /** Set the direction of the node composition **/
    public void setDirection(ArrayList<String> direction) {
        _direction = direction;
    }
    
    /** Set the scars for a node **/
    public void setScars(ArrayList<String> scars) {
        _scars = scars;
    }
    
    /** Set a special sequence for a merged node **/
    public void setSpecialSeq(String seq) {
        _specialSeq = seq;
    }
    
    /** Set a special sequence for a merged node **/
    public void setPCRSeq(String seq) {
        _PCRSeq = seq;
    }
    
    /** Set a special sequence for a merged node **/
    public void setLeftSeq(String seq) {
        _lSeq = seq;
    }
    
    /** Set a special sequence for a merged node **/
    public void setRightSeq(String seq) {
        _rSeq = seq;
    }
    
    //FIELDS
    private int _successCnt;
    private int _failureCnt;
    private double _efficiency;
    private double _modularity;
    private boolean _recommended;
    private boolean _discouraged;
    private ArrayList<RNode> _neighbors;
    private ArrayList<String> _direction;
    private String _uuid;
    private ArrayList<String> _composition;
    private ArrayList<String> _type;
    private ArrayList<String> _scars;
    private String _lOverhang;
    private String _rOverhang;
    private RVector _vector;
    private String _name;
    private String _specialSeq;
    private String _PCRSeq;
    private String _lSeq;
    private String _rSeq;
    private int _nodeID;
    private int _stage;
    private static int _nodeCount = 0;
}
