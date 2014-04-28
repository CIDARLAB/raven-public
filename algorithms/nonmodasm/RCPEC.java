/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms.nonmodasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.RGeneral;
import Controller.datastructures.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author evanappleton
 */
public class RCPEC extends RGeneral {
    
    /**
     * Clotho part wrapper for CPEC *
     */
    public ArrayList<RGraph> cpecClothoWrapper(HashSet<Part> gps, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Double> efficiencies, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs) throws Exception {

        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;
        ArrayList<Part> goalParts = new ArrayList<Part>(gps);

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(goalParts, partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(gps);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, false);
        assignOverhangs(optimalGraphs, stageVectors);

        return optimalGraphs;
    }
    
    /** Assign overhangs for scarless assembly **/
    private void assignOverhangs(ArrayList<RGraph> asmGraphs, HashMap<Integer, Vector> stageVectors) {
        
        //Initialize fields that record information to save complexity for future steps
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        
        HashMap<Integer, RVector> stageRVectors = new HashMap<Integer, RVector>();
        for (Integer stage : stageVectors.keySet()) {
            RVector vec = ClothoReader.vectorImportClotho(stageVectors.get(stage));
            stageRVectors.put(stage, vec);
        }
        
        for (int i = 0; i < asmGraphs.size(); i++) {
            
            RGraph graph = asmGraphs.get(i);
            RNode root = graph.getRootNode();
            RVector vector = stageRVectors.get(root.getStage() % stageRVectors.size());
            
            ArrayList<String> composition = root.getComposition();
            ArrayList<String> direction = root.getDirection();
            
            //Assign overhangs of vector and goal part if a vector exists              
            RVector newVector = new RVector(composition.get(0) + direction.get(0), composition.get(composition.size() - 1) + direction.get(composition.size() - 1), root.getStage(), vector.getName(), null);
            root.setVector(newVector);
            root.setLOverhang(vector.getName() + "_R");
            root.setROverhang(vector.getName() + "_L");

            ArrayList<RNode> neighbors = root.getNeighbors();
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);

            //Make a new dummy root node to accomodate overhang assignment
            RNode fakeRootClone = root.clone();
            RVector fakeRootVec = new RVector(vector.getName() + "_R", vector.getName() + "_L", root.getStage(), "dummyVec", null);
            fakeRootClone.setVector(fakeRootVec);
            assignOverhangsHelper(fakeRootClone, neighbors, root, stageRVectors);
        }
        
        //
        for (RGraph graph : asmGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<String> rootDir = new ArrayList<String>();
            ArrayList<String> direction = root.getDirection();
            rootDir.addAll(direction);
            ArrayList<RNode> l0Nodes = _rootBasicNodeHash.get(root);

            //Determine which levels each basic node impacts            
            for (int i = 0; i < l0Nodes.size(); i++) {

                //Determine direction of basic level 0 nodes               
                RNode l0Node = l0Nodes.get(i);               
                String l0Direction = rootDir.get(0);               
                if (l0Node.getComposition().size() == 1) {
                    ArrayList<String> l0Dir = new ArrayList<String>();
                    l0Dir.add(l0Direction);
                    l0Node.setDirection(l0Dir);
                }               
                int size = l0Node.getDirection().size();
                rootDir.subList(0, size).clear();
            }
        }
        
    }
    
    /** Overhang assignment helper **/
    private void assignOverhangsHelper(RNode parent, ArrayList<RNode> neighbors, RNode root, HashMap<Integer, RVector> stageRVectors) {
        
        ArrayList<RNode> children = new ArrayList<RNode>();
        
        //Get children
        for (int i = 0; i < neighbors.size(); i++) {
            RNode current = neighbors.get(i);
            if (current.getStage() < parent.getStage()) {
                children.add(current);
            }            
        }
        
        //For each of the children, assign overhangs based on neighbors
        for (int j = 0; j < children.size(); j++) {
            RNode child = children.get(j);
            
            //Assign overhangs of vector and goal part if a vector exists
            RVector vector = stageRVectors.get(child.getStage() % stageRVectors.size());
            
            if (j == 0) {
                ArrayList<String> nextComp = children.get(j + 1).getComposition();
                ArrayList<String> nextDir = children.get(j + 1).getDirection();
                child.setROverhang(nextComp.get(0));
                child.setLOverhang(parent.getLOverhang());
                
                if (vector != null && child.getStage() != 0) {
                    RVector newVector = new RVector(parent.getVector().getLOverhang(), nextComp.get(0) + nextDir.get(0), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                }          

            } else if (j == children.size() - 1) {
                ArrayList<String> prevComp = children.get(j - 1).getComposition();
                ArrayList<String> prevDir = children.get(j - 1).getDirection();
                child.setLOverhang(prevComp.get(prevComp.size() - 1));
                child.setROverhang(parent.getROverhang());
                
                if (vector != null && child.getStage() != 0) {
                    RVector newVector = new RVector(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1), parent.getVector().getROverhang(), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                }
                
            } else {
                ArrayList<String> nextComp = children.get(j + 1).getComposition();
                ArrayList<String> prevComp = children.get(j - 1).getComposition();
                ArrayList<String> nextDir = children.get(j + 1).getDirection();
                ArrayList<String> prevDir = children.get(j - 1).getDirection();
                child.setLOverhang(prevComp.get(prevComp.size() - 1));
                child.setROverhang(nextComp.get(0));
                
                if (vector != null && child.getStage() != 0) {
                    RVector newVector = new RVector(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1), nextComp.get(0) + nextDir.get(0), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                }               
            }

            if (child.getStage() == 0) {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
            
            ArrayList<RNode> grandChildren = child.getNeighbors();           
            assignOverhangsHelper(child, grandChildren, root, stageRVectors);
        }
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }

    //FIELDS
    private static HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
}
