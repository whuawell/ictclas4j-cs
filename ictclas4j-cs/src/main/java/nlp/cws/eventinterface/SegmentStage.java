package nlp.cws.eventinterface;

public enum SegmentStage {
	BeginSegment, // 开始分词
	AtomSegment, // 原子切分
	GenSegGraph, // 生成SegGraph
	GenBiSegGraph, // 生成BiSegGraph
	NShortPath, // N最短路径计算
	BeforeOptimize, // 对N最短路径进一步整理得到的结果
	OptimumSegment, // 初始OptimumSegmentGraph
	PersonAndPlaceRecognition, // 人名与地名识别后的OptimumSegmentGraph
	BiOptimumSegment, // 生成BiOptimumSegmentGraph
	FinishSegment; // 完成分词，输出结果

	public int getValue() {
		return this.ordinal();
	}

	public static SegmentStage forValue(int value) {
		return values()[value];
	}
}