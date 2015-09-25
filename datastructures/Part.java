/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author jenhantao
 */
public class Part {

    public static Part generateComposite(String name, ArrayList<Part> newComposition, ArrayList<String> scarSeqs, ArrayList<String> scars, ArrayList<String> linkers, ArrayList<String> directions, String LO, String RO, ArrayList<String> type) {
        Part newComposite = new Part();
        String sequence = "";
        
        //Get part sequences
        for (int i = 0; i < newComposition.size(); i++) {
            Part p = newComposition.get(i);
            if ("".equals(p.getSeq())) {
                sequence = "";
                break;
            }

            if (i > 0 && scarSeqs.size() == (newComposition.size() - 1)) {

                //Add sequences for scars and linkers
                String scarSeq = scarSeqs.get(i - 1).trim().toUpperCase();
                
                //Add scar seq if it exists - for blank scar seqeunces, this adds trimmed whitespace
                if (!scarSeq.isEmpty()) {
                    sequence = sequence + scarSeq + p.getSeq();
                } else {
                    sequence = sequence + p.getSeq();
                }
            } else {
                sequence = sequence + p.getSeq();
            }
        }

        newComposite.sequence = sequence;
        newComposite.composition = newComposition;
        newComposite.name = name;
        newComposite.uuid = "part_" + String.valueOf(UUID);
        newComposite.isComposite = true;
        newComposite._transient = true;        
        newComposite.directions = directions;
        newComposite.scars = scars;
        newComposite.linkers = linkers;
        newComposite.type = type;
        newComposite.leftOverhang = LO;
        newComposite.rightOverhang = RO;
        return newComposite;
    }

    private Part() {
        UUID++;
        this.uuid = "part_" + String.valueOf(UUID);
    }

    public static Part generateBasic(String name, String sequence, ArrayList<Part> composition, ArrayList<String> type, ArrayList<String> directions, String leftOverhang, String rightOverhang) {
        Part newBasic = new Part();
        newBasic.name = name;
        newBasic.sequence = sequence;
        newBasic.isComposite = false;
        newBasic.composition = new ArrayList();
        if (composition == null) {
            newBasic.composition.add(newBasic);
        } else {
            newBasic.composition.addAll(composition);
        }
        newBasic.directions = directions;
        newBasic.scars = new ArrayList();
        newBasic.linkers = new ArrayList();
        newBasic.type = type;
        newBasic.leftOverhang = leftOverhang;
        newBasic.rightOverhang = rightOverhang;
        
        newBasic._transient = true;
        return newBasic;
    }

    public String getName() {
        return this.name;
    }

    public String getUUID() {
        return this.uuid;
    }

    public boolean isComposite() {
        return this.isComposite;
    }

    public String getSeq() {
        return this.sequence;
    }

    public boolean isBasic() {
        return !isComposite;
    }

    //for a composite part, returns an ordered list of parts that describes its composition
    public ArrayList<Part> getComposition() {
        return this.composition;
    }

    public ArrayList<String> getStringComposition() {
        ArrayList<String> toReturn = new ArrayList();
        for (Part p : this.composition) {
            toReturn.add(p.getName());
        }
        return toReturn;
    }
    
    public String getPartKey (String dir, Boolean OHAssignment) {
        
        //Forward key information
        ArrayList<String> _composition = this.getStringComposition();
        ArrayList<String> _directions = this.getDirections();
        ArrayList<String> _scars = this.getScars();
        ArrayList<String> _linkers = this.getLinkers();
        String LO = this.getLeftOverhang();
        String RO = this.getRightOverhang();
        
        if (dir.equals("+")) {
            String aPartLOcompRO;
            if (!OHAssignment) {
                aPartLOcompRO = _composition + "|" + _directions + "|" + _scars + "|" + _linkers + "|" + LO + "|" + RO;
            } else {
                aPartLOcompRO = _composition + "|" + LO + "|" + RO + "|" + _directions;
            }
            return aPartLOcompRO;
        } else {
            
            //Backward key information
            ArrayList<String> revComp = (ArrayList<String>) _composition.clone();
            Collections.reverse(revComp);
            
            ArrayList<String> invertedDirections = new ArrayList();

            for(String d: _directions) {
                if(d.equals("+")) {
                    invertedDirections.add(0,"-");
                } else {
                    invertedDirections.add(0,"+");
                }
            }
            
            ArrayList<String> invertedScars = new ArrayList();
            for (String scar: _scars) {
                if (scar.contains("*")) {
                    scar = scar.replace("*", "");
                    invertedScars.add(0,scar);
                } else {
                    scar = scar + "*";
                    invertedScars.add(0,scar);
                }
            }
            
            ArrayList<String> invertedLinkers = new ArrayList();
            for (String linker: _linkers) {
                if (linker.contains("*")) {
                    linker = linker.replace("*", "");
                    invertedLinkers.add(0,linker);
                } else {
                    linker = linker + "*";
                    invertedLinkers.add(0,linker);
                }
            }
 
            String invertedLeftOverhang = RO;
            String invertedRightOverhang = LO;
            if (invertedLeftOverhang.contains("*")) {
                invertedLeftOverhang = invertedLeftOverhang.replace("*", "");
            } else {
                if (!invertedLeftOverhang.isEmpty()) {
                    invertedLeftOverhang = invertedLeftOverhang + "*";
                } else {
                    invertedLeftOverhang = invertedLeftOverhang;
                }                
            }
            if (invertedRightOverhang.contains("*")) {
                invertedRightOverhang = invertedRightOverhang.replace("*", "");
            } else {
                if (!invertedRightOverhang.isEmpty()) {
                    invertedRightOverhang = invertedRightOverhang + "*";
                } else {
                    invertedRightOverhang = invertedRightOverhang;
                }  
            }
            
            String aPartLOcompRO;
            if (!OHAssignment) {
                aPartLOcompRO = revComp + "|" + invertedDirections + "|" + invertedScars + "|" + invertedLinkers + "|" + invertedLeftOverhang + "|" + invertedRightOverhang;
            } else {
                aPartLOcompRO = revComp + "|" + invertedLeftOverhang + "|" + invertedRightOverhang + "|" + invertedDirections;
            }
            return aPartLOcompRO;
        }
    }

    //returns this part, or an exact match
    public Part saveDefault(Collector col) {
        Part toReturn = col.addPart(this);
        return toReturn;
    }

    public Boolean isTransient() {
        return _transient;
    }

    public void setTransientStatus(Boolean b) {
        _transient = b;
    }

    public void setComposition(ArrayList<Part> comp) {
        this.composition = comp;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    //Left overhang
    @Getter
    @Setter
    private String leftOverhang;
    
    //Right overhang
    @Getter
    @Setter
    private String rightOverhang;
    
    //Type
    @Getter
    @Setter
    private ArrayList<String> type;
    
    //Scars
    @Getter
    @Setter
    private ArrayList<String> scars;
    
    //Directions
    @Getter
    @Setter
    private ArrayList<String> directions;
    
    //Linker
    @Getter
    @Setter
    private ArrayList<String> linkers;
    
    //Fields
    private ArrayList<Part> composition;
    private String name;
    private String sequence;
    private Boolean isComposite = false;
    private String uuid;
    private boolean _transient = true;
    private static int UUID = 0;
}
