package nlp.cws.utility;
import java.util.regex.Pattern;

public class Regex {

	public static boolean IsMatch(String sourceStr, String targetStr) {
		Pattern p = Pattern.compile(targetStr);
		return p.matcher(sourceStr).matches();
	}
}
