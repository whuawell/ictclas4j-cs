package nlp.cws.utility;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import nlp.cws.segment.dynamicarray.DynamicArray.RefObject;


public class WordDictionary {
	public boolean bReleased = true;

	public IndexTableItem[] indexTable;
	public ModifyTableItem[] modifyTable;


	public final boolean Load(String sFilename) {
		return Load(sFilename, false);
	}

	// ====================================================================
	// Func Name : Load
	// Description: Load the dictionary from the file .dct
	// Parameters : sFilename: the file name
	// Returns : success or fail
	// ====================================================================
	public final boolean Load(String sFilename, boolean bReset) {
		int frequency, wordLength, pos; // Ƶ�ʡ��ʳ�����ȡ����
		boolean isSuccess = true;

		// BufferedReader reader = new BufferedReader(new InputStreamReader(new
		// FileInputStream(sFilename), "gb2312"));
		DataInputStream in = null;
		try {
			in = new DataInputStream(
					new BufferedInputStream(new FileInputStream(sFilename)));

			indexTable = new IndexTableItem[Predefine.CC_NUM];

			bReleased = false;
			for (int i = 0; i < Predefine.CC_NUM; i++) {
				// ��ȡ�Ըú��ִ�ͷ�Ĵ��ж��ٸ�
				indexTable[i] = new IndexTableItem();

				indexTable[i].nCount = GFCommon
						.bytes2int(Utility.readBytes(in, 4), false);

				if (indexTable[i].nCount <= 0) {
					continue;
				}

				indexTable[i].WordItems = new WordItem[indexTable[i].nCount];

				for (int j = 0; j < indexTable[i].nCount; j++) {
					indexTable[i].WordItems[j] = new WordItem();

					frequency = GFCommon.bytes2int(Utility.readBytes(in, 4),
							false); // ��ȡƵ��
					wordLength = GFCommon.bytes2int(Utility.readBytes(in, 4),
							false); // ��ȡ�ʳ�
					pos = GFCommon.bytes2int(Utility.readBytes(in, 4), false); // ��ȡ����

					if (wordLength > 0) {
						indexTable[i].WordItems[j].sWord = Utility
								.ByteArray2String(
										Utility.readBytes(in, wordLength));
					} else {
						indexTable[i].WordItems[j].sWord = "";
					}

					// Reset the frequency
					if (bReset) {
						indexTable[i].WordItems[j].nFrequency = 0;
					} else {
						indexTable[i].WordItems[j].nFrequency = frequency;
					}

					indexTable[i].WordItems[j].nWordLen = wordLength;
					indexTable[i].WordItems[j].nPOS = pos;
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			isSuccess = false;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		return isSuccess;
	}


	// ====================================================================
	// Func Name : Save
	// Description: Save the dictionary as the file .dct
	// Parameters : sFilename: the file name
	// Returns : success or fail
	// ====================================================================
	public final boolean Save(String sFilename) {
		boolean isSuccess = true;

		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new FileOutputStream(sFilename));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {

			// 对图一中所示的6768个数据块进行遍历
			for (int i = 0; i < Predefine.CC_NUM; i++) {
				// 如果发生了修改，则完成indexTable与modifyTable归并排序式的合并工作并存盘（排序原则是先安sWord排，然后再按词性排）
				if (modifyTable != null)
					MergeAndSaveIndexTableItem(out, indexTable[i],
							modifyTable[i]);
				else
					// 否则直接写入indexTable
					SaveIndexTableItem(out, indexTable[i]);
			}
		} catch (java.lang.Exception e) {
			isSuccess = false;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		return isSuccess;
	}

	private void MergeAndSaveIndexTableItem(DataOutputStream writer,
			IndexTableItem item, ModifyTableItem modifyItem) {

		try {

			int j, nCount; // 频率、词长、读取词性
			WordChain pCur;

			// 计算修改后有效词块的数目
			nCount = item.nCount + modifyItem.nCount - modifyItem.nDelete;
			writer.write(GFCommon.int2bytes(nCount, false));

			pCur = modifyItem.pWordItemHead;

			j = 0;
			// 对原表中的词块和修改表中的词块进行遍历,并把修改后的添加到原表中
			while (pCur != null && j < item.nCount) {
				// 如果修改表中的词小于原表中对应位置的词或者长度相等但nHandle值比原表中的小,则把修改表中的写入到词典文件当中.
				if (Utility.CCStringCompare(pCur.data.sWord,
						item.WordItems[j].sWord) < 0
						|| ((pCur.data.sWord.equals(item.WordItems[j].sWord))
								&& (pCur.data.nPOS < item.WordItems[j].nPOS))) {
					// Output the modified data to the file
					SaveWordItem(writer, pCur.data);
					pCur = pCur.next;
				}
				// 频度nFrequecy等于-1说明该词已被删除,跳过它
				else if (item.WordItems[j].nFrequency == -1) {
					j++;
				}
				// 如果修改表中的词长度比原表中的长度大或 长度相等但句柄值要多,就把原表的词写入的词典文件中
				else if (Utility.CCStringCompare(pCur.data.sWord,
						item.WordItems[j].sWord) > 0
						|| ((pCur.data.sWord.equals(item.WordItems[j].sWord))
								&& (pCur.data.nPOS > item.WordItems[j].nPOS))) {
					// Output the index table data to the file
					SaveWordItem(writer, item.WordItems[j]);
					j++;
				}
			}
			// 如果归并结束后indexTable有剩余，则继续写完indexTable中的数据
			if (j < item.nCount) {
				for (int i = j; i < item.nCount; i++) {
					if (item.WordItems[j].nFrequency != -1) {
						SaveWordItem(writer, item.WordItems[i]);
					}
				}
			}
			// 否则继续写完modifyTable中的数据
			else {
				while (pCur != null) {
					// Output the modified data to the file
					SaveWordItem(writer, pCur.data);
					pCur = pCur.next;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void SaveIndexTableItem(DataOutputStream writer,
			IndexTableItem item) {

		try {
			writer.write(item.nCount);

			for (int i = 0; i < item.nCount; i++) {
				SaveWordItem(writer, item.WordItems[i]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void SaveWordItem(DataOutputStream writer, WordItem item) {

		try {
			int frequency = item.nFrequency;
			int wordLength = item.nWordLen;
			int handle = item.nPOS;

			writer.write(frequency);
			writer.write(wordLength);
			writer.write(handle);

			if (wordLength > 0) {
				writer.write(new String(item.sWord).getBytes("gb2312"));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	// ====================================================================
	// Func Name : AddItem
	// Description: Add a word item to the dictionary
	// Parameters : sWord: the word
	// nHandle:the handle number
	// nFrequency: the frequency
	// Returns : success or fail
	// ====================================================================
	public final boolean AddItem(String sWord, int nPOS, int nFrequency) {
		int nPos = 0, nFoundPos = 0;
		WordChain pRet = null, pTemp, pNext;
		String sWordAdd = null;

		// Ԥ����,ȥ���ʵ�ǰ��Ŀո�
		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_nPos = new RefObject<Integer>(nPos);
		RefObject<String> tempRef_sWordAdd = new RefObject<String>(sWordAdd);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_nPos,
				tempRef_sWordAdd);
		sWord = tempRef_sWord.argvalue;
		nPos = tempRef_nPos.argvalue;
		sWordAdd = tempRef_sWordAdd.argvalue;
		if (tempVar) {
			return false;
		}

		RefObject<Integer> tempRef_nFoundPos = new RefObject<Integer>(
				nFoundPos);
		boolean tempVar2 = FindInOriginalTable(nPos, sWordAdd, nPOS,
				tempRef_nFoundPos);
		nFoundPos = tempRef_nFoundPos.argvalue;
		if (tempVar2) {
			// The word exists in the original table, so add the frequency
			// Operation in the index table and its items
			if (indexTable[nPos].WordItems[nFoundPos].nFrequency == -1) {
				// The word item has been removed
				indexTable[nPos].WordItems[nFoundPos].nFrequency = nFrequency;

				if (modifyTable == null) {
					modifyTable = new ModifyTableItem[Predefine.CC_NUM];
				}

				modifyTable[nPos].nDelete -= 1;
			} else {
				indexTable[nPos].WordItems[nFoundPos].nFrequency += nFrequency;
			}
			return true;
		}

		// The items not exists in the index table.
		// As following, we have to find the item whether exists in the modify
		// data region
		// If exists, change the frequency .or else add a item
		if (modifyTable == null) {
			modifyTable = new ModifyTableItem[Predefine.CC_NUM];
			for (int i = 0; i < Predefine.CC_NUM; i++) {
				modifyTable[i] = new ModifyTableItem();
			}
		}

		RefObject<WordChain> tempRef_pRet = new RefObject<WordChain>(pRet);
		boolean tempVar3 = FindInModifyTable(nPos, sWordAdd, nPOS,
				tempRef_pRet);
		pRet = tempRef_pRet.argvalue;
		if (tempVar3) {
			if (pRet != null) {
				pRet = pRet.next;
			} else {
				pRet = modifyTable[nPos].pWordItemHead;
			}

			pRet.data.nFrequency += nFrequency;
			return true;
		}

		// find the proper position to add the word to the modify data table and
		// link
		pTemp = new WordChain(); // Allocate the word chain node
		pTemp.data = new WordItem();
		pTemp.data.nPOS = nPOS; // store the handle
		pTemp.data.nWordLen = Utility.GetWordLength(sWordAdd);
		pTemp.data.sWord = sWordAdd;
		pTemp.data.nFrequency = nFrequency;
		pTemp.next = null;
		if (pRet != null) {
			pNext = pRet.next; // Get the next item before the current item
			pRet.next = pTemp; // link the node to the chain
		} else {
			pNext = modifyTable[nPos].pWordItemHead;
			modifyTable[nPos].pWordItemHead = pTemp; // Set the pAdd as the head
														// node
		}
		pTemp.next = pNext; // Very important!!!! or else it will lose some node

		modifyTable[nPos].nCount++; // the number increase by one
		return true;
	}



	public final boolean DelItem(String sWord, int nPOS) {
		String sWordDel = null;
		int nPos = 0, nFoundPos = 0, nTemp = 0;
		WordChain pPre = null, pCur;

		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_nPos = new RefObject<Integer>(nPos);
		RefObject<String> tempRef_sWordDel = new RefObject<String>(sWordDel);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_nPos,
				tempRef_sWordDel);
		sWord = tempRef_sWord.argvalue;
		nPos = tempRef_nPos.argvalue;
		sWordDel = tempRef_sWordDel.argvalue;
		if (tempVar) {
			return false;
		}

		RefObject<Integer> tempRef_nFoundPos = new RefObject<Integer>(
				nFoundPos);
		boolean tempVar2 = FindInOriginalTable(nPos, sWordDel, nPOS,
				tempRef_nFoundPos);
		nFoundPos = tempRef_nFoundPos.argvalue;
		if (tempVar2) {
			// Not prepare the buffer
			if (modifyTable == null) {
				modifyTable = new ModifyTableItem[Predefine.CC_NUM];
			}

			indexTable[nPos].WordItems[nFoundPos].nFrequency = -1;
			modifyTable[nPos].nDelete += 1;

			// Remove all items which word is sWordDel,ignoring the handle
			if (nPOS == -1) {
				nTemp = nFoundPos + 1; // Check its previous position
				while (nTemp < indexTable[nPos].nCount
						&& indexTable[nPos].WordItems[nFoundPos].sWord
								.compareTo(sWordDel) == 0) {
					indexTable[nPos].WordItems[nTemp].nFrequency = -1;
					modifyTable[nPos].nDelete += 1;
					nTemp += 1;
				}
			}
			return true;
		}

		// Operation in the modify table and its items
		RefObject<WordChain> tempRef_pPre = new RefObject<WordChain>(pPre);
		boolean tempVar3 = FindInModifyTable(nPos, sWordDel, nPOS,
				tempRef_pPre);
		pPre = tempRef_pPre.argvalue;
		if (tempVar3) {
			pCur = modifyTable[nPos].pWordItemHead;
			if (pPre != null) {
				pCur = pPre.next;
			}
			while (pCur != null
					&& pCur.data.sWord.compareToIgnoreCase(sWordDel) == 0
					&& (pCur.data.nPOS == nPOS || nPOS < 0)) {
				// pCur is the first item
				if (pPre != null) {
					pPre.next = pCur.next;
				} else {
					modifyTable[nPos].pWordItemHead = pCur.next;
				}

				pCur = pCur.next;
			}
			return true;
		}
		return false;
	}



	// ====================================================================
	// Func Name : IsExist
	// Description: Check the sWord with nHandle whether exist
	// Parameters : sWord: the word
	// : nHandle: the nHandle
	// Returns : Is Exist
	// ====================================================================
	public final boolean IsExist(String sWord, int nHandle) {
		String sWordFind = null;
		int nPos = 0;

		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_nPos = new RefObject<Integer>(nPos);
		RefObject<String> tempRef_sWordFind = new RefObject<String>(sWordFind);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_nPos,
				tempRef_sWordFind);
		sWord = tempRef_sWord.argvalue;
		nPos = tempRef_nPos.argvalue;
		sWordFind = tempRef_sWordFind.argvalue;
		if (tempVar) {
			return false;
		}

		return (FindInOriginalTable(nPos, sWordFind, nHandle)
				|| FindInModifyTable(nPos, sWordFind, nHandle));
	}



	// ====================================================================
	// Func Name : GetWordType
	// Description: Get the type of word
	// Parameters : sWord: the word
	// Returns : the type
	// ====================================================================
	public final int GetWordType(String sWord) {
		int nType = Utility.charType(sWord.toCharArray()[0]);
		int nLen = Utility.GetWordLength(sWord);

		// Chinese word
		if (nLen > 0 && nType == Predefine.CT_CHINESE
				&& Utility.IsAllChinese(sWord)) {
			return Predefine.WT_CHINESE;
		}
		// Delimiter
		else if (nLen > 0 && nType == Predefine.CT_DELIMITER) {
			return Predefine.WT_DELIMITER;
		}
		// other invalid
		else {
			return Predefine.WT_OTHER;
		}
	}



	public final WordInfo GetWordInfo(String sWord) {
		WordInfo info = new WordInfo();
		info.sWord = sWord;

		String sWordGet = null;
		int nFirstCharId = 0, nFoundPos = 0;
		WordChain pPre = null, pCur;

		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_nFirstCharId = new RefObject<Integer>(
				nFirstCharId);
		RefObject<String> tempRef_sWordGet = new RefObject<String>(sWordGet);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_nFirstCharId,
				tempRef_sWordGet);
		sWord = tempRef_sWord.argvalue;
		nFirstCharId = tempRef_nFirstCharId.argvalue;
		sWordGet = tempRef_sWordGet.argvalue;
		if (tempVar) {
			return null;
		}

		RefObject<Integer> tempRef_nFoundPos = new RefObject<Integer>(
				nFoundPos);
		boolean tempVar2 = FindFirstMatchItemInOrgTbl(nFirstCharId, sWordGet,
				tempRef_nFoundPos);
		nFoundPos = tempRef_nFoundPos.argvalue;
		if (tempVar2) {
			while (nFoundPos < indexTable[nFirstCharId].nCount
					&& indexTable[nFirstCharId].WordItems[nFoundPos].sWord
							.compareTo(sWordGet) == 0) {
				info.POSs.add(
						indexTable[nFirstCharId].WordItems[nFoundPos].nPOS);
				info.Frequencies.add(
						indexTable[nFirstCharId].WordItems[nFoundPos].nFrequency);
				info.Count++;

				nFoundPos++;
			}
			return info;
		}

		// Operation in the index table and its items
		RefObject<WordChain> tempRef_pPre = new RefObject<WordChain>(pPre);
		boolean tempVar3 = FindInModifyTable(nFirstCharId, sWordGet,
				tempRef_pPre);
		pPre = tempRef_pPre.argvalue;
		if (tempVar3) {
			pCur = modifyTable[nFirstCharId].pWordItemHead;

			if (pPre != null) {
				pCur = pPre.next;
			}

			while (pCur != null
					&& pCur.data.sWord.compareToIgnoreCase(sWordGet) == 0) {
				info.POSs.add(pCur.data.nPOS);
				info.Frequencies.add(pCur.data.nFrequency);
				info.Count++;
				pCur = pCur.next;
			}
			return info;
		}
		return null;
	}



	// ====================================================================
	// Func Name : GetMaxMatch
	// Description: Get the max match to the word
	// Parameters : nHandle: the only handle which will be attached to the word
	// Returns : success or fail
	// ====================================================================
	public final boolean GetMaxMatch(String sWord, RefObject<String> sWordRet,
			RefObject<Integer> nPOSRet) {
		String sWordGet = null, sFirstChar = null;
		int nFirstCharId = 0;
		WordChain pCur;

		sWordRet.argvalue = "";
		nPOSRet.argvalue = -1;

		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_nFirstCharId = new RefObject<Integer>(
				nFirstCharId);
		RefObject<String> tempRef_sWordGet = new RefObject<String>(sWordGet);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_nFirstCharId,
				tempRef_sWordGet);
		sWord = tempRef_sWord.argvalue;
		nFirstCharId = tempRef_nFirstCharId.argvalue;
		sWordGet = tempRef_sWordGet.argvalue;
		if (tempVar) {
			return false;
		}

		sFirstChar = (new Character(Utility.CC_ID2Char(nFirstCharId)))
				.toString();

		// ��indexTable�м�����sWordGet��ͷ����Ŀ
		int i = 0;
		while (i < indexTable[nFirstCharId].nCount) {
			if (indexTable[nFirstCharId].WordItems[i].sWord
					.startsWith(sWordGet)) {
				sWordRet.argvalue = sFirstChar
						+ indexTable[nFirstCharId].WordItems[i].sWord;
				nPOSRet.argvalue = indexTable[nFirstCharId].WordItems[i].nPOS;
				return true;
			}
			i++;
		}

		// ��indexTable��û���ҵ�����modifyTable��ȥ��
		if (modifyTable == null) {
			return false;
		}

		pCur = modifyTable[nFirstCharId].pWordItemHead;
		while (pCur != null) {
			if (pCur.data.sWord.startsWith(sWordGet)) {
				sWordRet.argvalue = sFirstChar + pCur.data.sWord;
				nPOSRet.argvalue = pCur.data.nPOS;
				return true;
			}
			pCur = pCur.next;
		}

		return false;
	}



	// ====================================================================
	// ���Ҵ���ΪnPOS��sWord�Ĵ�Ƶ
	// ====================================================================
	public final int GetFrequency(String sWord, int nPOS) {
		String sWordFind = null;
		int firstCharCC_ID = 0, nIndex = 0;
		WordChain pFound = null;

		RefObject<String> tempRef_sWord = new RefObject<String>(sWord);
		RefObject<Integer> tempRef_firstCharCC_ID = new RefObject<Integer>(
				firstCharCC_ID);
		RefObject<String> tempRef_sWordFind = new RefObject<String>(sWordFind);
		boolean tempVar = !PreProcessing(tempRef_sWord, tempRef_firstCharCC_ID,
				tempRef_sWordFind);
		sWord = tempRef_sWord.argvalue;
		firstCharCC_ID = tempRef_firstCharCC_ID.argvalue;
		sWordFind = tempRef_sWordFind.argvalue;
		if (tempVar) {
			return 0;
		}

		RefObject<Integer> tempRef_nIndex = new RefObject<Integer>(nIndex);
		boolean tempVar2 = FindInOriginalTable(firstCharCC_ID, sWordFind, nPOS,
				tempRef_nIndex);
		nIndex = tempRef_nIndex.argvalue;
		if (tempVar2) {
			return indexTable[firstCharCC_ID].WordItems[nIndex].nFrequency;
		}

		RefObject<WordChain> tempRef_pFound = new RefObject<WordChain>(pFound);
		boolean tempVar3 = FindInModifyTable(firstCharCC_ID, sWordFind, nPOS,
				tempRef_pFound);
		pFound = tempRef_pFound.argvalue;
		if (tempVar3) {
			return pFound.data.nFrequency;
		}

		return 0;
	}



	public final void ReleaseDict() {
		for (int i = 0; i < Predefine.CC_NUM; i++) {
			for (int j = 0; indexTable[i] != null
					&& j < indexTable[i].nCount; j++) {
				indexTable[i] = null;
			}
		}

		modifyTable = null;
	}



	// ====================================================================
	// Func Name : MergePOS
	// Description: Merge all the POS into nPOS,
	// just get the word in the dictionary and set its POS as nPOS
	// Parameters : nPOS: the only handle which will be attached to the word
	// Returns : the type
	// ====================================================================
	public final boolean MergePOS(int nPOS) {
		int i, j, nCompare;
		String sWordPrev;
		WordChain pPre, pCur;

		// Not prepare the buffer
		if (modifyTable == null) {
			modifyTable = new ModifyTableItem[Predefine.CC_NUM];
		}

		// Operation in the index table
		for (i = 0; i < Predefine.CC_NUM; i++) {
			// delete the memory of word item array in the dictionary
			sWordPrev = null; // Set empty
			for (j = 0; j < indexTable[i].nCount; j++) {
				nCompare = Utility.CCStringCompare(sWordPrev,
						indexTable[i].WordItems[j].sWord);
				if ((j == 0 || nCompare < 0)
						&& indexTable[i].WordItems[j].nFrequency != -1) {
					// Need to modify its handle
					indexTable[i].WordItems[j].nPOS = nPOS; // Change its handle
					sWordPrev = indexTable[i].WordItems[j].sWord;
					// Refresh previous Word
				} else if (nCompare == 0
						&& indexTable[i].WordItems[j].nFrequency != -1) {
					// Need to delete when not delete and same as previous word
					indexTable[i].WordItems[j].nFrequency = -1; // Set delete
																// flag
					modifyTable[i].nDelete += 1; // Add the number of being
													// deleted
				}
			}
		}
		for (i = 0; i < Predefine.CC_NUM; i++)
		// Operation in the modify table
		{
			pPre = null;
			pCur = modifyTable[i].pWordItemHead;
			sWordPrev = null; // Set empty
			while (pCur != null) {
				if (Utility.CCStringCompare(pCur.data.sWord, sWordPrev) > 0) {
					// The new word
					pCur.data.nPOS = nPOS; // Chang its handle
					sWordPrev = pCur.data.sWord; // Set new previous word
					pPre = pCur; // New previous pointer
					pCur = pCur.next;
				} else {
					if (pPre != null)
					// pCur is the first item
					{
						pPre.next = pCur.next;
					} else {
						modifyTable[i].pWordItemHead = pCur.next;
					}
					pCur = pCur.next;
				}
			}
		}
		return true;
	}



	// public final boolean ToTextFile(String sFileName) {
	// boolean isSuccess = true;
	// FileStream outputFile = null;
	// StreamWriter writer = null;
	//
	// // Modification made, not to output when modify table exists.
	// if (modifyTable != null) {
	// return false;
	// }
	//
	// try {
	// outputFile = new FileStream(sFileName, FileMode.Create,
	// FileAccess.Write);
	// if (outputFile == null) {
	// return false;
	// }
	//
	// writer = new StreamWriter(outputFile,
	// Encoding.GetEncoding("gb2312"));
	//
	// for (int j = 0; j < Predefine.CC_NUM; j++) {
	// writer.WriteLine(
	// "====================================\r\n����:{0}, ID ��{1}\r\n",
	// Utility.CC_ID2Char(j), j);
	//
	// writer.WriteLine(" �ʳ� Ƶ�� ���� ��");
	// for (int i = 0; i < indexTable[j].nCount; i++) {
	// writer.WriteLine("{0,5} {1,6} {2,5} ({3}){4}",
	// indexTable[j].WordItems[i].nWordLen,
	// indexTable[j].WordItems[i].nFrequency,
	// Utility.GetPOSString(
	// indexTable[j].WordItems[i].nPOS),
	// Utility.CC_ID2Char(j),
	// indexTable[j].WordItems[i].sWord);
	// }
	// }
	// } catch (java.lang.Exception e) {
	// isSuccess = false;
	// } finally {
	// if (writer != null) {
	// writer.Close();
	// }
	//
	// if (outputFile != null) {
	// outputFile.Close();
	// }
	// }
	// return isSuccess;
	// }



	// ====================================================================
	// Merge dict2 into current dictionary and the frequency ratio from dict2
	// and current dict is nRatio
	// ====================================================================
	public final boolean Merge(WordDictionary dict2, int nRatio) {
		int i, j, k, nCmpValue;
		String sWord;

		// Modification made, not to output when modify table exists.
		if (modifyTable != null || dict2.modifyTable != null) {
			return false;
		}

		for (i = 0; i < Predefine.CC_NUM; i++) {
			j = 0;
			k = 0;
			while (j < indexTable[i].nCount && k < dict2.indexTable[i].nCount) {
				nCmpValue = Utility.CCStringCompare(
						indexTable[i].WordItems[j].sWord,
						dict2.indexTable[i].WordItems[k].sWord);
				if (nCmpValue == 0)
				// Same Words and determine the different handle
				{
					if (indexTable[i].WordItems[j].nPOS < dict2.indexTable[i].WordItems[k].nPOS) {
						nCmpValue = -1;
					} else if (indexTable[i].WordItems[j].nPOS > dict2.indexTable[i].WordItems[k].nPOS) {
						nCmpValue = 1;
					}
				}

				if (nCmpValue == 0) {
					indexTable[i].WordItems[j].nFrequency = (nRatio
							* indexTable[i].WordItems[j].nFrequency
							+ dict2.indexTable[i].WordItems[k].nFrequency)
							/ (nRatio + 1);
					j += 1;
					k += 1;
				}
				// Get next word in the current dictionary
				else if (nCmpValue < 0) {
					indexTable[i].WordItems[j].nFrequency = (nRatio
							* indexTable[i].WordItems[j].nFrequency)
							/ (nRatio + 1);
					j += 1;
				} else
				// Get next word in the second dictionary
				{
					if (dict2.indexTable[i].WordItems[k].nFrequency > (nRatio
							+ 1) / 10) {
						sWord = String.format("%1$s%2$s",
								(new Character(Utility.CC_ID2Char(i)))
										.toString(),
								dict2.indexTable[i].WordItems[k].sWord);
						AddItem(sWord, dict2.indexTable[i].WordItems[k].nPOS,
								dict2.indexTable[i].WordItems[k].nFrequency
										/ (nRatio + 1));
					}
					k += 1;
				}
			}

			// words in current dictionary are left
			while (j < indexTable[i].nCount) {
				indexTable[i].WordItems[j].nFrequency = (nRatio
						* indexTable[i].WordItems[j].nFrequency) / (nRatio + 1);
				j += 1;
			}

			// words in Dict2 are left
			while (k < dict2.indexTable[i].nCount) {
				if (dict2.indexTable[i].WordItems[k].nFrequency > (nRatio + 1)
						/ 10) {
					sWord = String.format("%1$s%2$s",
							(new Character(Utility.CC_ID2Char(i))).toString(),
							dict2.indexTable[i].WordItems[k].sWord);
					AddItem(sWord, dict2.indexTable[i].WordItems[k].nPOS,
							dict2.indexTable[i].WordItems[k].nFrequency
									/ (nRatio + 1));
				}
				k += 1;
			}
		}
		return true;
	}


	// ====================================================================
	// Delete word item which
	// (1)frequency is 0
	// (2)word is same as following but the POS value is parent set of the
	// following
	// for example "������/n/0" will deleted, because "������/nr/0" is more
	// detail and correct
	// ====================================================================
	public final boolean Optimum() {
		int nPrevPOS, i, j, nPrevFreq;
		String sPrevWord, sCurWord;
		for (i = 0; i < Predefine.CC_NUM; i++) {
			j = 0;
			sPrevWord = null;
			nPrevPOS = 0;
			nPrevFreq = -1;
			while (j < indexTable[i].nCount) {
				sCurWord = String.format("%1$s%2$s",
						(new Character(Utility.CC_ID2Char(i))).toString(),
						indexTable[i].WordItems[j].sWord);
				if (nPrevPOS == 30720 || nPrevPOS == 26368 || nPrevPOS == 29031
						|| (sPrevWord.equals(sCurWord) && nPrevFreq == 0
								&& indexTable[i].WordItems[j].nPOS / 256
										* 256 == nPrevPOS)) {
					// Delete Previous word item
					// Delete word with POS 'x','g' 'qg'
					DelItem(sPrevWord, nPrevPOS);
				}
				sPrevWord = sCurWord;
				nPrevPOS = indexTable[i].WordItems[j].nPOS;
				nPrevFreq = indexTable[i].WordItems[j].nFrequency;
				j += 1; // Get next item in the original table.
			}
		}
		return true;
	}



	// ====================================================================
	// Func Name : PreProcessing
	// Description: Get the type of word
	// Parameters : sWord: the word
	// Returns : the type
	// ====================================================================
	private boolean PreProcessing(RefObject<String> sWord,
			RefObject<Integer> nId, RefObject<String> sWordRet) {
		sWord.argvalue = sWord.argvalue.trim();

		// Position for the delimeters
		int nType = Utility.charType(sWord.argvalue.toCharArray()[0]);

		if (sWord.argvalue.length() != 0) {
			// Chinese word
			if (nType == Predefine.CT_CHINESE) {
				// Get the inner code of the first Chinese Char
				byte[] byteArray = Utility.String2ByteArray(sWord.argvalue);
				nId.argvalue = Utility.CC_ID(byteArray[0], byteArray[1]);

				// store the word,not store the first Chinese Char
				sWordRet.argvalue = sWord.argvalue.substring(1);
				return true;
			}

			// Delimiter
			if (nType == Predefine.CT_DELIMITER) {
				nId.argvalue = 3755;
				// Get the inner code of the first Chinese Char
				sWordRet.argvalue = sWord.argvalue; // store the word, not store
													// the first Chinese Char
				return true;
			}
		}

		nId.argvalue = 0;
		sWordRet.argvalue = "";
		return false; // other invalid
	}



	// ====================================================================
	// Func Name : FindInOriginalTable
	// Description: judge the word and handle exist in the inner table and its
	// items
	// Parameters : nInnerCode: the inner code of the first CHines char
	// sWord: the word
	// nHandle:the handle number
	// *nPosRet:the position which node is matched
	// Returns : success or fail
	// ====================================================================
	private boolean FindInOriginalTable(int nInnerCode, String sWord, int nPOS,
			RefObject<Integer> nPosRet) {
		WordItem[] pItems = indexTable[nInnerCode].WordItems;

		int nStart = 0, nEnd = indexTable[nInnerCode].nCount - 1;
		int nMid = (nStart + nEnd) / 2, nCmpValue;

		while (nStart <= nEnd)
		// Binary search
		{
			nCmpValue = Utility.CCStringCompare(pItems[nMid].sWord, sWord);
			if (nCmpValue == 0 && (pItems[nMid].nPOS == nPOS || nPOS == -1)) {
				if (nPOS == -1)
				// Not very strict match
				{
					nMid -= 1;
					while (nMid >= 0
							&& pItems[nMid].sWord.compareTo(sWord) == 0)
					// Get the first item which match the current word
					{
						nMid--;
					}
					if (nMid < 0 || pItems[nMid].sWord.compareTo(sWord) != 0) {
						nMid++;
					}
				}
				nPosRet.argvalue = nMid;
				return true; // find it
			} else if (nCmpValue < 0 || (nCmpValue == 0
					&& pItems[nMid].nPOS < nPOS && nPOS != -1)) {
				nStart = nMid + 1;
			} else if (nCmpValue > 0 || (nCmpValue == 0
					&& pItems[nMid].nPOS > nPOS && nPOS != -1)) {
				nEnd = nMid - 1;
			}
			nMid = (nStart + nEnd) / 2;
		}

		// Get the previous position
		nPosRet.argvalue = nMid - 1;
		return false;
	}

	// ====================================================================
	// Func Name : FindInOriginalTable
	// Description: judge the word and handle exist in the inner table and its
	// items
	// Parameters : nInnerCode: the inner code of the first CHines char
	// sWord: the word
	// nHandle:the handle number
	// Returns : success or fail
	// ====================================================================
	private boolean FindInOriginalTable(int nInnerCode, String sWord,
			int nPOS) {
		WordItem[] pItems = indexTable[nInnerCode].WordItems;

		int nStart = 0, nEnd = indexTable[nInnerCode].nCount - 1;
		int nMid = (nStart + nEnd) / 2, nCmpValue;

		// Binary search
		while (nStart <= nEnd) {
			nCmpValue = Utility.CCStringCompare(pItems[nMid].sWord, sWord);

			if (nCmpValue == 0 && (pItems[nMid].nPOS == nPOS || nPOS == -1)) {
				return true; // find it
			} else if (nCmpValue < 0 || (nCmpValue == 0
					&& pItems[nMid].nPOS < nPOS && nPOS != -1)) {
				nStart = nMid + 1;
			} else if (nCmpValue > 0 || (nCmpValue == 0
					&& pItems[nMid].nPOS > nPOS && nPOS != -1)) {
				nEnd = nMid - 1;
			}

			nMid = (nStart + nEnd) / 2;
		}
		return false;
	}


	// ====================================================================
	// Func Name : FindInModifyTable
	// Description: judge the word and handle exist in the modified table and
	// its items
	// Parameters : nInnerCode: the inner code of the first CHines char
	// sWord: the word
	// nHandle:the handle number
	// *pFindRet: the node found
	// Returns : success or fail
	// ====================================================================
	private boolean FindInModifyTable(int nInnerCode, String sWord, int nPOS,
			RefObject<WordChain> pFindRet) {
		WordChain pCur, pPre;
		if (modifyTable != null) {
			pCur = modifyTable[nInnerCode].pWordItemHead;
			pPre = null;
			while (pCur != null
					&& (Utility.CCStringCompare(pCur.data.sWord, sWord) < 0
							|| (pCur.data.sWord.compareToIgnoreCase(sWord) == 0
									&& pCur.data.nPOS < nPOS)))
			// sort the link chain as alphabet
			{
				pPre = pCur;
				pCur = pCur.next;
			}

			pFindRet.argvalue = pPre;

			if (pCur != null && pCur.data.sWord.compareToIgnoreCase(sWord) == 0
					&& pCur.data.nPOS == nPOS)
			// The node exists, delete the node and return
			{
				return true;
			} else {
				return false;
			}
		}

		pFindRet.argvalue = null;
		return false;
	}

	// ====================================================================
	// Func Name : FindInModifyTable
	// Description: judge the word and handle exist in the modified table and
	// its items
	// Parameters : nInnerCode: the inner code of the first CHines char
	// sWord: the word
	// nHandle:the handle number
	// *pFindRet: the node found
	// Returns : success or fail
	// ====================================================================
	private boolean FindInModifyTable(int nInnerCode, String sWord,
			RefObject<WordChain> pFindRet) {
		WordChain pCur, pPre;
		if (modifyTable != null) {
			pCur = modifyTable[nInnerCode].pWordItemHead;
			pPre = null;
			while (pCur != null
					&& (Utility.CCStringCompare(pCur.data.sWord, sWord) < 0)) {
				pPre = pCur;
				pCur = pCur.next;
			}

			pFindRet.argvalue = pPre;

			if (pCur != null
					&& pCur.data.sWord.compareToIgnoreCase(sWord) == 0) {
				return true;
			} else {
				return false;
			}
		}

		pFindRet.argvalue = null;
		return false;
	}

	// ====================================================================
	// Func Name : FindInModifyTable
	// Description: judge the word and handle exist in the modified table and
	// its items
	// Parameters : nInnerCode: the inner code of the first CHines char
	// sWord: the word
	// nHandle:the handle number
	// Returns : success or fail
	// ====================================================================
	private boolean FindInModifyTable(int nInnerCode, String sWord, int nPOS) {
		WordChain pCur;
		if (modifyTable != null) {
			pCur = modifyTable[nInnerCode].pWordItemHead;
			// sort the link chain as alphabet
			while (pCur != null
					&& (Utility.CCStringCompare(pCur.data.sWord, sWord) < 0
							|| (pCur.data.sWord.compareToIgnoreCase(sWord) == 0
									&& pCur.data.nPOS < nPOS))) {
				pCur = pCur.next;
			}

			// The node exists
			if (pCur != null && pCur.data.sWord.compareToIgnoreCase(sWord) == 0
					&& (pCur.data.nPOS == nPOS || nPOS < 0)) {
				return true;
			}
		}
		return false;
	}



	// ====================================================================
	// ���ҵ�һ�����㣨int nInnerCode, string sWordFunc Name��������λ��
	// ====================================================================
	private boolean FindFirstMatchItemInOrgTbl(int nInnerCode, String sWord,
			RefObject<Integer> nPosRet) {
		WordItem[] pItems = indexTable[nInnerCode].WordItems;

		int nStart = 0, nEnd = indexTable[nInnerCode].nCount - 1;
		int nMid = (nStart + nEnd) / 2, nCmpValue;

		if (sWord.length() == 0) {
			nPosRet.argvalue = 0;
			return true;
		}

		while (nStart <= nEnd) {
			nCmpValue = Utility.CCStringCompare(pItems[nMid].sWord, sWord);
			if (nCmpValue == 0) {
				// Get the first item which match the current word
				while (nMid >= 0 && pItems[nMid].sWord.equals(sWord)) {
					nMid--;
				}

				nPosRet.argvalue = ++nMid;
				return true;
			} else if (nCmpValue < 0) {
				nStart = nMid + 1;
			} else if (nCmpValue > 0) {
				nEnd = nMid - 1;
			}

			nMid = (nStart + nEnd) / 2;
		}

		nPosRet.argvalue = -1;
		return false;
	}



}