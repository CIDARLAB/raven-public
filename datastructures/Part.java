/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author jenhantao
 */
public class Part {

    public static Part generateComposite(ArrayList<Part> newComposition, String name) {
        Part newComposite = new Part();
        String sequence = "";
        for (Part p : newComposition) {
            sequence = sequence + p.getSeq();
        }
        newComposite.sequence = sequence;
        newComposite.composition = newComposition;
        newComposite.name = name;
        newComposite.uuid = "part_" + String.valueOf(UUID);
        newComposite.isComposite = true;
        newComposite._transient = true;
        return newComposite;
    }

    private Part() {
        UUID++;
        this.uuid = "part_" + String.valueOf(UUID);
    }

    public static Part generateBasic(String name, String sequence) {
        Part newBasic = new Part();
        newBasic.name = name;
        newBasic.sequence = sequence;
        newBasic.isComposite = false;
        newBasic.composition = new ArrayList<Part>();
        newBasic.composition.add(newBasic);
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

    public ArrayList<String> getSearchTags() {
        return this.searchTags;
    }

    public void setSearchTags(ArrayList<String> searchTags) {
        this.searchTags = searchTags;
    }

    public void addSearchTag(String string) {
        this.searchTags.add(string);
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

    //returns this part, or an exact match
    public Part saveDefault(Collector col) {
        Part toReturn = col.addPart(this);
        if (!this.equals(toReturn)) {
            UUID--;
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

    public String getType() {
        String toReturn = "";
        for (String tag : this.searchTags) {
            if (tag.startsWith("Type:")) {
                toReturn = tag.substring(6);
            }
        }
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

    public ArrayList<String> getDirections() {
        ArrayList<String> toReturn = new ArrayList();
        for (String tag : this.searchTags) {
            if (tag.startsWith("Direction:")) {
                toReturn = new ArrayList(Arrays.asList(tag.substring(12, tag.length() - 1).split(",")));
            }
        }
        return toReturn;
    }
    //Fields
    private ArrayList<Part> composition;
    private String name;
    private String sequence;
    private Boolean isComposite = false;
    private String uuid;
    private ArrayList<String> searchTags = new ArrayList();
    private boolean _transient = true;
    private static int UUID = 0;
}
