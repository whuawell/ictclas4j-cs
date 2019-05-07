
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
		
		String str2 = "艾力夏提•艾则孜，男，户籍地址：焉耆县新城路16号院3号楼7单元402室，现住址：焉耆县文苑小区5号楼1单元402室，联系电话：18599240007，此人2016年7月毕业于焉耆县第一中学，2016年7月考上了北京体育大学教育学院体育教育专业，2016年9月至2018年7月在新疆师范大学上预科班，预科班班主任：母洁，联系电话：18690973207。艾力夏提•艾则孜2018年8月19日8时41分从焉耆县坐火车前往乌鲁木齐，2018年13时53份从乌鲁木齐坐火车前往北京，2018年8月23日向北京体育大学报到。                                  \r\n" + 
				"父亲:艾则孜•艾沙，男，维，身份证号码：652826197507023212,职业：焉耆县第二小学体育教师，户籍地址：焉耆县新城路16号院3号楼7单元402室，现住址：焉耆县文苑小区5号楼1单元402室，联系电话：13150231222；" + 
				"母亲：阿依先木•吉力力，女，维， 身份证号码：652826197103182620，职业：焉耆县第三中学教师，户籍地址：焉耆县新城路16号院3号楼7单元402室，现住址：焉耆县文苑小区5号楼1单元402室，联系电话：18599240005。" + 
				"弟弟：艾力扎提·艾则孜，身份证号码：652826200812040013，焉耆县第二小学四年级学生。                                                     " + 
				"艾力夏提•艾则孜是2017年8月自治区一体化推送人员，属于五类不予收押收教人员，此人舅舅阿力普·吉力力在焉耆县教培局学习的收教人员。艾力夏提•艾则孜父母都是教育系统工作人员，经通过电话联系新疆师范大学预科班班主任母洁，母洁老师称：艾力夏提•艾则孜在师范大学上预科期间现实表现良好，思想动态正常，能与同学之间和睦相处，无异常情况 。此人及父母现实表现正常，思想动态正常，暂未发现异常情况。";
		//testStrSeg(sample, str2);
		
		String str3 = "弟弟：艾力扎提·艾则孜，身份证号码：652826200812040013，焉耆县第二小学四年级学生。    ";
		//testStrSeg(sample, str3);

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