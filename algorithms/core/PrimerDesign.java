/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.algorithms.core;

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
    public static int getPrimerHomologyLength(Double meltingTemp, Integer targetLength, Integer maxHomolLength, Integer minHomolLength, String sequence, boolean fivePrime) {

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
                    
                    //If the sequence is becoming longer than max, return this length
                    if (length >= maxHomolLength) {
                        return length;
                    }
                    
                    candidateSeq = sequence.substring(0, length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {
                while (candidateTemp > meltingTemp) {
                    
                    //If the length is less than minimum, return it
                    if (length <= minHomolLength) {
                        return targetLength;
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
                    
                    //If the sequence length is under this length, return the whole sequence length
                    if (sequence.length() < length) {
                        return sequence.length();
                    }
                    
                    //If the sequence is becoming longer than max, return this length
                    if (length >= maxHomolLength) {
                        return length;
                    }
                    
                    candidateSeq = sequence.substring(sequence.length() - length);
                    candidateTemp = getMeltingTemp(candidateSeq);
                }

            //Remove base pairs until candidate temp reaches the desired temp if too high
            } else if (candidateTemp > meltingTemp) {

                //Call the melting temperature function
                while (candidateTemp > meltingTemp) {
                    
                    //If the length is less than minimum, return it
                    if (length <= minHomolLength) {
                        return targetLength;
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
     * Logic for going from OH variable place holders to actual sequences for MoClo *
     */
    public static HashMap<String, String> getMoCloOHseqs() {

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
        overhangVariableSequenceHash.put("21", "cggc");
        overhangVariableSequenceHash.put("22", "aaga");
        overhangVariableSequenceHash.put("23", "tacg");
        overhangVariableSequenceHash.put("24", "ctaa");
        overhangVariableSequenceHash.put("25", "gtgc");
        overhangVariableSequenceHash.put("26", "gact");
        overhangVariableSequenceHash.put("27", "actc");
        overhangVariableSequenceHash.put("28", "aagt");
        overhangVariableSequenceHash.put("29", "cctg");
        overhangVariableSequenceHash.put("30", "ccag");
        overhangVariableSequenceHash.put("31", "ttga");
        overhangVariableSequenceHash.put("32", "ttac");
        overhangVariableSequenceHash.put("33", "agca");
        overhangVariableSequenceHash.put("34", "gaat");
        overhangVariableSequenceHash.put("35", "ccta");
        overhangVariableSequenceHash.put("36", "gtcc");
        overhangVariableSequenceHash.put("37", "tgag");
        overhangVariableSequenceHash.put("38", "aggc");
        overhangVariableSequenceHash.put("39", "cact");
        overhangVariableSequenceHash.put("40", "gcga");
        overhangVariableSequenceHash.put("41", "attg");
        overhangVariableSequenceHash.put("42", "taac");
        overhangVariableSequenceHash.put("43", "ggct");
        overhangVariableSequenceHash.put("44", "ccga");
        overhangVariableSequenceHash.put("45", "tatg");
        overhangVariableSequenceHash.put("46", "atac");
        overhangVariableSequenceHash.put("47", "gcgt");
        overhangVariableSequenceHash.put("48", "cgca");
        overhangVariableSequenceHash.put("49", "tctg");
        overhangVariableSequenceHash.put("50", "atag");
        overhangVariableSequenceHash.put("51", "catc");
        overhangVariableSequenceHash.put("52", "gtga");
        overhangVariableSequenceHash.put("53", "tgat");
        overhangVariableSequenceHash.put("54", "aact");
        overhangVariableSequenceHash.put("55", "ggtc");
        overhangVariableSequenceHash.put("56", "ctcg");
        overhangVariableSequenceHash.put("57", "acga");
        overhangVariableSequenceHash.put("58", "taca");
        overhangVariableSequenceHash.put("59", "gcag");
        overhangVariableSequenceHash.put("60", "cggt");
        overhangVariableSequenceHash.put("61", "tatc");
        overhangVariableSequenceHash.put("62", "atga");
        overhangVariableSequenceHash.put("63", "ggat");
        overhangVariableSequenceHash.put("64", "cttg");
        overhangVariableSequenceHash.put("65", "tacc");
        overhangVariableSequenceHash.put("66", "acgg");
        overhangVariableSequenceHash.put("67", "gctc");
        overhangVariableSequenceHash.put("68", "cgaa");
        overhangVariableSequenceHash.put("69", "atct");
        overhangVariableSequenceHash.put("70", "taag");
        overhangVariableSequenceHash.put("71", "gcca");
        overhangVariableSequenceHash.put("72", "cgtc");
        overhangVariableSequenceHash.put("73", "atgt");
        overhangVariableSequenceHash.put("74", "tcca");
        overhangVariableSequenceHash.put("75", "cagc");
        overhangVariableSequenceHash.put("76", "gtag");
        overhangVariableSequenceHash.put("77", "tgtc");
        overhangVariableSequenceHash.put("78", "ccat");
        overhangVariableSequenceHash.put("79", "gatt");
        overhangVariableSequenceHash.put("80", "ttca");
        overhangVariableSequenceHash.put("81", "agac");
        overhangVariableSequenceHash.put("82", "cacg");
        overhangVariableSequenceHash.put("83", "gggt");
        overhangVariableSequenceHash.put("84", "atta");
        overhangVariableSequenceHash.put("85", "tccg");
        overhangVariableSequenceHash.put("86", "gggc");
        overhangVariableSequenceHash.put("87", "acaa");
        overhangVariableSequenceHash.put("88", "cttt");
        overhangVariableSequenceHash.put("89", "aagg");
        overhangVariableSequenceHash.put("90", "tatt");
        overhangVariableSequenceHash.put("91", "ggcg");
        overhangVariableSequenceHash.put("92", "ccaa");
        overhangVariableSequenceHash.put("93", "ttcc");
        overhangVariableSequenceHash.put("94", "tggt");
        overhangVariableSequenceHash.put("95", "ccac");
        overhangVariableSequenceHash.put("96", "gttg");
        overhangVariableSequenceHash.put("97", "aaac");
        overhangVariableSequenceHash.put("98", "ggtg");
        overhangVariableSequenceHash.put("99", "ccca");
        overhangVariableSequenceHash.put("100", "gtgt");
        overhangVariableSequenceHash.put("101", "taaa");
        overhangVariableSequenceHash.put("102", "cggg");
        overhangVariableSequenceHash.put("103", "attt");
        overhangVariableSequenceHash.put("104", "tccc");
        overhangVariableSequenceHash.put("105", "agaa");
        overhangVariableSequenceHash.put("106", "ccct");
        overhangVariableSequenceHash.put("107", "gagg");
        overhangVariableSequenceHash.put("108", "tttc");
        overhangVariableSequenceHash.put("109", "agag");
        overhangVariableSequenceHash.put("110", "cttc");
        overhangVariableSequenceHash.put("111", "gaga");
        overhangVariableSequenceHash.put("112", "tcct");
        overhangVariableSequenceHash.put("113", "gcgg");
        overhangVariableSequenceHash.put("114", "ataa");
        overhangVariableSequenceHash.put("115", "tgtt");
        overhangVariableSequenceHash.put("116", "aacc");
        overhangVariableSequenceHash.put("117", "tttg");
//        overhangVariableSequenceHash.put("118", "aatt");
//        overhangVariableSequenceHash.put("119", "ggcc");        
//        overhangVariableSequenceHash.put("120", "ctag");
//        overhangVariableSequenceHash.put("121", "tcga");
//        overhangVariableSequenceHash.put("122", "gtac");
//        overhangVariableSequenceHash.put("123", "tgca");
//        overhangVariableSequenceHash.put("124", "agct");
//        overhangVariableSequenceHash.put("125", "gatc");
//        overhangVariableSequenceHash.put("126", "acgt");
//        overhangVariableSequenceHash.put("127", "catg");
        
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
        overhangVariableSequenceHash.put("14*", "gttc");
        overhangVariableSequenceHash.put("15*", "tcag");
        overhangVariableSequenceHash.put("16*", "ctgt");
        overhangVariableSequenceHash.put("17*", "gcta");
        overhangVariableSequenceHash.put("18*", "cgat");
        overhangVariableSequenceHash.put("19*", "actg");
        overhangVariableSequenceHash.put("20*", "ttgc");
        overhangVariableSequenceHash.put("21*", "gccg");
        overhangVariableSequenceHash.put("22*", "tctt");
        overhangVariableSequenceHash.put("23*", "cgta");
        overhangVariableSequenceHash.put("24*", "ttag");
        overhangVariableSequenceHash.put("25*", "gcac");
        overhangVariableSequenceHash.put("26*", "agtc");
        overhangVariableSequenceHash.put("27*", "gagt");
        overhangVariableSequenceHash.put("28*", "actt");
        overhangVariableSequenceHash.put("29*", "cagg");
        overhangVariableSequenceHash.put("30*", "ctgg");
        overhangVariableSequenceHash.put("31*", "tcaa");
        overhangVariableSequenceHash.put("32*", "gtaa");
        overhangVariableSequenceHash.put("33*", "tgct");
        overhangVariableSequenceHash.put("34*", "attc");
        overhangVariableSequenceHash.put("35*", "tagg");
        overhangVariableSequenceHash.put("36*", "ggac");
        overhangVariableSequenceHash.put("37*", "ctca");
        overhangVariableSequenceHash.put("38*", "gcct");
        overhangVariableSequenceHash.put("39*", "agtg");
        overhangVariableSequenceHash.put("40*", "tcgc");
        overhangVariableSequenceHash.put("41*", "caat");
        overhangVariableSequenceHash.put("42*", "gtta");
        overhangVariableSequenceHash.put("43*", "agcc");
        overhangVariableSequenceHash.put("44*", "tcgg");
        overhangVariableSequenceHash.put("45*", "cata");
        overhangVariableSequenceHash.put("46*", "gtat");
        overhangVariableSequenceHash.put("47*", "acgc");
        overhangVariableSequenceHash.put("48*", "tgcg");
        overhangVariableSequenceHash.put("49*", "caga");
        overhangVariableSequenceHash.put("50*", "ctat");
        overhangVariableSequenceHash.put("51*", "gatg");
        overhangVariableSequenceHash.put("52*", "tcac");
        overhangVariableSequenceHash.put("53*", "atca");
        overhangVariableSequenceHash.put("54*", "agtt");
        overhangVariableSequenceHash.put("55*", "gacc");
        overhangVariableSequenceHash.put("56*", "cgag");
        overhangVariableSequenceHash.put("57*", "tcgt");
        overhangVariableSequenceHash.put("58*", "tgta");
        overhangVariableSequenceHash.put("59*", "ctgc");
        overhangVariableSequenceHash.put("60*", "accg");
        overhangVariableSequenceHash.put("61*", "gata");
        overhangVariableSequenceHash.put("62*", "tcat");
        overhangVariableSequenceHash.put("63*", "atcc");
        overhangVariableSequenceHash.put("64*", "caag");
        overhangVariableSequenceHash.put("65*", "ggta");
        overhangVariableSequenceHash.put("66*", "ccgt");
        overhangVariableSequenceHash.put("67*", "gagc");
        overhangVariableSequenceHash.put("68*", "ttcg");
        overhangVariableSequenceHash.put("69*", "agat");
        overhangVariableSequenceHash.put("70*", "ctta");
        overhangVariableSequenceHash.put("71*", "tggc");
        overhangVariableSequenceHash.put("72*", "gacg");
        overhangVariableSequenceHash.put("73*", "acat");
        overhangVariableSequenceHash.put("74*", "tgga");
        overhangVariableSequenceHash.put("75*", "gctg");
        overhangVariableSequenceHash.put("76*", "ctac");
        overhangVariableSequenceHash.put("77*", "gaca");
        overhangVariableSequenceHash.put("78*", "atgg");
        overhangVariableSequenceHash.put("79*", "aatc");
        overhangVariableSequenceHash.put("80*", "tgaa");
        overhangVariableSequenceHash.put("81*", "gtct");
        overhangVariableSequenceHash.put("82*", "cgtg");
        overhangVariableSequenceHash.put("83*", "accc");
        overhangVariableSequenceHash.put("84*", "taat");
        overhangVariableSequenceHash.put("85*", "cgga");
        overhangVariableSequenceHash.put("86*", "gccc");
        overhangVariableSequenceHash.put("87*", "ttgt");
        overhangVariableSequenceHash.put("88*", "aaag");
        overhangVariableSequenceHash.put("89*", "cctt");
        overhangVariableSequenceHash.put("90*", "aata");
        overhangVariableSequenceHash.put("91*", "cgcc");
        overhangVariableSequenceHash.put("92*", "ttgg");
        overhangVariableSequenceHash.put("93*", "ggaa");
        overhangVariableSequenceHash.put("94*", "acca");
        overhangVariableSequenceHash.put("95*", "gtgg");
        overhangVariableSequenceHash.put("96*", "caac");
        overhangVariableSequenceHash.put("97*", "gttt");
        overhangVariableSequenceHash.put("98*", "cacc");
        overhangVariableSequenceHash.put("99*", "tggg");
        overhangVariableSequenceHash.put("100*", "acac");
        overhangVariableSequenceHash.put("101*", "ttta");
        overhangVariableSequenceHash.put("102*", "cccg");
        overhangVariableSequenceHash.put("103*", "aaat");
        overhangVariableSequenceHash.put("104*", "ggga");
        overhangVariableSequenceHash.put("105*", "ttct");
        overhangVariableSequenceHash.put("106*", "aggg");
        overhangVariableSequenceHash.put("107*", "cctc");
        overhangVariableSequenceHash.put("108*", "gaaa");
        overhangVariableSequenceHash.put("109*", "ctct");
        overhangVariableSequenceHash.put("110*", "gaag");
        overhangVariableSequenceHash.put("111*", "tctc");
        overhangVariableSequenceHash.put("112*", "agga");
        overhangVariableSequenceHash.put("113*", "ccgc");
        overhangVariableSequenceHash.put("114*", "ttat");
        overhangVariableSequenceHash.put("115*", "aaca");
        overhangVariableSequenceHash.put("116*", "ggtt");
        overhangVariableSequenceHash.put("117*", "caaa");
//        overhangVariableSequenceHash.put("118*", "aatt");
//        overhangVariableSequenceHash.put("119*", "ggcc");
//        overhangVariableSequenceHash.put("120*", "ctag");
//        overhangVariableSequenceHash.put("121*", "tcga");
//        overhangVariableSequenceHash.put("122*", "gtac");
//        overhangVariableSequenceHash.put("123*", "tgca");
//        overhangVariableSequenceHash.put("124*", "agct");
//        overhangVariableSequenceHash.put("125*", "gatc");
//        overhangVariableSequenceHash.put("126*", "acgt");
//        overhangVariableSequenceHash.put("127*", "catg");
        
        return overhangVariableSequenceHash;
    }
    
    public static HashMap<String, String> getGatewayGibsonOHseqs() {
        
        HashMap<String, String> overhangVariableSequenceHash = new HashMap<String, String>();

        overhangVariableSequenceHash.put("UNS1", "ggtttaccgagctcttattggttttcaaacttcattgactgtgcc");
        overhangVariableSequenceHash.put("UNS2", "ggtgcgtttttatgcttgtagtattgtataatgtttttaagatcc");
        overhangVariableSequenceHash.put("UNS3", "ggtctaatacccaatctctcgtcttatccagatgttttatacgcc");
        overhangVariableSequenceHash.put("UNS4", "ggtgaattcccttatgtgagtgtaaaaggcaggcgagtttgtccc");
        overhangVariableSequenceHash.put("UNS5", "ggttgcttgcaaaagcagtaattggaaagcactctcaaagaatcc");
        overhangVariableSequenceHash.put("UNS6", "ggtagataagttgatttagccataaaatattgtttccgtgacccc");
        overhangVariableSequenceHash.put("UNS7", "ggttctgagtcacggcttcattggcattccgtacaacgaacgtcc");
        overhangVariableSequenceHash.put("UNS8", "ggtcctcagagagcctatagcggtaaaacaacaccatgcatcccc");
        overhangVariableSequenceHash.put("UNS9", "ggtcgcagtcgcttcctgtaaatagagcattaaattccatagtcc");
        overhangVariableSequenceHash.put("UNS10", "ggtacttaatcgaaaaaaaaacagacagccatgtgctcttcggcc");
        overhangVariableSequenceHash.put("UNS11", "ggtggacatgattatggaacacacacacgctagccgcccagttcc");
        overhangVariableSequenceHash.put("UNSX", "ggtatcactagtattacagaggtaagttataacagtcgcctaacc");
        
        overhangVariableSequenceHash.put("UNS1*", "ggcacagtcaatgaagtttgaaaaccaataagagctcggtaaacc");
        overhangVariableSequenceHash.put("UNS2*", "ggatcttaaaaacattatacaatactacaagcataaaaacgcacc");
        overhangVariableSequenceHash.put("UNS3*", "ggcgtataaaacatctggataagacgagagattgggtattagacc");
        overhangVariableSequenceHash.put("UNS4*", "gggacaaactcgcctgccttttacactcacataagggaattcacc");
        overhangVariableSequenceHash.put("UNS5*", "ggattctttgagagtgctttccaattactgcttttgcaagcaacc");
        overhangVariableSequenceHash.put("UNS6*", "ggggtcacggaaacaatattttatggctaaatcaacttatctacc");
        overhangVariableSequenceHash.put("UNS7*", "ggacgttcgttgtacggaatgccaatgaagccgtgactcagaacc");
        overhangVariableSequenceHash.put("UNS8*", "ggggatgcatggtgttgttttaccgctataggctctctgaggacc");
        overhangVariableSequenceHash.put("UNS9*", "ggactatggaatttaatgctctatttacaggaagcgactgcgacc");
        overhangVariableSequenceHash.put("UNS10*", "ggccgaagagcacatggctgtctgtttttttttcgattaagtacc");
        overhangVariableSequenceHash.put("UNS11*", "ggaactgggcggctagcgtgtgtgtgttccataatcatgtccacc");
        
        overhangVariableSequenceHash.put("attB4", "caactttgtatagaaaagttg");
        overhangVariableSequenceHash.put("attB1", "caagtttgtacaaaaaagcag");
        overhangVariableSequenceHash.put("attB2", "ccactttgtacaagaaagctg");
        overhangVariableSequenceHash.put("attR1", "tcgggccccaaataatgattttattttgactgatagtgacctgttcgttgcaacaaattgatgagcaatgcttttttataatgccaactttgtacaaaaaagcaggctga");
        overhangVariableSequenceHash.put("attR2", "tacccagctttcttgtacaaagttggcattataagaaagcattgcttatcaatttgttgcaacgaacaggtcactatcagtcaaaataaaatcattatttg");
        overhangVariableSequenceHash.put("attR4", "caaataatgattttattttgactgatagtgacctgttcgttgcaacaaattgataagcaatgcttttttataatgccaactttgtatagaaaagttg");
        overhangVariableSequenceHash.put("attL1", "caagtttgtacaaaaaagttgaacgagaaacgtaaaatgatataaatatcaatatattaaattagattttgcataaaaaacagactacataatactgtaaaacacaacatatgcagtcactatga");
        overhangVariableSequenceHash.put("attL2", "caactttgtatagaaaagttgaacgagaaacgtaaaatgatataaatatcaatatattaaattagattttgcataaaaaacagactacataatactgtaaaacacaacatatccagtcactatgg");
        overhangVariableSequenceHash.put("attL4", "catagtgactggatatgttgtgttttacagtattatgtagtctgttttttatgcaaaatctaatttaatatattgatatttatatcattttacgtttctcgttcagctttcttgtacaaagtggt");
        
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
