package hpr.util;


/**
 * Knuth-Morris-Pratt Algorithm for Pattern Matching
 */


public class KMPMatch {

    /**
     * Finds the first occurrence of the pattern in the text.
     */
    static public int indexOf(byte[] data, int beginPos, int length, byte[] pattern) {
        int[] failure = computeFailure(pattern);

        if (data.length == 0) 
        	return -1;

        if (data.length < beginPos + length) {
   //     System.out.println(data.length +" " + beginPos + " " + length);
        	return -1;
        }
        
        int j = 0;
        for (int i = beginPos; i < beginPos + length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
 //       System.out.println("!" + data.length +" " + beginPos + " " + length);
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    static private int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}