package nlp.cws.segment;

public class WordLinkedArray {
	public WordNode first = null;
	public WordNode last = null;
	public int Count = 0;

	public final void AppendNode(WordNode node) {
		if (first == null && last == null) {
			first = node;
			last = node;
		} else {
			last.next = node;
			last = node;
		}

		Count++;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		WordNode cur = first;
		while (cur != null) {
			sb.append(String.format("%1$s, ", cur.theWord.sWord));
			cur = cur.next;
		}

		return sb.toString();
	}
}