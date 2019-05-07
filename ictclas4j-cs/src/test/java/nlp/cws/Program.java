
package nlp.cws;

import nlp.cws.utility.Utility;
import nlp.cws.utility.WordResult;

public class Program {

	static final String testDir = "D:\\gitWorkspace\\ictclas4j\\data\\";

	public static void main(String[] args) {
		String DictPath = testDir;
		System.out.println("正在初始化字典库，请稍候...");
		WordSegmentSample sample = new WordSegmentSample(DictPath, 2);

		String str = "王晓平在1月份滦南大会上说的确实在理";
		testStrSeg(sample, str);
		

		
		String str3 = "弟弟：艾力扎提·艾则孜，身份证号码：65282620081204001X，焉耆县第二小学四年级学生。    ";
		testStrSeg(sample, str3);

	}

	private static void testStrSeg(WordSegmentSample sample, String str) {
		
		String str2 = str.replaceAll("\r\n", " ");
		java.util.ArrayList<WordResult[]> result = sample.Segment(str2);

		PrintResult(result);
	}

	private static void PrintResult(java.util.ArrayList<WordResult[]> result) {
		System.out.println();
		for (int i = 0; i < result.size(); i++) {
			for (int j = 1; j < result.get(i).length - 1; j++) {
				System.out.printf("%1$s /%2$s ", result.get(i)[j].sWord,
						Utility.GetPOSString(result.get(i)[j].nPOS));
			}

			System.out.println();
		}
	}
}