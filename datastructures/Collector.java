/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author jenhantao
 */
public class Collector {

    public Part getPart(String uuid, boolean allowTransient) {
        Part toReturn = partUUIDHash.get(uuid);
        if (toReturn != null) {
            if (!toReturn.isTransient() || allowTransient) {
                return toReturn;
            }
        }
        return null;
    }

    public Vector getVector(String uuid, boolean allowTransient) {
        Vector toReturn = vectorUUIDHash.get(uuid);
        if (toReturn != null) {
            if (!toReturn.isTransient() || allowTransient) {
                return toReturn;
            }
        }
        return null;
    }

    public ArrayList<Vector> getAllVectors(boolean allowTransient) {
        ArrayList<Vector> returnCandidates = new ArrayList(vectorUUIDHash.values());
        ArrayList<Vector> toReturn = new ArrayList();
        for (Vector v : returnCandidates) {
            if (!v.isTransient() || allowTransient) {
                toReturn.add(v);
            }
        }
        return toReturn;
    }

    public ArrayList<Part> getAllParts(boolean allowTransient) {
        ArrayList<Part> returnCandidates = new ArrayList(partUUIDHash.values());
        ArrayList<Part> toReturn = new ArrayList();
        for (Part p : returnCandidates) {
            if (!p.isTransient() || allowTransient) {
                toReturn.add(p);
            }
        }
        return toReturn;
    }

    public ArrayList<Part> getAllPartsWithName(String name, boolean allowTransient) {
        ArrayList<Part> toReturn = new ArrayList();
        HashSet<String> partUUIDs = partNameHash.get(name);
        if (partUUIDs != null) {
            for (String uuid : partUUIDs) {
                Part current = partUUIDHash.get(uuid);
                if (allowTransient || !current.isTransient()) {
                    toReturn.add(current);
                }
            }
        }
        return toReturn;
    }

    public ArrayList<Vector> getAllVectorsWithName(String name, boolean allowTransient) {
        ArrayList<Vector> toReturn = new ArrayList();
        HashSet<String> vectorUUIDs = vectorNameHash.get(name);
        if (vectorUUIDs != null) {
            for (String uuid : vectorUUIDs) {
                Vector current = vectorUUIDHash.get(uuid);
                if (allowTransient || !current.isTransient()) {
                    toReturn.add(current);
                }
            }
        }
        return toReturn;
    }

     public Part getExactPart(String name, String seq, ArrayList<String> composition, String leftOverhang, String rightOverhang, ArrayList<String> type, ArrayList<String> scars, ArrayList<String> directions, boolean allowTransient) {

        Part toReturn = null;
        ArrayList<Part> allPartsWithName = this.getAllPartsWithName(name, allowTransient);

        if (allPartsWithName != null) {
            for (Part p : allPartsWithName) {
                if (allowTransient || !p.isTransient()) {
                    if (seq != null) {
                        if (p.getSeq().equals(seq) && composition.equals(p.getStringComposition()) && p.getLeftOverhang().equals(leftOverhang) && p.getRightOverhang().equals(rightOverhang) && p.getType().equals(type) && p.getScars().equals(scars) && p.getDirections().equals(directions)) {
                            toReturn = p;
                            return toReturn;
                        }
                    } else {
                        if (composition.equals(p.getStringComposition()) && p.getLeftOverhang().equals(leftOverhang) && p.getRightOverhang().equals(rightOverhang) && p.getType().equals(type) && p.getScars().equals(scars) && p.getDirections().equals(directions)) {
                            toReturn = p;
                            return toReturn;
                        }
                    }
                }
            }
        }
        return toReturn;
    }

    //returns the part you added or an existing part that matches exactly
    public Part addPart(Part aPart) {
        Part existingPart = this.getExactPart(aPart.getName(), aPart.getSeq(), aPart.getStringComposition(), aPart.getLeftOverhang(), aPart.getRightOverhang(), aPart.getType(), aPart.getScars(), aPart.getDirections(), false);
        if (existingPart != null) {
            return existingPart;
        }
        if (!partNameHash.containsKey(aPart.getName())) {
            partNameHash.put(aPart.getName(), new HashSet());
        }
        partNameHash.get(aPart.getName()).add(aPart.getUUID());
        partUUIDHash.put(aPart.getUUID(), aPart);
        return aPart;

    }

    //returns the vector you added or an existing vector that matches exactly
    public Vector getExactVector(String name, String seq, boolean allowTransient, String leftOverhang, String rightOverhang, String type, String vector, String composition, String resistance, int level) {
        Vector toReturn = null;
        ArrayList<Vector> allVectorsWithName = this.getAllVectorsWithName(name, allowTransient);
        if (allVectorsWithName != null) {
            for (Vector v : allVectorsWithName) {
                if (allowTransient || !v.isTransient()) {
                    if (v.getSeq().equals(seq) && v.getLeftOverhang().equals(leftOverhang) && v.getRightOverhang().equals(rightOverhang) && v.getType().equals(type) && v.getVector().equals(vector) && v.getComposition().equals(composition) && v.getResistance().equals(resistance) && v.getLevel() == level) {
                        toReturn = v;
                        return toReturn;
                    }
                }
            }
        }
        return toReturn;
    }

    //returns the vector you added or an existing vector that matches exactly
    public Vector addVector(Vector aVector) {
        Vector existingVector = this.getExactVector(aVector.getName(), aVector.getSeq(), false, aVector.getLeftOverhang(), aVector.getRightOverhang(), aVector.getType(), aVector.getVector(), aVector.getComposition(), aVector.getResistance(), aVector.getLevel());
        if (existingVector != null) {
            return existingVector;
        }
        if (!vectorNameHash.containsKey(aVector.getName())) {
            vectorNameHash.put(aVector.getName(), new HashSet());
        }
        vectorNameHash.get(aVector.getName()).add(aVector.getUUID());
        vectorUUIDHash.put(aVector.getUUID(), aVector);
        return aVector;

    }

    public boolean removePart(String uuid) throws Exception {
        Part toRemovePart = partUUIDHash.get(uuid);
        partNameHash.get(toRemovePart.getName()).remove(uuid);
        partUUIDHash.remove(uuid);
        return true;
    }

    public boolean removeVector(String uuid) throws Exception {
        Vector toRemoveVector = vectorUUIDHash.get(uuid);
        vectorNameHash.get(toRemoveVector.getName()).remove(uuid);
        vectorUUIDHash.remove(uuid);
        return true;
    }
    private HashMap<String, Vector> vectorUUIDHash = new HashMap(); //key: uuid, value: vector
    private HashMap<String, HashSet<String>> vectorNameHash = new HashMap(); //key name, value hashset of uuids
    private HashMap<String, HashSet<String>> partNameHash = new HashMap(); //key name, value hashset of uuids
    private HashMap<String, Part> partUUIDHash = new HashMap(); //key:uuid, value: part

    public void purge() {
        vectorUUIDHash = new HashMap();
        vectorNameHash = new HashMap();
        partNameHash = new HashMap();
        partUUIDHash = new HashMap();
    }
}
