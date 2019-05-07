package nlp.cws.segment.nshortpath;

public class QueueElement {
	public int nParent;
	public int nIndex;
	public double eWeight;
	public QueueElement next = null;

	public QueueElement(int nParent, int nIndex, double eWeight) {
		this.nParent = nParent;
		this.nIndex = nIndex;
		this.eWeight = eWeight;
	}
}