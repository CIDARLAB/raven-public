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
 * @author jenhantao
 */
public class RGatewayGibson extends RGeneral {

    /**
     * Clotho part wrapper for sequence dependent one pot reactions *
     */
    public ArrayList<RGraph> gatewayGibsonWrapper(HashSet<Part> gps, ArrayList<Vector> vectorLibrary, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, ArrayList<Part> partLibrary, boolean modular, HashMap<Integer, Double> efficiencies, HashMap<Integer, Vector> stageVectors, ArrayList<Double> costs, HashMap<String, String> libraryOHs, Collector collector) throws Exception {
        
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
        HashSet<String> starts = new HashSet<String>();
        starts.add("promoter");
        HashSet<String> ends = new HashSet<String>();
        ends.add("gene");
        ends.add("reporter");
        ArrayList<ArrayList<String>> reqTUs = getSingleTranscriptionalUnits(gpsNodes, starts, ends);
        for (int i = 0; i < reqTUs.size(); i++) {
            required.add(reqTUs.get(i).toString());
        }

        //Run hierarchical Raven Algorithm
        ArrayList<RGraph> optimalGraphs = createAsmGraph_mgp(gpsNodes, partHash, required, recommended, forbidden, discouraged, efficiencies, true);
        
//        //Pull out graphs with one node i.e. either in the library already or require only a PCR
//        ArrayList<RGraph> singlePartGraphs = new ArrayList<RGraph>();
//        for (RGraph optimalGraph : optimalGraphs) {
//            if (optimalGraph.getStages() == 0) {
//                RNode root = optimalGraph.getRootNode();
//                String OHs = libraryOHs.get(root.getUUID());
//                String[] tokens = OHs.split("\\|");
//                if (tokens.length == 2) {
//                    boolean allInts = true;
//                    for (String token : tokens) {
//                        if (!token.matches("[*]?\\d+")) {
//                            allInts = false;
//                        }
//                    }
//                    if (allInts) {
//                        singlePartGraphs.add(optimalGraph);
//                    }
//                }
//            }
//        }
//        
//        optimalGraphs.removeAll(singlePartGraphs);
//        
//        //Assign overhangs based upon input
//        for (RGraph spGraph : singlePartGraphs) {
//            RNode root = spGraph.getRootNode();
//            String OHs = libraryOHs.get(root.getUUID());
//            String[] tokens = OHs.split("\\|");
//            root.setLOverhang(tokens[0]);
//            root.setROverhang(tokens[1]);
//            RVector newVector = new RVector(tokens[0], tokens[1], 0, stageVectors.get(0).getName(), null);
//            root.setVector(newVector);
//        }
        
                
        //Pre-processing to adjust stages for Gateway steps
        ArrayList<RGraph> singlePartGraphs = new ArrayList<RGraph>();
        for (RGraph optimalGraph : optimalGraphs) {
            if (singleBasicPartInGraph(optimalGraph)) {
                stageAdjuster(optimalGraph, -1);
                
                //Special case where there is only on Gibson step
                if (optimalGraph.getStages() == 0) {
                    RNode root = optimalGraph.getRootNode();
                    root.setLOverhang("UNS1");
                    root.setROverhang("UNS2");
                    RVector newVector = new RVector("UNS1", "UNS2", 0, stageVectors.get(0).getName(), null);
                    root.setVector(newVector);
                    singlePartGraphs.add(optimalGraph);
                }
            }
        }
        
        optimalGraphs.removeAll(singlePartGraphs);
        
        //Overhang assignment for Gibson
        if (!optimalGraphs.isEmpty()) {
            propagatePrimaryOverhangs(optimalGraphs);
            HashMap<String, String> forcedGibsonOHs = getForcedGibsonOHs(optimalGraphs);
            
//            maximizeOverhangSharing(optimalGraphs);
            HashMap<String, String> forcedOverhangHash = new HashMap<String, String>();
            cartesianLibraryAssignment(optimalGraphs, forcedGibsonOHs, forcedOverhangHash, stageVectors, true);
        }
        
        optimalGraphs.addAll(singlePartGraphs);

        //After Gibson overhangs assigned, correct stages, assign overhangs for gateway
        
        for (RGraph optimalGraph : optimalGraphs) {
            gatewayOverhangs(optimalGraph.getRootNode(), optimalGraph.getRootNode().getNeighbors(), stageVectors);
            if (singleBasicPartInGraph(optimalGraph)) {
                stageAdjuster(optimalGraph, 1);
            }
            assignScars(optimalGraph.getRootNode(), optimalGraph.getRootNode().getNeighbors());
            addAdaptor(optimalGraph, collector, stageVectors);
        }
        
        return optimalGraphs;
    }

    /**
     * Overhang scars method *
     */
    private ArrayList<String> assignScars(RNode parent, ArrayList<RNode> children) {

        ArrayList<String> scars = new ArrayList<String>();

        //Loop through each one of the children to assign rule-instructed overhangs... enumerated numbers currently
        for (int i = 0; i < children.size(); i++) {

            RNode child = children.get(i);

            if (i > 0) {
                if (child.getLOverhang().isEmpty()) {
                    scars.add("_");
                } else if (child.getLOverhang().startsWith("attL") || child.getLOverhang().startsWith("attR")) {
                    scars.add("attB" + child.getLOverhang().substring(4));
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

                ArrayList<String> childScars = assignScars(child, grandChildren);
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
    
    //Add adapter for Gibson steps
    private void addAdaptor (RGraph optimalGraph, Collector collector, HashMap<Integer, Vector> stageVectors) {
        
        //Adapt root node overhangs and root node vector overhangs
        RNode rootNodeClone = optimalGraph.getRootNode().clone(true);
        String rootNodeOldROverhang = rootNodeClone.getROverhang();
        
        RNode adaptor = new RNode();
        adaptor.setLOverhang(rootNodeClone.getROverhang());
        adaptor.setROverhang("UNSX");
        
        ArrayList<String> direction = new ArrayList<String>();
        direction.add("+");
        direction.add("+");
        adaptor.setDirection(direction);
        ArrayList<String> adaptorComp = new ArrayList<String>();
        adaptorComp.add("insulator");
        adaptorComp.add("kanR");
        adaptor.setComposition(adaptorComp);
        ArrayList<String> adaptorType = new ArrayList<String>();
        adaptorType.add("spacer");
        adaptorType.add("resistance");
        adaptor.setType(adaptorType);
        ArrayList<String> adaptorScars = new ArrayList<String>();
        adaptorScars.add("UNS2");
        adaptor.setScars(adaptorScars);
        
        //Make KanR part if it does not exist yet
        String adaptorSeq = _insulator + PrimerDesign.getGatewayGibsonOHseqs().get("UNS2") + _kanR;
        
        RVector newVector = new RVector(adaptor.getLOverhang(), "UNSX", 0, stageVectors.get(0).getName(), null);
        adaptor.setVector(newVector);
        
        Part exactPart = collector.getExactPart("adaptor" + "_" + adaptor.getLOverhang() + "_" + adaptor.getROverhang(), adaptorSeq, adaptorComp, adaptor.getLOverhang(), "UNSX", adaptor.getType(), adaptor.getScars(), adaptor.getDirection(), true);
        
        ArrayList<String> spacer = new ArrayList();
        spacer.add("spacer");
        Part newSpacer = Part.generateBasic("insulator", _insulator, null, spacer, new ArrayList(), adaptor.getLOverhang(), "UNS2");
        newSpacer.getDirections().add("+");
        newSpacer = newSpacer.saveDefault(collector);

        if (exactPart == null) {
            
            ArrayList<String> kanRTags = new ArrayList<String>();
            kanRTags.add("LO: UNS2");
            kanRTags.add("RO: UNSX");
            kanRTags.add("Type: resistance");
            kanRTags.add("Direction: [+]");
            kanRTags.add("Scars: []");
            ArrayList kanRComp = new ArrayList<String>();
            kanRComp.add("kanR");
            
            ArrayList<String> dirs = new ArrayList();
            dirs.add("+");
            ArrayList<String> types = new ArrayList();
            types.add("resistance");
            
            Part exactKanR = collector.getExactPart("kanR", _kanR, kanRComp, "UNS2", "UNSX", types, new ArrayList(), dirs, true);
            
            if (exactKanR == null) {
                Part newKanR = Part.generateBasic("kanR", _kanR, null, types, new ArrayList(), "UNS2", "UNSX");
                newKanR.getDirections().add("+");
                newKanR = newKanR.saveDefault(collector);
                exactKanR = newKanR;
            }
            
            ArrayList<String> scarSeqs = new ArrayList<String>();
            scarSeqs.add(PrimerDesign.getGatewayGibsonOHseqs().get("UNS2"));
            ArrayList<Part> composition = new ArrayList<Part>();
            composition.add(newSpacer);
            composition.add(exactKanR);
            
            Part newBasicPart = Part.generateComposite("adaptor" + "_" + adaptor.getLOverhang() + "_" + adaptor.getROverhang(), composition, scarSeqs, adaptor.getScars(), null, adaptor.getDirection(), adaptor.getLOverhang(), "UNSX", adaptor.getType());
            newBasicPart = newBasicPart.saveDefault(collector);
            
            adaptor.setUUID(newBasicPart.getUUID());
            
        } else {
            adaptor.setUUID(exactPart.getUUID());
        }
        
        //Fix root node
        ArrayList<String> comp = new ArrayList<String>();
        comp.addAll(rootNodeClone.getComposition());
        comp.add("insulator");
        rootNodeClone.setComposition(comp);
        
        ArrayList<String> dir = new ArrayList<String>();
        dir.addAll(rootNodeClone.getDirection());
        dir.add("+");
        rootNodeClone.setDirection(dir);
        
        ArrayList<String> type = new ArrayList<String>();
        type.addAll(rootNodeClone.getType());
        type.add("spacer");
        rootNodeClone.setType(type);
        
        ArrayList<String> scr = new ArrayList<String>();
        scr.addAll(rootNodeClone.getScars());
        scr.add(rootNodeOldROverhang);
        rootNodeClone.setScars(scr);
        
        rootNodeClone.getVector().setROverhang("UNSX");
        rootNodeClone.setROverhang("UNS2");
        
        //Fix target part
        Part targetPart = collector.getPart(rootNodeClone.getUUID(), true);
        ArrayList<Part> newComposition = new ArrayList<Part>();
        newComposition.addAll(targetPart.getComposition());
        newComposition.add(newSpacer);
                
        ArrayList<String> newTargetPartDirections = new ArrayList<String>();
        newTargetPartDirections.addAll(targetPart.getDirections());
        newTargetPartDirections.add("+");
        
        ArrayList<String> newTargetPartScars = new ArrayList<String>();
        newTargetPartScars.addAll(targetPart.getScars());
        newTargetPartScars.add(rootNodeOldROverhang);
        
        ArrayList<String> scarSeqs = new ArrayList<String>();
        for (int i = 0; i < newTargetPartScars.size(); i++) {
            scarSeqs.add(PrimerDesign.getGatewayGibsonOHseqs().get(newTargetPartScars.get(i)));
        }
        
        ArrayList<String> plasmid = new ArrayList();
        plasmid.add("plasmid");
        Part newTargetPart = Part.generateComposite(targetPart.getName() + "_GatewayGibson", newComposition, scarSeqs, newTargetPartScars, null, newTargetPartDirections, "", "", plasmid);
        newTargetPart.saveDefault(collector);
        rootNodeClone.setUUID(newTargetPart.getUUID());
        
        rootNodeClone.addNeighbor(adaptor);
        adaptor.addNeighbor(rootNodeClone);
        optimalGraph.setRootNode(rootNodeClone);
        
    }
    
    /* 
     * Gateway overhang assignment helper 
     */
    private void gatewayOverhangs (RNode parent, ArrayList<RNode> children, HashMap<Integer, Vector> stageVectors) {
        
        //If this is a Gateway parent, assign Gateway overhangs
        if (parent.getStage() == 0) {
            
            //Convert vectors to RVectors
            HashMap<Integer, RVector> stageRVectors = new HashMap<Integer, RVector>();
            for (Integer stage : stageVectors.keySet()) {
                RVector vec = ClothoReader.vectorImportClotho(stageVectors.get(stage));
                stageRVectors.put(stage, vec);
            }
            RVector levelVector = stageRVectors.get((parent.getStage()) % stageRVectors.size());          
            
            //Assign gateway overhangs and vectors to Gateway parts
            int count = 5;
            for (int i = 0; i < children.size(); i++) {
                
                if (i == 0) {
                    children.get(i).setLOverhang("attL4");
                    children.get(i).setROverhang("attR1");
                    ArrayList<String> scars = new ArrayList<String>();
                    scars.add("attP1");
                    parent.setScars(scars);
                    RVector newLVector = new RVector("attL4", "attR1", -1, levelVector.getName(), null);
                    children.get(i).setVector(newLVector);
                } else if (i == (children.size() - 1)) {         
                    children.get(i).setLOverhang("attL" + children.get(i-1).getROverhang().substring(4));
                    children.get(i).setROverhang("attL2");
                    RVector newRVector = new RVector("attL1", "attL2", -1, levelVector.getName(), null);
                    children.get(i).setVector(newRVector);
                } else {
                    children.get(i).setLOverhang("attL" + children.get(i-1).getROverhang().substring(4));
                    children.get(i).setROverhang("attR" + count);
                    ArrayList<String> scars = parent.getScars();
                    scars.add("attP" + count);
                    count++;
                    RVector newRVector = new RVector("attL1", "attL2", -1, levelVector.getName(), null);
                    children.get(i).setVector(newRVector);
                    
                }
            }
        
        //Otherwise make recursive traversal call
        } else {
            for (int i = 0; i < children.size(); i++) {
                RNode child = children.get(i);
                ArrayList<RNode> grandchildren = new ArrayList<RNode>();
                
                for (int j = 0; j < child.getNeighbors().size(); j++) {
                    if (child.getNeighbors().get(j).getStage() < child.getStage()) {
                        grandchildren.add(child.getNeighbors().get(j));
                    }
                }
                
                //Recursive call if there are grandchildren
                if (!grandchildren.isEmpty()) {
                    gatewayOverhangs(child, grandchildren, stageVectors);
                }
            }
        }
    }
    
    //Overhang validation
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
                if (parent.getNeighbors().size() > 1) {
                    for (int i = 0; i < parent.getNeighbors().size(); i++) {
                        RNode child = parent.getNeighbors().get(i);
                        if (!seenNodes.contains(child)) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
        return toReturn;
    }
    
    /* 
     * Forcing overhangs for simpler Gibson 
     */
    private HashMap<String,String> getForcedGibsonOHs (ArrayList<RGraph> optimalGraphs) {
        
        HashMap<String,String> predeterminedAssignment = new HashMap<String,String>();
        for (RGraph optimalGraph : optimalGraphs) {
            ArrayList<RNode> gibsonBPs = _rootBasicNodeHash.get(optimalGraph.getRootNode());
            
            int count = 0;
            for (int i = 0; i < gibsonBPs.size(); i++) {
                predeterminedAssignment.put(gibsonBPs.get(i).getLOverhang(), Integer.toString(count));
                count++;
                predeterminedAssignment.put(gibsonBPs.get(i).getROverhang(), Integer.toString(count));
            }
        }
        
        return predeterminedAssignment;
    }
    
    /**
     * Generation of new MoClo primers for parts *
     */
    public static String[] generatePartPrimers(RNode node, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        String[] oligos = new String[2];
        String partPrimerPrefix = "atc";
        String partPrimerSuffix = "atc";
        String fwdEnzymeRecSite1 = "ggtctc";
        String revEnzymeRecSite1 = "gagacc";
        String Q1 = "GCTT";
        String QX = "AGGT";

        Part currentPart = coll.getPart(node.getUUID(), true);
        String seq = currentPart.getSeq();

        String fwdHomology;
        String revHomology;

        String forwardOligoSequence;
        String reverseOligoSequence;
        if (seq.length() > minPCRLength) {
            fwdHomology = seq.substring(0, Math.min(seq.length(), PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, seq, true)));
            revHomology = seq.substring(Math.max(0, seq.length() - PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength - 14, minPCRLength, PrimerDesign.reverseComplement(seq), true)));
            forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "t" + Q1 + fwdHomology;
            reverseOligoSequence = PrimerDesign.reverseComplement(revHomology + QX + "a" + revEnzymeRecSite1 + partPrimerSuffix);
        
        } else {
            if (seq.equals("")) {
                fwdHomology = "[ PART " + currentPart.getName() + " FORWARD HOMOLOGY REGION ]";
                revHomology = "[ PART " + currentPart.getName() + " REVERSE HOMOLOGY REGION ]";
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "t" + Q1 + fwdHomology;
                reverseOligoSequence = PrimerDesign.reverseComplement(QX + "a" + revEnzymeRecSite1 + partPrimerSuffix) + revHomology;
            } else {
                fwdHomology = seq;
                forwardOligoSequence = partPrimerPrefix + fwdEnzymeRecSite1 + "t" + Q1 + fwdHomology + QX + "a" + revEnzymeRecSite1 + partPrimerSuffix;
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

        HashMap<String, String> overhangVariableSequenceHash = PrimerDesign.getGatewayGibsonOHseqs();
        String vectorPrimerPrefix = "act";
        String vectorPrimerSuffix = "act";
        String ISceI = "tagggataacagggtaat";
        String BsaIfwd = "ggtctc";
        String BsaIrev = "gagacc";
        String Q1 = "GCTT";
        String QX = "AGGT";

        String[] oligos = new String[2];

        String forwardOligoSequence;
        String reverseOligoSequence;
        
        //Gateway destination vectors
        if (vector.getLevel() == 0) {
            forwardOligoSequence = vectorPrimerPrefix + BsaIfwd + "a" + Q1 + "tgagaccacctctgacacatgcag";
            reverseOligoSequence = PrimerDesign.reverseComplement("gaggcggtttgcgtattggtctca" + QX + "t" + BsaIrev + vectorPrimerSuffix);

        //Gibson destination vectors
        } else {
            forwardOligoSequence = vectorPrimerPrefix + BsaIfwd + "a" + Q1 + ISceI + overhangVariableSequenceHash.get(vector.getLOverhang()).toUpperCase();
            reverseOligoSequence = PrimerDesign.reverseComplement(overhangVariableSequenceHash.get(vector.getROverhang()).toUpperCase() + ISceI + QX + "t" + BsaIrev + vectorPrimerSuffix);
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        return oligos;
    }
    
    String _kanR = "tatgagccatattcaacgggaaacgtcgaggccgcgattaaattccaacatggatgctgatttatatgggtataaatgggctcgcgataatgtcgggcaatcaggtgcgacaatctatcgcttgtatgggaagcccgatgcgccagagttgtttctgaaacatggcaaaggtagcgttgccaatgatgttacagatgagatggtcagactaaactggctgacggaatttatgcctcttccgaccatcaagcattttatccgtactcctgatgatgcatggttactcaccactgcgatccccggaaaaacagcattccaggtattagaagaatatcctgattcaggtgaaaatattgttgatgcgctggcagtgttcctgcgccggttgcattcgattcctgtttgtaattgtccttttaacagcgatcgcgtatttcgtctcgctcaggcgcaatcacgaatgaataacggtttggttgatgcgagtgattttgatgacgagcgtaatggctggcctgttgaacaagtctggaaagaaatgcataaacttttgccattctcaccggattcagtcgtcactcatggtgatttctcacttgataaccttatttttgacgaggggaaattaataggttgtattgatgttggacgagtcggaatcgcagaccgataccaggatcttgccatcctatggaactgcctcggtgagttttctccttcattacagaaacggctttttcaaaaatatggtattgataatcctgatatgaataaattgcagtttcatttgatgctcgatgagtttttctaatcagaattggttaattggttgtaacactggcagagcattacgctgacttgacgggacggcgcaagctcatgaccaaaatcccttaacgtgagttacgcgtcgttccactgagcgtcaga";
    String _insulator = "tctggcccgtgtctcaaaatctctgatgttacattgcacaagataaaataatatcatcatgaacaataaaactgtctgcttacataaacagtaatacaaggggtgt";
}
