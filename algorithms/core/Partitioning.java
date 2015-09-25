/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.core;

import org.cidarlab.raven.datastructures.RNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 *
 * @author evanappleton
 */
public class Partitioning {
    
    /**
     * ************************************************************************
     *
     * PARTITIONING
     *
     *************************************************************************
     */
    
    
    /** For n-way assembly, must find all ways to "break" a part i.e. all possible partition of size maxNeighbors and less **/
    protected HashMap<Integer, ArrayList<int[]>> getPartitions(ArrayList<Integer> indexes, HashMap<Integer, ArrayList<int[]>> forbiddenPartitions) {

        int[] newIndexes = buildIntArray(indexes);
        HashMap<Integer, ArrayList<int[]>> partitions = new HashMap<Integer, ArrayList<int[]>>();
        Set<Integer> keySet = forbiddenPartitions.keySet();
        ArrayList<Integer> keys = new ArrayList<Integer>(keySet);
        Collections.sort(keys);

        for (Integer numBreaks : keys) {
            ArrayList<int[]> subsets = getSubsets(newIndexes, numBreaks, forbiddenPartitions.get(numBreaks));
            partitions.put(numBreaks, subsets);
        }

        return partitions;
    }
    
    /** Get all subsets of a set for a specific sized subset **/
    protected ArrayList<int[]> getSubsets(int[] set, int k, ArrayList<int[]> forbiddenPartitions) {
//        int[] subset = new int[k];
//        ArrayList<int[]> bestIndexes = new ArrayList<int[]>();
//        _subsetScore = -1;
//        getSubsetsHelper(set, subset, 0, 0, bestIndexes);
//        return bestIndexes;

        int numel = (int) Math.ceil((double) set.length / k);
        int[][] subsets = new int[k][numel];
        double space = (double) set.length / k;

        int start = 0;
        int row = 0;
        int col = 0;

        //Divide the set into equal peices
        for (int i = 0; i < set.length; i++) {
            int end = (int) (i / space);
            if (end > start) {
                start = end;
                row++;
                col = 0;
                subsets[row][col] = set[i];
                col++;
            } else {
                subsets[row][col] = set[i];
                col++;
            }

        }

        ArrayList<int[]> allSets = new ArrayList<int[]>();
        int[] solution = new int[subsets.length];
        _subsetScore = -1;
        getSets(set, subsets, solution, 0, allSets, forbiddenPartitions);

        return allSets;
    }

    /** Search permutations for the best score **/
    private void getSets(int[] set, int[][] subsets, int[] subset, int subsetSize, ArrayList<int[]> optimal, ArrayList<int[]> forbiddenSets) {

        if (subsetSize == subset.length) {

            boolean noZeros = true;
            for (int i = 0; i < subset.length; i++) {
                if (subset[i] == 0) {
                    noZeros = false;
                }
            }

            //If this set is forbidden
            boolean forbidden = false;
            for (int j = 0; j < forbiddenSets.size(); j++) {
                if (Arrays.equals(subset, forbiddenSets.get(j))) {
                    forbidden = true;
                }
            }

            //If this set has no zeros and is not forbidden
            if (noZeros && !forbidden) {
                int score = 0;

                //Determine score of subset
                int[] setCopy = set.clone();
                for (int k = 0; k < subset.length; k++) {
                    for (int l = 0; l < setCopy.length; l++) {
                        if (setCopy[l] == subset[k]) {
                            setCopy[l] = 0;
                        }
                    }
                }
                int start = 0;
                for (int i = 0; i < setCopy.length; i++) {
                    if (setCopy[i] == 0) {
                        score = score + (i - start) * (i - start);
                        start = i + 1;
                    }
                    if (i == (setCopy.length - 1)) {
                        score = score + (i + 1 - start) * (i + 1 - start);
                    }
                }

                //If this is the best score, put it into the optimal solutions
                if (_subsetScore == -1) {
                    _subsetScore = score;
                    optimal.add(subset);

                    //If this is the new best score
                } else if (score < _subsetScore) {
                    _subsetScore = score;
                    optimal.clear();
                    optimal.add(subset);

                    //If current score equal to the best score
                } else if (score == _subsetScore) {
                    optimal.add(subset);
                }
            }
        } else {

            //Get all the subsets of interest
            int col = subsets[0].length;
            for (int k = 0; k < col; k++) {
                int[] sub = subset.clone();
                sub[subsetSize] = subsets[subsetSize][k];
                getSets(set, subsets, sub, subsetSize + 1, optimal, forbiddenSets);
            }
        }
    }

//    private void getSubsetsHelper(int[] set, int[] subset, int subsetSize, int nextIndex, ArrayList<int[]> bestIndexes) { 
//        
//        //Subset optimization
//        if (subsetSize == subset.length) {
//            int score = 0;
//            int[] setCopy = set.clone();
//            
//            //Set all indexes of the regular set that match the subsets to 0 for splitting
//            for (int k = 0; k < subset.length; k++) {
//                for (int l = 0; l < setCopy.length; l++) {
//                    if (setCopy[l] == subset[k]) {
//                        setCopy[l] = 0;
//                    }
//                }
//            }
//            int start = 0;
//            
//            //Calculate spacing scores
//            for (int i = 0; i < setCopy.length; i++) {
//                if (setCopy[i] == 0) {
//                    score = score + (i-start)*(i-start);
//                    start = i + 1;
//                }
//                if (i == (setCopy.length - 1)) {
//                    score = score + (i+1-start)*(i+1-start);
//                }
//            }     
//            
//            //If the first pass through the algorithm
//            if (_subsetScore == -1) {      
//                _subsetScore = score;
//                bestIndexes.add(subset);          
//            
//            //If the current score is least squares
//            } else if (score < _subsetScore) {
//                _subsetScore = score;
//                bestIndexes.clear();
//                bestIndexes.add(subset);             
//            
//            //If current score equal to the best score
//            } else if (score == _subsetScore) {
//                bestIndexes.add(subset);
//            }
//        } else {
//            
//            //Recursive call to search more index combinations
//            for (int j = nextIndex; j < set.length; j++) {
//                int[] sub = subset.clone();
//                sub[subsetSize] = set[j];
//                getSubsetsHelper(set, sub, subsetSize + 1, j + 1, bestIndexes);
//            }
//        }
//    }
    /**
     * Convert an ArrayList of integers to an integer array *
     */
    protected int[] buildIntArray(ArrayList<Integer> integers) {
        int[] ints = new int[integers.size()];
        int i = 0;
        for (Integer n : integers) {
            ints[i++] = n;
        }
        return ints;
    }
    
    /**
     * ************************************************************************
     *
     * REQUIRED
     *
     *************************************************************************
     */
    
    /** For all goal parts, search for conflicts with required parts **/
    protected HashSet<String> conflictSearchRequired(RNode gp, HashSet<String> required) throws Exception {

        //Initialize hash to keep track of potential intermediate conflicts
        ArrayList<String> gpComp = gp.getComposition();
        ArrayList<String> gpDir = gp.getDirection();
        int gpSize = gp.getComposition().size();
        HashMap<ArrayList<Integer>, String> indexes = new HashMap<ArrayList<Integer>, String>();

        //Scan through all intermediates to see if any are required
        for (int start = 0; start < gpSize; start++) {
            for (int end = start + 2; end < gpSize + 1; end++) {
                ArrayList<String> gpSub = new ArrayList<String>();
                List<String> gpSubListComp = gpComp.subList(start, end);
                List<String> gpSubListDir = gpDir.subList(start, end);
                for (int i = 0; i < gpSubListComp.size(); i++) {
                    gpSub.add(gpSubListComp.get(i) + "|" + gpSubListDir.get(i));
                }

                //If an intermediate matches a composition in the required part hash
                if (required.contains(gpSub.toString())) {

                    //Record required indices
                    ArrayList<Integer> foundIndices = new ArrayList<Integer>();
                    int endCheck = end - 1;
                    foundIndices.add(start);
                    foundIndices.add(endCheck);
                    indexes.put(foundIndices, gpSub.toString());

                    //Detect required part conflicts
                    Collection<ArrayList<Integer>> values = indexes.keySet();
                    for (ArrayList<Integer> index : values) {

                        //If in between the indices being checked
                        if ((start >= index.get(0) && endCheck <= index.get(1)) || index.get(0) >= start && index.get(1) <= endCheck) {
                            //If completely outside indices
                        } else if (endCheck < index.get(0) || start > index.get(1)) {
                            //If conflicts, remove this transcriptional unit from the list of required
                        } else {
                            required.remove(gpSub.toString());
//                            JOptionPane.showMessageDialog(null, "Required part conflict discovered with \"" + gpSub.toString() + "\"...\nThis intermediate overlaps with another required intermediate and both cannot appear in one assmebly graph.\nPlease remove this required part or the part(s) that conflict.\nIf using MoClo, beware that all basic transcriptional units are required without manual selection");
//                            throw new Exception("Required part conflict discovered with \"" + gpSub.toString() + "\"...\nThis intermediate overlaps with another required intermediate and both cannot appear in one assmebly graph.\nPlease remove this required part or the part(s) that conflict.\nIf using MoClo, beware that all basic transcriptional units are required without manual selection");
                        }
                    }
                }
            }
        }
        
        return required;
    }
    
    //Fields
    private int _subsetScore;
}
