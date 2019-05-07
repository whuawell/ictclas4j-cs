package nlp.cws.segment.dynamicarray;

public class ColumnFirstDynamicArray<T> extends DynamicArray<T> {

	public final ChainItem<T> GetFirstElementOfCol(int nCol) {
		ChainItem<T> pCur = pHead;

		while (pCur != null && pCur.col != nCol) {
			pCur = pCur.next;
		}

		return pCur;
	}

	public final ChainItem<T> GetFirstElementOfCol(int nCol,
			ChainItem<T> startFrom) {
		ChainItem<T> pCur = startFrom;

		while (pCur != null && pCur.col != nCol) {
			pCur = pCur.next;
		}

		return pCur;
	}

	// ====================================================================
	// ���û����һ���µĽ��
	// ====================================================================
	@Override
	public void SetElement(int nRow, int nCol, T content) {
		ChainItem<T> pCur = pHead, pPre = null, pNew; // The pointer of array
														// chain

		if (nRow > RowCount) // Set the array row
		{
			RowCount = nRow;
		}

		if (nCol > ColumnCount) // Set the array col
		{
			ColumnCount = nCol;
		}

		while (pCur != null
				&& (pCur.col < nCol || (pCur.col == nCol && pCur.row < nRow))) {
			pPre = pCur;
			pCur = pCur.next;
		}

		if (pCur != null && pCur.row == nRow && pCur.col == nCol) // Find the
																	// same
																	// position
		{
			pCur.Content = content; // Set the value
		} else {
			pNew = new ChainItem<T>(); // malloc a new node
			pNew.col = nCol;
			pNew.row = nRow;
			pNew.Content = content;

			pNew.next = pCur;

			if (pPre == null) // link pNew after the pPre
			{
				pHead = pNew;
			} else {
				pPre.next = pNew;
			}
		}
	}

}