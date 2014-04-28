/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;

/**
 *
 * @author jenhantao
 */
public class Vector {

    public static Vector generateVector(String name, String sequence) {
        Vector newVector = new Vector();
        newVector.sequence = sequence;
        newVector.name = name;
        newVector.searchTags = new ArrayList<String>();

        return newVector;
    }

    private Vector() {
        UUID++;
        this.uuid = "vector_"+String.valueOf(UUID);
    }

    public static Vector generateVector(String name) {
        Vector newVector = new Vector();
        newVector.name = name;
        newVector.searchTags = new ArrayList<String>();
        return newVector;
    }


    public String getUUID() {
        return this.uuid;
    }

    public ArrayList<String> getSearchTags() {
        return this.searchTags;
    }

    public void addSearchTag(String string) {
        this.searchTags.add(string);
    }

    //returns this vector or an exact match
    public Vector saveDefault(Collector coll) {
        Vector toReturn =  coll.addVector(this);
        if(!this.equals(toReturn)) {
            UUID--;
        }
        return toReturn;
    }

    public String getName() {
        return this.name;
    }

    public int getLevel() {
        int level = -1;
        for (String tag : this.searchTags) {
            if (tag.startsWith("Level:")) {
                level = Integer.parseInt(tag.substring(7).trim());
            }
        }
        return level;
    }

    public String getResistance() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Resistance:")) {
                toReturn = tag.substring(12).trim();
            }
        }
        return toReturn;
    }

    public String getLeftOverhang() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("LO:")) {
                toReturn = tag.substring(4);
            }
        }
        return toReturn;
    }

    public String getRightOverhang() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("RO:")) {
                toReturn = tag.substring(4);
            }
        }
        return toReturn;
    }
    
    public String getComposition() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Composition:")) {
                toReturn = tag.substring(13);
            }
        }
        return toReturn;
    }
    
    public String getVector() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Vector:")) {
                toReturn = tag.substring(8);
            }
        }
        return toReturn;
    }

    public String getType() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Type:")) {
                toReturn = tag.substring(6);
            }
        }
        return toReturn;
    }

    public String getSeq() {
        return this.sequence;
    }

    public Boolean isTransient() {
        return _transient;
    }

    public void setTransientStatus(Boolean b) {
        _transient = b;
    }
    //Fields
    protected static int UUID = 0;
    private String name;
    private ArrayList<String> searchTags = new ArrayList<String>();
    private String sequence;
    private String uuid;
    private Boolean _transient = true;
}
