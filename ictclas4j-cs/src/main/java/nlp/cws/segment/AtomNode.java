package nlp.cws.segment;

public class AtomNode {
	public String sWord;
	public int nPOS;

	public AtomNode() {
	}

	public AtomNode(String sWord, int nPOS) {
		this.sWord = sWord;
		this.nPOS = nPOS;
	}
}