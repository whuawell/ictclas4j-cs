package nlp.cws.segment.dynamicarray;

public class ChainContent {
	public String sWord;
	public int nPOS;
	public double eWeight;

	public ChainContent() {
	}

	public ChainContent(double eWeight) {
		this.eWeight = eWeight;
	}

	public ChainContent(double eWeight, int nPos) {
		this.eWeight = eWeight;
		this.nPOS = nPos;
	}

	public ChainContent(double eWeight, int nPos, String sWord) {
		this.eWeight = eWeight;
		this.nPOS = nPos;
		this.sWord = sWord;
	}

	@Override
	public String toString() {
		return String.format("eWeight:%10.2f,   nPOS:%7s,   sWord:%s", eWeight,
				nPOS, sWord);
	}
}