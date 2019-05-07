package nlp.cws.segment;


import nlp.cws.eventinterface.SegmentEventArgs;
import nlp.cws.eventinterface.SegmentStage;
import nlp.cws.segment.dynamicarray.ChainContent;
import nlp.cws.segment.dynamicarray.ChainItem;
import nlp.cws.segment.dynamicarray.ColumnFirstDynamicArray;
import nlp.cws.segment.dynamicarray.RowFirstDynamicArray;
import nlp.cws.segment.dynamicarray.DynamicArray.RefObject;
import nlp.cws.segment.nshortpath.NShortPath;
import nlp.cws.top.WordSegment;
import nlp.cws.utility.Predefine;
import nlp.cws.utility.Regex;
import nlp.cws.utility.Utility;
import nlp.cws.utility.WordDictionary;
import nlp.cws.utility.WordInfo;
import nlp.cws.utility.WordResult;


public class Segment {
	private WordDictionary biDict, coreDict;

	public java.util.ArrayList<AtomNode> atomSegment;
	public RowFirstDynamicArray<ChainContent> segGraph;
	public ColumnFirstDynamicArray<ChainContent> biGraphResult;
	public RowFirstDynamicArray<ChainContent> m_graphOptimum;
	public java.util.ArrayList<WordResult[]> m_pWordSeg; // 存放多个分词结果

	public Segment(WordDictionary biDict, WordDictionary coreDict) {
		this.biDict = biDict;
		this.coreDict = coreDict;
	}

	public final int BiSegment(String sSentence, double smoothPara, int nKind) {
		WordResult[] tmpResult;
		WordLinkedArray linkedArray;

		if (biDict == null || coreDict == null) {
			throw new RuntimeException("biDict 或 coreDict 尚未初始化！");
		}

		// ---原子分词
		atomSegment = AtomSegment(sSentence);
		OnAtomSegment(atomSegment);

		// ---检索词库，加入所有可能分词方案并存入链表结构
		segGraph = GenerateWordNet(atomSegment, coreDict);
		OnGenSegGraph(segGraph);

		// ---检索所有可能的两两组合
		biGraphResult = BiGraphGenerate(segGraph, smoothPara, biDict, coreDict);
		OnGenBiSegGraph(biGraphResult);

		// ---N 最短路径计算出多个分词方案
		NShortPath.Calculate(biGraphResult, nKind);
		java.util.ArrayList<Integer[]> spResult = NShortPath
				.GetNPaths(Predefine.MAX_SEGMENT_NUM);
		OnNShortPath(spResult, segGraph);

		m_pWordSeg = new java.util.ArrayList<WordResult[]>();
		m_graphOptimum = new RowFirstDynamicArray<ChainContent>();

		for (int i = 0; i < spResult.size(); i++) {
			linkedArray = BiPath2LinkedArray(spResult.get(i), segGraph,
					atomSegment);
			tmpResult = GenerateWord(spResult.get(i), linkedArray,
					m_graphOptimum);

			if (tmpResult != null) {
				m_pWordSeg.add(tmpResult);
			}
		}

		OnBeforeOptimize(m_pWordSeg);

		return m_pWordSeg.size();
	}

	public final int BiOptimumSegment(int nResultCount, double dSmoothingPara) {
		WordResult[] tmpResult;
		WordLinkedArray linkedArray;

		// Generate the biword link net
		ColumnFirstDynamicArray<ChainContent> aBiwordsNet = BiGraphGenerate(
				m_graphOptimum, dSmoothingPara, biDict, coreDict);
		OnGenBiOptimumSegGraph(aBiwordsNet);

		NShortPath.Calculate(aBiwordsNet, nResultCount);
		java.util.ArrayList<Integer[]> spResult = NShortPath
				.GetNPaths(Predefine.MAX_SEGMENT_NUM);

		m_pWordSeg = new java.util.ArrayList<WordResult[]>();
		segGraph = m_graphOptimum;
		m_graphOptimum = new RowFirstDynamicArray<ChainContent>();

		for (int i = 0; i < spResult.size(); i++) {
			linkedArray = BiPath2LinkedArray(spResult.get(i), segGraph,
					atomSegment);
			tmpResult = GenerateWord(spResult.get(i), linkedArray,
					m_graphOptimum);

			if (tmpResult != null) {
				m_pWordSeg.add(tmpResult);
			}
		}

		return m_pWordSeg.size();
	}

	// ====================================================================
	// 对sSentence进行单个汉字的切割
	// ====================================================================
	public static java.util.ArrayList<AtomNode> AtomSegment(String sSentence) {
		java.util.ArrayList<AtomNode> atomSegment = new java.util.ArrayList<AtomNode>();
		AtomNode tmpEnd = null;
		int startIndex = 0, length = sSentence.length(), pCur = 0, nCurType,
				nNextType;
		StringBuilder sb = new StringBuilder();
		char c;

		// 如果是开始符号
		if (sSentence.startsWith(Predefine.SENTENCE_BEGIN)) {
			atomSegment.add(new AtomNode(Predefine.SENTENCE_BEGIN,
					Predefine.CT_SENTENCE_BEGIN));
			startIndex = Predefine.SENTENCE_BEGIN.length();
			length -= startIndex;
		}

		// 如果是结束符号
		if (sSentence.endsWith(Predefine.SENTENCE_END)) {
			tmpEnd = new AtomNode(Predefine.SENTENCE_END,
					Predefine.CT_SENTENCE_END);
			length -= Predefine.SENTENCE_END.length();
		}

		// ==============================================================================================
		// by zhenyulu:
		//
		// TODO: 使用一系列正则表达式将句子中的完整成分（百分比、日期、电子邮件、URL等）预先提取出来
		// ==============================================================================================

		// char[] charArray = sSentence.ToCharArray(startIndex, length);

		char[] charArray = sSentence.substring(startIndex, startIndex + length)
				.toCharArray();
		int[] charTypeArray = new int[charArray.length];

		// 生成对应单个汉字的字符类型数组
		for (int i = 0; i < charArray.length; i++) {
			c = charArray[i];
			charTypeArray[i] = Utility.charType(c);

			if (c == '.' && i < (charArray.length - 1)
					&& Utility.charType(charArray[i + 1]) == Predefine.CT_NUM) {
				charTypeArray[i] = Predefine.CT_NUM;
			} else if (c == '.' && i < (charArray.length - 1)
					&& charArray[i + 1] >= '0' && charArray[i + 1] <= '9') {
				charTypeArray[i] = Predefine.CT_SINGLE;
			} else if (charTypeArray[i] == Predefine.CT_LETTER) {
				charTypeArray[i] = Predefine.CT_SINGLE;
			}
		}

		// 根据字符类型数组中的内容完成原子切割
		while (pCur < charArray.length) {
			nCurType = charTypeArray[pCur];

			if (nCurType == Predefine.CT_CHINESE
					|| nCurType == Predefine.CT_INDEX
					|| nCurType == Predefine.CT_DELIMITER
					|| nCurType == Predefine.CT_OTHER) {
				if ((new Character(charArray[pCur])).toString().trim()
						.length() != 0) {
					atomSegment.add(new AtomNode(
							(new Character(charArray[pCur])).toString(),
							nCurType));
				}
				pCur++;
			}
			// 如果是字符、数字或者后面跟随了数字的小数点"."则一直取下去。
			else if (pCur < charArray.length - 1
					&& (nCurType == Predefine.CT_SINGLE
							|| nCurType == Predefine.CT_NUM)) {
				sb.delete(0, sb.length());
				sb.append(charArray[pCur]);

				boolean reachEnd = true;
				while (pCur < charArray.length - 1) {
					nNextType = charTypeArray[++pCur];

					if (nNextType == nCurType) {
						sb.append(charArray[pCur]);
					} else {
						reachEnd = false;
						break;
					}
				}
				atomSegment.add(new AtomNode(sb.toString(), nCurType));
				if (reachEnd) {
					pCur++;
				}
			}
			// 对于所有其它情况 CT_LETTER
			else {
				atomSegment.add(new AtomNode(
						(new Character(charArray[pCur])).toString(), nCurType));
				pCur++;
			}
		}

		// 增加结束标志
		if (tmpEnd != null) {
			atomSegment.add(tmpEnd);
		}

		return atomSegment;
	}

	// ====================================================================
	// Func Name : GenerateWordNet
	// Description: Generate the segmentation word net according
	// the original sentence
	// Parameters : sSentence: the sentence
	// dictCore : core dictionary
	// bOriginalFreq=false: output original frequency
	// Returns : bool
	// ====================================================================
	public static RowFirstDynamicArray<ChainContent> GenerateWordNet(
			java.util.ArrayList<AtomNode> atomSegment,
			WordDictionary coreDict) {
		String sWord = "", sMaxMatchWord = null;
		int nPOSRet = 0, nPOS, nTotalFreq;
		double dValue = 0;

		RowFirstDynamicArray<ChainContent> m_segGraph = new RowFirstDynamicArray<ChainContent>();
		m_segGraph.SetEmpty();

		// 将原子部分存入m_segGraph
		for (int i = 0; i < atomSegment.size(); i++) // Init the cost array
		{
			if (atomSegment.get(i).nPOS == Predefine.CT_CHINESE) {
				m_segGraph.SetElement(i, i + 1,
						new ChainContent(0, 0, atomSegment.get(i).sWord));
			} else {
				sWord = atomSegment.get(i).sWord; // init the word
				dValue = Predefine.MAX_FREQUENCE;
				switch (atomSegment.get(i).nPOS) {
					case Predefine.CT_INDEX :
					case Predefine.CT_NUM :
						nPOS = -27904; // 'm'*256
						sWord = "未##数";
						dValue = 0;
						break;
					case Predefine.CT_DELIMITER :
						nPOS = 30464; // 'w'*256;
						break;
					case Predefine.CT_LETTER :
						nPOS = -28280; // -'n' * 256 - 'x';
						dValue = 0;
						sWord = "未##串";
						break;
					case Predefine.CT_SINGLE : // 12021-2129-3121
						if (Regex.IsMatch(atomSegment.get(i).sWord,
								"^(-?\\d+)(\\.\\d+)?$")) // 匹配浮点数

						{
							nPOS = -27904; // 'm'*256
							sWord = "未##数";
						} else {
							nPOS = -28280; // -'n' * 256 - 'x'
							sWord = "未##串";
						}
						dValue = 0;
						break;
					default :
						nPOS = atomSegment.get(i).nPOS; // '?'*256;
						break;
				}
				m_segGraph.SetElement(i, i + 1,
						new ChainContent(dValue, nPOS, sWord)); // init the link
																// with minimum
			}
		}

		// 将所有可能的组词存入m_segGraph
		for (int i = 0; i < atomSegment.size(); i++) // All the word
		{
			sWord = atomSegment.get(i).sWord; // Get the current atom
			int j = i + 1;

			RefObject<String> tempRef_sMaxMatchWord = new RefObject<String>(
					sMaxMatchWord);
			RefObject<Integer> tempRef_nPOSRet = new RefObject<Integer>(
					nPOSRet);
			boolean tempVar = j < atomSegment.size() && coreDict
					.GetMaxMatch(sWord, tempRef_sMaxMatchWord, tempRef_nPOSRet);
			sMaxMatchWord = tempRef_sMaxMatchWord.argvalue;
			nPOSRet = tempRef_nPOSRet.argvalue;
			while (tempVar) {
				if (sWord.equals(sMaxMatchWord)) // 就是我们要找的词
				{
					WordInfo info = coreDict.GetWordInfo(sWord); // 该词可能就有多种词性

					// 计算该词的所有词频之和
					nTotalFreq = 0;
					for (int k = 0; k < info.Count; k++) {
						nTotalFreq += info.Frequencies.get(k);
					}

					// 限制出现某些特殊词
					if (sWord.length() == 2
							&& (sWord.startsWith("年") || sWord.startsWith("月"))
							&& i >= 1
							&& (Utility.IsAllNum(atomSegment.get(i - 1).sWord)
									|| Utility.IsAllChineseNum(
											atomSegment.get(i - 1).sWord))) {
						// 1年内、1999年末
						if ((new String("末内中底前间初"))
								.indexOf(sWord.substring(1)) >= 0) {
							break;
						}
					}

					// 如果该词只有一个词性，则存储，否则词性记录为 0
					if (info.Count == 1) {
						m_segGraph.SetElement(i, j, new ChainContent(nTotalFreq,
								info.POSs.get(0), sWord));
					} else {
						m_segGraph.SetElement(i, j,
								new ChainContent(nTotalFreq, 0, sWord));
					}
				}

				sWord += atomSegment.get(j++).sWord;
				RefObject<String> tempRef_sMaxMatchWord2 = new RefObject<String>(
						sMaxMatchWord);
				RefObject<Integer> tempRef_nPOSRet2 = new RefObject<Integer>(
						nPOSRet);
				tempVar = j < atomSegment.size() && coreDict.GetMaxMatch(sWord,
						tempRef_sMaxMatchWord2, tempRef_nPOSRet2);
				sMaxMatchWord = tempRef_sMaxMatchWord2.argvalue;
				nPOSRet = tempRef_nPOSRet2.argvalue;
			}
		}
		return m_segGraph;
	}

	// ====================================================================
	// 生成两两词之间的二叉图表
	// ====================================================================
	public static ColumnFirstDynamicArray<ChainContent> BiGraphGenerate(
			RowFirstDynamicArray<ChainContent> aWord, double smoothPara,
			WordDictionary biDict, WordDictionary coreDict) {
		ColumnFirstDynamicArray<ChainContent> aBiWordNet = new ColumnFirstDynamicArray<ChainContent>();

		ChainItem<ChainContent> pCur, pNextWords;
		int nTwoWordsFreq = 0, nCurWordIndex, nNextWordIndex;
		double dCurFreqency, dValue, dTemp;
		String sTwoWords;
		StringBuilder sb = new StringBuilder();

		// Record the position map of possible words
		int[] m_npWordPosMapTable = PreparePositionMap(aWord);

		pCur = aWord.GetHead();
		while (pCur != null) {
			if (pCur.Content.nPOS >= 0)
			// It's not an unknown words
			{
				dCurFreqency = pCur.Content.eWeight;
			} else
			// Unknown words
			{
				dCurFreqency = coreDict.GetFrequency(pCur.Content.sWord, 2);
			}

			// Get next words which begin with pCur.col（注：很特殊的对应关系）
			pNextWords = aWord.GetFirstElementOfRow(pCur.col);

			while (pNextWords != null && pNextWords.row == pCur.col) {
				sb.delete(0, sb.length());
				sb.append(pCur.Content.sWord);
				sb.append(Predefine.WORD_SEGMENTER);
				sb.append(pNextWords.Content.sWord);

				sTwoWords = sb.toString();

				// Two linked Words frequency
				nTwoWordsFreq = biDict.GetFrequency(sTwoWords, 3);

				// Smoothing
				dTemp = 1.0 / Predefine.MAX_FREQUENCE;

				// -log{a*P(Ci-1)+(1-a)P(Ci|Ci-1)} Note 0<a<1
				dValue = -Math.log(smoothPara * (1.0 + dCurFreqency)
						/ (Predefine.MAX_FREQUENCE + 80000.0)
						+ (1.0 - smoothPara) * ((1.0 - dTemp) * nTwoWordsFreq
								/ (1.0 + dCurFreqency) + dTemp));

				// Unknown words: P(Wi|Ci);while known words:1
				if (pCur.Content.nPOS < 0) {
					dValue += pCur.Content.nPOS;
				}

				// Get the position index of current word in the position map
				// table
				nCurWordIndex = Utility.BinarySearch(
						pCur.row * Predefine.MAX_SENTENCE_LEN + pCur.col,
						m_npWordPosMapTable);
				nNextWordIndex = Utility.BinarySearch(
						pNextWords.row * Predefine.MAX_SENTENCE_LEN
								+ pNextWords.col,
						m_npWordPosMapTable);

				aBiWordNet.SetElement(nCurWordIndex, nNextWordIndex,
						new ChainContent(dValue, pCur.Content.nPOS, sTwoWords));

				pNextWords = pNextWords.next; // Get next word
			}
			pCur = pCur.next;
		}

		return aBiWordNet;
	}

	// ====================================================================
	// 准备PositionMap，用于记录词的位置
	// ====================================================================
	private static int[] PreparePositionMap(
			RowFirstDynamicArray<ChainContent> aWord) {
		int[] m_npWordPosMapTable;
		ChainItem<ChainContent> pTail = null, pCur = null;
		int nWordIndex = 0, m_nWordCount;

		// Get tail element and return the words count
		RefObject<ChainItem<ChainContent>> tempRef_pTail = new RefObject<ChainItem<ChainContent>>(
				pTail);
		m_nWordCount = aWord.GetTail(tempRef_pTail);
		pTail = tempRef_pTail.argvalue;

		if (m_nWordCount > 0) {
			m_npWordPosMapTable = new int[m_nWordCount];
		} else {
			m_npWordPosMapTable = null;
		}

		// Record the position of possible words
		pCur = aWord.GetHead();
		while (pCur != null) {
			m_npWordPosMapTable[nWordIndex++] = pCur.row
					* Predefine.MAX_SENTENCE_LEN + pCur.col;
			pCur = pCur.next;
		}

		return m_npWordPosMapTable;
	}

	// ====================================================================
	// 将BiPath转换为LinkedArray
	// 例如"他说的确实在理"
	// BiPath：（0, 1, 2, 3, 6, 9, 11, 12）
	// 0 1 2 3 4 5 6 7 8 9 10 11 12
	// 始##始 他 说 的 的确 确 确实 实 实在 在 在理 理 末##末
	// ====================================================================
	private static WordLinkedArray BiPath2LinkedArray(Integer[] biPath,
			RowFirstDynamicArray<ChainContent> segGraph,
			java.util.ArrayList<AtomNode> atomSegment) {
		java.util.ArrayList<ChainItem<ChainContent>> list = segGraph
				.ToListItems();
		StringBuilder sb = new StringBuilder();

		WordLinkedArray result = new WordLinkedArray();

		for (int i = 0; i < biPath.length; i++) {
			WordNode node = new WordNode();

			node.row = list.get(biPath[i]).row;
			node.col = list.get(biPath[i]).col;
			node.sWordInSegGraph = list.get(biPath[i]).Content.sWord;

			node.theWord = new WordResult();
			if (node.sWordInSegGraph.equals("未##人")
					|| node.sWordInSegGraph.equals("未##地")
					|| node.sWordInSegGraph.equals("未##数")
					|| node.sWordInSegGraph.equals("未##时")
					|| node.sWordInSegGraph.equals("未##串")) {
				sb.delete(0, sb.length());
				for (int j = node.row; j < node.col; j++) {
					sb.append(atomSegment.get(j).sWord);
				}

				node.theWord.sWord = sb.toString();
			} else {
				node.theWord.sWord = list.get(biPath[i]).Content.sWord;
			}

			node.theWord.nPOS = list.get(biPath[i]).Content.nPOS;
			node.theWord.dValue = list.get(biPath[i]).Content.eWeight;

			result.AppendNode(node);
		}

		return result;
	}

	// ====================================================================
	// Generate Word according the segmentation route
	// ====================================================================
	private static WordResult[] GenerateWord(Integer[] uniPath,
			WordLinkedArray linkedArray,
			RowFirstDynamicArray<ChainContent> m_graphOptimum) {
		if (linkedArray.Count == 0) {
			return null;
		}

		// --------------------------------------------------------------------
		// Merge all seperate continue num into one number
		RefObject<WordLinkedArray> tempRef_linkedArray = new RefObject<WordLinkedArray>(
				linkedArray);
		MergeContinueNumIntoOne(tempRef_linkedArray);
		linkedArray = tempRef_linkedArray.argvalue;

		// --------------------------------------------------------------------
		// The delimiter "－－"
		RefObject<WordLinkedArray> tempRef_linkedArray2 = new RefObject<WordLinkedArray>(
				linkedArray);
		ChangeDelimiterPOS(tempRef_linkedArray2);
		linkedArray = tempRef_linkedArray2.argvalue;

		// --------------------------------------------------------------------
		// 如果前一个词是数字，当前词以"－"或"-"开始，并且不止这一个字符，
		// 那么将此"－"符号从当前词中分离出来。
		// 例如 "3 / -4 / 月"需要拆分成"3 / - / 4 / 月"
		RefObject<WordLinkedArray> tempRef_linkedArray3 = new RefObject<WordLinkedArray>(
				linkedArray);
		SplitMiddleSlashFromDigitalWords(tempRef_linkedArray3);
		linkedArray = tempRef_linkedArray3.argvalue;

		// --------------------------------------------------------------------
		// 1、如果当前词是数字，下一个词是"月、日、时、分、秒、月份"中的一个，则合并,且当前词词性是时间
		// 2、如果当前词是可以作为年份的数字，下一个词是"年"，则合并，词性为时间，否则为数字。
		// 3、如果最后一个汉字是"点" ，则认为当前数字是时间
		// 4、如果当前串最后一个汉字不是"∶·．／"和半角的'.''/'，那么是数
		// 5、当前串最后一个汉字是"∶·．／"和半角的'.''/'，且长度大于1，那么去掉最后一个字符。例如"1."
		RefObject<WordLinkedArray> tempRef_linkedArray4 = new RefObject<WordLinkedArray>(
				linkedArray);
		CheckDateElements(tempRef_linkedArray4);
		linkedArray = tempRef_linkedArray4.argvalue;

		// --------------------------------------------------------------------
		// 输出结果
		WordResult[] result = new WordResult[linkedArray.Count];

		WordNode pCur = linkedArray.first;
		int i = 0;
		while (pCur != null) {
			WordResult item = new WordResult();
			item.sWord = pCur.theWord.sWord;
			item.nPOS = pCur.theWord.nPOS;
			item.dValue = pCur.theWord.dValue;
			result[i] = item;

			m_graphOptimum.SetElement(pCur.row, pCur.col, new ChainContent(
					item.dValue, item.nPOS, pCur.sWordInSegGraph));

			pCur = pCur.next;
			i++;
		}

		return result;
	}

	private static void MergeContinueNumIntoOne(
			RefObject<WordLinkedArray> linkedArray) {
		if (linkedArray.argvalue.Count < 2) {
			return;
		}

		String tmp;
		WordNode pCur = linkedArray.argvalue.first;
		WordNode pNext = pCur.next;

		while (pNext != null) {
			if ((Utility.IsAllNum(pCur.theWord.sWord)
					|| Utility.IsAllChineseNum(pCur.theWord.sWord))
					&& (Utility.IsAllNum(pNext.theWord.sWord)
							|| Utility.IsAllChineseNum(pNext.theWord.sWord))) {
				tmp = pCur.theWord.sWord + pNext.theWord.sWord;
				if (Utility.IsAllNum(tmp) || Utility.IsAllChineseNum(tmp)) {
					pCur.theWord.sWord += pNext.theWord.sWord;
					pCur.col = pNext.col;
					pCur.next = pNext.next;
					linkedArray.argvalue.Count--;
					pNext = pCur.next;
					continue;
				}
			}

			pCur = pCur.next;
			pNext = pNext.next;
		}
	}

	/// #region ChangeDelimiterPOS Method

	private static void ChangeDelimiterPOS(
			RefObject<WordLinkedArray> linkedArray) {
		WordNode pCur = linkedArray.argvalue.first;
		while (pCur != null) {
			if (pCur.theWord.sWord.equals("－－")
					|| pCur.theWord.sWord.equals("—")
					|| pCur.theWord.sWord.equals("-")) {
				pCur.theWord.nPOS = 30464; // 'w'*256;Set the POS with 'w'
				pCur.theWord.dValue = 0;
			}

			pCur = pCur.next;
		}
	}

	// ====================================================================
	// 如果前一个词是数字，当前词以"－"或"-"开始，并且不止这一个字符，
	// 那么将此"－"符号从当前词中分离出来。
	// 例如 "3 / -4 / 月"需要拆分成"3 / - / 4 / 月"
	// ====================================================================
	private static void SplitMiddleSlashFromDigitalWords(
			RefObject<WordLinkedArray> linkedArray) {
		if (linkedArray.argvalue.Count < 2) {
			return;
		}

		WordNode pCur = linkedArray.argvalue.first.next;
		WordNode pPre = linkedArray.argvalue.first;

		while (pCur != null) {
			// 27904='m'*256
			if ((Math.abs(pPre.theWord.nPOS) == 27904
					|| Math.abs(pPre.theWord.nPOS) == 29696)
					&& (Utility.IsAllNum(pCur.theWord.sWord)
							|| Utility.IsAllChineseNum(pCur.theWord.sWord))
					&& ((new String("-－"))
							.indexOf(pCur.theWord.sWord.toCharArray()[0]) >= 0)
					&& pCur.theWord.sWord.length() > 1) {
				// 将"－"拆分出来。
				WordNode newNode = new WordNode();
				newNode.row = pCur.row + 1;
				newNode.col = pCur.col;
				newNode.sWordInSegGraph = pCur.theWord.sWord.substring(1);
				WordResult theWord = new WordResult();
				theWord.sWord = newNode.sWordInSegGraph;
				theWord.nPOS = 27904;
				theWord.dValue = pCur.theWord.dValue;
				newNode.theWord = theWord;

				pCur.col = pCur.row + 1;
				pCur.theWord.sWord = pCur.theWord.sWord.substring(0, 1);
				pCur.theWord.nPOS = 30464; // 'w'*256;
				pCur.theWord.dValue = 0;

				newNode.next = pCur.next;
				pCur.next = newNode;

				linkedArray.argvalue.Count++;
			}
			pCur = pCur.next;
			pPre = pPre.next;
		}
	}

	// ====================================================================
	// 1、如果当前词是数字，下一个词是"月、日、时、分、秒、月份"中的一个，则合并且当前词词性是时间
	// 2、如果当前词是可以作为年份的数字，下一个词是"年"，则合并，词性为时间，否则为数字。
	// 3、如果最后一个汉字是"点" ，则认为当前数字是时间
	// 4、如果当前串最后一个汉字不是"∶·．／"和半角的'.''/'，那么是数
	// 5、当前串最后一个汉字是"∶·．／"和半角的'.''/'，且长度大于1，那么去掉最后一个字符。例如"1."
	// ====================================================================
	private static void CheckDateElements(
			RefObject<WordLinkedArray> linkedArray) {
		if (linkedArray.argvalue.Count < 2) {
			return;
		}

		String nextWord;
		WordNode pCur = linkedArray.argvalue.first;
		WordNode pNext = pCur.next;

		while (pNext != null) {
			if (Utility.IsAllNum(pCur.theWord.sWord)
					|| Utility.IsAllChineseNum(pCur.theWord.sWord)) {
				// ===== 1、如果当前词是数字，下一个词是"月、日、时、分、秒、月份"中的一个，则合并且当前词词性是时间
				nextWord = pNext.theWord.sWord;
				if ((nextWord.length() == 1
						&& (new String("月日时分秒")).indexOf(nextWord) != -1)
						|| (nextWord.length() == 2 && nextWord.equals("月份"))) {
					// 2001年
					pCur.theWord.sWord += nextWord;
					pCur.col = pNext.col;
					pCur.sWordInSegGraph = "未##时";
					pCur.theWord.nPOS = -29696; // 't'*256;//Set the POS with
												// 'm'
					pCur.next = pNext.next;
					pNext = pCur.next;
					linkedArray.argvalue.Count--;
				}
				// ===== 2、如果当前词是可以作为年份的数字，下一个词是"年"，则合并，词性为时间，否则为数字。
				else if (nextWord.equals("年")) {
					if (IsYearTime(pCur.theWord.sWord)) {
						pCur.theWord.sWord += nextWord;
						pCur.col = pNext.col;
						pCur.sWordInSegGraph = "未##时";
						pCur.theWord.nPOS = -29696; // 't'*256;//Set the POS
													// with 'm'
						pCur.next = pNext.next;
						pNext = pCur.next;
						linkedArray.argvalue.Count--;
					}
					// ===== 否则当前词就是数字了 =====
					else {
						pCur.sWordInSegGraph = "未##数";
						pCur.theWord.nPOS = -27904; // Set the POS with 'm'
					}
				} else {
					// ===== 3、如果最后一个汉字是"点" ，则认为当前数字是时间
					if (pCur.theWord.sWord.endsWith("点")) {
						pCur.sWordInSegGraph = "未##时";
						pCur.theWord.nPOS = -29696; // Set the POS with 't'
					} else {
						char[] tmpcharArray = pCur.theWord.sWord.toCharArray();
						String lastChar = (new Character(
								tmpcharArray[tmpcharArray.length - 1]))
										.toString();
						// ===== 4、如果当前串最后一个汉字不是"∶·．／"和半角的'.''/'，那么是数
						if ((new String("∶·．／./")).indexOf(lastChar) == -1) {
							pCur.sWordInSegGraph = "未##数";
							pCur.theWord.nPOS = -27904; // 'm'*256;Set the POS
														// with 'm'
						}
						// =====
						// 5、当前串最后一个汉字是"∶·．／"和半角的'.''/'，且长度大于1，那么去掉最后一个字符。例如"1."
						else if (pCur.theWord.sWord.length() > 1) {
							pCur.theWord.sWord = pCur.theWord.sWord.substring(0,
									pCur.theWord.sWord.length() - 1);

							pCur.sWordInSegGraph = "未##数";
							pCur.theWord.nPOS = -27904; // 'm'*256;Set the POS
														// with 'm'
						}
					}
				}
			}

			pCur = pCur.next;
			pNext = pNext.next;
		}
	}

	/// #region IsYearTime Method

	private static boolean IsYearTime(String sNum) {
		// Judge whether the sNum is a num genearating year
		int nLen = sNum.length();
		char[] charArray = sNum.toCharArray();

		// 1992年, 90年
		if (Utility.IsAllNum(sNum) && (nLen == 4 || nLen == 2
				&& (new String("５６７８９56789")).indexOf(charArray[0]) != -1)) {
			return true;
		}

		if (Utility.GetCharCount("零○一二三四五六七八九壹贰叁肆伍陆柒捌玖", sNum) == nLen
				&& nLen >= 2) {
			return true;
		}

		// 二仟零二年
		if (nLen == 4 && Utility.GetCharCount("千仟零○", sNum) == 2) {
			return true;
		}

		if (nLen == 1 && Utility.GetCharCount("千仟", sNum) == 1) {
			return true;
		}

		if (nLen == 2 && Regex.IsMatch(sNum, "^[甲乙丙丁戊己庚辛壬癸][子丑寅卯辰巳午未申酉戌亥]$")) {
			return true;
		}

		return false;
	}

	/// #region Events

	private void SendEvents(SegmentEventArgs e) {

		WordSegment.SendEvents(e);
	}

	private void OnAtomSegment(java.util.ArrayList<AtomNode> nodes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nodes.size(); i++) {
			sb.append(String.format("%s, ", nodes.get(i).sWord));
		}

		sb.append("\r\n");

		SendEvents(
				new SegmentEventArgs(SegmentStage.AtomSegment, sb.toString()));
	}

	private void OnGenSegGraph(RowFirstDynamicArray<ChainContent> segGraph) {
		SendEvents(new SegmentEventArgs(SegmentStage.GenSegGraph,
				segGraph.toString()));
	}

	private void OnGenBiSegGraph(
			ColumnFirstDynamicArray<ChainContent> biGraph) {
		SendEvents(new SegmentEventArgs(SegmentStage.GenBiSegGraph,
				biGraph.toString()));
	}

	private void OnNShortPath(java.util.ArrayList<Integer[]> paths,
			RowFirstDynamicArray<ChainContent> segGraph) {
		java.util.ArrayList<ChainItem<ChainContent>> list = segGraph
				.ToListItems();
		String theWord;

		Integer[] aPath;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < paths.size(); i++) {
			aPath = paths.get(i);
			for (int j = 0; j < aPath.length; j++) {
				theWord = list.get(aPath[j]).Content.sWord;
				if (theWord.equals("未##人") || theWord.equals("未##地")
						|| theWord.equals("未##数") || theWord.equals("未##时")
						|| theWord.equals("未##串")) {
					for (int k = list.get(aPath[j]).row; k < list
							.get(aPath[j]).col; k++) {
						sb.append(atomSegment.get(k).sWord);
					}
					sb.append(", ");
				} else {
					sb.append(String.format("%s, ",
							list.get(aPath[j]).Content.sWord));
				}
			}

			sb.append("\r\n");
		}

		SendEvents(
				new SegmentEventArgs(SegmentStage.NShortPath, sb.toString()));
	}

	private void OnBeforeOptimize(
			java.util.ArrayList<WordResult[]> m_pWordSeg) {
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < m_pWordSeg.size(); k++) {
			for (int j = 0; j < m_pWordSeg.get(k).length; j++) {
				sb.append(String.format("%s, ", m_pWordSeg.get(k)[j].sWord));
			}
			sb.append("\r\n");
		}

		SendEvents(new SegmentEventArgs(SegmentStage.BeforeOptimize,
				sb.toString()));
	}

	@SuppressWarnings("unused")
	private void OnOptimumSegment(
			RowFirstDynamicArray<ChainContent> m_graphOptimum) {
		SendEvents(new SegmentEventArgs(SegmentStage.OptimumSegment,
				m_graphOptimum.toString()));
	}

	private void OnGenBiOptimumSegGraph(
			ColumnFirstDynamicArray<ChainContent> biOptGraph) {
		SendEvents(new SegmentEventArgs(SegmentStage.GenBiSegGraph,
				biOptGraph.toString()));
	}

}