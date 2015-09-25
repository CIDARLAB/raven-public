/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.datastructures;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Jenhan Tao <jenhantao@gmail.com>
 */
public class CartesianNode {

    private static int count = 0;
    public int id;

    public CartesianNode() {
        this.id = count;
        this.neighbors = new ArrayList();
        this.level = 0;
        this.abstractOverhang = null;
        this.concreteOverhang = null;
        this.usedOverhangs = new HashSet();
        count++;
    }

    public String getLibraryOverhang() {
        return concreteOverhang;
    }

    public void setLibraryOverhang(String overhang) {
        this.concreteOverhang = overhang;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public ArrayList<CartesianNode> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<CartesianNode> neighbors) {
        this.neighbors = neighbors;
    }

    public HashSet getUsedOverhangs() {
        return usedOverhangs;
    }

    public void setUsedOverhangs(HashSet usedOverhangs) {
        this.usedOverhangs = usedOverhangs;
    }

    public void addNeighbor(CartesianNode node) {
        this.neighbors.add(node);
    }

    public String getAbstractOverhang() {
        return abstractOverhang;
    }

    public void setNodeOverhang(String abstractOverhang) {
        this.abstractOverhang = abstractOverhang;
    }
    //fields
    public String concreteOverhang; //right concreteOverhang option specified by this CartesianNode
    public int level; //level of this node
    public ArrayList<CartesianNode> neighbors; //neighbors of this node
    public HashSet usedOverhangs;
    public String abstractOverhang;
}
