package nlp.cws.utility;

import java.util.Arrays;

//--------------------------------------------------
   //data structure for dictionary index table item
   //--------------------------------------------------
   public class IndexTableItem
   {
	  //The count number of words which initial letter is sInit
	  public int nCount;

	  //The  head of word items
	  public WordItem[] WordItems;

	@Override
	public String toString() {
		return "IndexTableItem [nCount=" + nCount + ", WordItems="
				+ Arrays.toString(WordItems) + "]";
	}
	  
	  
   }