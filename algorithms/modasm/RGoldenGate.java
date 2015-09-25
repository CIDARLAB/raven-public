/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.modasm;

import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.Collector;
import org.cidarlab.raven.datastructures.RVector;
import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RNode;
import org.cidarlab.raven.datastructures.Vector;
import org.cidarlab.raven.accessibility.ClothoReader;
import org.cidarlab.raven.algorithms.core.PrimerDesign;
import org.cidarlab.raven.algorithms.core.RGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jenhantao,evanappleton
 */
public class RGoldenGate extends RGeneral {

    /**
     * Clotho part wrapper for Golden Gate assembly *
     */
    public ArrayList<RGraph> goldenGateClothoWrapper(HashSet<Part> gps, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Double> efficiencies, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs) throws Exception {

        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(gps);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, false);
        assignOverhangs(optimalGraphs, stageVectors);

        return optimalGraphs;
    }
    
    /** Assign overhangs for scarless assembly **/
    private void assignOverhangs(ArrayList<RGraph> asmGraphs, HashMap<Integer, Vector> stageVectors) {
        
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
            
            //Assign overhangs of vector and goal part if a vector exists
            if (vector != null) {                
                RVector newVector = new RVector(composition.get(0), composition.get(composition.size()-1), root.getStage(), vector.getName(), null);
                root.setVector(newVector);              
                root.setLOverhang(vector.getName() + "_R");
                root.setROverhang(vector.getName() + "_L");
            } else {
                root.setLOverhang(composition.get(composition.size() - 1));
                root.setROverhang(composition.get(0));
            }
                        
            ArrayList<RNode> neighbors = root.getNeighbors();
            assignOverhangsHelper(root, neighbors, root, stageRVectors);
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
                child.setROverhang(nextComp.get(0) + nextDir.get(0));
                child.setLOverhang(parent.getLOverhang());
                
                if (child.getStage() > 0) {
                if (vector != null) {
                    RVector newVector = new RVector(parent.getVector().getLOverhang(), nextComp.get(0) + nextDir.get(0), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                }
                }

            } else if (j == children.size() - 1) {
                ArrayList<String> prevComp = children.get(j - 1).getComposition();
                ArrayList<String> prevDir = children.get(j - 1).getDirection();
                child.setLOverhang(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1));
                child.setROverhang(parent.getROverhang());
                
                if (child.getStage() > 0) {
                if (vector != null) {
                    RVector newVector = new RVector(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1), parent.getVector().getROverhang(), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                }
                }

            } else {
                ArrayList<String> nextComp = children.get(j + 1).getComposition();
                ArrayList<String> prevComp = children.get(j - 1).getComposition();
                ArrayList<String> nextDir = children.get(j + 1).getDirection();
                ArrayList<String> prevDir = children.get(j - 1).getDirection();
                child.setLOverhang(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1));
                child.setROverhang(nextComp.get(0) + nextDir.get(0));
                
                if (child.getStage() > 0) {
                if (vector != null) {
                    RVector newVector = new RVector(prevComp.get(prevComp.size() - 1) + prevDir.get(prevComp.size() - 1), nextComp.get(0) + nextDir.get(0), child.getStage(), vector.getName(), null);
                    child.setVector(newVector);
                } 
                }
            }
            
            ArrayList<RNode> grandChildren = child.getNeighbors();           
            assignOverhangsHelper(child, grandChildren, root, stageRVectors);
        }
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        return true;
    }
    
    /*
     * Determine GoldenGate Fusion Sites
     */
    public static HashMap<RNode, String[]> getFusionSites(RNode node, RNode root, Collector coll, HashMap<RNode, String[]> fusionSites) {
        
        //Initialize primer parameters
        String[] sites = new String[2];
        String lSeq;
        String rSeq;
        
        boolean missingLeftSequence = false;
        boolean missingSequence = false;
        boolean missingRightSequence = false;
        
        Part rootPart = coll.getPart(root.getUUID(), true);
        ArrayList<Part> composition = rootPart.getComposition();
        Part leftNeighbor;
        Part rightNeighbor;
        String currentSeq;
        
        //Edge case where the node in question is the root node
        if (node == root) {
            
            String seq = "";
            ArrayList<String> type = new ArrayList();
            ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(node.getName(), true);
            if (!allPartsWithName.isEmpty()) {
                seq = allPartsWithName.get(0).getSeq();
                for (int i = 0; i < allPartsWithName.size(); i++) {
                    type = allPartsWithName.get(i).getType();
                    if (!type.contains("plasmid")) {
                        break;
                    }
                }
            }
            Part currentPart = coll.getExactPart(node.getName(), null, node.getComposition(), node.getLOverhang(), node.getROverhang(), type, node.getScars(), node.getDirection(), true);
            currentSeq = currentPart.getSeq();            
            Vector vector = coll.getVector(node.getVector().getUUID(), true);
            rSeq = vector.getSeq();
            lSeq = vector.getSeq();
        
        } else {

            String seq = "";
            ArrayList<String> type = new ArrayList();
            ArrayList<Part> allPartsWithName = coll.getAllPartsWithName(node.getName(), true);
            if (!allPartsWithName.isEmpty()) {
                seq = allPartsWithName.get(0).getSeq();
                if (node.getDirection().size() == 1) {
                    if (node.getDirection().get(0).equals("-") && allPartsWithName.get(0).getDirections().get(0).equals("+")) {
                        seq = PrimerDesign.reverseComplement(seq);
                    } else if (node.getDirection().get(0).equals("+") && allPartsWithName.get(0).getDirections().get(0).equals("-")) {
                        seq = PrimerDesign.reverseComplement(seq);
                    }
                }
                for (int i = 0; i < allPartsWithName.size(); i++) {
                    type = allPartsWithName.get(i).getType();
                    if (!type.contains("plasmid")) {
                        break;
                    }
                }
            }
            
            Part currentPart = coll.getExactPart(node.getName(), seq, node.getComposition(), node.getLOverhang(), node.getROverhang(), type, node.getScars(), node.getDirection(), true);
            currentSeq = currentPart.getSeq();

            if (currentPart.isBasic()) {

                //Get neighbor sequences
                int indexOf = composition.indexOf(currentPart);
                
                //If this part is the left-most library part, the vector is the left neighbor 
                if (indexOf == 0) {
                    Vector vector = coll.getVector(root.getVector().getUUID(), true);
                    rightNeighbor = composition.get(indexOf + 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = vector.getSeq();
                    
                //If this part is the right-most library part, the vector is the right neighbor    
                } else if (indexOf == composition.size() - 1) {
                    Vector vector = coll.getVector(root.getVector().getUUID(), true);
                    leftNeighbor = composition.get(indexOf - 1);
                    rSeq = vector.getSeq();
                    lSeq = leftNeighbor.getSeq();
                
                //Otherwise neighbors are adjacent parts
                } else {
                    rightNeighbor = composition.get(indexOf + 1);
                    leftNeighbor = composition.get(indexOf - 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = leftNeighbor.getSeq();
                }
            } else {

                Part first = currentPart.getComposition().get(0);
                int indexOfFirst = composition.indexOf(first);
                Part last = currentPart.getComposition().get(currentPart.getComposition().size() - 1);
                int indexOfLast = composition.indexOf(last);

                //Get neighbor sequences of beginning of part
                if (indexOfFirst == 0) {
                    leftNeighbor = composition.get(composition.size() - 1);
                    lSeq = leftNeighbor.getSeq();
                } else {
                    leftNeighbor = composition.get(indexOfFirst - 1);
                    lSeq = leftNeighbor.getSeq();
                }

                //Get neighbor sequences of beginning of part
                if (indexOfLast == composition.size() - 1) {
                    rightNeighbor = composition.get(0);
                    rSeq = rightNeighbor.getSeq();
                } else {
                    rightNeighbor = composition.get(indexOfFirst + 1);
                    rSeq = rightNeighbor.getSeq();
                }
            }
        }
        
        //Look to see if there are blank sequences for the right or left part
        String LO = "";
        String RO = "";
        
        if (lSeq.equals("")) {
            missingLeftSequence = true;
        } else {
            LO = LO + lSeq.substring(lSeq.length()-2);
        }

        if (rSeq.equals("")) {
            missingRightSequence = true;
        } else {
            RO = RO + rSeq.substring(0, 2);
        }
        
        if (currentSeq.equals("")) {
            missingSequence = true;
        } else {
            LO = LO + currentSeq.substring(0, 2);
            RO = currentSeq.substring(currentSeq.length()-2) + RO;
        }
        
        if (missingSequence || missingLeftSequence || missingRightSequence) {
            sites[0] = "nnnn";
            sites[1] = "nnnn";
        } else {
            sites[0] = LO;
            sites[1] = RO;
        }
        
        fusionSites.put(node, sites);    
        return fusionSites;
    }
    
    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, String[] fusionSites, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        //Initialize primer parameters
        String[] oligos = new String[2];
        String forwardOligoSequence;
        String reverseOligoSequence;
        
        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();

        String fwdHomology;
        String revHomology;
        String partPrimerPrefix = "at";
        String partPrimerSuffix = "gt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";

        if (seq.length() > minPCRLength) {
            fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, seq, true)));
            revHomology = seq.substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + fusionSites[0].toUpperCase() + fwdHomology;
            reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + fusionSites[1].toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix);

        } else {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + fusionSites[0].toUpperCase() + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(fusionSites[1].toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix) + revHomology;
            } else {
                fwdHomology = seq;
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + fusionSites[0].toUpperCase() + fwdHomology + fusionSites[1].toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix;
                reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);
            }
        }
        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }

    public static String[] generateVectorPrimers(RVector vector, String[] fusionSites) {

        String vectorPrimerPrefix = "actagtg";
        String vectorPrimerSuffix = "tactagt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";

        String[] oligos = new String[2];
        
        //Level 0, 2, 4, 6, etc. vectors
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (vector.getLevel() % 2 == 0) {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite2 + "a" + fusionSites[0].toUpperCase() + "at" + revEnzymeRecSite1 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite1 + "gt" + fusionSites[1].toUpperCase() + "a" + revEnzymeRecSite2 + vectorPrimerSuffix);

        //Level 1, 3, 5, 7, etc. vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite1 + "at" + fusionSites[0].toUpperCase() + "a" + revEnzymeRecSite2 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite2 + "t" + fusionSites[1].toUpperCase() + "at" + revEnzymeRecSite1 + vectorPrimerSuffix);
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }
}
