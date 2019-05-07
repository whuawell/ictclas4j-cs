package nlp.cws.segment.nshortpath;

public class PathNode {
	public int nParent;
	public int nIndex;

	public PathNode(int parent, int index) {
		this.nParent = parent;
		this.nIndex = index;
	}
}