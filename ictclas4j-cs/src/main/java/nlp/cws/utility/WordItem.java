package nlp.cws.utility;

   //--------------------------------------------------
   // data structure for word item
   //--------------------------------------------------
   public class WordItem
   {
	  public int nWordLen;

	  //The word 
	  public String sWord;

	  //the process or information handle of the word
	  public int nPOS;

	  //The count which it appear
	  public int nFrequency;

	@Override
	public String toString() {
		return "WordItem [nWordLen=" + nWordLen + ", sWord=" + sWord + ", nPOS="
				+ nPOS + ", nFrequency=" + nFrequency + "]";
	}
	  
	  
   }