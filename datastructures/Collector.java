/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JOptionPane;

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

    public Part getExactPart(String name, String seq, ArrayList<String> tags, boolean allowTransient) {
//        System.out.println("looking for: " + name);
//        System.out.println("query: " + new HashSet(tags));
        Part toReturn = null;
        HashSet<String> queryTags;
        HashSet<String> currentTags;
        ArrayList<Part> allPartsWithName = this.getAllPartsWithName(name, allowTransient);
        if (allPartsWithName != null) {
            for (Part p : allPartsWithName) {
                if (allowTransient || !p.isTransient()) {
                    queryTags = new HashSet(tags);
                    currentTags = new HashSet(p.getSearchTags());
//                    System.out.println("exist: " + currentTags);
                    if (currentTags.equals(queryTags) && p.getSeq().equals(seq)) {
                        toReturn = p;
//                        System.out.println("returning: " + toReturn);
                        return toReturn;
                    }
                }
            }
        }
//        System.out.println("returning: " + toReturn);
        return toReturn;
    }

    //returns the part you added or an existing part that matches exactly
    public Part addPart(Part aPart) {
        Part existingPart = this.getExactPart(aPart.getName(), aPart.getSeq(), aPart.getSearchTags(), false);
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
    public Vector getExactVector(String name, String seq, ArrayList<String> tags, boolean allowTransient) {
        Vector toReturn = null;
        ArrayList<Vector> allVectorsWithName = this.getAllVectorsWithName(name, allowTransient);
        if (allVectorsWithName != null) {
            for (Vector p : allVectorsWithName) {
                if (allowTransient || !p.isTransient()) {
                    HashSet<String> queryTags = new HashSet(tags);
                    HashSet<String> currentTags = new HashSet(p.getSearchTags());
                    if (currentTags.equals(queryTags) && p.getSeq().equals(seq)) {
                        toReturn = p;
                        return toReturn;
                    }
                }
            }
        }
        return toReturn;
    }

    //returns the vector you added or an existing vector that matches exactly
    public Vector addVector(Vector aVector) {
        Vector existingVector = this.getExactVector(aVector.getName(), aVector.getSeq(), aVector.getSearchTags(), false);
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
