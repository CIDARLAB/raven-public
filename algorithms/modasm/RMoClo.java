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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.cidarlab.raven.accessibility.ClothoWriter;

/**
 *
 * @author jenhantao
 */
public class RMoClo extends RGeneral {

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<RGraph> mocloClothoWrapper(HashSet<Part> gps, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs, HashMap<String, String> libraryOHs, Collector collector) throws Exception {
        
        _partLibrary = partLibrary;
        _vectorLibrary = vectorLibrary;
        
        //Designate how many parts can be efficiently ligated in one step
        int max = 0;
        Set<Integer> keySet = efficiencies.keySet();
        for (Integer key : keySet) {
            if (key > max) {
                max = key;
            }
        }
        _maxNeighbors = max;

        //Create hashMem parameter for createAsmGraph_sgp() call
        HashMap<String, RGraph> partHash = ClothoReader.partImportClotho(partLibrary, discouraged, recommended); //key: composiion, direction || value: library graph

        //Put all parts into hash for mgp algorithm            
        ArrayList<RNode> gpsNodes = ClothoReader.gpsToNodesClotho(gps);

        //Add single transcriptional units to the required hash
//        HashSet<String> starts = new HashSet<String>();
//        starts.add("promoter");
//        HashSet<String> ends = new HashSet<String>();
//        ends.add("terminator");
//        ArrayList<ArrayList<String>> reqTUs = getSingleTranscriptionalUnits(gpsNodes, starts, ends);
//        for (int i = 0; i < reqTUs.size(); i++) {
//            required.add(reqTUs.get(i).toString());
//        }
        
        //Positional scoring of transcriptional units
//        HashMap<Integer, HashMap<String, Double>> positionScores = new HashMap<Integer, HashMap<String, Double>>();
//        if (modular) {
//            ArrayList<ArrayList<String>> TUs = getTranscriptionalUnits(gpsNodes, 1);
//            positionScores = getPositionalScoring(TUs);
//        }

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        
        //Pull out graphs with one node i.e. either in the library already or require only a PCR
        ArrayList<RGraph> singlePartGraphs = new ArrayList<RGraph>();
        for (RGraph optimalGraph : optimalGraphs) {
            if (optimalGraph.getSteps() == 0) {
                RNode root = optimalGraph.getRootNode();
                String OHs = libraryOHs.get(root.getUUID());
                
                if (OHs != null) {
                    String[] tokens = OHs.split("\\|");
                    if (tokens.length == 2) {
                        boolean allInts = true;
                        for (String token : tokens) {
                            if (!token.matches("[*]?\\d+")) {
                                allInts = false;
                            }
                        }
                        if (allInts) {
                            singlePartGraphs.add(optimalGraph);
                        }
                    }
                } else {
                    singlePartGraphs.add(optimalGraph);
                }
            }
        }
        
        optimalGraphs.removeAll(singlePartGraphs);
        
        //Assign overhangs based upon input
        for (RGraph spGraph : singlePartGraphs) {
            RNode root = spGraph.getRootNode();
            String OHs = libraryOHs.get(root.getUUID());
            String[] tokens = OHs.split("\\|");
            root.setLOverhang(tokens[0]);
            root.setROverhang(tokens[1]);
            RVector newVector = new RVector(tokens[0], tokens[1], 0, stageVectors.get(0).getName(), null);
            root.setVector(newVector);
        }
        
        //Overhang assignment
        if (!optimalGraphs.isEmpty()) {
            propagatePrimaryOverhangs(optimalGraphs);
            maximizeOverhangSharing(optimalGraphs);
//        HashMap<String, String> forcedOverhangHash = assignForcedOverhangs(optimalGraphs);
            HashMap<String, String> forcedOverhangHash = new HashMap<String, String>();
            cartesianLibraryAssignment(optimalGraphs, null, forcedOverhangHash, stageVectors, false);
            assignLinkerFusions(optimalGraphs, collector);
            assignScars(optimalGraphs);            
        }

        optimalGraphs.addAll(singlePartGraphs);
        return optimalGraphs;
    }

    /**
     * Determine overhang scars *
     */
    private void assignScars(ArrayList<RGraph> optimalGraphs) {

        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            root.setScars(assignScarsHelper(root, children));
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
                } else {
                    scars.add(child.getLOverhang());
                }
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
    
    /* 
     * Determine fusion site for linker
     */
    private String getLinkerFusionSite (String linkerSeq, HashSet<String> takenOHs) {
        
        String fS = "";
        HashMap<String, String> moCloOHseqs = PrimerDesign.getMoCloOHseqs();
        ArrayList<String> sortedOHStrings = new ArrayList(moCloOHseqs.keySet());
        ArrayList<Integer> sortedOHs = new ArrayList<>();
        for (String OHString : sortedOHStrings) {
            if (!OHString.contains("*")) {
                int OH = Integer.parseInt(OHString);
                sortedOHs.add(OH);
            }
        }

        Collections.sort(sortedOHs);
        
        //Loop through the overhang list in series to find the first fusion site contained in this linker
        for (int i = 0; i < sortedOHs.size(); i++) {
            
            int OHnum = sortedOHs.get(i);
            String OH = String.valueOf(OHnum);
            
            //If the fusion site is found in the linker sequence, going in order for only forward sites not in the taken set
            if (linkerSeq.toLowerCase().contains(moCloOHseqs.get(OH)) && !takenOHs.contains(OH)) {
                
                //Hacky if statement to correct for choice made with the helical linker
                if (!OH.equals("15")) {
                    fS = OH;
                    return fS;
                }
            }
        }
        
        return fS;
    }
    
    /*
     * Assign linker fusions as post-processing... seems cleaner than getting it in the middle of overhang assignment
     */
    private void assignLinkerFusions(ArrayList<RGraph> optimalGraphs, Collector collector) {
        
        //Loop through each optimal graph and grab the root node to prime for the traversal
        for (RGraph graph : optimalGraphs) {
            RNode root = graph.getRootNode();
            ArrayList<RNode> children = root.getNeighbors();
            assignLinkerFusionsHelper(root, children, collector);
        }
    }
    
    /**
     * Overhang fusion linkers helper *
     */
    private void assignLinkerFusionsHelper(RNode parent, ArrayList<RNode> children, Collector collector) {
        
        int compositionIndex = 0;
        ArrayList<String> linkers = new ArrayList<String>(parent.getLinkers());

        //Loop through children to get takenOHs for the fusion site selection
        HashSet<String> takenOHs = new HashSet<>();
        for (RNode child : children) {
            takenOHs.add(child.getLOverhang());
            takenOHs.add(child.getROverhang());
        }
        
        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);
            int size = child.getComposition().size();
            ArrayList<String> childLinkers = new ArrayList<String>();
            childLinkers.addAll(linkers.subList(compositionIndex, compositionIndex + size - 1));
            child.setLinkers(childLinkers);
            
            //Replace overhangs with linker overhang
            String leftLinker = "_";
            String rightLinker = "_";
            if (i == children.size() - 1) {
                leftLinker = linkers.get(compositionIndex - 1);
            } else if (i == 0) {
                rightLinker = linkers.get(compositionIndex + size - 1);
            } else {
                rightLinker = linkers.get(compositionIndex + size - 1);
                leftLinker = linkers.get(compositionIndex - 1);
            }
            
            if (!leftLinker.equals("_")) {
                String linkerSeq = ClothoWriter.getLinkerSeq(collector, leftLinker);
                String linkerFS = getLinkerFusionSite(linkerSeq, takenOHs);
                child.setLOverhang(linkerFS + "(" + leftLinker + ")");
                child.getVector().setLOverhang(linkerFS);
            }
            if (!rightLinker.equals("_")) {
                String linkerSeq = ClothoWriter.getLinkerSeq(collector, rightLinker);
                String linkerFS = getLinkerFusionSite(linkerSeq, takenOHs);
                child.setROverhang(linkerFS + "(" + rightLinker + ")");
                child.getVector().setROverhang(linkerFS);
            }
            
            compositionIndex = compositionIndex + size;
            
            //Make recursive call
            if (child.getStage() > 0) {

                //Remove the current parent from the list
                ArrayList<RNode> grandChildren = new ArrayList<RNode>();
                grandChildren.addAll(child.getNeighbors());
                if (grandChildren.contains(parent)) {
                    grandChildren.remove(parent);
                }
                
                assignLinkerFusionsHelper(child, grandChildren, collector);
            }
        }   
    }
    
    public static boolean validateOverhangs(ArrayList<RGraph> graphs) {
        boolean toReturn = true;
        for (RGraph graph : graphs) {
            RNode root = graph.getRootNode();
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList();
            queue.add(root);
            while (!queue.isEmpty()) {
                RNode parent = queue.get(0);
                queue.remove(0);
                seenNodes.add(parent);
                if (parent.getLOverhang().equals(parent.getROverhang())) {
                    System.out.println(parent.getComposition() + " has the same left overhang as it's right overhang");
                    toReturn = false;
                }
                if (parent.getNeighbors().size() > 1) {
                    RNode previous = null;
                    HashMap<String, Integer> leftFrequencyHash = new HashMap();
                    HashMap<String, Integer> rightFrequencyHash = new HashMap();
                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        RNode child = parent.getNeighbors().get(i);
                        if (!seenNodes.contains(child)) {
                            if (leftFrequencyHash.get(child.getLOverhang()) != null) {
                                leftFrequencyHash.put(child.getLOverhang(), leftFrequencyHash.get(child.getLOverhang()) + 1);
                            } else {
                                leftFrequencyHash.put(child.getLOverhang(), 1);
                            }
                            if (rightFrequencyHash.get(child.getROverhang()) != null) {
                                rightFrequencyHash.put(child.getROverhang(), rightFrequencyHash.get(child.getROverhang()) + 1);
                            } else {
                                rightFrequencyHash.put(child.getROverhang(), 1);
                            }
                            if (i == 0) {
                                if (!child.getLOverhang().equals(parent.getLOverhang())) {
                                    System.out.println(child.getComposition() + ", which is the 1st part, doesnt have the same left overhang as its parent");
                                    toReturn = false;
                                }
                            }
                            if (i == parent.getNeighbors().size() - 1) {
                                if (!child.getROverhang().equals(parent.getROverhang())) {
                                    System.out.println(child.getComposition() + ", which is the last part, doesnt have the same right overhang as its parent");

                                    toReturn = false;
                                }
                            }
                            if (previous != null) {
                                if (!child.getLOverhang().equals(previous.getROverhang())) {
                                    System.out.println(child.getComposition() + " has a left overhang that doesn't match the right overhang of its neighbor");
                                    toReturn = false;
                                }
                            }

                            previous = child;
                            queue.add(child);
                        }
                    }
                    if (leftFrequencyHash.containsValue(2) || rightFrequencyHash.containsValue(2)) {
                        System.out.println("in " + parent.getComposition() + ", an overhang is used twice for the left overhang or twice for the right overhang\n");
                        System.out.println("leftFrequencyHash: " + leftFrequencyHash);
                        System.out.println("rightFrequencyHash: " + rightFrequencyHash);

                        toReturn = false;
                    }
                }
            }
        }
        return toReturn;
    }
    
    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getMoCloOHseqs();
        String[] oligos = new String[2];
        String partPrimerPrefix = "at";
        String partPrimerSuffix = "gt";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";

        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();

        String leftLink = "";
        String rightLink = "";
        
        String lOverhang = node.getLOverhang();
        if (lOverhang.contains("(")) {
            String linkerSeq = ClothoWriter.getLinkerSeq(coll, lOverhang.substring(lOverhang.indexOf("(") + 1, lOverhang.length() - 1));
            lOverhang = lOverhang.substring(0, lOverhang.indexOf("("));
            leftLink = linkerSeq.substring(linkerSeq.indexOf(overhangVariableSequenceHash.get(lOverhang).toUpperCase()) + 4, linkerSeq.length());
        }
        
        String rOverhang = node.getROverhang();
        if (rOverhang.contains("(")) {
            String linkerSeq = ClothoWriter.getLinkerSeq(coll, rOverhang.substring(rOverhang.indexOf("(") + 1, rOverhang.length() - 1));            
            rOverhang = rOverhang.substring(0, rOverhang.indexOf("("));
            rightLink = linkerSeq.substring(0, linkerSeq.indexOf(overhangVariableSequenceHash.get(rOverhang).toUpperCase()));
        }
        
        String fwdHomology;
        String revHomology;

        String forwardOligoSequence;
        String reverseOligoSequence;
        if (seq.length() > minPCRLength) {
            fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, seq, true)));
            revHomology = seq.substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(lOverhang).toUpperCase() + leftLink + fwdHomology;
            reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + rightLink + overhangVariableSequenceHash.get(rOverhang).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix);
        
        } else {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(lOverhang).toUpperCase() + leftLink + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(rightLink + overhangVariableSequenceHash.get(rOverhang).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix) + revHomology;
            } else {
                fwdHomology = seq;
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(lOverhang).toUpperCase() + leftLink + fwdHomology + rightLink + overhangVariableSequenceHash.get(rOverhang).toUpperCase() + "ag" + revEnzymeRecSite1 + partPrimerSuffix;
                reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);

            }
        }
        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }

    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generateVectorPrimers(RVector vector) {

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getMoCloOHseqs();
        String vectorPrimerPrefix = "gttctttactagtg";
        String vectorPrimerSuffix = "tactagtagcggccgc";
        String fwdEnzymeRecSite1 = "gaagac";
        String revEnzymeRecSite1 = "gtcttc";
        String fwdEnzymeRecSite2 = "ggtctc";
        String revEnzymeRecSite2 = "gagacc";

        String[] oligos = new String[2];

        String lOverhang = vector.getLOverhang();
        if (lOverhang.contains("(")) {
            lOverhang = lOverhang.substring(0, lOverhang.indexOf("("));
        }
        
        String rOverhang = vector.getROverhang();
        if (rOverhang.contains("(")) {
            rOverhang = rOverhang.substring(0, rOverhang.indexOf("("));
        }
        
        //Level 0, 2, 4, 6, etc. vectors
        String forwardOligoSequence;
        String reverseOligoSequence;
        if (vector.getLevel() % 2 == 0) {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite2 + "a" + overhangVariableSequenceHash.get(lOverhang).toUpperCase() + "at" + revEnzymeRecSite1 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite1 + "gt" + overhangVariableSequenceHash.get(rOverhang).toUpperCase() + "a" + revEnzymeRecSite2 + vectorPrimerSuffix);

            //Level 1, 3, 5, 7, etc. vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + fwdEnzymeRecSite1 + "at" + overhangVariableSequenceHash.get(lOverhang).toUpperCase() + "a" + revEnzymeRecSite2 + "tgcaccatatgcggtgtgaaatac";
            reverseOligoSequence = PrimerDesign.reverseComplement("ttaatgaatcggccaacgcgcggg" + fwdEnzymeRecSite2 + "t" + overhangVariableSequenceHash.get(rOverhang).toUpperCase() + "at" + revEnzymeRecSite1 + vectorPrimerSuffix);
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }
}
