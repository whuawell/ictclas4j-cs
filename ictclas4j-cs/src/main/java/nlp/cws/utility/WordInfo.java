package nlp.cws.utility;

   //--------------------------------------------------
   // return value of GetWordInfos Method in Dictionary.cs
   //--------------------------------------------------
   public class WordInfo
   {
	  public String sWord;
	  public int Count = 0;

	  public java.util.ArrayList<Integer> POSs = new java.util.ArrayList<Integer>();
	  public java.util.ArrayList<Integer> Frequencies = new java.util.ArrayList<Integer>();
   }