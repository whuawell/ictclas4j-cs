package nlp.cws.unknown;

import nlp.cws.segment.AtomNode;
import nlp.cws.segment.dynamicarray.ChainContent;
import nlp.cws.segment.dynamicarray.ChainItem;
import nlp.cws.segment.dynamicarray.RowFirstDynamicArray;
import nlp.cws.tag.Span;
import nlp.cws.tag.TAG_TYPE;
import nlp.cws.utility.Predefine;
import nlp.cws.utility.WordDictionary;
import nlp.cws.utility.WordResult;

public class UnknowWord {
	public WordDictionary m_dict = new WordDictionary(); // Unknown dictionary

	private Span m_roleTag = new Span(); // Role tagging
	private int m_nPOS; // The POS of such a category
	private String m_sUnknownFlags = null;


	// Unknown word recognition
	// pWordSegResult:word Segmentation result;
	// graphOptimum: The optimized segmentation graph
	// graphSeg: The original segmentation graph
	public final boolean Recognition(WordResult[] pWordSegResult,
			RowFirstDynamicArray<ChainContent> graphOptimum,
			java.util.ArrayList<AtomNode> atomSegment,
			WordDictionary dictCore) {
		ChainItem<ChainContent> item;
		int nStartPos = 0, j = 0, nAtomStart, nAtomEnd;
		double dValue;
		m_roleTag.POSTagging(pWordSegResult, dictCore, m_dict);
		// Tag the segmentation with unknown recognition roles according the
		// core dictionary and unknown recognition dictionary
		for (int i = 0; i < m_roleTag.m_nUnknownWordsCount; i++) {
			while (j < atomSegment.size()
					&& nStartPos < m_roleTag.m_nUnknownWords[i][0]) {
				nStartPos += atomSegment.get(j++).sWord.length();
			}

			nAtomStart = j;
			while (j < atomSegment.size()
					&& nStartPos < m_roleTag.m_nUnknownWords[i][1]) {
				nStartPos += atomSegment.get(j++).sWord.length();
			}

			nAtomEnd = j;
			if (nAtomStart < nAtomEnd) {
				item = graphOptimum.GetElement(nAtomStart, nAtomEnd);
				if (item != null) {
					dValue = item.Content.eWeight;
				} else {
					dValue = Predefine.INFINITE_VALUE;
				}

				if (dValue > m_roleTag.m_dWordsPossibility[i])
				// Set the element with less frequency
				{
					graphOptimum.SetElement(nAtomStart, nAtomEnd,
							new ChainContent(m_roleTag.m_dWordsPossibility[i],
									m_nPOS, m_sUnknownFlags));
				}
			}
		}
		return true;
	}




	// Load unknown recognition dictionary
	// Load context
	// type: Unknown words type (including person,place,transliterion and so on)
	  public final boolean Configure(String sConfigFile, TAG_TYPE type)
	  {
		 //Load the unknown recognition dictionary
		 m_dict.Load(sConfigFile + ".dct");

		 //Load the unknown recognition context
		 m_roleTag.LoadContext(sConfigFile + ".ctx");

		 //Set the tagging type
		 m_roleTag.SetTagType(type);
		 switch (type)
		 {
			case TT_PERSON:
			case TT_TRANS_PERSON:
			   //Set the special flag for transliterations
			   m_nPOS = -28274; //-'n'*256-'r';
			   m_sUnknownFlags = "未##人";
			   break;
			case TT_PLACE:
			   m_nPOS = -28275; //-'n'*256-'s';
			   m_sUnknownFlags = "未##地";
			   break;
			default:
			   m_nPOS = 0;
			   break;
		 }
		 return true;
	  }



	// Judge whether the name is a given name
	public final boolean IsGivenName(String sName) {
		char sFirstChar, sSecondChar;
		double dGivenNamePossibility = 0, dSingleNamePossibility = 0;
		if (sName.length() != 2) {
			return false;
		}

		sFirstChar = sName.toCharArray()[0];
		sSecondChar = sName.toCharArray()[1];

		// The possibility of P(Wi|Ti)
		dGivenNamePossibility += Math
				.log((double) m_dict
						.GetFrequency((new Character(sFirstChar)).toString(), 2)
						+ 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 2) + 1.0);
		dGivenNamePossibility += Math
				.log((double) m_dict.GetFrequency(
						(new Character(sSecondChar)).toString(), 3) + 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 3) + 1.0);
		// The possibility of conversion from 2 to 3
		dGivenNamePossibility += Math
				.log(m_roleTag.m_context.GetContextPossibility(0, 2, 3) + 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 2) + 1.0);

		// The possibility of P(Wi|Ti)
		dSingleNamePossibility += Math
				.log((double) m_dict
						.GetFrequency((new Character(sFirstChar)).toString(), 1)
						+ 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 1) + 1.0);
		dSingleNamePossibility += Math
				.log((double) m_dict.GetFrequency(
						(new Character(sSecondChar)).toString(), 4) + 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 4) + 1.0);
		// The possibility of conversion from 1 to 4
		dSingleNamePossibility += Math
				.log(m_roleTag.m_context.GetContextPossibility(0, 1, 4) + 1.0)
				- Math.log(m_roleTag.m_context.GetFrequency(0, 1) + 1.0);

		if (dSingleNamePossibility >= dGivenNamePossibility)
		// ����||m_dict.GetFrequency(sFirstChar,1)/m_dict.GetFrequency(sFirstChar,2)>=10
		// The possibility being a single given name is more than being a 2-char
		// given name
		{
			return false;
		}
		return true;
	}



	public final void ReleaseUnknowWord() {
		m_dict.ReleaseDict();
		m_roleTag.ReleaseSpan();
	}



}