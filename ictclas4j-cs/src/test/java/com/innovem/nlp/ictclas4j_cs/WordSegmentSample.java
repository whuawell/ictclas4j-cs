package com.innovem.nlp.ictclas4j_cs;


import nlp.cws.top.WordSegment;
import nlp.cws.utility.WordResult;

public class WordSegmentSample {
	private int nKind = 1; // 在NShortPath方法中用来决定初步切分时分成几种结果
	private WordSegment wordSegment;

	// =======================================================
	// 构造函数，在没有指明nKind的情况下，nKind 取 1
	// =======================================================
	public WordSegmentSample(String dictPath) {
		this(dictPath, 1);
	}

	// =======================================================
	// 构造函数
	// =======================================================
	public WordSegmentSample(String dictPath, int nKind) {
		this.nKind = nKind;
		this.wordSegment = new WordSegment();
		// wordSegment.PersonRecognition = false;
		// wordSegment.PlaceRecognition = false;
		// wordSegment.TransPersonRecognition = false;

		// ---------- 订阅分词过程中的事件 ----------
		// C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style
		// event wireups:
		//wordSegment.OnSegmentEvent += new SegmentEventHandler(this.OnSegmentEventHandler);
		wordSegment.InitWordSegment(dictPath);
	}

	// =======================================================
	// 开始分词
	// =======================================================
	public final java.util.ArrayList<WordResult[]> Segment(String sentence) {
		return wordSegment.Segment(sentence, nKind);
	}


}