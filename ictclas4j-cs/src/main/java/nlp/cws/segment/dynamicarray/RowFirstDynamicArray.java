package nlp.cws.segment.dynamicarray;

public class RowFirstDynamicArray<T> extends DynamicArray<T> {

	// ====================================================================
	// ������Ϊ nRow �ĵ�һ�����
	// ====================================================================
	public final ChainItem<T> GetFirstElementOfRow(int nRow) {
		ChainItem<T> pCur = pHead;

		while (pCur != null && pCur.row != nRow) {
			pCur = pCur.next;
		}

		return pCur;
	}

	// ====================================================================
	// �� startFrom ����������Ϊ nRow �ĵ�һ�����
	// ====================================================================
	public final ChainItem<T> GetFirstElementOfRow(int nRow,
			ChainItem<T> startFrom) {
		ChainItem<T> pCur = startFrom;

		while (pCur != null && pCur.row != nRow) {
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
				&& (pCur.row < nRow || (pCur.row == nRow && pCur.col < nCol))) {
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