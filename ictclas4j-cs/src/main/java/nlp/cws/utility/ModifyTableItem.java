package nlp.cws.utility;

   //--------------------------------------------------
   //data structure for dictionary index table item
   //--------------------------------------------------
   public class ModifyTableItem
   {
	  //The count number of words which initial letter is sInit
	  public int nCount;

	  //The number of deleted items in the index table
	  public int nDelete;

	  //The head of word items
	  public WordChain pWordItemHead;
   }