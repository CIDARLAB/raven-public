/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller.algorithms;

import java.util.HashMap;

/**
 *
 * @author evanappleton
 */
public class PrimerDesign {

    /**
     * ************************************************************************
     *
     * PRIMER DESIGN
     *
     *************************************************************************
     */
    public static String reverseComplement(String seq) {
//        String lSeq = seq.toLowerCase();
        String lSeq = seq;
        String revComplement = "";
        for (int i = 0; i < lSeq.length(); i++) {
            if (lSeq.charAt(i) == 'a') {
                revComplement = "t" + revComplement;
            } else if (lSeq.charAt(i) == 'g') {
                revComplement = "c" + revComplement;
            } else if (lSeq.charAt(i) == 'c') {
                revComplement = "g" + revComplement;
            } else if (lSeq.charAt(i) == 't') {
                revComplement = "a" + revComplement;
            } else if (lSeq.charAt(i) == 'w') {
                revComplement = "w" + revComplement;
            } else if (lSeq.charAt(i) == 's') {
                revComplement = "s" + revComplement;
            } else if (lSeq.charAt(i) == 'm') {
                revComplement = "k" + revComplement;
            } else if (lSeq.charAt(i) == 'k') {
                revComplement = "m" + revComplement;
            } else if (lSeq.charAt(i) == 'r') {
                revComplement = "y" + revComplement;
            } else if (lSeq.charAt(i) == 'y') {
                revComplement = "r" + revComplement;
            } else if (lSeq.charAt(i) == 'b') {
                revComplement = "v" + revComplement;
            } else if (lSeq.charAt(i) == 'd') {
                revComplement = "h" + revComplement;
            } else if (lSeq.charAt(i) == 'h') {
                revComplement = "d" + revComplement;
            } else if (lSeq.charAt(i) == 'v') {
                revComplement = "b" + revComplement;
            } else if (lSeq.charAt(i) == 'n') {
                revComplement = "n" + revComplement;
            } else if (lSeq.charAt(i) == 'A') {
                revComplement = "T" + revComplement;
            } else if (lSeq.charAt(i) == 'G') {
                revComplement = "C" + revComplement;
            } else if (lSeq.charAt(i) == 'C') {
                revComplement = "G" + revComplement;
            } else if (lSeq.charAt(i) == 'T') {
                revComplement = "A" + revComplement;
            } else if (lSeq.charAt(i) == 'W') {
                revComplement = "W" + revComplement;
            } else if (lSeq.charAt(i) == 'S') {
                revComplement = "S" + revComplement;
            } else if (lSeq.charAt(i) == 'M') {
                revComplement = "K" + revComplement;
            } else if (lSeq.charAt(i) == 'K') {
                revComplement = "M" + revComplement;
            } else if (lSeq.charAt(i) == 'R') {
                revComplement = "Y" + revComplement;
            } else if (lSeq.charAt(i) == 'Y') {
                revComplement = "R" + revComplement;
            } else if (lSeq.charAt(i) == 'B') {
                revComplement = "V" + revComplement;
            } else if (lSeq.charAt(i) == 'D') {
                revComplement = "H" + revComplement;
            } else if (lSeq.charAt(i) == 'H') {
                revComplement = "D" + revComplement;
            } else if (lSeq.charAt(i) == 'V') {
                revComplement = "B" + revComplement;
            } else {
                revComplement = "N" + revComplement;
            }
        }
        return revComplement;
    }

    //calculates the length of homology required for primers based on nearest neighbor calculations
    public static int getPrimerHomologyLength(Double meltingTemp, Integer targetLength, String sequence, boolean fivePrime, boolean forceLength) {

        //If no melting temp is input, return the given length
        if (meltingTemp == null) {
            if (sequence.length() < targetLength) {
                return sequence.length();
            } else {
                return targetLength;
            }
        }

        //If the sequence length is under this length, return the whole sequence length
        if (sequence.length() < targetLength) {
            return sequence.length();
        }
        
        int length = targetLength;
        
        //If determining the homology of the five prime side of a part
        if (fivePrime) {

            String candidateSeq = sequence.substring(0, length);
            double candidateTemp = getMeltingTemp(candidateSeq);
            
            //Add base pairs until candidate temp reaches the desired temp if too low
            if (candidateTemp < meltingTemp) {
                while (candidateTemp < meltingTemp) {
                    length++;
                    
                    //If the sequence length is under this length, return the whole sequence length
                    if (sequence.length() < length) {
                        return sequence.length();
                    }
                    candidateSeq = sequence.substring(0, length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {

                while (candidateTemp > meltingTemp) {
                    
                    //If the input length is forced, candidate length is under this length, return the target length
                    if (forceLength) {
                        if (length <= targetLength) {
                            return targetLength;
                        }
                    }

                    length--;
                    candidateSeq = sequence.substring(0, length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }
            }

        //If determining the homology length of the three prime side
        } else {

            String candidateSeq = sequence.substring(sequence.length() - length);
            double candidateTemp = getMeltingTemp(candidateSeq);

            //Add base pairs until candidate temp reaches the desired temp if too low
            if (candidateTemp < meltingTemp) {
                
                while (candidateTemp < meltingTemp) {
                    length++;
                    if (sequence.length() < length) {
                        return sequence.length();
                    }
                    candidateSeq = sequence.substring(sequence.length() - length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {

                //Call the melting temperature function
                while (candidateTemp > meltingTemp) {
                    
                    //If the input length is forced, candidate length is under this length, return the target length
                    if (forceLength) {
                        if (length <= targetLength) {
                            return targetLength;
                        }
                    }
                    
                    length--;
                    candidateSeq = sequence.substring(sequence.length() - length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }
            }
        }
        
        //Extra check to make sure no indexing errors occur
        if (sequence.length() > length) {
            return length;
        } else {
            length = sequence.length();
        }
        return length;
    }

    /**
     * Logic for going from OH variable place holders to actual sequences *
     */
    public static HashMap<String, String> getModularOHseqs() {

        HashMap<String, String> overhangVariableSequenceHash = new HashMap<String, String>();
        overhangVariableSequenceHash.put("0", "ggag");
        overhangVariableSequenceHash.put("1", "tact");
        overhangVariableSequenceHash.put("2", "aatg");
        overhangVariableSequenceHash.put("3", "aggt");
        overhangVariableSequenceHash.put("4", "gctt");
        overhangVariableSequenceHash.put("5", "cgct");
        overhangVariableSequenceHash.put("6", "tgcc");
        overhangVariableSequenceHash.put("7", "acta");
        overhangVariableSequenceHash.put("8", "tcta");
        overhangVariableSequenceHash.put("9", "cgac");
        overhangVariableSequenceHash.put("10", "cgtt");
        overhangVariableSequenceHash.put("11", "tgtg");
        overhangVariableSequenceHash.put("12", "atgc");
        overhangVariableSequenceHash.put("13", "gtca");
        overhangVariableSequenceHash.put("14", "gaac");
        overhangVariableSequenceHash.put("15", "ctga");
        overhangVariableSequenceHash.put("16", "acag");
        overhangVariableSequenceHash.put("17", "tagc");
        overhangVariableSequenceHash.put("18", "atcg");
        overhangVariableSequenceHash.put("19", "cagt");
        overhangVariableSequenceHash.put("20", "gcaa");
        overhangVariableSequenceHash.put("0*", "ctcc");
        overhangVariableSequenceHash.put("1*", "agta");
        overhangVariableSequenceHash.put("2*", "catt");
        overhangVariableSequenceHash.put("3*", "acct");
        overhangVariableSequenceHash.put("4*", "aagc");
        overhangVariableSequenceHash.put("5*", "agcg");
        overhangVariableSequenceHash.put("6*", "ggca");
        overhangVariableSequenceHash.put("7*", "tagt");
        overhangVariableSequenceHash.put("8*", "taga");
        overhangVariableSequenceHash.put("9*", "gtcg");
        overhangVariableSequenceHash.put("10*", "aacg");
        overhangVariableSequenceHash.put("11*", "caca");
        overhangVariableSequenceHash.put("12*", "gcat");
        overhangVariableSequenceHash.put("13*", "tgac");
        overhangVariableSequenceHash.put("14*", "gtcc");
        overhangVariableSequenceHash.put("15*", "tcag");
        overhangVariableSequenceHash.put("16*", "ctgt");
        overhangVariableSequenceHash.put("17*", "gcta");
        overhangVariableSequenceHash.put("18*", "cgat");
        overhangVariableSequenceHash.put("19*", "actg");
        overhangVariableSequenceHash.put("20*", "ttgc");
        return overhangVariableSequenceHash;
    }

    public static double getMeltingTemp(String sequence) {

        /* Resources:
         * http://en.wikipedia.org/wiki/DNA_melting#Nearest-neighbor_method
         * http://www.basic.northwestern.edu/biotools/oligocalc.html
         * http://dna.bio.puc.cl/cardex/servers/dnaMATE/tm-pred.html
         */

        String seq = sequence;
        int length = sequence.length();
        double concP = 10 * java.lang.Math.pow(10, -9);
        double dH = 0;
        double dS = 0;
        double R = 1.987;
        double temp;
        String pair;
        seq = seq.toUpperCase();

        // Checks terminal base pairs
        char init = seq.charAt(0);
        if (init == 'G' || init == 'C') {
            dH += 0.1;
            dS += -2.8;
        } else if (init == 'A' || init == 'T') {
            dH += 2.3;
            dS += 4.1;
        }
        init = seq.charAt(length - 1);
        if (init == 'G' || init == 'C') {
            dH += 0.1;
            dS += -2.8;
        } else if (init == 'A' || init == 'T') {
            dH += 2.3;
            dS += 4.1;
        }

        // Checks nearest neighbor pairs
        for (int i = 0; i < length - 1; i++) {
            pair = seq.substring(i, i + 2);
            if (pair.equals("AA") || pair.equals("TT")) {
                dH += -7.9;
                dS += -22.2;
            } else if (pair.equals("AG") || pair.equals("CT")) {
                dH += -7.8;
                dS += -21.0;
            } else if (pair.equals("AT")) {
                dH += -7.2;
                dS += -20.4;
            } else if (pair.equals("AC") || pair.equals("GT")) {
                dH += -8.4;
                dS += -22.4;
            } else if (pair.equals("GA") || pair.equals("TC")) {
                dH += -8.2;
                dS += -22.2;
            } else if (pair.equals("GG") || pair.equals("CC")) {
                dH += -8.0;
                dS += -19.9;
            } else if (pair.equals("GC")) {
                dH += -9.8;
                dS += -24.4;
            } else if (pair.equals("TA")) {
                dH += -7.2;
                dS += -21.3;
            } else if (pair.equals("TG") || pair.equals("CA")) {
                dH += -8.5;
                dS += -22.7;
            } else if (pair.equals("CG")) {
                dH += -10.6;
                dS += -27.2;
            }
        }

        // Checks for self-complementarity
        int mid;
        if (length % 2 == 0) {
            mid = length / 2;
            if (seq.substring(0, mid).equals(reverseComplement(seq.substring(mid+1)))) {
                dS += -1.4;
            }
        } else {
            mid = (length - 1) / 2;
            if (seq.substring(0, mid).equals(reverseComplement(seq.substring(mid+1)))) {
                dS += -1.4;
            }
        }

        // dH is in kCal, dS is in Cal - equilibrating units
        dH = dH * 1000;

        double logCt = java.lang.Math.log(concP/4);
        temp = (dH / (dS + (R * logCt))) - 273.15;

        //return temp;
        return temp;
    }
}
