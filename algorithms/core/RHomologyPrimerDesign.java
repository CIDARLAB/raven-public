/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.core;

import org.cidarlab.raven.datastructures.Collector;
import org.cidarlab.raven.datastructures.Part;
import org.cidarlab.raven.datastructures.RNode;
import org.cidarlab.raven.datastructures.RVector;
import org.cidarlab.raven.datastructures.Vector;
import java.util.ArrayList;

/**
 *
 * @author evanappleton
 */
public class RHomologyPrimerDesign {

    public static String[] homolRecombPartPrimers(RNode node, RNode root, Collector coll, Double meltingTemp, Integer targetLength, Integer minPCRLength, Integer maxPrimerLength) {

        //Initialize primer parameters
        String[] oligos = new String[2];
        String forwardOligoSequence;
        String reverseOligoSequence;
        String lSeq;
        String rSeq;
        
        boolean missingLeftSequence = false;
        boolean missingSequence = false;
        boolean missingRightSequence = false;
        
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
                if (!type.contains("plasmid") && !type.contains("multitype")){
                    break;
                }
            }
            
        }
        
        Part currentPart = coll.getExactPart(node.getName(), seq, node.getComposition(), node.getLOverhang(), node.getROverhang(), type, node.getScars(), node.getDirection(), true);

        Part leftNeighbor;
        Part rightNeighbor;
        Part rootPart = coll.getPart(root.getUUID(), true);
        ArrayList<Part> composition = rootPart.getComposition();             
        Vector vector = coll.getVector(root.getVector().getUUID(), true);
        String extraSeq = "";
        
        //Determine part neighbors based upon root composition
        //Edge case where a plasmid only has one part or a part is re-used from the library
        if (root.getNeighbors().isEmpty()) {                                  
            lSeq = vector.getSeq();
            rSeq = vector.getSeq();
        
        //Edge case of merged parts
        } else if (node.getSpecialSeq() != null) {
            lSeq = node.getLeftSeq();
            rSeq = node.getRightSeq();
            String specialSeq = node.getSpecialSeq();
            extraSeq = specialSeq.substring(0, specialSeq.length() - node.getPCRSeq().length());

        } else {
            
            //If the current part is a basic part, as opposed to composite
            if (currentPart.isBasic()) {

                //Get neighbor sequences
                int indexOf = composition.indexOf(currentPart);
                if (indexOf == 0) {
                    rightNeighbor = composition.get(indexOf + 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = vector.getSeq();
                    
                } else if (indexOf == composition.size() - 1) {
                    leftNeighbor = composition.get(indexOf - 1);
                    rSeq = vector.getSeq();
                    lSeq = leftNeighbor.getSeq();
                    
                } else {
                    rightNeighbor = composition.get(indexOf + 1);
                    leftNeighbor = composition.get(indexOf - 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = leftNeighbor.getSeq();
                }
                
            //If the level 0 node is a re-used composite part
            } else {

                Part first = currentPart.getComposition().get(0);
                int indexOfFirst = composition.indexOf(first);
                Part last = currentPart.getComposition().get(currentPart.getComposition().size() - 1);
                int indexOfLast = composition.indexOf(last);

                //Get neighbor sequences of beginning of part
                if (indexOfFirst == 0) {
                    lSeq = vector.getSeq();
                } else {
                    leftNeighbor = composition.get(indexOfFirst - 1);
                    lSeq = leftNeighbor.getSeq();
                }

                //Get neighbor sequences of beginning of part
                if (indexOfLast == composition.size() - 1) {
                    rSeq = vector.getSeq();
                } else {
                    rightNeighbor = composition.get(indexOfFirst + 1);
                    rSeq = rightNeighbor.getSeq();
                }
            }
        }

        String currentSeq = currentPart.getSeq();
        
        //Look to see if there are blank sequences for the right or left part
        if (lSeq.equals("")) {
            missingLeftSequence = true;
        } 
        if (rSeq.equals("")) {
            missingRightSequence = true;
        }
        if (currentSeq.equals("")) {
            missingSequence = true;
        }

        int lNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, lSeq, false);
        int rNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, PrimerDesign.reverseComplement(rSeq), false);

        int currentPartLHomologyLength;
        int currentPartRHomologyLength;
        
        //Edge determine part homology depending on whether or not this is a merged node
        if (!extraSeq.isEmpty()) {
            currentPartLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, node.getPCRSeq(), true);
            currentPartRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, PrimerDesign.reverseComplement(node.getPCRSeq()), true);
        } else {
            currentPartLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, currentSeq, true);
            currentPartRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2, minPCRLength, PrimerDesign.reverseComplement(currentSeq), true);
        }
        
        //If there are any missing sequences, return default homology indications
        if (missingSequence || missingLeftSequence || missingRightSequence) {
            forwardOligoSequence = "[" + currentPart.getLeftOverhang() + " HOMOLOGY][" + currentPart.getName() + " HOMOLOGY]";
            reverseOligoSequence = "[" + currentPart.getRightOverhang() + " HOMOLOGY][" + currentPart.getName() + " HOMOLOGY]";

        } else {
            
            //If the total length of a merged node primer exceeds the max length, it must be synthesized
            if (!extraSeq.isEmpty()) {
                int totalLengthL = lNeighborHomologyLength + extraSeq.length() + currentPartLHomologyLength;
                int totalLengthR = currentPartRHomologyLength + rNeighborHomologyLength;
                
                if (totalLengthL >= maxPrimerLength || totalLengthR >= maxPrimerLength) {
                    forwardOligoSequence = "SYNTHESIZE";
                    reverseOligoSequence = "SYNTHESIZE";
                    node.setPCRSeq("synthesize");
                } else {
                    forwardOligoSequence = lSeq.substring(Math.max(0, lSeq.length() - lNeighborHomologyLength)) + extraSeq + currentSeq.substring(0, Math.min(currentSeq.length(), currentPartLHomologyLength));
                    reverseOligoSequence = PrimerDesign.reverseComplement(currentSeq.substring(Math.max(0, currentSeq.length() - currentPartRHomologyLength)) + rSeq.substring(0, Math.min(rSeq.length(), rNeighborHomologyLength)));
                }
                
            } else {

                if (currentSeq.length() > minPCRLength) {
                    forwardOligoSequence = lSeq.substring(Math.max(0, lSeq.length() - lNeighborHomologyLength)) + currentSeq.substring(0, Math.min(currentSeq.length(), currentPartLHomologyLength));
                    reverseOligoSequence = PrimerDesign.reverseComplement(currentSeq.substring(Math.max(0, currentSeq.length() - currentPartRHomologyLength)) + rSeq.substring(0, Math.min(rSeq.length(), rNeighborHomologyLength)));
                } else {
                    forwardOligoSequence = lSeq.substring(Math.max(0, lSeq.length() - lNeighborHomologyLength)) + currentSeq + rSeq.substring(0, Math.max(0, currentPartRHomologyLength));
                    reverseOligoSequence = PrimerDesign.reverseComplement(forwardOligoSequence);
                }
            }
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;

        return oligos;
    }
    
    public static String[] homolRecombVectorPrimers(RVector vector, RNode root, Collector coll, Double meltingTemp, Integer targetLength, Integer maxPrimerLength, Integer minPCRLength) {
        
        //Initialize primer parameters
        String[] oligos = new String[2];
        String forwardOligoSequence;
        String reverseOligoSequence;
        String lSeq;
        String rSeq;
        
        boolean missingLeftSequence = false;
        boolean missingSequence = false;
        boolean missingRightSequence = false;

        String currentSeq = "";
        ArrayList<Vector> allVectorsWithName = coll.getAllVectorsWithName(vector.getName(), true);
        if (!allVectorsWithName.isEmpty()) {
            currentSeq = allVectorsWithName.get(0).getSeq();
        }

        //Get the left flanking sequence
        String rPartName = vector.getROverhang().substring(0, vector.getROverhang().length() - 1);
        ArrayList<Part> allPartsWithNameL = coll.getAllPartsWithName(rPartName, true);
        if (!allPartsWithNameL.isEmpty()) {
            lSeq = allPartsWithNameL.get(0).getSeq();
            if (vector.getROverhang().substring(vector.getROverhang().length() - 1).equals("-")) {
                lSeq = PrimerDesign.reverseComplement(lSeq);
            }

        //If the overhang isn't a part name, it's a vector name
        } else {
            String vecName = vector.getROverhang().substring(0, vector.getROverhang().length() - 2);
            lSeq = coll.getAllVectorsWithName(vecName, true).get(0).getSeq();
        }

        //Get the right flanking sequence
        String lPartName = vector.getLOverhang().substring(0, vector.getLOverhang().length() - 1);
        ArrayList<Part> allPartsWithNameR = coll.getAllPartsWithName(lPartName, true);
        if (!allPartsWithNameR.isEmpty()) {
            rSeq = allPartsWithNameR.get(0).getSeq();
            if (vector.getLOverhang().substring(vector.getLOverhang().length() - 1).equals("-")) {
                rSeq = PrimerDesign.reverseComplement(rSeq);
            }

        //If the overhang isn't a part name, it's a vector name
        } else {
            String vecName = vector.getLOverhang().substring(0, vector.getLOverhang().length() - 2);
            rSeq = coll.getAllVectorsWithName(vecName, true).get(0).getSeq();
        }

        //Look to see if there are blank sequences for the right or left part
        if (lSeq.equals("")) {
            missingLeftSequence = true;
        }
        if (rSeq.equals("")) {
            missingRightSequence = true;
        }
        if (currentSeq.equals("")) {
            missingSequence = true;
        }

        int lNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength/2 - 4, minPCRLength, lSeq, false);
        int rNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength/2 - 4, minPCRLength, PrimerDesign.reverseComplement(rSeq), false);
        int vectorLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength/2 - 4, minPCRLength, currentSeq, true);
        int vectorRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, maxPrimerLength / 2 - 4, minPCRLength, PrimerDesign.reverseComplement(currentSeq), true);

        String NotIsite = "gcggccgc";

        //If there are any missing sequences, return default homology indications
        if (missingSequence || missingLeftSequence || missingRightSequence) {
            forwardOligoSequence = "[" + vector.getLOverhang() + " HOMOLOGY][NotI Site][" + vector.getName() + " HOMOLOGY]";
            reverseOligoSequence = "[" + vector.getROverhang() + " HOMOLOGY][NotI Site][" + vector.getName() + " HOMOLOGY]";

        } else {
            forwardOligoSequence = lSeq.substring(Math.max(0, lSeq.length() - lNeighborHomologyLength)) + NotIsite + currentSeq.substring(0, Math.min(currentSeq.length(), vectorLHomologyLength));
            reverseOligoSequence = PrimerDesign.reverseComplement(currentSeq.substring(Math.max(0, currentSeq.length() - vectorRHomologyLength)) + NotIsite + rSeq.substring(0, Math.min(rSeq.length(), rNeighborHomologyLength)));
        }

        oligos[0]=forwardOligoSequence;
        oligos[1]=reverseOligoSequence;
        
        return oligos;
        
    }
}

//OLD CODE DUMP, MIGHT REVISIT LATER
    /**
     * Traverses graphs and looks for parts that are too small for homologous
     * recombination and merges them with their neighbor(s) *
     */
//    public void smallPartHomologyGraphMerge(ArrayList<RGraph> optimalGraphs) {
//    }

//Keep getting the sequences of the leftmost neighbors until the min sequence size is satisfied
//        while (leftNeighborSeqLength < targetLength) {
//            
//            if (indexOf == 0) {
//                leftNeighbor = composition.get(composition.size() - 1);
//                String seq = leftNeighbor.getSeq();
//                if (seq.equals("")) {
//                    missingLeftSequence = true;
//                    break;
//                }
//                ArrayList<String> leftNeighborDirection = leftNeighbor.getDirections();
//                if ("-".equals(leftNeighborDirection.get(0))) {
//                    seq = PrimerDesign.reverseComplement(seq);
//                }
//
//                leftNeighborSeq = leftNeighborSeq + seq;
//                leftNeighborSeqLength = leftNeighborSeq.length();
//                indexOf = composition.size() - 1;
//            
//            } else {
//                leftNeighbor = composition.get(indexOf - 1);
//                String seq = leftNeighbor.getSeq();
//                
//                if (seq.equals("")) {
//                    missingLeftSequence = true;
//                    break;
//                }
//                
//                ArrayList<String> leftNeighborDirection = leftNeighbor.getDirections();
//                if ("-".equals(leftNeighborDirection.get(0))) {
//                    seq = PrimerDesign.reverseComplement(seq);
//                }
//
//                leftNeighborSeq = leftNeighborSeq + seq;
//                leftNeighborSeqLength = leftNeighborSeq.length();
//                indexOf--;
//            }
//        }
//
//        //Keep getting the sequences of the righttmost neighbors until the min sequence size is satisfied
//        while (rightNeighborSeqLength < targetLength) {
//            
//            if (indexOf == composition.size() - 1) {
//                rightNeighbor = composition.get(0);
//                String seq = rightNeighbor.getSeq();
//                ArrayList<String> leftNeighborDirection = rightNeighbor.getDirections();
//                
//                if (seq.equals("")) {
//                    missingRightSequence = true;
//                    break;
//                }
//                
//                if ("-".equals(leftNeighborDirection.get(0))) {
//                    seq = PrimerDesign.reverseComplement(seq);
//                }
//
//                rightNeighborSeq = rightNeighborSeq + seq;
//                rightNeighborSeqLength = rightNeighborSeq.length();
//                indexOf = 0;
//            
//            } else {
//                rightNeighbor = composition.get(indexOf + 1);
//                String seq = rightNeighbor.getSeq();
//                ArrayList<String> leftNeighborDirection = rightNeighbor.getDirections();
//                
//                if (seq.equals("")) {
//                    missingRightSequence = true;
//                    break;
//                }
//                
//                if ("-".equals(leftNeighborDirection.get(0))) {
//                    seq = PrimerDesign.reverseComplement(seq);
//                }
//
//                rightNeighborSeq = rightNeighborSeq + seq;
//                rightNeighborSeqLength = rightNeighborSeq.length();
//                indexOf++;
//            }
//        }
