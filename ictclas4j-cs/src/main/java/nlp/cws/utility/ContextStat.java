package nlp.cws.utility;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nlp.cws.segment.dynamicarray.DynamicArray.RefObject;



public class ContextStat {
	private int m_nTableLen;
	private int[] m_pSymbolTable;
	private ContextItem m_pContext = null;

	public ContextStat() {
		m_pSymbolTable = null; // new buffer for symbol
	}

	public final void SetSymbol(int[] nSymbol) {
		m_pSymbolTable = nSymbol;
	}

	public final boolean Load(String sFilename) {
		boolean isSuccess = true;

		File file = new File(sFilename);
		if (!file.canRead())
			return false;// fail while opening the file

		ContextItem pCur = null, pPre = null;

		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(file));

			m_nTableLen = GFCommon.bytes2int(Utility.readBytes(in, 4), false); // write
																				// the
																				// table
																				// length
			m_pSymbolTable = new int[m_nTableLen]; // new buffer for symbol

			for (int i = 0; i < m_nTableLen; i++) // write the symbol table
			{
				m_pSymbolTable[i] = GFCommon.bytes2int(Utility.readBytes(in, 4),
						false);
			}

			long fileLen = file.length();
			long curLen = 4 + m_nTableLen * 4;
			while (curLen < fileLen) {
				// Read the context
				pCur = new ContextItem();
				pCur.next = null;
				pCur.nKey = GFCommon.bytes2int(Utility.readBytes(in, 4), false);
				pCur.nTotalFreq = GFCommon.bytes2int(Utility.readBytes(in, 4),
						false);

				curLen += 4 + 4;

				pCur.aTagFreq = new int[m_nTableLen];
				for (int i = 0; i < m_nTableLen; i++) // the every POS frequency
				{
					pCur.aTagFreq[i] = GFCommon
							.bytes2int(Utility.readBytes(in, 4), false);
					curLen += 4;
				}

				pCur.aContextArray = new int[m_nTableLen][];
				for (int i = 0; i < m_nTableLen; i++) {
					pCur.aContextArray[i] = new int[m_nTableLen];
					for (int j = 0; j < m_nTableLen; j++) {
						pCur.aContextArray[i][j] = GFCommon
								.bytes2int(Utility.readBytes(in, 4), false);
						curLen += 4;
					}
				}

				if (pPre == null) {
					m_pContext = pCur;
				} else {
					pPre.next = pCur;
				}

				pPre = pCur;
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

	// =========================================================
	// 返回nKey为指定nKey的结点，如果没找到，则返回前一个结点
	// =========================================================
	public final boolean GetItem(int nKey, RefObject<ContextItem> pItemRet) {
		ContextItem pCur = m_pContext, pPrev = null;
		if (nKey == 0 && m_pContext != null) {
			pItemRet.argvalue = m_pContext;
			return true;
		}

		while (pCur != null && pCur.nKey < nKey) {
			// delete the context array
			pPrev = pCur;
			pCur = pCur.next;
		}

		if (pCur != null && pCur.nKey == nKey) {
			// find it and return the current item
			pItemRet.argvalue = pCur;
			return true;
		}

		pItemRet.argvalue = pPrev;
		return false;
	}

	public final double GetContextPossibility(int nKey, int nPrev, int nCur) {
		ContextItem pCur = null;
		int nCurIndex = Utility.BinarySearch(nCur, m_pSymbolTable);
		int nPrevIndex = Utility.BinarySearch(nPrev, m_pSymbolTable);

		// return a lower value, not 0 to prevent data sparse
		RefObject<ContextItem> tempRef_pCur = new RefObject<ContextItem>(pCur);
		boolean tempVar = !GetItem(nKey, tempRef_pCur);
		pCur = tempRef_pCur.argvalue;
		tempVar = tempVar || nCurIndex == -1 || nPrevIndex == -1
				|| pCur.aTagFreq[nPrevIndex] == 0
				|| pCur.aContextArray[nPrevIndex][nCurIndex] == 0;

		if (tempVar) {
			return 0.000001;
		}

		int nPrevCurConFreq = pCur.aContextArray[nPrevIndex][nCurIndex];
		int nPrevFreq = pCur.aTagFreq[nPrevIndex];

		// 0.9 and 0.1 is a value based experience
		return 0.9 * (double) nPrevCurConFreq / (double) nPrevFreq
				+ 0.1 * (double) nPrevFreq / (double) pCur.nTotalFreq;
	}

	// =========================================================
	// Get the frequency which nSymbol appears
	// =========================================================
	public final int GetFrequency(int nKey, int nSymbol) {
		ContextItem pFound = null;

		int nIndex, nFrequency = 0;
		RefObject<ContextItem> tempRef_pFound = new RefObject<ContextItem>(
				pFound);
		boolean tempVar = !GetItem(nKey, tempRef_pFound);
		pFound = tempRef_pFound.argvalue;
		if (tempVar)
		// Not found such a item
		{
			return 0;
		}

		nIndex = Utility.BinarySearch(nSymbol, m_pSymbolTable);
		if (nIndex == -1)
		// error finding the symbol
		{
			return 0;
		}

		nFrequency = pFound.aTagFreq[nIndex]; // Add the frequency
		return nFrequency;
	}

	public final void SetTableLen(int nTableLen) {
		m_nTableLen = nTableLen;
		m_pSymbolTable = new int[nTableLen];
		m_pContext = null;
	}

	public final void ReleaseContextStat() {
		m_pContext = null;
		m_pSymbolTable = null;
	}

	public final boolean Add(int nKey, int nPrevSymbol, int nCurSymbol,
			int nFrequency) {
		// Add the context symbol to the array
		ContextItem pRetItem = null, pNew = null;
		int nPrevIndex, nCurIndex;

		// Not get it
		RefObject<ContextItem> tempRef_pRetItem = new RefObject<ContextItem>(
				pRetItem);
		boolean tempVar = !GetItem(nKey, tempRef_pRetItem);
		pRetItem = tempRef_pRetItem.argvalue;
		if (tempVar) {
			pNew = new ContextItem();
			pNew.nKey = nKey;
			pNew.nTotalFreq = 0;
			pNew.next = null;
			pNew.aContextArray = new int[m_nTableLen][];
			pNew.aTagFreq = new int[m_nTableLen];
			for (int i = 0; i < m_nTableLen; i++) {
				pNew.aContextArray[i] = new int[m_nTableLen];
			}

			if (pRetItem == null)
			// Empty, the new item is head
			{
				m_pContext = pNew;
			} else
			// Link the new item between pRetItem and its next item
			{
				pNew.next = pRetItem.next;
				pRetItem.next = pNew;
			}
			pRetItem = pNew;
		}

		nPrevIndex = Utility.BinarySearch(nPrevSymbol, m_pSymbolTable);
		if (nPrevSymbol > 256 && nPrevIndex == -1)
		// Not find, just for 'nx' and other uncommon POS
		{
			nPrevIndex = Utility.BinarySearch(nPrevSymbol - nPrevSymbol % 256,
					m_pSymbolTable);
		}

		nCurIndex = Utility.BinarySearch(nCurSymbol, m_pSymbolTable);

		if (nCurSymbol > 256 && nCurIndex == -1)
		// Not find, just for 'nx' and other uncommon POS
		{
			nCurIndex = Utility.BinarySearch(nCurSymbol - nCurSymbol % 256,
					m_pSymbolTable);
		}

		if (nPrevIndex == -1 || nCurIndex == -1)
		// error finding the symbol
		{
			return false;
		}

		// Add the frequency
		pRetItem.aContextArray[nPrevIndex][nCurIndex] += nFrequency;
		pRetItem.aTagFreq[nPrevIndex] += nFrequency;
		pRetItem.nTotalFreq += nFrequency;

		return true;
	}

	// public final boolean Save(String sFilename) {
	// boolean isSuccess = true;
	// FileStream outputFile = null;
	// FileStream logFile = null;
	// BinaryWriter writer = null;
	// StreamWriter sw = null;
	// StringBuilder sb = new StringBuilder();
	//
	// try {
	// outputFile = new FileStream(sFilename, FileMode.Create,
	// FileAccess.Write);
	// if (outputFile == null) {
	// return false;
	// }
	//
	// logFile = new FileStream(sFilename + ".shw", FileMode.Create,
	// FileAccess.Write);
	// if (logFile == null) {
	// outputFile.Close();
	// return false;
	// }
	//
	// writer = new BinaryWriter(outputFile,
	// Encoding.GetEncoding("gb2312"));
	// sw = new StreamWriter(logFile);
	//
	// writer.Write(m_nTableLen); // write the table length
	// sb.append(
	// String.format("Table Len=%s\r\nSymbol:\r\n", m_nTableLen));
	// for (int i = 0; i < m_nTableLen; i++) // write the symbol table
	// {
	// writer.Write(m_pSymbolTable[i]);
	// sb.append(String.format("%s ", m_pSymbolTable[i]));
	// }
	// sb.append("\r\n");
	//
	// ContextItem pCur = m_pContext;
	// while (pCur != null) {
	// writer.Write(pCur.nKey);
	// writer.Write(pCur.nTotalFreq);
	// sb.append(String.format("nKey=%s,Total frequency=%s:\r\n",
	// pCur.nKey, pCur.nTotalFreq));
	//
	// for (int i = 0; i < m_nTableLen; i++) {
	// writer.Write(pCur.aTagFreq[i]);
	// }
	//
	// // the every POS frequency
	// for (int i = 0; i < m_nTableLen; i++) {
	// for (int j = 0; j < m_nTableLen; j++) {
	// writer.Write(pCur.aContextArray[i][j]);
	// }
	//
	// sb.append(String.format("No.%2s=%3s: ", i,
	// m_pSymbolTable[i]));
	// for (int j = 0; j < m_nTableLen; j++) {
	// sb.append(String.format("%5s ",
	// pCur.aContextArray[i][j]));
	// }
	//
	// sb.append(String.format("total=%s:\r\n", pCur.aTagFreq[i]));
	// }
	// pCur = pCur.next;
	// }
	//
	// sw.Write(sb.toString());
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
	//
	// if (sw != null) {
	// sw.Close();
	// }
	//
	// if (logFile != null) {
	// logFile.Close();
	// }
	// }
	//
	// return isSuccess;
	// }

	// C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
	/// #endregion

}