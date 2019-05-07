package nlp.cws.top;

import nlp.cws.eventinterface.SegmentEventArgs;
import nlp.cws.eventinterface.SegmentStage;
import nlp.cws.segment.Segment;
import nlp.cws.segment.dynamicarray.ChainContent;
import nlp.cws.segment.dynamicarray.RowFirstDynamicArray;
import nlp.cws.tag.Span;
import nlp.cws.tag.TAG_TYPE;
import nlp.cws.unknown.UnknowWord;
import nlp.cws.utility.Predefine;
import nlp.cws.utility.Utility;
import nlp.cws.utility.WordDictionary;
import nlp.cws.utility.WordResult;

public class WordSegment {
	private double m_dSmoothingPara;
	private String m_pNewSentence;
	private Segment m_Seg; // Seg class
	private WordDictionary m_dictCore, m_dictBigram; // Core dictionary,bigram
														// dictionary
	private Span m_POSTagger; // POS tagger
	private UnknowWord m_uPerson, m_uTransPerson, m_uPlace; // Person
															// recognition

	public boolean PersonRecognition = true; // �Ƿ��������ʶ��
	public boolean TransPersonRecognition = true; // �Ƿ���з�������ʶ��
	public boolean PlaceRecognition = true; // �Ƿ���е���ʶ��

	public WordSegment() {
		this(0.1);
	}

	public WordSegment(double SmoothingParameter) {
		m_dictCore = new WordDictionary();
		m_dictBigram = new WordDictionary();
		m_POSTagger = new Span();
		m_uPerson = new UnknowWord();
		m_uTransPerson = new UnknowWord();
		m_uPlace = new UnknowWord();
		m_Seg = new Segment(m_dictBigram, m_dictCore);

		m_dSmoothingPara = SmoothingParameter; // Smoothing parameter
	}

	public final boolean InitWordSegment(String pPath) {
		String filename;

		filename = pPath + "coreDict.dct";
		if (!m_dictCore.Load(filename)) {
			return false;
		}

		filename = pPath + "lexical.ctx";
		if (!m_POSTagger.LoadContext(filename)) {
			return false;
		}
		m_POSTagger.SetTagType();

		filename = pPath + "nr";
		if (!m_uPerson.Configure(filename, TAG_TYPE.TT_PERSON)) {
			return false;
		}

		filename = pPath + "ns";
		if (!m_uPlace.Configure(filename, TAG_TYPE.TT_PLACE)) {
			return false;
		}

		filename = pPath + "tr";
		if (!m_uTransPerson.Configure(filename, TAG_TYPE.TT_TRANS_PERSON)) {
			return false;
		}

		filename = pPath + "BigramDict.dct";
		if (!m_dictBigram.Load(filename)) {
			return false;
		}

		return true;
	}

	public final java.util.ArrayList<WordResult[]> Segment(String sentence) {
		return Segment(sentence, 1);
	}

	public final java.util.ArrayList<WordResult[]> Segment(String sentence,
			int nKind) {
		OnBeginSegment(sentence);

		m_pNewSentence = Predefine.SENTENCE_BEGIN + sentence
				+ Predefine.SENTENCE_END;
		int nResultCount = m_Seg.BiSegment(m_pNewSentence, m_dSmoothingPara,
				nKind);

		for (int i = 0; i < nResultCount; i++) {
			if (this.PersonRecognition) {
				m_uPerson.Recognition(m_Seg.m_pWordSeg.get(i),
						m_Seg.m_graphOptimum, m_Seg.atomSegment, m_dictCore);
			}

			if (this.TransPersonRecognition) {
				m_uTransPerson.Recognition(m_Seg.m_pWordSeg.get(i),
						m_Seg.m_graphOptimum, m_Seg.atomSegment, m_dictCore);
			}

			if (this.PlaceRecognition) {
				m_uPlace.Recognition(m_Seg.m_pWordSeg.get(i),
						m_Seg.m_graphOptimum, m_Seg.atomSegment, m_dictCore);
			}
		}
		OnPersonAndPlaceRecognition(m_Seg.m_graphOptimum);

		m_Seg.BiOptimumSegment(1, m_dSmoothingPara);

		for (int i = 0; i < m_Seg.m_pWordSeg.size(); i++) {
			m_POSTagger.POSTagging(m_Seg.m_pWordSeg.get(i), m_dictCore,
					m_dictCore);
		}

		OnFinishSegment(m_Seg.m_pWordSeg);

		return m_Seg.m_pWordSeg;
	}

	public static void SendEvents(SegmentEventArgs e) {

		switch (e.Stage) {
			case BeginSegment :
				System.out.println("\r\n==== 原始句子：\r\n");
				System.out.println(e.Info + "\r\n");
				break;
			case AtomSegment :
				System.out.println("\r\n==== 原子切分：\r\n");
				System.out.println(e.Info);
				break;
			case GenSegGraph :
				System.out.println("\r\n==== 生成 segGraph：\r\n");
				System.out.println(e.Info);
				break;
			case GenBiSegGraph :
				System.out.println("\r\n==== 生成 biSegGraph：\r\n");
				System.out.println(e.Info);
				break;
			case NShortPath :
				System.out.println("\r\n==== NShortPath 初步切分的到的 N 个结果：\r\n");
				System.out.println(e.Info);
				break;
			case BeforeOptimize :
				System.out.println("\r\n==== 经过数字、日期合并等策略处理后的 N 个结果：\r\n");
				System.out.println(e.Info);
				break;
			case OptimumSegment :
				System.out.println("\r\n==== 将 N 个结果归并入OptimumSegment：\r\n");
				System.out.println(e.Info);
				break;
			case PersonAndPlaceRecognition :
				System.out.println("\r\n==== 加入对姓名、翻译人名以及地名的识别：\r\n");
				System.out.println(e.Info);
				break;
			case BiOptimumSegment :
				System.out.println(
						"\r\n==== 对加入对姓名、地名的OptimumSegment生成BiOptimumSegment：\r\n");
				System.out.println(e.Info);
				break;
			case FinishSegment :
				System.out.println("\r\n==== 最终识别结果：\r\n");
				System.out.println(e.Info);
				break;
		}

	}

	private void OnBeginSegment(String sentence) {
		SendEvents(new SegmentEventArgs(SegmentStage.BeginSegment, sentence));
	}

	private void OnPersonAndPlaceRecognition(
			RowFirstDynamicArray<ChainContent> m_graphOptimum) {
		SendEvents(new SegmentEventArgs(SegmentStage.PersonAndPlaceRecognition,
				m_graphOptimum.toString()));
	}

	private void OnFinishSegment(java.util.ArrayList<WordResult[]> m_pWordSeg) {
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < m_pWordSeg.size(); k++) {
			for (int j = 1; j < m_pWordSeg.get(k).length - 1; j++) {
				sb.append(String.format("%1$s /%2$s ",
						m_pWordSeg.get(k)[j].sWord,
						Utility.GetPOSString(m_pWordSeg.get(k)[j].nPOS)));
			}
			sb.append("\r\n");
		}

		SendEvents(new SegmentEventArgs(SegmentStage.FinishSegment,
				sb.toString()));
	}

	@SuppressWarnings("unused")
	private void OnSegmentEventHandler(Object sender, SegmentEventArgs e) {
		SendEvents(e);
	}

}