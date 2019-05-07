package nlp.cws.segment.dynamicarray;

public abstract class DynamicArray<T> {
	protected ChainItem<T> pHead; // The head pointer of array chain
	public int ColumnCount, RowCount; // The row and col of the array

	public DynamicArray() {
		pHead = null;
		RowCount = 0;
		ColumnCount = 0;
	}

	public final int getItemCount() {
		ChainItem<T> pCur = pHead;
		int nCount = 0;
		while (pCur != null) {
			nCount++;
			pCur = pCur.next;
		}
		return nCount;
	}

	// ====================================================================
	// �����С���ֵΪnRow, nCol�Ľ��
	// ====================================================================
	public final ChainItem<T> GetElement(int nRow, int nCol) {
		ChainItem<T> pCur = pHead;

		while (pCur != null && !(pCur.col == nCol && pCur.row == nRow)) {
			pCur = pCur.next;
		}

		return pCur;
	}

	// ====================================================================
	// ���û����һ���µĽ��
	// ====================================================================
	public abstract void SetElement(int nRow, int nCol, T content);

	// ====================================================================
	// Return the head element of ArrayChain
	// ====================================================================
	public final ChainItem<T> GetHead() {
		return pHead;
	}

	public static class RefObject<T> {
		public T argvalue;
		public RefObject(T refarg) {
			argvalue = refarg;
		}
	}
	// ====================================================================
	// Get the tail Element buffer and return the count of elements
	// ====================================================================
	public final int GetTail(RefObject<ChainItem<T>> pTailRet) {
		ChainItem<T> pCur = pHead, pPrev = null;
		int nCount = 0;
		while (pCur != null) {
			nCount++;
			pPrev = pCur;
			pCur = pCur.next;
		}
		pTailRet.argvalue = pPrev;
		return nCount;
	}

	// ====================================================================
	// Set Empty
	// ====================================================================
	public final void SetEmpty() {
		pHead = null;
		ColumnCount = 0;
		RowCount = 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		ChainItem<T> pCur = pHead;

		while (pCur != null) {
			sb.append(String.format("row:%1$3s,  col:%2$3s,  ", pCur.row,
					pCur.col));
			sb.append(pCur.Content.toString());
			sb.append("\r\n");
			pCur = pCur.next;
		}

		return sb.toString();
	}

	public final java.util.ArrayList<ChainItem<T>> ToListItems() {
		java.util.ArrayList<ChainItem<T>> result = new java.util.ArrayList<ChainItem<T>>();

		ChainItem<T> pCur = pHead;
		while (pCur != null) {
			result.add(pCur);
			pCur = pCur.next;
		}

		return result;
	}

}