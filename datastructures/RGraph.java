/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.datastructures;

import Communication.WeyekinPoster;
import Controller.accessibility.ClothoReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author jenhantao, evanappleton
 */
public class RGraph {

    /**
     * SDSGraph constructor, no specified root node *
     */
    public RGraph() {
        _rootNode = new RNode();
        _subGraphs = new ArrayList<RGraph>();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _modularityFactor = 0;
        _efficiencyArray = new ArrayList<Double>();
        _reactions = 0;
    }

    /**
     * SDSGraph constructor, specified root node *
     */
    public RGraph(RNode node) {
        _rootNode = node;
        _subGraphs = new ArrayList();
        _stages = 0;
        _steps = 0;
        _recCnt = 0;
        _disCnt = 0;
        _modularityFactor = 0;
        _efficiencyArray = new ArrayList();
        _reactions = 0;
    }

    /**
     * Clone method for an SDSGraph *
     */
    @Override
    public RGraph clone() {
        RGraph clone = new RGraph();
        clone._rootNode = this._rootNode.clone();
        clone._recCnt = this._recCnt;
        clone._disCnt = this._disCnt;
        clone._stages = this._stages;
        clone._steps = this._steps;
        clone._modularityFactor = this._modularityFactor;
        clone._efficiencyArray = this._efficiencyArray;
        clone._reactions = this._reactions;
        return clone;
    }

    /**
     * Pin a graph - pin and set steps to 0 *
     */
    public void pin() {
        this._pinned = true;
        this._steps = 0;
    }

    /**
     * Return all parts in this graph *
     */
    public ArrayList<Part> getPartsInGraph(Collector coll) {
        ArrayList<Part> toReturn = new ArrayList<Part>();
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());
        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);
            Part toAdd = coll.getPart(current.getUUID(), true);
            if (toAdd != null) {
                toReturn.add(toAdd);
            }
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return toReturn;
    }

    /**
     * Return all parts in this graph *
     */
    public HashMap<Part, Vector> getPartVectorsInGraph(Collector coll) {
        HashMap<Part, Vector> toReturn = new HashMap();
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());
        
        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);
            
            Part toAdd = coll.getPart(current.getUUID(), true);
            if (toAdd != null) {
                if (current.getVector() != null) {

                    toReturn.put(toAdd, coll.getVector(current.getVector().getUUID(), true));
                } else {
                    toReturn.put(toAdd, null);
                }
            }
            
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return toReturn;
    }

    /**
     * Return all vectors in this graph *
     */
    public ArrayList<Vector> getVectorsInGraph(Collector coll) {
        ArrayList<Vector> toReturn = new ArrayList<Vector>();
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());
        while (!queue.isEmpty()) {
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);
            if (current.getVector() != null) {
                Vector toAdd = coll.getVector(current.getVector().getUUID(), true);
                if (toAdd != null) {
                    toReturn.add(toAdd);
                }
            }
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return toReturn;
    }

    /**
     * Merge all graphs from a set of graphs *
     */
    public static ArrayList<RGraph> mergeGraphs(ArrayList<RGraph> graphs) {

        ArrayList<RGraph> mergedGraphs = new ArrayList();
        HashMap<String, RNode> mergedNodesHash = new HashMap();

        //Traverse and merge graphs
        for (int i = 0; i < graphs.size(); i++) {

            RGraph aGraph = graphs.get(i);
            boolean hasParent = true;

            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList();
            queue.add(aGraph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);

                //Get forward and reverse part key string
                String currentCompDirOHStage = current.getNodeKey("+") + "|" + current.getStage();
                String currentCompDirOHStageRev = current.getNodeKey("-") + "|" + current.getStage();

                //If a node with this composition, overhangs and stage has not been seen before
                if (mergedNodesHash.containsKey(currentCompDirOHStage) == false && mergedNodesHash.containsKey(currentCompDirOHStageRev) == false) {
                    mergedNodesHash.put(currentCompDirOHStage, current);

                    for (RNode neighbor : current.getNeighbors()) {
                        if (!seenNodes.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }

                    //If it has been seen merge the node in the hash and disconnect this node from solution
                } else {

                    RNode finalNode;
                    if (mergedNodesHash.containsKey(currentCompDirOHStageRev)) {
                        finalNode = mergedNodesHash.get(currentCompDirOHStageRev);
                    } else {
                        finalNode = mergedNodesHash.get(currentCompDirOHStage);
                    }
                    ArrayList<RNode> neighbors = current.getNeighbors();

                    //Remove parent from current node's neighbors, add it to the hashed node's nieghbors
                    hasParent = false;
                    for (int j = 0; j < neighbors.size(); j++) {
                        if (neighbors.get(j).getStage() > current.getStage()) {
                            RNode parent = neighbors.get(j);
                            hasParent = true;
                            parent.replaceNeighbor(current, finalNode);
                            finalNode.addNeighbor(parent);
                            current.removeNeighbor(parent);
                        }
                    }

                    //Edge case where multiple goal parts have the same composition
                    if (hasParent == false) {
                        
                        ArrayList<RNode> fNeighbors = finalNode.getNeighbors();
                        for (int k = 0; k < neighbors.size(); k++) {
                            current.replaceNeighbor(neighbors.get(k), fNeighbors.get(k));
                            neighbors.get(k).removeNeighbor(current);
                            fNeighbors.get(k).addNeighbor(current);
                        }
                    }
                }
            }

            if (hasParent == true) {
                mergedGraphs.add(aGraph);
            }
        }

        //Remove graphs that have identical nodes to ones already seen from returned set
        HashSet<RNode> seenNodes = new HashSet();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        ArrayList<RGraph> remGraphs = new ArrayList<RGraph>();

        for (RGraph graph : mergedGraphs) {
            queue.add(graph.getRootNode());
            boolean newNodes = seenNodes.add(graph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);

                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        queue.add(neighbor);
                        newNodes = true;
                    }
                }
            }

            if (newNodes == false) {
                remGraphs.add(graph);
            }
        }

        mergedGraphs.removeAll(remGraphs);
        return mergedGraphs;
    }

    /**
     * Get graph statistics *
     */
    public static void getGraphStats(ArrayList<RGraph> allGraphs, ArrayList<Part> partLib, ArrayList<Vector> vectorLib, HashSet<String> recommended, HashSet<String> discouraged, boolean scarless, Double stepCost, Double stepTime, Double pcrCost, Double pcrTime) {
        //don't count library parts and vectors 
        HashSet<String> seenPartKeys = getExistingPartKeys(partLib);
        HashSet<String> seenVectorKeys = getExistingVectorKeys(vectorLib);

        //Will get stats for a set of graphs and assign the values to the individual graphs
        for (int i = 0; i < allGraphs.size(); i++) {

            int numPCRs = 0;
            int steps = 0;
            int recCount = 0;
            int disCount = 0;
            int stages = 0;
            boolean addStage = false;
            int shared = 0;
            ArrayList<Double> efficiency = new ArrayList();
            RGraph currentGraph = allGraphs.get(i);
            HashSet<RNode> seenNodes = new HashSet();
            ArrayList<RNode> queue = new ArrayList();
            queue.add(currentGraph.getRootNode());

            //Traverse the graph
            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);
                int numParents = 0;

                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) { //TODO: FIX APPLIED HERE
                        queue.add(neighbor);
                    }
                    if (neighbor.getStage() > current.getStage()) {
                        numParents++;
                    }
                }

                ArrayList<String> composition = current.getComposition();
                String currentPartKey = current.getNodeKey("+");

                String currentVectorKey = "";
                if (current.getVector() != null) {
                    currentVectorKey = current.getVector().getVectorKey("+");
                }

                if (!currentVectorKey.equals("")) {
                    //If there is a vector present on a stage zero node, and both part and vector do not yet exist ,it is considered a step 
                    if (current.getStage() == 0) {
                        if (!seenVectorKeys.contains(currentVectorKey) || !seenPartKeys.contains(currentPartKey)) {
                            addStage = true;
                            steps++;
                            if (numParents > 1) {
                                shared++;
                            }

                        }
                    }
                    //If a vector with this composition and overhangs doesn't exist, there must be a PCR done
                    if (!seenVectorKeys.contains(currentVectorKey)) {
                        numPCRs++;
                    }
                }
                //If a part with this composition and overhangs doesn't exist, there must be a PCR done                
                if (current.getStage() == 0 && !seenPartKeys.contains(currentPartKey)) {
                    numPCRs++;
                }

                //If the node is grater than stage 0, it is a step and add to efficiency list
                if (current.getStage() > 0 && !seenPartKeys.contains(currentPartKey)) {
                    steps++;
                    efficiency.add(current.getEfficiency());
                    if (numParents > 1) {
                        shared++;
                    }
                }

                //Save max stage
                if (current.getStage() > stages) {
                    stages = current.getStage();
                }

                //Add it to recommended count if it's recommended
                if (recommended.contains(current.getComposition().toString())) {
                    recCount++;
                }

                //Add it to discouraged count if it's discouraged
                if (discouraged.contains(current.getComposition().toString())) {
                    disCount++;
                }
                seenPartKeys.add(currentPartKey);
                seenVectorKeys.add(currentVectorKey);
            }

            currentGraph.setReactions(numPCRs);

            if (addStage == true) {
                stages++;
            }

            //Estimated time and cost
            double estCost = (steps * stepCost) + (pcrCost * numPCRs);
            double estTime = (stages * stepTime) + pcrTime;

            currentGraph.setSteps(steps);
            currentGraph.setDiscouragedCount(disCount);
            currentGraph.setReccomendedCount(recCount);
            currentGraph.setStages(stages);
            currentGraph.setEfficiencyArray(efficiency);
            currentGraph.setSharing(shared);
            currentGraph.setEstCost(estCost);
            currentGraph.setEstTime(estTime);
        }
    }

    /**
     * Returns a part library and finds all forward and reverse characteristics
     * of each part *
     */
    public static HashSet<String> getExistingPartKeys(ArrayList<Part> partLib) {

        HashSet<String> keys = new HashSet();

        //Go through parts library, put all compositions into hash of things that already exist
        for (Part aPart : partLib) {

            //Get forward and reverse part key string
            ArrayList<Part> partComp = aPart.getComposition();
            ArrayList<String> comp = new ArrayList();
            for (int j = 0; j < partComp.size(); j++) {
                String name = partComp.get(j).getName();
                comp.add(name);
            }

            ArrayList<String> searchTags = aPart.getSearchTags();
            RNode node = new RNode(false, false, comp, ClothoReader.parseTags(searchTags, "Direction:"), null, ClothoReader.parseTags(searchTags, "Scars:"), aPart.getLeftOverhang(), aPart.getRightOverhang(), 0, 0, null);
            keys.add(node.getNodeKey("+"));
            keys.add(node.getNodeKey("-"));
        }

        return keys;
    }

    /**
     * Returns a vector library and finds all forward and reverse characteristics
     * of each part *
     */
    public static HashSet<String> getExistingVectorKeys(ArrayList<Vector> vectorLib) {

        HashSet<String> startVectorsLOlevelRO = new HashSet<String>();

        //Go through vectors library, put all compositions into hash of things that already exist
        for (Vector aVec : vectorLib) {

            String lOverhang = aVec.getLeftOverhang();
            String rOverhang = aVec.getRightOverhang();
            int stage = aVec.getLevel();
            String name = aVec.getName();
            RVector vector = new RVector(lOverhang, rOverhang, stage, name, null);
            startVectorsLOlevelRO.add(vector.getVectorKey("+"));
            startVectorsLOlevelRO.add(vector.getVectorKey("-"));
        }  

        return startVectorsLOlevelRO;
    }
    
    /**
     * Make keys for part, vector pair that are both in the library
     */
    public static HashSet<String> getExistingPairs (HashMap<Part, Vector> compPartsVectors) {
        
        //Loop though all part keys and build map for parts and vectors
        Set<Part> parts = compPartsVectors.keySet();
        HashSet<String> keys = new HashSet<String>();

        for (Part part : parts) {

            Vector aVec = compPartsVectors.get(part);
            String vName = null;
            if (aVec != null) {
                vName = aVec.getName();
            }

            //Get forward and reverse part key string
            ArrayList<Part> partComp = part.getComposition();
            ArrayList<String> comp = new ArrayList();
            for (int j = 0; j < partComp.size(); j++) {
                String name = partComp.get(j).getName();
                comp.add(name);
            }

            ArrayList<String> searchTags = part.getSearchTags();
            RNode node = new RNode(false, false, comp, ClothoReader.parseTags(searchTags, "Direction:"), null, ClothoReader.parseTags(searchTags, "Scars:"), part.getLeftOverhang(), part.getRightOverhang(), 0, 0, null);
            String nodeKey = node.getNodeKey("+");
            String revNodeKey = node.getNodeKey("-");

            keys.add(nodeKey + "|" + vName);
            keys.add(revNodeKey + "|" + vName);

        }
        return keys;
    }



    /**
     * ************************************************************************
     *
     * GRAPH EXPORT METHODS
     *
     *************************************************************************
     */
    /**
     * Get all the edges of an SDSGraph in Post Order *
     */
    public ArrayList<String> getPostOrderEdges() {
        ArrayList<String> edges = new ArrayList();
        HashSet<String> seenUUIDs = new HashSet();
        seenUUIDs.add(this._rootNode.getUUID());

        //Start at the root node and look at all children
        for (RNode neighbor : this._rootNode.getNeighbors()) {
            seenUUIDs.add(neighbor.getUUID());
            edges = getPostOrderEdgesHelper(neighbor, this._rootNode, edges, seenUUIDs, true);
        }
        //first edge is the vector
        if (this._rootNode.getVector() != null) {
            edges.add(0, this._rootNode.getUUID() + " -> " + this._rootNode.getVector().getUUID());
        }
//        edges.add("node -> vector");
        return edges;
    }

    /**
     * Return graph edges in an order specified for puppetshow *
     */
    private ArrayList<String> getPostOrderEdgesHelper(RNode current, RNode parent, ArrayList<String> edges, HashSet<String> seenUUIDs, boolean recurse) {
        ArrayList<String> edgesToAdd = new ArrayList();

        //Do a recursive call if there are unseen neighbors
        if (recurse) {

            //For all of this node's neighbors
            for (RNode neighbor : current.getNeighbors()) {

                //If the neighbor's composition is not that of the parent
                if (!parent.getUUID().equals(neighbor.getUUID())) {

                    //If this neighbor's composition hasn't been seen before, add it to the seen composition list and do a recursive call of this node, this time as this node being the parent
                    if (!seenUUIDs.contains(neighbor.getUUID())) {
                        seenUUIDs.add(neighbor.getUUID());
                        edges = getPostOrderEdgesHelper(neighbor, current, edges, seenUUIDs, true);

                        //If this neighbor has been seen, do not recursively call
                    } else {
                        edges = getPostOrderEdgesHelper(neighbor, current, edges, seenUUIDs, false);
                    }
                }
            }
        }

        //For all current neighbors... this is always done on any call
        for (RNode neighbor : current.getNeighbors()) {

            //Write arc connecting to the parent
            if (neighbor.getComposition().toString().equals(parent.getComposition().toString())) {
                if (current.getStage() != 0) {
                    if (current.getVector() != null) {
                        edgesToAdd.add(current.getUUID() + " -> " + current.getVector().getUUID());
                    }
                }
                //Make the edge going in the direction of the node with the greatest composition, whether this is parent or child
                if (current.getComposition().size() > neighbor.getComposition().size()) {
                    edgesToAdd.add(current.getUUID() + " -> " + neighbor.getUUID());
                } else if (current.getComposition().size() < neighbor.getComposition().size()) {
                    edgesToAdd.add(neighbor.getUUID() + " -> " + current.getUUID());
                }
            }
        }
        for (String s : edgesToAdd) {
            edges.add(s);
        }
        return edges;
    }

    /**
     * Print edges arc file *
     */
    public String printArcsFile(Collector coll, ArrayList<String> edges, String method) {

        //Build String for export
        //Header
        StringBuilder arcsText = new StringBuilder();
        DateFormat dateFormat = new SimpleDateFormat("MMddyyyy@HHmm");
        Date date = new Date();
        arcsText.append("# AssemblyMethod: " + method + "\n# ").append(" ").append(dateFormat.format(date)).append("\n");
        arcsText.append("# ").append(coll.getPart(this._rootNode.getUUID(), true)).append("\n");
        arcsText.append("# ").append(this._rootNode.getUUID()).append("\n\n");

        //Build arc file 
        HashMap<String, String> nodeMap = new HashMap<String, String>();//key is uuid, value is name
        for (String s : edges) {
            String[] tokens = s.split("->");
            String vertex1Name = null;
            String vertex2Name = null;
            String vertex1UUID = tokens[0].trim();
            String vertex2UUID = tokens[1].trim();

            if (coll.getPart(vertex1UUID, true) != null) {
                vertex1Name = coll.getPart(vertex1UUID, true).getName();
            }
            if (coll.getPart(vertex2UUID, true) != null) {
                vertex2Name = coll.getPart(vertex2UUID, true).getName();
            }
            if (vertex1Name == null) {
                vertex1Name = coll.getVector(vertex1UUID, true).getName();
            }
            if (vertex2Name == null) {
                vertex2Name = coll.getVector(vertex2UUID, true).getName();
            }
            nodeMap.put(vertex1UUID, vertex1Name);
            nodeMap.put(vertex2UUID, vertex2Name);
            arcsText.append("# ").append(vertex1Name).append(" -> ").append(vertex2Name).append("\n");
            arcsText.append(s).append("\n");
        }

        //Build key
        Stack<RNode> stack = new Stack<RNode>();
        HashSet<RNode> seenNodes = new HashSet<RNode>();
        HashMap<String, String> compositionHash = new HashMap<String, String>();
        stack.add(this._rootNode);
        while (!stack.isEmpty()) {
            RNode current = stack.pop();
            seenNodes.add(current);
            compositionHash.put(current.getUUID(), current.getComposition().toString());
            if (current.getVector() != null) {
                compositionHash.put(current.getVector().getUUID(), current.getVector().getName());
            }
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    stack.add(neighbor);
                }
            }
        }
        arcsText.append("\n# Key\n");
        for (String key : nodeMap.keySet()) {
            arcsText.append("# ").append(nodeMap.get(key)).append("\n");
            arcsText.append("# ").append(key).append("\n");
            String compositionString = compositionHash.get(key);
            arcsText.append("# (").append(compositionString.substring(1, compositionString.length() - 1)).append(")\n");
        }
        return arcsText.toString();
    }

    /**
     * Generate a Weyekin image file for a this graph *
     */
    public String generateWeyekinFile(ArrayList<Part> partLib, ArrayList<Vector> vectorLib, HashMap<Part, Vector> compPartsVectors, ArrayList<RNode> goalPartNodes, boolean scarless, String method) {

        //Initiate weyekin file
        StringBuilder weyekinText = new StringBuilder();
        String edgeLines = "";
        weyekinText.append("digraph {\n");

        HashSet<RNode> seenNodes = new HashSet<RNode>();
        ArrayList<RNode> queue = new ArrayList<RNode>();
        queue.add(this.getRootNode());

        HashSet<String> gpComps = new HashSet<String>();
        for (RNode rootNode : goalPartNodes) {
            gpComps.add(rootNode.getComposition().toString());
        }

        HashSet<String> startPartsLOcompRO = getExistingPartKeys(partLib);
        HashSet<String> startVectorsLOlevelRO = getExistingVectorKeys(vectorLib);
        HashSet<String> startPartVectorPairs = getExistingPairs(compPartsVectors);

        //Traverse the graph
        while (!queue.isEmpty()) {

            String pigeonLine;
            RNode current = queue.get(0);
            seenNodes.add(current);
            queue.remove(0);

            RVector vector = current.getVector();
            String vecName = null;
            if (vector != null) {
                vecName = vector.getName();
            }

            ArrayList<String> composition = current.getComposition();
            ArrayList<String> type = current.getType();
            ArrayList<String> scars = current.getScars();
            ArrayList<String> direction = current.getDirection();
            String lOverhang = current.getLOverhang();
            String rOverhang = current.getROverhang();
            String nodeID = composition + "|" + direction + "|" + scars + "|" + lOverhang + "|" + rOverhang + "|" + vecName;

            if (scarless) {
                if (gpComps.contains(composition.toString())) {
                    if (vecName == null) {
                        vecName = "";
                    }
                    pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, nodeID, "", "", vecName);
                    weyekinText.append(pigeonLine);
                } else {
                    pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, nodeID, lOverhang, rOverhang, vecName);
                    weyekinText.append(pigeonLine);
                }
            } else {
                pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, nodeID, lOverhang, rOverhang, vecName);
                weyekinText.append(pigeonLine);
            }

            //If there needs to be new level-0 reactions and or vectors
            if (!startPartVectorPairs.contains(nodeID)) {

                //Add PCR edges for level 0 nodes
                if (current.getStage() == 0) {

                    boolean basicNode = false;
                    String nodeIDB = composition + "|" + direction + "|" + scars + "|" + lOverhang + "|" + rOverhang;

                    //If the original node had no vector, 'null' was added to the string and this must be corrected and no redundant edges should be added                                
                    if (nodeID.endsWith("null")) {
                        if (!nodeIDB.equals(nodeID.substring(0, nodeID.length() - 5))) {
                            edgeLines = edgeLines + "\"" + nodeIDB + "\"" + " -> " + "\"" + nodeID + "\"" + "\n";
                            pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, nodeIDB, lOverhang, rOverhang, null);
                            weyekinText.append(pigeonLine.toString());
                        } else {
                            basicNode = true;
                        }
                    } else {
                        edgeLines = edgeLines + "\"" + nodeIDB + "\"" + " -> " + "\"" + nodeID + "\"" + "\n";
                        pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, nodeIDB, lOverhang, rOverhang, null);
                        weyekinText.append(pigeonLine.toString());
                    }

                    if (!startPartsLOcompRO.contains(nodeIDB)) {
                        if (basicNode == true) {
                            nodeIDB = nodeID;
                        }
                        String NnodeID = composition + "|" + direction + "|" + scars;
                        edgeLines = edgeLines + "\"" + NnodeID + "\"" + " -> " + "\"" + nodeIDB + "\"" + "\n";
                        pigeonLine = generatePigeonCodeOld(composition, type, direction, scars, NnodeID, null, null, null);
                        weyekinText.append(pigeonLine.toString());
                    }
                }

                //Get vector, make an extra edge if a PCR is required
                if (vector != null) {

                    String vecLO = vector.getLOverhang();
                    String vecRO = vector.getROverhang();
                    int vecL = vector.getLevel();
                    ArrayList<String> vecComposition = new ArrayList<String>();
                    ArrayList<String> vecTypes = new ArrayList<String>();
                    ArrayList<String> vecDirection = new ArrayList<String>();
                    vecComposition.add("lacZ");
                    vecTypes.add("lacZ");
                    vecDirection.add("+");
                    String vecID = vecName + "|" + vecLO + "|" + vecL + "|" + vecRO;
                    edgeLines = edgeLines + "\"" + vecID + "\"" + " -> " + "\"" + nodeID + "\"" + "\n";
                    
                    //For MoClo and Golden Gate, destination vectors are made and they will show a lacZ PCR and destination vector as opposed to a PCRed vector
                    if (method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("goldengate")) {
                        pigeonLine = generatePigeonCodeOld(vecComposition, vecTypes, vecDirection, new ArrayList<String>(), vecID, vecLO, vecRO, vecName);
                    } else {
                        pigeonLine = generatePigeonCodeOld(null, null, null, null, vecID, vecLO, vecRO, vecName);
                    }
                    weyekinText.append(pigeonLine.toString());

                    if (!startVectorsLOlevelRO.contains(vecID)) {
                        String NvecID = vecName + "|" + vecL;
                        edgeLines = edgeLines + "\"" + NvecID + "\"" + " -> " + "\"" + vecID + "\"" + "\n";
                        
                        if (method.equalsIgnoreCase("moclo") || method.equalsIgnoreCase("goldengate")) {
                            pigeonLine = generatePigeonCodeOld(vecComposition, vecTypes, vecDirection, new ArrayList<String>(), NvecID, null, null, null);
                        } else {
                            pigeonLine = generatePigeonCodeOld(null, null, null, null, NvecID, null, null, vecName);
                        }
                        weyekinText.append(pigeonLine.toString());
                    }
                }
            }

            //Add unseen neighbors to the queue
            for (RNode neighbor : current.getNeighbors()) {
                if (!seenNodes.contains(neighbor)) {
                    if (!queue.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }

                //If one of the neighbors is a parent, add an edge
                if (neighbor.getStage() > current.getStage()) {

                    RVector vectorN = neighbor.getVector();
                    String vecNameN = null;
                    if (vectorN != null) {
                        vecNameN = vectorN.getName();
                    }

                    ArrayList<String> compositionN = neighbor.getComposition();
                    ArrayList<String> directionN = neighbor.getDirection();
                    ArrayList<String> scarsN = neighbor.getScars();
                    String lOverhangN = neighbor.getLOverhang();
                    String rOverhangN = neighbor.getROverhang();
                    String nodeIDN = compositionN + "|" + directionN + "|" + scarsN + "|" + lOverhangN + "|" + rOverhangN + "|" + vecNameN;
                    edgeLines = edgeLines + "\"" + nodeID + "\"" + " -> " + "\"" + nodeIDN + "\"" + "\n";
                }
            }
        }
            
        //Write edge lines
        weyekinText.append(edgeLines);
        weyekinText.append("}");
        return weyekinText.toString();
    }

    //returns a json string that can be parsed by the client
    public static JSONObject generateD3Graph(ArrayList<RGraph> graphs, ArrayList<Part> partLib, ArrayList<Vector> vectorLib) throws Exception {
        HashMap<String, String> imageURLs = new HashMap();
        HashSet<String> edgeSet = new HashSet();
        int nodeCount = 0;
        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();
        HashSet<String> startPartsLOcompRO = getExistingPartKeys(partLib);
        HashSet<String> startVectorsLOlevelRO = getExistingVectorKeys(vectorLib);
        for (RGraph graph : graphs) {
            HashSet<RNode> seenNodes = new HashSet<RNode>();
            ArrayList<RNode> queue = new ArrayList<RNode>();
            queue.add(graph.getRootNode());

            while (!queue.isEmpty()) {
                RNode current = queue.get(0);
                seenNodes.add(current);
                queue.remove(0);

                RVector vector = current.getVector();
                String vecName = "";
                if (vector != null) {
                    vecName = vector.getName();
                }

                ArrayList<String> composition = current.getComposition();
                ArrayList<String> type = current.getType();
                ArrayList<String> scars = current.getScars();
                ArrayList<String> direction = current.getDirection();
                String lOverhang = current.getLOverhang();
                String rOverhang = current.getROverhang();
                String nodeID = composition + "|" + direction + "|" + scars + "|" + lOverhang + "|" + rOverhang + "|" + vecName;
                imageURLs.put(nodeID, generatePigeonCode(composition, type, direction, scars, lOverhang, rOverhang, vecName));

                //Add PCR edges for level 0 nodes
                if (current.getStage() == 0) {

                    boolean basicNode = false;
                    String nodeIDB = composition + "|" + direction + "|" + scars + "|" + lOverhang + "|" + rOverhang;

                    //If the original node had no vector, 'null' was added to the string and this must be corrected and no redundant edges should be added
                    if (!nodeIDB.equals(nodeID.substring(0, nodeID.length() - 5))) {
                        edgeSet.add("\"" + nodeIDB + "\"" + " -> " + "\"" + nodeID + "\"");
                        imageURLs.put(nodeIDB, generatePigeonCode(composition, type, direction, scars, lOverhang, rOverhang, null));

                    } else {
                        basicNode = true;
                    }

                    if (!startPartsLOcompRO.contains(nodeIDB)) {
                        if (basicNode == true) {
                            nodeIDB = nodeID;
                        }
                        String NnodeID = composition + "|" + direction + "|" + scars;
                        edgeSet.add("\"" + NnodeID + "\"" + " -> " + "\"" + nodeIDB + "\"");
                        imageURLs.put(NnodeID, generatePigeonCode(composition, type, direction, scars, null, null, null));
                    }
                }

                //Get vector, make an extra edge if a PCR is required
                if (vector != null) {
                    String vecLO = vector.getLOverhang();
                    String vecRO = vector.getROverhang();
                    int vecL = vector.getLevel();
                    String vecID = vecName + "|" + vecLO + "|" + vecL + "|" + vecRO;
                    edgeSet.add("\"" + vecID + "\"" + " -> " + "\"" + nodeID + "\"");
                    imageURLs.put(vecID, generatePigeonCode(null, null, null, null, vecLO, vecRO, vecName));

                    if (!startVectorsLOlevelRO.contains(vecID)) {
                        String NvecID = vecName + "|" + vecL;
                        edgeSet.add("\"" + NvecID + "\"" + " -> " + "\"" + vecID + "\"" + "\n");
                        imageURLs.put(NvecID, generatePigeonCode(null, null, null, null, null, null, vecName));
                    }
                }

                //Add unseen neighbors to the queue
                for (RNode neighbor : current.getNeighbors()) {
                    if (!seenNodes.contains(neighbor)) {
                        if (!queue.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                    //If one of the neighbors is a parent, add an edge
                    if (neighbor.getStage() > current.getStage()) {

                        RVector vectorN = neighbor.getVector();
                        String vecNameN = null;
                        if (vectorN != null) {
                            vecNameN = vectorN.getName();
                        }
                        ArrayList<String> compositionN = neighbor.getComposition();
                        ArrayList<String> directionN = neighbor.getDirection();
                        ArrayList<String> scarsN = neighbor.getScars();
                        String lOverhangN = neighbor.getLOverhang();
                        String rOverhangN = neighbor.getROverhang();
                        String nodeIDN = compositionN + "|" + directionN + "|" + scarsN + "|" + lOverhangN + "|" + rOverhangN + "|" + vecNameN;
                        edgeSet.add("\"" + nodeID + "\"" + " -> " + "\"" + nodeIDN + "\"");
                    }
                }
            }

        }
        JSONObject graphData = new JSONObject();
//        graphData.put("edges", edgeSet);
//        graphData.put("images", imageURLs);
        graphData.put("edges", edges);
        graphData.put("nodes", nodes);
        return graphData;

    }

    private static String generatePigeonCode(ArrayList<String> composition, ArrayList<String> types, ArrayList<String> direction, ArrayList<String> scars, String LO, String RO, String vecName) {

        StringBuilder pigeonLine = new StringBuilder();
        //Assign left overhang if it exists                
//        pigeonLine.append("3 ").append(LO).append("\n");
        if (LO != null) {
            pigeonLine.append("5 ").append(LO).append("\n");
        }

        if (composition != null) {
            for (int i = 0; i < composition.size(); i++) {

                String name = composition.get(i);
                String type = types.get(i);
                String dir = "";

                //Turn direction of glyph in reverse if reverse direction
                if (!direction.isEmpty()) {
                    dir = direction.get(i);
                    if ("-".equals(dir)) {
                        pigeonLine.append("<");
                    }
                }

                //Write pigeon code for a recognized regular part type
                if (type.equalsIgnoreCase("promoter") || type.equalsIgnoreCase("p")) {
                    pigeonLine.append("P ").append(name).append(" 4" + "\n");
                } else if (type.equalsIgnoreCase("RBS") || type.equalsIgnoreCase("r")) {
                    pigeonLine.append("r ").append(name).append(" 5" + "\n");
                } else if (type.equalsIgnoreCase("gene") || type.equalsIgnoreCase("g")) {
                    pigeonLine.append("c ").append(name).append(" 1" + "\n");
                } else if (type.equalsIgnoreCase("reporter") || type.equalsIgnoreCase("gr")) {
                    pigeonLine.append("c ").append(name).append(" 2" + "\n");
                } else if (type.equalsIgnoreCase("terminator") || type.equalsIgnoreCase("t")) {
                    pigeonLine.append("T ").append(name).append(" 6" + "\n");
                } else if (type.equalsIgnoreCase("invertase site") || type.equalsIgnoreCase("is")) {
                    if ("-".equals(dir)) {
                        pigeonLine.append(" ").append(name).append(" 12" + "\n");
                    } else {
                        pigeonLine.append("> ").append(name).append(" 12" + "\n");
                    }
                } else if (type.equalsIgnoreCase("spacer") || type.equalsIgnoreCase("s")) {
                    pigeonLine.append("s ").append(name).append(" 10" + "\n");
                } else if (type.equalsIgnoreCase("origin") || type.equalsIgnoreCase("o")) {
                    pigeonLine.append("z ").append(name).append(" 14" + "\n");
                } else if (type.equalsIgnoreCase("fusion") || type.equalsIgnoreCase("fu")) {
                    pigeonLine.append("f1");
                    String[] fusionParts = name.split("-");
                    for (int j = 1; j < fusionParts.length; j++) {
                        int color = j % 13 + 1;
                        pigeonLine.append("-").append(color);
                    }
                    pigeonLine.append(" ").append(name).append("\n");
                } else {
                    pigeonLine.append("c ").append(name).append(" 13" + "\n");
                }

                //Scars
                if (!scars.isEmpty()) {
                    if (i < composition.size() - 1) {
                        if (!"_".equals(scars.get(i))) {
                            pigeonLine.append("= ").append(scars.get(i)).append(" 14" + "\n");
                        }
                    }
                }
            }
        }

        //Assign right overhang                
        if (RO != null) {
            pigeonLine.append("3 ").append(RO).append("\n");
        }
//        pigeonLine.append("5 ").append(RO).append("\n");

        //Vectors
        if (vecName != null) {
            pigeonLine.append("v ").append(vecName).append("\n");
        }
        pigeonLine.append("# Arcs\n");
        WeyekinPoster.setPigeonText(pigeonLine.toString());
        WeyekinPoster.postMyBird();
        return WeyekinPoster.getmPigeonURI().toString();
    }

    /**
     * Pigeon code generation *
     */
    @Deprecated
    public static String generatePigeonCodeOld(ArrayList<String> composition, ArrayList<String> types, ArrayList<String> direction, ArrayList<String> scars, String compLORO, String LO, String RO, String vecName) {

        StringBuilder pigeonLine = new StringBuilder();
        pigeonLine.append("PIGEON_START\n");
        pigeonLine.append("\"").append(compLORO).append("\"\n");

        //Assign left overhang if it exists                
//        pigeonLine.append("3 ").append(LO).append("\n");
        if (LO != null) {
            if (!LO.isEmpty()) {
                pigeonLine.append("5 ").append(LO).append("\n");
            }
        }

        if (composition != null) {
            for (int i = 0; i < composition.size(); i++) {

                String name = composition.get(i);
                String type = types.get(i);
                String dir = "";

                //Turn direction of glyph in reverse if reverse direction
                if (!direction.isEmpty()) {
                    dir = direction.get(i).trim();
                    if ("-".equals(dir)) {
                        pigeonLine.append("<");
                    }
                }

                //Write pigeon code for a recognized regular part type
                if (type.equalsIgnoreCase("promoter") || type.equalsIgnoreCase("p")) {
                    pigeonLine.append("P ").append(name).append(" 4" + "\n");
                } else if (type.equalsIgnoreCase("RBS") || type.equalsIgnoreCase("r")) {
                    pigeonLine.append("r ").append(name).append(" 5" + "\n");
                } else if (type.equalsIgnoreCase("gene") || type.equalsIgnoreCase("g")) {
                    pigeonLine.append("c ").append(name).append(" 1" + "\n");
                } else if (type.equalsIgnoreCase("reporter") || type.equalsIgnoreCase("rep")) {
                    pigeonLine.append("c ").append(name).append(" 9" + "\n");
                } else if (type.equalsIgnoreCase("lacZ") || type.equalsIgnoreCase("l")) {
                    pigeonLine.append("c ").append(name).append(" 2" + "\n");
                } else if (type.equalsIgnoreCase("resistance") || type.equalsIgnoreCase("res")) {
                    pigeonLine.append("g ").append(name).append(" 2" + "\n");
                } else if (type.equalsIgnoreCase("terminator") || type.equalsIgnoreCase("t")) {
                    pigeonLine.append("T ").append(name).append(" 6" + "\n");
                } else if (type.equalsIgnoreCase("invertase site") || type.equalsIgnoreCase("ins")) {
                    if ("-".equals(dir)) {
                        pigeonLine.append(" ").append(name).append(" 12" + "\n");
                    } else {
                        pigeonLine.append("> ").append(name).append(" 12" + "\n");
                    }
                } else if (type.equalsIgnoreCase("spacer") || type.equalsIgnoreCase("s")) {
                    pigeonLine.append("s ").append(name).append(" 10" + "\n");
                } else if (type.equalsIgnoreCase("origin") || type.equalsIgnoreCase("o")) {
                    pigeonLine.append("z ").append(name).append(" 14" + "\n");
                } else if (type.equalsIgnoreCase("fusion") || type.equalsIgnoreCase("fus")) {
                    pigeonLine.append("f1");
                    String[] fusionParts = name.split("-");
                    for (int j = 1; j < fusionParts.length; j++) {
                        int color = j % 13 + 1;
                        pigeonLine.append("-").append(color);
                    }
                    pigeonLine.append(" ").append(name).append("\n");
                } else {
                    if ("-".equals(dir)) {
                        pigeonLine.deleteCharAt(pigeonLine.length() - 1);
                    }
                    pigeonLine.append("? ").append(name).append(" 13" + "\n");
                }

                //Scars
                if (!scars.isEmpty()) {
                    if (i < composition.size() - 1) {
                        if (!"_".equals(scars.get(i))) {
                            pigeonLine.append("= ").append(scars.get(i)).append(" 14" + "\n");
                        }
                    }
                }
            }
        }

        //Assign right overhang                
        if (RO != null) {
            if (!RO.isEmpty()) {
                pigeonLine.append("3 ").append(RO).append("\n");
            }
        }
//        pigeonLine.append("5 ").append(RO).append("\n");

        //Vectors
        if (vecName != null) {
            pigeonLine.append("v ").append(vecName).append("\n");
        }
        pigeonLine.append("# Arcs\n");
        pigeonLine.append("PIGEON_END\n\n");
        return pigeonLine.toString();
    }

    /**
     * Merge multiple arc files into one file with one graph *
     */
    public static String mergeArcFiles(ArrayList<String> inputFiles) {
        String outFile = "";
        String header = "";

        //Grab the header from the first file; first two lines of header should be the same for all of the files
        String[] firstFileLines = inputFiles.get(0).split("\n"); //should split file into separate lines
        for (int i = 0; i < 2; i++) {
            header = header + firstFileLines[i] + "\n";
        }
        ArrayList<String> keyLines = new ArrayList<String>(); //stores the lines in all of the keys
        HashSet<String> seenArcLines = new HashSet(); //stores arc lines

        //Iterate through each arc file; each one is represented by a string
        for (String inputFile : inputFiles) {
            String[] lines = inputFile.split("\n"); //should split file into separate lines
            boolean seenKey = false;

            //Apend to the header
            for (int j = 2; j < 4; j++) {
                header = header + lines[j] + "\n";
            }

            //Apend to the key section
            for (int k = 4; k < lines.length; k++) {//first 4 lines are the header
                if (lines[k].contains("# Key")) {

                    //Once this line appears, store the following lines (which are lines of the key) into the keyLines arrayList.
                    seenKey = true;
                }
                if (seenKey) {

                    //If the key file doesnt have the current line in the current key, add it
                    if (!keyLines.contains(lines[k])) {
                        keyLines.add(lines[k]);
                    }
                } else {

                    //If the line isn't an empty line
                    if (lines[k].length() > 0 && !seenArcLines.contains(lines[k])) {
                        outFile = outFile + lines[k] + "\n";
                        seenArcLines.add(lines[k]);
                    }
                }
            }
        }

        //Apend key to toReturn
        outFile = outFile + "\n";
        for (int l = 0; l < keyLines.size(); l++) {
            outFile = outFile + keyLines.get(l) + "\n";
        }

        //Add header to toReturn
        outFile = header + "\n" + outFile;
        return outFile;
    }

    /**
     * Merge multiple graphviz files into one file with one graph *
     */
    public static String mergeWeyekinFiles(ArrayList<String> filesToMerge) {

        //Repeated edges should only appear in the same graph; an edge that appears in one graph should not appear in another
        String mergedFile = "";
        HashSet<String> seenLines = new HashSet<String>();
        HashSet<ArrayList<String>> seenEdges = new HashSet<ArrayList<String>>();
        ArrayList<String> edgeList = new ArrayList<String>();

        //For each file to merge
        for (String graphFile : filesToMerge) {

            String[] fileLines = graphFile.split("\n");
            HashSet<String> currentSeenLines = new HashSet<String>();
            boolean keepGoing = false;
            boolean lookAtNext = false;

            //For all the lines in each file
            for (int i = 1; i < fileLines.length - 1; i++) {

                //If this is the line directly after PIGEON_START
                if (lookAtNext) {

                    //If the line PIGEON_END is reached, include it and go on to larger if statement in next iteration
                    if (fileLines[i].equalsIgnoreCase("PIGEON_END")) {
                        if (keepGoing) {
                            mergedFile = mergedFile + "PIGEON_END\n\n";
                        }
                        lookAtNext = false;
                        keepGoing = false;
                        continue;
                    }

                    //If the name of the pigeon node hasn't been seen before, save it and all other lines until PIGEON_END
                    if (keepGoing) {
                        mergedFile = mergedFile + fileLines[i] + "\n";
                    } else if (!seenLines.contains(fileLines[i])) {
                        mergedFile = mergedFile + "PIGEON_START\n" + fileLines[i] + "\n";
                        keepGoing = true;
                    }

                    //All things needed multiple times, never stop them from being added to merged file
                } else if (!(fileLines[i].equalsIgnoreCase("digraph {") || fileLines[i].equalsIgnoreCase("}") || fileLines[i].equalsIgnoreCase("\n"))) {

                    //If the line hasn't been seen before
                    if (!seenLines.contains(fileLines[i])) {

                        //If there appears to be an edge
                        if (fileLines[i].contains("->") || fileLines[i].contains("<-")) {
                            ArrayList<String> nodePair = new ArrayList<String>();
                            String[] twoNodesA = fileLines[i].split(" <- ");
                            String[] twoNodesB = fileLines[i].split(" -> ");
                            if (twoNodesA.length == 2) {
                                nodePair.add(twoNodesA[0]);
                                nodePair.add(twoNodesA[1]);
                            } else if (twoNodesB.length == 2) {
                                nodePair.add(twoNodesB[0]);
                                nodePair.add(twoNodesB[1]);
                            }

                            //Search to see if a pair with these edges exists in another file in the merge in reverse order
                            boolean seenPair = false;
                            for (ArrayList<String> edges : seenEdges) {
                                if (edges.contains(nodePair.get(0)) && edges.contains(nodePair.get(1))) {
                                    seenPair = true;
                                }
                            }

                            //If the pair has already been seen
                            if (!seenPair) {
                                seenEdges.add(nodePair);
                                edgeList.add(fileLines[i]);
                            }
                        } else {

                            //Append this line to the merged file
                            mergedFile = mergedFile + fileLines[i] + "\n";
                        }

                        //Add this line to the lines seen in this file
                        currentSeenLines.add(fileLines[i]);
                    } else if (fileLines[i].equalsIgnoreCase("PIGEON_START")) {
                        lookAtNext = true;
                    }
                }
            }

            Iterator<String> iterator = currentSeenLines.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                seenLines.add(next);
            }
        }

        for (String edge : edgeList) {
            mergedFile = mergedFile + edge + "\n";
        }
        mergedFile = "digraph{\n" + mergedFile + "\n}";

        return mergedFile;
    }

    /**
     * ************************************************************************
     *
     * GETTER AND SETTER METHODS
     *
     *************************************************************************
     */
    /**
     * Add a subgraph to a graph *
     */
    public void addSubgraph(RGraph graph) {
        _subGraphs.add(graph);
    }

    /**
     * Get graph root node *
     */
    public RNode getRootNode() {
        return _rootNode;
    }

    /**
     * Find how many stages for a given SDSGraph *
     */
    public int getStages() {
        return _stages;
    }

    /**
     * Find how many steps for a given SDSGraph *
     */
    public int getSteps() {
        return _steps;
    }

    /**
     * Find how many recommended intermediates for a given SDSGraph *
     */
    public int getReccomendedCount() {
        return _recCnt;
    }

    /**
     * Find how many discouraged intermediates for a given SDSGraph *
     */
    public int getDiscouragedCount() {
        return _disCnt;
    }

    /**
     * Determine if the graph in question is pinned *
     */
    public boolean getPinned() {
        return _pinned;
    }

    /**
     * Find sharing score for a given SDSGraph *
     */
    public int getModularityFactor() {
        return _modularityFactor;
    }

    /**
     * Get the number of shared steps in a graph
     */
    public int getSharing() {
        return _sharedSteps;
    }

    /**
     * Get all subgraphs of this graph *
     */
    public ArrayList<RGraph> getSubGraphs() {
        return _subGraphs;
    }

    /**
     * Get the array of efficiency scores for all nodes of a graph *
     */
    public ArrayList<Double> getEfficiencyArray() {
        return _efficiencyArray;
    }

    /**
     * Get the average efficiency score of a graph *
     */
    public double getAveEff() {

        ArrayList<Double> efficiencyArray = this.getEfficiencyArray();
        double sumEff = 0;
        double aveEff;
        for (int i = 0; i < efficiencyArray.size(); i++) {
            sumEff = sumEff + efficiencyArray.get(i);
        }
        aveEff = sumEff / efficiencyArray.size();
        return aveEff;
    }

    /**
     * Get the reaction score of a graph *
     */
    public int getReaction() {
        return _reactions;
    }

    /**
     * Get the estimated graph time *
     */
    public double getEstTime() {
        return _estTime;
    }

    /**
     * Get the estimated graph cost *
     */
    public double getEstCost() {
        return _estCost;
    }

    /**
     * Set the number of stages for an SDSGraph *
     */
    public void setStages(int stages) {
        _stages = stages;
    }

    /**
     * Set the number of steps for an SDSGraph *
     */
    public void setSteps(int steps) {
        _steps = steps;
    }

    /**
     * Set the number of recommended intermediates for an SDSGraph *
     */
    public void setReccomendedCount(int count) {
        _recCnt = count;
    }

    /**
     * Set the number of recommended intermediates for an SDSGraph *
     */
    public void setDiscouragedCount(int count) {
        _disCnt = count;
    }

    /**
     * Set graph root node *
     */
    public void setRootNode(RNode newRoot) {
        _rootNode = newRoot;
    }

    /**
     * Find sharing score for a given SDSGraph *
     */
    public void setModularityFactor(int modularity) {
        _modularityFactor = modularity;
    }

    /**
     * Set the number of shared steps in a graph
     */
    public void setSharing(int sharing) {
        _sharedSteps = sharing;
    }

    /**
     * Get all subgraphs of this graph *
     */
    public void setSubGraphs(ArrayList<RGraph> subGraphs) {
        _subGraphs = subGraphs;
    }

    /**
     * Set the efficiency score of a graph *
     */
    public void setEfficiencyArray(ArrayList<Double> efficiency) {
        _efficiencyArray = efficiency;
    }

    /**
     * Set the reaction score of a graph *
     */
    public void setReactions(int numReactions) {
        _reactions = numReactions;
    }

    /**
     * Set the estimated graph time *
     */
    public void setEstTime(Double estTime) {
        _estTime = estTime;
    }

    /**
     * Set the estimated graph time *
     */
    public void setEstCost(Double estCost) {
        _estCost = estCost;
    }
    //FIELDS
    private ArrayList<RGraph> _subGraphs;
    private RNode _rootNode;
    private int _stages;
    private int _steps;
    private int _sharedSteps;
    private ArrayList<Double> _efficiencyArray;
    private int _recCnt;
    private int _disCnt;
    private int _modularityFactor;
    private int _reactions;
    private double _estCost;
    private double _estTime;
    private boolean _pinned;
}
