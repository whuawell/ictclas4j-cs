package nlp.cws.segment.nshortpath;

public class CQueue {
	private QueueElement pHead = null;
	private QueueElement pLastAccess = null;

	// ====================================================================
	// ��QueueElement����eWeight��С�����˳��������
	// ====================================================================
	public final void EnQueue(QueueElement newElement) {
		QueueElement pCur = pHead, pPre = null;

		while (pCur != null && pCur.eWeight < newElement.eWeight) {
			pPre = pCur;
			pCur = pCur.next;
		}

		newElement.next = pCur;

		if (pPre == null) {
			pHead = newElement;
		} else {
			pPre.next = newElement;
		}
	}

	// ====================================================================
	// �Ӷ�����ȡ��ǰ���һ��Ԫ��
	// ====================================================================
	public final QueueElement DeQueue() {
		if (pHead == null) {
			return null;
		}

		QueueElement pRet = pHead;
		pHead = pHead.next;

		return pRet;
	}

	// ====================================================================
	// ��ȡ��һ��Ԫ�أ�����ִ��DeQueue����
	// ====================================================================
	public final QueueElement GetFirst() {
		pLastAccess = pHead;
		return pLastAccess;
	}

	// ====================================================================
	// ��ȡ�ϴζ�ȡ�����һ��Ԫ�أ���ִ��DeQueue����
	// ====================================================================
	public final QueueElement GetNext() {
		if (pLastAccess != null) {
			pLastAccess = pLastAccess.next;
		}

		return pLastAccess;
	}

	// ====================================================================
	// �Ƿ���Ȼ����һ��Ԫ�ؿɹ���ȡ
	// ====================================================================
	public final boolean getCanGetNext() {
		return (pLastAccess.next != null);
	}

	// ====================================================================
	// �������Ԫ��
	// ====================================================================
	public final void Clear() {
		pHead = null;
		pLastAccess = null;
	}
}