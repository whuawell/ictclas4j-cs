package nlp.cws.segment.dynamicarray;

public class ChainItem<T> {
	public int row;
	public int col;
	public T Content;
	public ChainItem<T> next;
}