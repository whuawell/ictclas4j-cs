package nlp.cws.segment.nshortpath;

import nlp.cws.segment.dynamicarray.ChainContent;
import nlp.cws.segment.dynamicarray.ChainItem;
import nlp.cws.segment.dynamicarray.ColumnFirstDynamicArray;
import nlp.cws.segment.dynamicarray.DynamicArray.RefObject;
import nlp.cws.utility.Predefine;

public class NShortPath {
	private static ColumnFirstDynamicArray<ChainContent> m_apCost;
	private static int m_nValueKind; // The number of value kinds
	private static int m_nNode; // The number of Node in the graph
	private static CQueue[][] m_pParent; // The 2-dimension array for the nodes
	private static double[][] m_pWeight; // The weight of node

	private NShortPath() {
	}

	private static void InitNShortPath(
			ColumnFirstDynamicArray<ChainContent> apCost, int nValueKind) {
		m_apCost = apCost; // Set the cost
		m_nValueKind = nValueKind; // Set the value kind

		// ��ȡ�������Ŀ
		// ----------------- ע��by zhenyulu ------------------
		// ԭ������Ϊm_nNode = Math.Max(apCost.ColumnCount, apCost.RowCount) + 1;
		// ��apCost.ColumnCountӦ��һ������apCost.RowCount�����Ըĳ�������
		m_nNode = apCost.ColumnCount + 1;

		m_pParent = new CQueue[m_nNode - 1][]; // not including the first node
		m_pWeight = new double[m_nNode - 1][];

		// The queue array for every node
		for (int i = 0; i < m_nNode - 1; i++) {
			m_pParent[i] = new CQueue[nValueKind];
			m_pWeight[i] = new double[nValueKind];

			for (int j = 0; j < nValueKind; j++) {
				m_pParent[i][j] = new CQueue();
			}
		}
	}

	// ====================================================================
	// ��������н���Ͽ��ܵ�·����Ϊ·�������ṩ����׼��
	// ====================================================================
	public static void Calculate(ColumnFirstDynamicArray<ChainContent> apCost,
			int nValueKind) {
		InitNShortPath(apCost, nValueKind);

		QueueElement tmpElement;
		CQueue queWork = new CQueue();
		double eWeight;

		nextnode : for (int nCurNode = 1; nCurNode < m_nNode; nCurNode++) {
			// �����е���ǰ��㣨nCurNode)���ܵı߸���eWeight����ѹ�����
			RefObject<CQueue> tempRef_queWork = new RefObject<CQueue>(queWork);
			EnQueueCurNodeEdges(tempRef_queWork, nCurNode);
			queWork = tempRef_queWork.argvalue;

			// ��ʼ����ǰ������бߵ�eWeightֵ
			for (int i = 0; i < m_nValueKind; i++) {
				m_pWeight[nCurNode - 1][i] = Predefine.INFINITE_VALUE;
			}

			// ��queWork�е�����װ��m_pWeight��m_pParent
			tmpElement = queWork.DeQueue();
			if (tmpElement != null) {
				for (int i = 0; i < m_nValueKind; i++) {
					eWeight = tmpElement.eWeight;
					m_pWeight[nCurNode - 1][i] = eWeight;
					do {
						m_pParent[nCurNode - 1][i].EnQueue(new QueueElement(
								tmpElement.nParent, tmpElement.nIndex, 0));
						tmpElement = queWork.DeQueue();
						if (tmpElement == null) {
							// C# TO JAVA CONVERTER TODO TASK: There is no
							// 'goto' in Java:
							continue nextnode;
						}

					} while (tmpElement.eWeight == eWeight);
				}
			}

		}
	}

	// ====================================================================
	// �����е���ǰ��㣨nCurNode�����ܵı߸���eWeight����ѹ�����
	// ====================================================================
	private static void EnQueueCurNodeEdges(RefObject<CQueue> queWork,
			int nCurNode) {
		int nPreNode;
		double eWeight;
		ChainItem<ChainContent> pEdgeList;

		queWork.argvalue.Clear();
		pEdgeList = m_apCost.GetFirstElementOfCol(nCurNode);

		// Get all the edges
		while (pEdgeList != null && pEdgeList.col == nCurNode) {
			nPreNode = pEdgeList.row; // ���ر�����������row��col�Ĺ�ϵ
			eWeight = pEdgeList.Content.eWeight; // Get the eWeight of edges

			for (int i = 0; i < m_nValueKind; i++) {
				// ��һ����㣬û��PreNode��ֱ�Ӽ������
				if (nPreNode == 0) {
					queWork.argvalue
							.EnQueue(new QueueElement(nPreNode, i, eWeight));
					break;
				}

				// ���PreNode��Weight ==
				// Predefine.INFINITE_VALUE����û�б�Ҫ������ȥ��
				if (m_pWeight[nPreNode - 1][i] == Predefine.INFINITE_VALUE) {
					break;
				}

				queWork.argvalue.EnQueue(new QueueElement(nPreNode, i,
						eWeight + m_pWeight[nPreNode - 1][i]));
			}
			pEdgeList = pEdgeList.next;
		}
	}

	// ====================================================================
	// ע��index �� 0 : ��̵�·���� index = 1 �� �ζ̵�·��
	// �������ơ�index <= this.m_nValueKind
	// ====================================================================
	public static java.util.ArrayList<Integer[]> GetPaths(int index) {
		assert index <= m_nValueKind && index >= 0;

		java.util.Stack<PathNode> stack = new java.util.Stack<PathNode>();
		int curNode = m_nNode - 1, curIndex = index;
		QueueElement element;
		PathNode node;
		Integer[] aPath;
		java.util.ArrayList<Integer[]> result = new java.util.ArrayList<Integer[]>();

		element = m_pParent[curNode - 1][curIndex].GetFirst();
		while (element != null) {
			// ---------- ͨ��ѹջ�õ�·�� -----------
			stack.push(new PathNode(curNode, curIndex));
			stack.push(new PathNode(element.nParent, element.nIndex));
			curNode = element.nParent;

			while (curNode != 0) {
				element = m_pParent[element.nParent - 1][element.nIndex]
						.GetFirst();
				stack.push(new PathNode(element.nParent, element.nIndex));
				curNode = element.nParent;
			}

			// -------------- ���·�� --------------

			PathNode[] nArray = stack.toArray(new PathNode[0]);
			aPath = new Integer[nArray.length];

			// reverse stack
			for (int i = 0; i < aPath.length; i++) {
				aPath[i] = nArray[aPath.length - 1 - i].nParent;
			}

			result.add(aPath);

			// -------------- ��ջ�Լ���Ƿ�������·�� --------------
			do {
				node = stack.pop();
				curNode = node.nParent;
				curIndex = node.nIndex;

			} while (curNode < 1 || (stack.size() != 0
					&& !m_pParent[curNode - 1][curIndex].getCanGetNext()));

			element = m_pParent[curNode - 1][curIndex].GetNext();
		}

		return result;
	}

	// ====================================================================
	// ��ȡΨһһ�����·������Ȼ���·�����ܲ�ֻһ��
	// ====================================================================
	public static Integer[] GetBestPath() {
		assert m_nNode > 2;

		java.util.Stack<Integer> stack = new java.util.Stack<Integer>();
		int curNode = m_nNode - 1, curIndex = 0;
		QueueElement element;

		element = m_pParent[curNode - 1][curIndex].GetFirst();

		stack.push(curNode);
		stack.push(element.nParent);
		curNode = element.nParent;

		while (curNode != 0) {
			element = m_pParent[element.nParent - 1][element.nIndex].GetFirst();
			stack.push(element.nParent);
			curNode = element.nParent;
		}

		return stack.toArray(new Integer[0]);
	}

	// ====================================================================
	// �Ӷ̵�����ȡ���� n ��·��
	// ====================================================================
	public static java.util.ArrayList<Integer[]> GetNPaths(int n) {
		java.util.ArrayList<Integer[]> result = new java.util.ArrayList<Integer[]>();
		java.util.ArrayList<Integer[]> tmp;
		int nCopy;

		for (int i = 0; i < m_nValueKind
				&& result.size() < Predefine.MAX_SEGMENT_NUM; i++) {
			tmp = GetPaths(i);

			if (n - result.size() < tmp.size()) {
				nCopy = n - result.size();
			} else {
				nCopy = tmp.size();
			}

			for (int j = 0; j < nCopy; j++) {
				result.add(tmp.get(j));
			}
		}

		return result;
	}

	public static void printResultByIndex() {
		QueueElement e;

		for (int i = 0; i < m_nValueKind; i++) {
			System.out.println(String
					.format("\n\r============ Index = {%s} ============", i));
			for (int nCurNode = 1; nCurNode < m_nNode; nCurNode++) {
				System.out.println(String.format("Node Num: {%s}", nCurNode));

				e = m_pParent[nCurNode - 1][i].GetFirst();
				while (e != null) {
					System.out.println(String.format(
							"({%s}, {%s})  eWeight = {%s}", e.nParent, e.nIndex,
							m_pWeight[nCurNode - 1][i]));
					e = m_pParent[nCurNode - 1][i].GetNext();
				}
				System.out.println("---------------------");
			}
		}
	}

	public static void printResultByNode() {
		QueueElement e;

		for (int nCurNode = 1; nCurNode < m_nNode; nCurNode++) {
			System.out.println(String.format(
					"\n\r============ Index = {%s} ============", nCurNode));
			for (int i = 0; i < m_nValueKind; i++) {
				System.out.println(String.format("N: {%s}", i));

				e = m_pParent[nCurNode - 1][i].GetFirst();
				while (e != null) {
					System.out.println(String.format(
							"({%s}, {%s})  eWeight = {%s}", e.nParent, e.nIndex,
							m_pWeight[nCurNode - 1][i]));
					e = m_pParent[nCurNode - 1][i].GetNext();
				}
				System.out.println("---------------------");
			}
		}
	}

}