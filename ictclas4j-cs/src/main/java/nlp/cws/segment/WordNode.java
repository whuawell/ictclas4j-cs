package nlp.cws.segment;

import nlp.cws.utility.WordResult;

public class WordNode {
	public int row;
	public int col;
	public WordResult theWord;
	public String sWordInSegGraph;

	public WordNode next;
}