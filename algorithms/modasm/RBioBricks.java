/*
 * This class contains the SDS++ algorithm
 * 
 */
package Controller.algorithms.modasm;

import Controller.accessibility.ClothoReader;
import Controller.algorithms.PrimerDesign;
import Controller.algorithms.RGeneral;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import Controller.datastructures.*;

/**
 *
 * @author evanappleton
 */
public class RBioBricks extends RGeneral {

    /**
     * Clotho part wrapper for BioBricks 3A
     */
    public ArrayList<RGraph> bioBricksClothoWrapper(HashSet<Part> goalPartsVectors, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs) throws Exception {

        //Try-Catch block around wrapper method
        _maxNeighbors = 2;

        //Initialize part hash and vector set
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(partLibrary, discouraged, recommended);

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(goalPartsVectors);

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, null, true);
        assignBioBricksOverhangs(optimalGraphs, stageVectors);
        assignScars(optimalGraphs);

        return optimalGraphs;
    }

    /**
     * First step of overhang assignment - enforce numeric place holders for
     * overhangs, ie no overhang redundancy in any step *
     */
    private void assignBioBricksOverhangs(ArrayList<RGraph> optimalGraphs, HashMap<Integer, Vector> stageVectors) {

        //Initialize fields that record information to save complexity for future steps
        _rootBasicNodeHash = new HashMap<RNode, ArrayList<RNode>>();
        HashMap<Integer, RVector> stageRVectors = new HashMap<Integer, RVector>();
        for (Integer stage : stageVectors.keySet()) {
            RVector vec = ClothoReader.vectorImportClotho(stageVectors.get(stage));
            stageRVectors.put(stage, vec);
        }
        
        //If the stageVector hash is empty, make a new default vector
        if (stageRVectors.size() == 1) {
            if (stageRVectors.get(1) == null) {
                stageRVectors.put(0, new RVector("EX", "SP", -1, "pSK1A2", null));
            }
        }
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            RVector vector = stageRVectors.get(root.getStage() % stageRVectors.size());
            RVector rootVector = new RVector("EX", "SP", -1, vector.getName(), null);           
            root.setVector(rootVector);
            root.setLOverhang("EX");
            root.setROverhang("SP");
            ArrayList<RNode> l0nodes = new ArrayList<RNode>();
            _rootBasicNodeHash.put(root, l0nodes);
            ArrayList<RNode> neighbors = root.getNeighbors();
            assignBioBricksOverhangsHelper(root, neighbors, root, stageRVectors);
        }

        //Determine which nodes impact which level to form the stageDirectionAssignHash
        for (RGraph graph : optimalGraphs) {
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

    /**
     * This helper method executes the loops necessary to enforce overhangs for
     * each graph in enforceOverhangRules *
     */
    private void assignBioBricksOverhangsHelper(RNode parent, ArrayList<RNode> children, RNode root, HashMap<Integer, RVector> stageRVectors) {

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            //Give biobricks overhangs
            RVector vector = stageRVectors.get(child.getStage() % stageRVectors.size());
            RVector newVector = new RVector("EX", "SP", -1, vector.getName(), null);
            child.setVector(newVector);
            child.setLOverhang("EX");
            child.setROverhang("SP");

            //Make recursive call
            if (child.getStage() > 0) {
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());

                //Remove the current parent from the list
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                assignBioBricksOverhangsHelper(child, grandChildren, root, stageRVectors);

                //Or record the level zero parts
            } else {
                ArrayList<RNode> l0nodes = _rootBasicNodeHash.get(root);
                l0nodes.add(child);
                _rootBasicNodeHash.put(root, l0nodes);
            }
        }
    }

    /**
     * Determine overhang scars *
     */
    private void assignScars(ArrayList<RGraph> optimalGraphs) {

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {

            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            assignScarsHelper(root, children);
        }
    }

    /**
     * Overhang scars helper *
     */
    private ArrayList<String> assignScarsHelper(RNode parent, ArrayList<RNode> children) {

        ArrayList<String> scars = new ArrayList<String>();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                }
                scars.add("BB");
            }

            //Make recursive call
            if (child.getStage() > 0) {

                //Remove the current parent from the list
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }

                ArrayList<String> childScars = assignScarsHelper(child, grandChildren);
                scars.addAll(childScars);
            } else {

                ArrayList<String> childScars = new ArrayList<String>();
                if (child.getComposition().size() > 1) {
                    if (!child.getScars().isEmpty()) {
                        childScars.addAll(child.getScars());
                    } else {

                        for (int j = 0; j < child.getComposition().size() - 1; j++) {
                            childScars.add("_");
                        }
                        child.setScars(childScars);
                    }
                }
                scars.addAll(childScars);
            }
        }

        //Keep scars for re-used parts with scars
        if (!scars.isEmpty()) {
            parent.setScars(scars);
            return scars;
        } else {
            return parent.getScars();
        } 
    }

    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {

        boolean valid = true;

        for (RGraph graph : graphs) {
            ArrayList<RNode> queue = new ArrayList<RNode>();
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            RNode root = graph.getRootNode();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                queue.remove(0);
                seenNodes.add(current);

                if (!("EX".equals(current.getLOverhang()) && "SP".equals(current.getROverhang()))) {
                    return false;
                }

                ArrayList<RNode> neighbors = current.getNeighbors();
                for (RNode neighbor : neighbors) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return valid;
    }

    /**
     * Generation of new BioBricks primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        //initialize primer parameters
        String[] oligos = new String[2];
        String partPrimerPrefix = "gaattcgcggccgcttctagag";
        String partPrimerSuffix = "tactagtagcggccgctgcag";
        String partPrimerPrefixAlt = "gaattcgcggccgcttctag";
        String forwardOligoSequence;
        String reverseOligoSequence;

        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();
        ArrayList<String> type = node.getType();
        String fwdHomology;
        String revHomology;
        
        //If the part is sufficiently large
        if (seq.length() > minPCRLength) {
            
            //Special case primers for coding sequences
            if (type.get(0).equals("gene") || type.get(0).equals("reporter")) {
                fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 20, minPCRLength, seq, true)));
                revHomology = seq.substring(Math.max(0, currentPart.getSeq().length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 21, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
                forwardOligoSequence = partPrimerPrefixAlt + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + partPrimerSuffix);

            } else {
                if (seq.equals("")) {
                    fwdHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                    revHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                } else {
                    fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 22, minPCRLength, seq, true)));
                    revHomology = seq.substring(Math.max(0, currentPart.getSeq().length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 21, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
                }
                forwardOligoSequence = partPrimerPrefix + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + partPrimerSuffix);

            }
        
        //Otherwise make annealing primers or synthesize
        } else {
            if (type.get(0).equals("gene") || type.get(0).equals("reporter")) {

                if (seq.equals("")) {
                    fwdHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                    revHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                    forwardOligoSequence = partPrimerPrefixAlt + fwdHomology;
                    reverseOligoSequence = PrimerDesign.reverseComplement(partPrimerSuffix) + revHomology;
                } else {
                    fwdHomology = seq;
                    forwardOligoSequence = partPrimerPrefixAlt + fwdHomology + partPrimerSuffix;
                    reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);
                }
            
            } else {
                
                if (seq.equals("")) {
                    fwdHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                    revHomology = "[ PART " + currentPart.getName() + " HOMOLOGY REGION ]";
                    forwardOligoSequence = partPrimerPrefix + fwdHomology;
                    reverseOligoSequence = PrimerDesign.reverseComplement(partPrimerSuffix) + revHomology;
                } else {
                    fwdHomology = seq;
                    forwardOligoSequence = partPrimerPrefix + fwdHomology + partPrimerSuffix;
                    reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);
                }
            }
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;

        return oligos;
    }

    /**
     * Generation of new BioBricks primers for parts *
     */
    public static String[] generateVectorPrimers(RVector vector, Collector coll, Double meltingTemp, Integer targetLength, Integer maxPrimerLength, Integer minPCRLength) {

        //initialize primer parameters
        String[] oligos = new String[2];
        String vectorPrimerPrefix = "gaattcgcggccgcttctagag";
        String vectorPrimerSuffix = "tactagtagcggccgctgcag";

        Vector currentVector = coll.getVector(vector.getUUID(), true);
        String seq = currentVector.getSeq();
        String fwdHomology;
        String revHomology;
        if (seq.equals("")) {
            fwdHomology = "[ VECTOR " + currentVector.getName() + " HOMOLOGY REGION ]";
            revHomology = "[ VECTOR " + currentVector.getName() + " HOMOLOGY REGION ]";
        } else {
            fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 23, minPCRLength, seq, true)));
            revHomology = PrimerDesign.reverseComplement(currentVector.getSeq().substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 22, minPCRLength, PrimerDesign.reverseComplement(seq), true))));
        }

        String forwardOligoSequence = vectorPrimerPrefix + fwdHomology;
        String reverseOligoSequence = PrimerDesign.reverseComplement(vectorPrimerSuffix)+revHomology;

        oligos[0]=forwardOligoSequence;
        oligos[1] =reverseOligoSequence;

        return oligos;
    }
    private HashMap<RNode, ArrayList<RNode>> _rootBasicNodeHash; //key: root node, value: ordered arrayList of level0 nodes in graph that root node belongs to
}