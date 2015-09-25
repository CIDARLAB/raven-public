/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.core;

import org.cidarlab.raven.datastructures.RGraph;
import org.cidarlab.raven.datastructures.RNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jenhantao,evanappleton
 */
public class RGeneral extends Modularity {

    /** Find assembly graph for multiple goal parts **/
    protected ArrayList<RGraph> createAsmGraph_mgp(ArrayList<RNode> gps, HashMap<String, RGraph> partHash, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, HashMap<Integer, Double> efficiencies, boolean sharing) throws Exception {
        
        //Search all goal parts for potential conflicts with requried parts, return a blank graph and error message if there is a conflict
        for (int i = 0; i < gps.size(); i++) {
            RNode gp = gps.get(i);
            required = conflictSearchRequired(gp, required);
        }

        HashSet<String> libCompDir = new HashSet(partHash.keySet());
        
        //Run algorithm for all goal parts separately, find the max stages
        HashMap<String, RGraph> slackLibrary = new HashMap<String, RGraph>();
        slackLibrary.putAll(partHash);
        int slack = determineSlack(gps, slackLibrary, libCompDir, required, recommended, forbidden);
        System.gc();
        
        //Compute sharing scores for all goal parts
        HashMap<String, Integer> sharingHash = new HashMap<String, Integer>();
        if (sharing == true) {
            sharingHash = computeIntermediateSharing(gps);
        }        

        //First initiate results and pinned hash
        ArrayList<RGraph> resultGraphs = new ArrayList<RGraph>();
        HashMap<String, RGraph> pinnedPartHash = new HashMap<String, RGraph>();

        //Iterate across all goal parts until we have a result graph for each goal part
        //Start finding graphs for each goal part until all goal parts are formed  
        while (!gps.isEmpty()) {

            //Reinitialize memoization hash with part library and pinned graphs each with zero cost
            HashMap<String, RGraph> hashMem = new HashMap<String, RGraph>();
            hashMem.putAll(partHash);
            hashMem.putAll(pinnedPartHash);

            //Call single-goal-part algorithm for each goal part and determine which of the graphs to pin
            int index = 0;
            RGraph pinnedGraph = null;
            for (int j = 0; j < gps.size(); j++) {
                RNode gp = gps.get(j);
//                RGraph newGraph = new RGraph();
                RGraph newGraph = createAsmGraph_sgp(gp, hashMem, libCompDir, required, recommended, forbidden, discouraged, slack, sharingHash, efficiencies);
                newGraph.getRootNode().setUUID(gp.getUUID());
                newGraph.getRootNode().setName(gp.getName());
                newGraph.getRootNode().setLinkers(gp.getLinkers());
                
                if (newGraph.getRootNode().getComposition().isEmpty()) {
                    System.out.println("WARNING, GOAL PART " + gp.getComposition() + " CANNOT BE BUILT WITH THIS FORBIDDEN SET!");
                    return null;
                    
                } else {
                    
                    //Pin graph if no existing pinned graph
                    if (pinnedGraph == null) {
                        pinnedGraph = newGraph.clone();
                        index = j;
                    }

                    //If there are any discouraged parts, pin the graph with the fewest discouraged parts
                    if (!discouraged.isEmpty()) {
                        if (newGraph.getDiscouragedCount() < pinnedGraph.getDiscouragedCount()) {
                            pinnedGraph = newGraph;
                            index = j;
                        }
                    }

                    //If there are any recommended parts, pin the graph with greatest recommended parts
                    if (!recommended.isEmpty()) {
                        if (newGraph.getReccomendedCount() > pinnedGraph.getReccomendedCount()) {
                            pinnedGraph = newGraph;
                            index = j;
                        }

                    //If no recommended parts, pin the graph with the most sharing
                    } else {
                        if (newGraph.getSharing() > pinnedGraph.getSharing()) {
                            pinnedGraph = newGraph;
                            index = j;
                        } else if (newGraph.getModularityFactor() > pinnedGraph.getModularityFactor()) {
                            pinnedGraph = newGraph;
                            index = j;
                        }
                    }
                }
            }
            
            //Add pinned graph and graph for each intermediate part to our hash of pinned graphs
            //Also search through the subgraphs of the bestGraph to see if it has any basic parts
            boolean cantMake = true;
            RNode pinnedRoot = pinnedGraph.getRootNode();
            pinnedPartHash.put(pinnedRoot.getComposition().toString() + pinnedRoot.getDirection().toString(), pinnedGraph.clone());
            
            if (!pinnedGraph.getSubGraphs().isEmpty()) {
                cantMake = false;
            }

            for (int k = 0; k < pinnedGraph.getSubGraphs().size(); k++) {
                RGraph subGraph = pinnedGraph.getSubGraphs().get(k);
                RGraph subGraphClone = subGraph.clone();
                subGraphClone.pin();
                RNode subGraphRoot = subGraph.getRootNode();
                pinnedPartHash.put(subGraphRoot.getComposition().toString() + subGraphRoot.getDirection().toString(), subGraphClone);

                //If a basic part is seen in the solution graph
                if (subGraph.getRootNode().getStage() > 0) {
                    cantMake = false;
                }
            }

            //Send warning if there might be missing basic parts or subgraphs
            if (cantMake) {
                System.out.println("WARNING, THERE ARE EITHER NO BASIC PARTS OR NO SUBGRAPHS... SOMETHING MAY ALREADY EXISTS OR SOMETHING CANNOT BE MADE");
            }

            //Remove pinned graph from goal part list and add to result list
            gps.remove(gps.get(index));
            resultGraphs.add(pinnedGraph);
        }

        return resultGraphs;
    }

    /** Find assembly graph for a single goal part factoring in slack and sharing **/
    protected RGraph createAsmGraph_sgp(RNode goalPartNode, HashMap<String, RGraph> partsHash, HashSet<String> libCompDir, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden, HashSet<String> discouraged, int slack, HashMap<String, Integer> sharingHash, HashMap<Integer, Double> efficiencies) {

        //If any of the parameters is null, must be set to a new object to avoid null pointer issues
        if (partsHash == null) {
            partsHash = new HashMap<String, RGraph>();
        }
        if (libCompDir == null) {
            libCompDir = new HashSet<String>();
        }
        if (sharingHash == null) {
            sharingHash = new HashMap<String, Integer>();
        }
        if (required == null) {
            required = new HashSet<String>();
        }
        if (forbidden == null) {
            forbidden = new HashSet<String>();
        }
        if (recommended == null) {
            recommended = new HashSet<String>();
        }
        if (discouraged == null) {
            discouraged = new HashSet<String>();
        }
        if (efficiencies == null) {
            efficiencies = new HashMap<Integer, Double>();
        }
        
        //Memoization Case - If graph already exists for this composition and direction. This is always the case for basic parts and library parts
        if (partsHash.containsKey(goalPartNode.getComposition().toString() + goalPartNode.getDirection().toString())) {
            return partsHash.get(goalPartNode.getComposition().toString() + goalPartNode.getDirection().toString());
        }
        
        RGraph bestGraph = new RGraph(goalPartNode);

        //Recursive Programing to find best graph
        int gpSize = goalPartNode.getComposition().size();
        ArrayList<String> gpComp = goalPartNode.getComposition();
        ArrayList<String> gpType = goalPartNode.getType();
        ArrayList<String> gpDir = goalPartNode.getDirection();
        
        //Create idexes for goal part
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        for (int i = 1; i < gpSize; i++) {
            indexes.add(i);
        }
        
        //Remove indexes for required parts
        for (int start = 0; start < gpSize; start++) {
            for (int end = start + 2; end < gpSize + 1; end++) {
                if (start == 0 && end == gpSize) {
                    continue;
                }
                ArrayList<String> gpSub = new ArrayList<String>();
                List<String> compSubList = gpComp.subList(start, end);
                List<String> dirSubList = gpDir.subList(start, end);
                for (int o = 0; o < compSubList.size(); o++) {
                    gpSub.add(compSubList.get(o) + "|" + dirSubList.get(o));
                }
                if (required.contains(gpSub.toString())) {
                    for (int j = start + 1; j < end; j++) {
                        indexes.remove(new Integer(j));
                    }
                }
            }
        }
        
        //Create an additional partition set if library parts already exist to explore more design space that includes library parts
        ArrayList<ArrayList<Integer>> libIndexes = new ArrayList<ArrayList<Integer>>();
        libIndexes.add(indexes);

        for (int startL = 0; startL < gpSize; startL++) {
            for (int endL = startL + 2; endL < gpSize + 1; endL++) {
                if (startL == 0 && endL == gpSize) {
                    continue;
                }
                ArrayList<String> gpSub = new ArrayList<String>();
                gpSub.addAll(gpComp.subList(startL, endL));
                
                if (libCompDir.contains(gpSub.toString())) {
                    ArrayList<Integer> aLibIndexes = new ArrayList<Integer>(indexes);
                    for (int j = startL + 1; j < endL; j++) {
                        aLibIndexes.remove(new Integer(j));
                    }
                    libIndexes.add(aLibIndexes);
                }
            }
        }
        
        //Initialize forbidden partition sets
        HashMap<Integer, ArrayList<int[]>> forbPartitionsBySize = new HashMap<Integer, ArrayList<int[]>>();
        for (int i = 1; i < _maxNeighbors; i++) {
            ArrayList<int[]> forbiddenPartitions = new ArrayList<int[]>();
            forbPartitionsBySize.put(i, forbiddenPartitions);
        }
        
        //For all of the allowed index sets, find the best way to break it into even peices for 1 to the maximum number of neighbors 
        HashMap<Integer, ArrayList<int[]>> partitionSetByNBreaks = new HashMap<Integer, ArrayList<int[]>>();
        for (int j = 0; j < libIndexes.size(); j++) {
            
            HashMap<Integer, ArrayList<int[]>> aPartitionSetByNBreaks = getPartitions(libIndexes.get(j), forbPartitionsBySize);
            Set<Integer> keySet = aPartitionSetByNBreaks.keySet();
            ArrayList<Integer> nNeighbors = new ArrayList<Integer>(keySet);
            Collections.sort(nNeighbors);
            
            //Add partitions to larger set
            if (partitionSetByNBreaks.isEmpty()) {
                partitionSetByNBreaks = aPartitionSetByNBreaks;
            } else {
                for (Integer k : nNeighbors) {
                    ArrayList<int[]> sets = partitionSetByNBreaks.get(k);
                    sets.addAll(aPartitionSetByNBreaks.get(k));
                    partitionSetByNBreaks.put(k, sets);
                }
            }
        }
        
        Set<Integer> keySet = partitionSetByNBreaks.keySet();
        ArrayList<Integer> keys = new ArrayList<Integer>(keySet);
        Collections.sort(keys);
        boolean canPartitionAny = false;

        //Iterate over all part "breaks"
        //Find best graph for all possible number of partition sizes        
        for (Integer nBreaks : keys) {

            ArrayList<int[]> forbiddenPartitions = new ArrayList<int[]>();
            ArrayList<int[]> candidatePartitions = partitionSetByNBreaks.get(nBreaks);
            
            //For each partition of this size
            while (!candidatePartitions.isEmpty()) {
                
                int[] thisPartition = candidatePartitions.get(0);
                candidatePartitions.remove(0);
                ArrayList<RNode> allSubParts = new ArrayList<RNode>();
                boolean canPartitionThis = true;

                //For each partition, create a new RNode and add it to the list
                for (int n = 0; n < (thisPartition.length + 1); n++) {
                    ArrayList<String> type = new ArrayList<String>();
                    ArrayList<String> comp = new ArrayList<String>();
                    ArrayList<String> dir = new ArrayList<String>();
                    ArrayList<String> compDir = new ArrayList<String>();
                    
                    if (n == 0) {
                        type.addAll(gpType.subList(0, thisPartition[n]));
                        comp.addAll(gpComp.subList(0, thisPartition[n]));
                        dir.addAll(gpDir.subList(0, thisPartition[n]));
                    } else if (n == thisPartition.length) {
                        type.addAll(gpType.subList(thisPartition[n - 1], gpSize));
                        comp.addAll(gpComp.subList(thisPartition[n - 1], gpSize));
                        dir.addAll(gpDir.subList(thisPartition[n - 1], gpSize));
                    } else {
                        type.addAll(gpType.subList(thisPartition[n - 1], thisPartition[n]));
                        comp.addAll(gpComp.subList(thisPartition[n - 1], thisPartition[n]));
                        dir.addAll(gpDir.subList(thisPartition[n - 1], thisPartition[n]));
                    }

                    //If any of the compositions is in the forbidden hash, this partition will not work
                    for (int m = 0; m < comp.size(); m++) {
                        compDir.add(comp.get(m) + "|" + dir.get(m));
                    }
                    
                    if (forbidden.contains(compDir.toString())) {
                        canPartitionThis = false;
                        forbiddenPartitions.add(thisPartition);
                        continue;
                    }

                    //If not, make the new RNode
                    boolean rec = recommended.contains(comp.toString());
                    boolean dis = discouraged.contains(comp.toString());
                    RNode aSubPart = new RNode(rec, dis, comp, dir, type, null, null, null, null, 0, 0, null);
                    allSubParts.add(aSubPart);
                }

                
                //If there are no forbidden parts amongst subparts, this partition is valid
                if (canPartitionThis) {
                    canPartitionAny = true;
                    ArrayList<RGraph> toCombine = new ArrayList<RGraph>();
                    ArrayList<String> combineDirection = new ArrayList<String>();
                    
                    //Recursive call
                    for (int o = 0; o < allSubParts.size(); o++) {
                        RNode oneSubPart = allSubParts.get(o);
                        RGraph solution = createAsmGraph_sgp(oneSubPart, partsHash, libCompDir, required, recommended, forbidden, discouraged, slack - 1, sharingHash, efficiencies);
                        
                        //If the solution is clear, meaning all possible partitions are forbidden and there is no answer, this partition must also be forbidden
                        if (solution.getRootNode().getComposition().isEmpty()) {                            
                            forbiddenPartitions.add(thisPartition);
                            toCombine.clear();
                            break;
                        }
                        toCombine.add(solution);
                        combineDirection.addAll(oneSubPart.getDirection());
                    }

                    //If the solution is not empty, combine graphs and score against the current best
                    if (!toCombine.isEmpty()) {
                        RGraph newGraph = combineGraphsModEff(toCombine, combineDirection, recommended, discouraged, sharingHash, efficiencies);
                        
                        //Edge case: best graph does not exist yet
                        if (bestGraph.getRootNode().getNeighbors().isEmpty()) {
                            bestGraph = newGraph;
                        } else {

                            // If cost of new graph is the best so far save it
                            bestGraph = minCostSlack(bestGraph, newGraph, slack);
                        }
                    }
                }

                //If none so far can be partitioned and the list is empty, find next most optimal set using partitioning method
                if (!canPartitionAny && candidatePartitions.isEmpty()) {
                    HashMap<Integer, ArrayList<int[]>> newForbPartitionsBySize = new HashMap<Integer, ArrayList<int[]>>();
                    newForbPartitionsBySize.put(nBreaks, forbiddenPartitions);                    
                    HashMap<Integer, ArrayList<int[]>> newPartitions = getPartitions(indexes, newForbPartitionsBySize);
                    
                    //If all possible partitions have been exhausted, return the best graph, which should be empty... this should rarely happen 
                    if (newPartitions.isEmpty()) {
                        return bestGraph;
                    }
                    candidatePartitions.addAll(newPartitions.get(nBreaks));
                }
            }
        }        
        
        //Save best graph for this intermediate
        RNode bestGraphRoot = bestGraph.getRootNode();
        partsHash.put(bestGraphRoot.getComposition().toString() + bestGraphRoot.getDirection().toString(), bestGraph);

//        bestGraph.getRootNode().setLinkers(goalPartNode.getLinkers());
//        
        //Return best graph for the initial goal part
        return bestGraph;
    }

    /** Combine multiple graphs, including efficiency and modularity scoring **/
    //Currently, it is assumed that efficiencies are additive and not multiplicative
    protected RGraph combineGraphsModEff(ArrayList<RGraph> graphs, ArrayList<String> direction, HashSet<String> recommended, HashSet<String> discouraged, HashMap<String, Integer> sharingHash, HashMap<Integer, Double> efficiencies) {
        
        //Call method without efficiency and modularity first
        RGraph combineGraphsME = combineGraphsShareRecDis(graphs, direction, recommended, discouraged, sharingHash);
        RNode root = combineGraphsME.getRootNode();

        //Get effiency of subgraphs
        double efficiency = 0;
        int numCombine = graphs.size();
        if (efficiencies.containsKey(numCombine)) {
            efficiency = efficiencies.get(numCombine);
            root.setEfficiency(efficiency);
        }
        ArrayList<Double> allEfficiencies = new ArrayList<Double>();
        allEfficiencies.add(efficiency);
        for (int i = 0; i < graphs.size(); i++) {
            allEfficiencies.addAll(graphs.get(i).getEfficiencyArray());
        }

        //Set the new graph's efficiency scores
        combineGraphsME.setEfficiencyArray(allEfficiencies);

        return combineGraphsME;
    }

    /** Combine multiple graphs, including sharing and recommended **/
    protected RGraph combineGraphsShareRecDis(ArrayList<RGraph> graphs, ArrayList<String> direction, HashSet<String> recommended, HashSet<String> discouraged, HashMap<String, Integer> sharingHash) {

        //Call method without sharing first
        RGraph combineGraphsSRD = combineGraphsStageStep(graphs, direction);
        RNode root = combineGraphsSRD.getRootNode();

        //Look in sharing and recommended hash to set sharing and recommended
        int graphSharing = 0;
        int recCount = 0;
        int disCount = 0;
        for (int i = 0; i < graphs.size(); i++) {
            graphSharing = graphs.get(i).getModularityFactor() + graphSharing;
            recCount = graphs.get(i).getReccomendedCount() + recCount;
            disCount = graphs.get(i).getDiscouragedCount() + disCount;
        }

        //If sharing hash contains the root's composition and sharing hash is not empty
        if (!sharingHash.isEmpty()) {
            if (sharingHash.containsKey(root.getComposition().toString())) {
                combineGraphsSRD.setModularityFactor(graphSharing + sharingHash.get(root.getComposition().toString()));
            } else {
                combineGraphsSRD.setModularityFactor(graphSharing);
            }
        }
        
        ArrayList<String> rootRecDis = new ArrayList<String>();
        ArrayList<String> composition = combineGraphsSRD.getRootNode().getComposition();
        for (int j = 0; j < composition.size(); j++) {
            rootRecDis.add(composition.get(j) + "|" + direction.get(j));
        }
        
        //If recommended hash contains the root's composition
        if (recommended.contains(rootRecDis.toString())) {
            combineGraphsSRD.setReccomendedCount(recCount + 1);
        } else {
            combineGraphsSRD.setReccomendedCount(recCount);
        }
        
        //If discouraged hash contains the root's composition
        if (discouraged.contains(rootRecDis.toString())) {
            combineGraphsSRD.setDiscouragedCount(disCount + 1);
        } else {
            combineGraphsSRD.setDiscouragedCount(disCount);
        }
        return combineGraphsSRD;
    }

    /** Combine multiple graphs, ignoring sharing and recommended **/
    protected RGraph combineGraphsStageStep(ArrayList<RGraph> graphs, ArrayList<String> direction) {
        RNode newRoot = new RNode();
        ArrayList<String> mergerComposition = new ArrayList<String>();
        ArrayList<String> mergerType = new ArrayList<String>();
        
        //Get all the children types, compositions and directions
        for (int i = 0; i < graphs.size(); i++) {
            RNode currentNeighbor = graphs.get(i).getRootNode();
            mergerComposition.addAll(currentNeighbor.getComposition());
            mergerType.addAll(currentNeighbor.getType());
        }
        newRoot.setComposition(mergerComposition);
        newRoot.setType(mergerType);
        newRoot.setDirection(direction);
        
        //Clone all the nodes from graphs being combined and then add to new root node
        for (int j = 0; j < graphs.size(); j++) {
            RNode toAdd = graphs.get(j).getRootNode().clone(true);
            toAdd.addNeighbor(newRoot);
            newRoot.addNeighbor(toAdd);
        }
        RGraph newGraph = new RGraph(newRoot);

        //Determine stages, steps, adjusted steps for the new graph
        int steps = 0;
        int maxStage = 0;

        //Stages and steps determined
        for (int k = 0; k < graphs.size(); k++) {
            int currentGraphStage = graphs.get(k).getStages();
            if (currentGraphStage > maxStage) {
                maxStage = currentGraphStage;
            }
            int currentGraphSteps = graphs.get(k).getSteps();
            steps = currentGraphSteps + steps;
            newGraph.addSubgraph(graphs.get(k));
            for (int l = 0; l < graphs.get(k).getSubGraphs().size(); l++) {
                newGraph.addSubgraph(graphs.get(k).getSubGraphs().get(l));
            }
        }
        newGraph.setSteps(steps + 1);
        newGraph.setStages(maxStage + 1);
        newRoot.setStage(maxStage + 1);

        return newGraph;
    }

    /** Find which of two graphs has a lower cost, including slack. Supports multi-goal-part assembly **/
    protected RGraph minCostSlack(RGraph g0, RGraph g1, int slack) {

        //Return pinned graphs
        if (g0.getPinned()) {
            return g0;
        } else if (g1.getPinned()) {
            return g1;
        }

        //If either graph has more stages than slack, ignore slack
        if (g0.getStages() > slack || g1.getStages() > slack) {
            return minCost(g0, g1);
        }

        //Efficiency
        if (g0.getAveEff() > g1.getAveEff()) {
            return g0;
        } else if (g1.getAveEff() > g0.getAveEff()) {
            return g1;
        }

        //Discouraged
        if (g0.getDiscouragedCount() > g1.getDiscouragedCount()) {
            return g1;
        } else if (g1.getDiscouragedCount() > g0.getDiscouragedCount()) {
            return g0;
        }
        
        //Recommended
        if (g0.getReccomendedCount() > g1.getReccomendedCount()) {
            return g0;
        } else if (g1.getReccomendedCount() > g0.getReccomendedCount()) {
            return g1;
        }

        //Steps
        if (g0.getSteps() < g1.getSteps()) {
            return g0;
        } else if (g1.getSteps() < g0.getSteps()) {
            return g1;
        }

        //Sharing factor
        if (g0.getModularityFactor() > g1.getModularityFactor()) {
            return g0;
        } else if (g1.getModularityFactor() > g0.getModularityFactor()) {
            return g1;
        }
        
        //If equal, return the original
        return g0;
    }

    /** Find which of two graphs has a lower cost, ignoring slack **/
    protected RGraph minCost(RGraph g0, RGraph g1) {

        //Stages
        if (g0.getStages() < g1.getStages()) {
            return g0;
        } else if (g1.getStages() < g0.getStages()) {
            return g1;
        }

        //Steps
        if (g0.getSteps() < g1.getSteps()) {
            return g0;
        } else if (g1.getSteps() < g0.getSteps()) {
            return g1;
        }

        //Discouraged
        if (g0.getDiscouragedCount() > g1.getDiscouragedCount()) {
            return g1;
        } else if (g1.getDiscouragedCount() > g0.getDiscouragedCount()) {
            return g0;
        }
        
        //Recommended
        if (g0.getReccomendedCount() > g1.getReccomendedCount()) {
            return g0;
        } else if (g1.getReccomendedCount() > g0.getReccomendedCount()) {
            return g1;
        }

        //If all else has failed, return the original graph
        return g0;
    }

    /** Find the maximum amount of stages for a set of goal parts with a library. This determines a mgp assembly slack factor **/
    protected int determineSlack(ArrayList<RNode> gps, HashMap<String, RGraph> library, HashSet<String> libCompDir, HashSet<String> required, HashSet<String> recommended, HashSet<String> forbidden) {
        int slack = 0;
        for (int i = 0; i < gps.size(); i++) {
            RNode gp = gps.get(i);
            RGraph graph = createAsmGraph_sgp(gp, library, libCompDir, required, null, forbidden, null, 0, null, null);
            if (graph.getStages() > slack) {
                slack = graph.getStages();
            }
        }

        return slack;
    }
    
    //Fields
    protected int _maxNeighbors;
}