package nlp.cws.utility;

public class ContextItem {
	public int nKey; // The key word
	public int[][] aContextArray; // The context array
	public int[] aTagFreq; // The total number a tag appears
	public int nTotalFreq; // The total number of all the tags

	public ContextItem next; // The chain pointer to next Context
}