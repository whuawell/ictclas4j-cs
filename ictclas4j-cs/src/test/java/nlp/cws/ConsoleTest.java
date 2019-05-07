package nlp.cws;

import java.text.MessageFormat;

import nlp.cws.segment.AtomNode;
import nlp.cws.segment.Segment;
import nlp.cws.segment.dynamicarray.ChainContent;
import nlp.cws.segment.dynamicarray.ColumnFirstDynamicArray;
import nlp.cws.segment.dynamicarray.RowFirstDynamicArray;
import nlp.cws.segment.nshortpath.NShortPath;
import nlp.cws.utility.Predefine;
import nlp.cws.utility.Utility;
import nlp.cws.utility.WordDictionary;

public class ConsoleTest {

	public static String DictPath = "D:\\gitWorkspace\\ictclas4j\\data\\";
	public static String coreDictFile = DictPath + "coreDict.dct";
	public static String biDictFile = DictPath + "BigramDict.dct";
	public static String contextFile = DictPath + "nr.ctx";
	public static String nrFile = DictPath + "tr.dct";

	public static void main(String[] args) {
		//TestDictionary();
		//TestNShortPath();
		// TestAtomSegment();
		TestGenerateWordNet();
		// TestBiGraphGenerate();
		// TestBiSegment();
		// TestContextStat();
		// TestCCStringCompare();
	}
	/// #region 测试字典的读取

	public static void TestDictionary() {
		WordDictionary dict = new WordDictionary();
		if (dict.Load(coreDictFile, false)) {
			for (int j = 0; j <= 6767; j++) {
				System.out.println(MessageFormat.format(
						"====================================\r\n汉字:{0}, ID ：{1}\r\n",
						Utility.CC_ID2Char(j), j));

				System.out.println("  词长  频率  词性   词");
				for (int i = 0; i < dict.indexTable[j].nCount; i++) {
				
					System.out.println(MessageFormat.format("{0} {1} {2}  ({3}){4}", 
							dict.indexTable[j].WordItems[i].nWordLen,
							dict.indexTable[j].WordItems[i].nFrequency,
							Utility.GetPOSString(
									dict.indexTable[j].WordItems[i].nPOS),
							Utility.CC_ID2Char(j),
							dict.indexTable[j].WordItems[i].sWord));
				}
			}
		} else {
			System.out.println("Wrong!");
		}
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试原子分词

	public static void TestAtomSegment() {
		String sSentence = "三星SHX-132型号的(手机)1元钱２５６.８９元12.14%百分比12％";
		sSentence = Predefine.SENTENCE_BEGIN + sSentence
				+ Predefine.SENTENCE_END;
		java.util.ArrayList<AtomNode> nodes = Segment.AtomSegment(sSentence);
		for (int i = 0; i < nodes.size(); i++) {
			System.out.println(MessageFormat.format("{0} {1}", nodes.get(i).sWord,
					nodes.get(i).nPOS));
		}
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试 N 最短路径

	public static void TestNShortPath() {
		int n = 2;
		java.util.ArrayList<Integer[]> result;
		Integer[] aPath;

		ColumnFirstDynamicArray<ChainContent> apCost = new ColumnFirstDynamicArray<ChainContent>();
		apCost.SetElement(0, 1, new ChainContent(1));
		apCost.SetElement(1, 2, new ChainContent(1));
		apCost.SetElement(1, 3, new ChainContent(2));
		apCost.SetElement(2, 3, new ChainContent(1));
		apCost.SetElement(2, 4, new ChainContent(1));
		apCost.SetElement(3, 4, new ChainContent(1));
		apCost.SetElement(4, 5, new ChainContent(1));
		apCost.SetElement(3, 6, new ChainContent(2));
		apCost.SetElement(4, 6, new ChainContent(3));
		apCost.SetElement(5, 6, new ChainContent(1));
		System.out.println(apCost.toString());

		NShortPath.Calculate(apCost, n);
		NShortPath.printResultByIndex();

		// ----------------------------------------------------
		// 所有路径
		// ----------------------------------------------------
		System.out.println("\r\n\r\n所有路径：");
		for (int i = 0; i < n; i++) {
			result = NShortPath.GetPaths(i);
			for (int j = 0; j < result.size(); j++) {
				aPath = result.get(j);
				for (int k = 0; k < aPath.length; k++) {
					System.out.printf("%1$s, ", aPath[k]);
				}

				System.out.println();
			}
			System.out.println("========================");
		}

		// ----------------------------------------------------
		// 最佳路径
		// ----------------------------------------------------
		System.out.println("\r\n最佳路径：");
		aPath = NShortPath.GetBestPath();
		for (int k = 0; k < aPath.length; k++) {
			System.out.printf("%1$s, ", aPath[k]);
		}

		System.out.println();

		// ----------------------------------------------------
		// 最多 n 个路径
		// ----------------------------------------------------
		System.out.println("\r\n最多5 条路径：");
		result = NShortPath.GetNPaths(5);
		for (int j = 0; j < result.size(); j++) {
			aPath = result.get(j);
			for (int k = 0; k < aPath.length; k++) {
				System.out.printf("%1$s, ", aPath[k]);
			}

			System.out.println();
		}
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试初始分词

	public static void TestGenerateWordNet() {
		WordDictionary coreDict = new WordDictionary();
		if (!coreDict.Load(coreDictFile)) {
			System.out.println("字典装入错误！");
			return;
		}

		String sSentence = "他说的确实在理";
		sSentence = Predefine.SENTENCE_BEGIN + sSentence
				+ Predefine.SENTENCE_END;

		java.util.ArrayList<AtomNode> atomSegment = Segment
				.AtomSegment(sSentence);
		RowFirstDynamicArray<ChainContent> m_segGraph = Segment
				.GenerateWordNet(atomSegment, coreDict);

		System.out.println(m_segGraph.toString());
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试初次分词产生的二叉表

	public static void TestBiGraphGenerate() {
		WordDictionary coreDict = new WordDictionary();
		if (!coreDict.Load(coreDictFile)) {
			System.out.println("coreDict 字典装入错误！");
			return;
		}

		WordDictionary biDict = new WordDictionary();
		if (!biDict.Load(biDictFile)) {
			System.out.println("字典装入错误！");
			return;
		}

		String sSentence = "他说的确实在理";
		sSentence = Predefine.SENTENCE_BEGIN + sSentence
				+ Predefine.SENTENCE_END;

		// ---原子分词
		java.util.ArrayList<AtomNode> atomSegment = Segment
				.AtomSegment(sSentence);

		// ---检索词库，加入所有可能分词方案并存入链表结构
		RowFirstDynamicArray<ChainContent> segGraph = Segment
				.GenerateWordNet(atomSegment, coreDict);

		// ---检索所有可能的两两组合
		ColumnFirstDynamicArray<ChainContent> biGraphResult = Segment
				.BiGraphGenerate(segGraph, 0.1, biDict, coreDict);

		System.out.println(biGraphResult.toString());
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试 Segment.BiSegment

	public static void TestBiSegment() {
		java.util.ArrayList<String> sentence = new java.util.ArrayList<String>();
		java.util.ArrayList<String> description = new java.util.ArrayList<String>();

		sentence.add("他说的确实在理");
		description.add("普通分词测试");

		sentence.add("张华平3－4月份来北京开会");
		description.add("数字切分");

		sentence.add("1.加强管理");
		description.add("剔除多余的“.”");

		sentence.add("他出生于1980年1月1日10点");
		description.add("日期合并");

		sentence.add("他出生于甲子年");
		description.add("年份识别");

		sentence.add("馆内陈列周恩来和邓颖超生前使用过的物品");
		description.add("姓名识别");

		WordDictionary coreDict = new WordDictionary();
		if (!coreDict.Load(coreDictFile)) {
			System.out.println("coreDict 字典装入错误！");
			return;
		}

		WordDictionary biDict = new WordDictionary();
		if (!biDict.Load(biDictFile)) {
			System.out.println("字典装入错误！");
			return;
		}

		String sSentence;
		String sDescription;

		for (int i = 0; i < sentence.size(); i++) {
			sSentence = sentence.get(i);
			sDescription = description.get(i);
			System.out.println(
					"\r\n============ " + sDescription + " ============");

			sSentence = Predefine.SENTENCE_BEGIN + sSentence
					+ Predefine.SENTENCE_END;

			java.util.ArrayList<AtomNode> nodes = Segment
					.AtomSegment(sSentence);
			System.out.println("原子切分：");
			for (int j = 0; j < nodes.size(); j++) {
				System.out.printf("%1$s, ", nodes.get(j).sWord);
			}

			System.out.println("\r\n\r\n实际切分：");
			Segment segment = new Segment(biDict, coreDict);
			segment.BiSegment(sSentence, 0.1, 1);

			for (int k = 0; k < segment.m_pWordSeg.size(); k++) {
				for (int j = 0; j < segment.m_pWordSeg.get(k).length; j++) {
					System.out.printf("%1$s, ",
							segment.m_pWordSeg.get(k)[j].sWord);
				}
				System.out.println();
			}
		}
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试 ContextStat

	// public static void TestContextStat() {
	// ContextStat cs = new ContextStat();
	//
	// if (cs.Load(contextFile)) {
	// if (!cs.Save(DictPath + "nr.ctx")) {
	// System.out.println("写文件失败！");
	// } else {
	// System.out.println("OK!");
	// }
	// } else {
	// System.out.println("文件装载失败！");
	// }
	// }

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #region 测试 CCStringCompare

	public static void TestCCStringCompare() {
		String[] s = {"公开赛", "公开赛", "公开信", "公开性", "公款", "公款吃喝", "公厘", "公理",
				"公理", "公里", "公里/小时", "公里／小时", "公里／小时", "公里数", "公历", "公例", "公立",
				"公粮", "公路", "公路", "公路局", "公路桥", "公路网"};
		String[] s1 = {"王@、", "王@。", "王@”", "王@』", "王@，", "王@霸", "王@传", "王@大夫",
				"王@大娘", "王@大爷", "王@道士", "王@的", "王@家", "王@老汉", "王@两", "王@末##末",
				"王@女士", "王@未##人", "王@未##它", "王@先生", "王@姓", "王朝@，", "王朝@的",
				"王储@殿下", "王储@兼", "王储@未##人", "王府井@百货大楼", "王府井@大街", "王公@贵族",
				"王宫@会见", "王国@。", "王国@”", "王国@，", "王国@的", "王国@里", "王国@政府",
				"王后@未##人", "王码@电脑", "王牌@。", "王牌@”", "王室@成员", "王营@煤矿", "王兆国@、",
				"王兆国@，", "王兆国@出席", "王兆国@等", "王兆国@对", "王兆国@会见", "王兆国@及",
				"王兆国@今天", "王兆国@受", "王兆国@说", "王兆国@在", "王兆国@指出", "王兆国@主持",
				"王子@的"};

		for (int i = 0; i < s.length - 1; i++) {
			if (Utility.CCStringCompare(s[i], s[i + 1]) >= 0
					&& s[i].compareTo(s[i + 1]) != 0) {
				System.out.println("出现错误：" + s[i] + "   <-->   " + s[i + 1]);
			}
		}

		for (int i = 0; i < s1.length - 1; i++) {
			if (Utility.CCStringCompare(s1[i], s1[i + 1]) >= 0
					&& s1[i].compareTo(s1[i + 1]) != 0) {
				System.out.println("出现错误：" + s1[i] + "   <-->   " + s1[i + 1]);
			}
		}
	}

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion
}