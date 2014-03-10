/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import Controller.datastructures.Collector;
import Controller.datastructures.Part;
import Controller.datastructures.RNode;
import java.util.ArrayList;

/**
 *
 * @author evanappleton
 */
public class RHomologyPrimerDesign {

    public static String[] homologousRecombinationPrimers(RNode node, RNode root, Collector coll, Double meltingTemp, Integer targetLength) {

        //Initialize primer parameters
        String[] oligos = new String[2];
        String forwardOligoSequence;
        String reverseOligoSequence;
        String lSeq;
        String rSeq;
        
        boolean missingLeftSequence = false;
        boolean missingSequence = false;
        boolean missingRightSequence = false;
        
        Part currentPart = coll.getPart(node.getUUID(), true);
        Part leftNeighbor;
        Part rightNeighbor;
        Part rootPart = coll.getPart(root.getUUID(), true);
        ArrayList<Part> composition = rootPart.getComposition();

        String currentSeq = currentPart.getSeq();
        ArrayList<String> direction = node.getDirection();
        
        //Reverse complement sequences that are on the reverse strand
        if ("-".equals(direction.get(0))) {
            currentSeq = PrimerDesign.reverseComplement(currentSeq);
        }
        
        if (currentSeq.equals("")) {
            missingSequence = true;
        }      
        
        //Edge case where a plasmid only has one part or a part is re-used from the library
        if (root.getNeighbors().isEmpty()) {
            leftNeighbor = currentPart;
            rightNeighbor = currentPart;
            lSeq = leftNeighbor.getSeq();
            rSeq = rightNeighbor.getSeq();
            
        } else {
            if (currentPart.isBasic()) {

                //Get neighbor sequences
                int indexOf = composition.indexOf(currentPart);
                if (indexOf == 0) {
                    leftNeighbor = composition.get(composition.size() - 1);
                    rightNeighbor = composition.get(indexOf + 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = leftNeighbor.getSeq();
                } else if (indexOf == composition.size() - 1) {
                    rightNeighbor = composition.get(0);
                    leftNeighbor = composition.get(indexOf - 1);
                    rSeq = rightNeighbor.getSeq();
                    lSeq = leftNeighbor.getSeq();
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
        if (lSeq.equals("")) {
            missingLeftSequence = true;
        } else if (rSeq.equals("")) {
            missingRightSequence = true;
        }

        //Reverse sequence direction for parts on the reverse strand            
        ArrayList<String> rightNeighborDirection = rightNeighbor.getDirections();
        ArrayList<String> leftNeighborDirection = leftNeighbor.getDirections();
        if ("-".equals(leftNeighborDirection.get(0))) {
            lSeq = PrimerDesign.reverseComplement(lSeq);
        }
        if ("-".equals(rightNeighborDirection.get(0))) {
            rSeq = PrimerDesign.reverseComplement(rSeq);
        }

        int lNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, lSeq, false, true);
        int rNeighborHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(rSeq), false, true);
        int currentPartLHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, currentSeq, true, true);
        int currentPartRHomologyLength = PrimerDesign.getPrimerHomologyLength(meltingTemp, targetLength, PrimerDesign.reverseComplement(currentSeq), true, true);
        
        //If there are any missing sequences, return default homology indications
        if (missingSequence || missingLeftSequence || missingRightSequence) {
            forwardOligoSequence = "["+leftNeighbor.getName()+" HOMOLOGY][" + currentPart.getName() + " HOMOLOGY]";
            reverseOligoSequence = "["+rightNeighbor.getName()+" HOMOLOGY][" + currentPart.getName() + " HOMOLOGY]";
        } else {
            forwardOligoSequence = lSeq.substring(Math.max(0, lSeq.length() - lNeighborHomologyLength)) + currentSeq.substring(0, Math.min(currentSeq.length(), currentPartLHomologyLength));
            reverseOligoSequence = PrimerDesign.reverseComplement(currentSeq.substring(Math.max(0, currentSeq.length() - currentPartRHomologyLength)) + rSeq.substring(0, Math.min(rSeq.length(), rNeighborHomologyLength)));
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
