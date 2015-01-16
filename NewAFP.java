import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


public class NewAFP {
	HashMap<Integer, Integer> freqItems = new HashMap<Integer, Integer>();
	int[] FArray;
	BitSet[] BitSetTable;
	
	int numOfTrans;
	int relativeMinSupp;
	int numOfFreqItems;
	int numOfUniqTrans;
	int numOfPatterns;
	
	int[] localFreqItems;
	int numOfLocalFreqItems;
	
	BitSet[] hintBitSets;
	int blockSize = 192;
	int numOfCompressedRows;
	
	//HashMap<Integer, Pattern> patterns = new HashMap<Integer, Pattern>();
	ArrayList<Pattern> patterns = new ArrayList<Pattern>();
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		//String infile = "chess.txt"; //"retail.txt"
		//float supp = 0.6f;   //"0.04f"
		boolean isDense = true;
		NewAFP afp = new NewAFP();
		
		String infile = args[0];
		float supp = Float.parseFloat(args[1]);
		if(args[2] == "s"){isDense = false;}
		
		long mEnd=0;
		long bStart=0;
		long bEnd=0;
		
		if(isDense){
			bStart = System.currentTimeMillis();
			afp.getCondFP(infile, supp);
			bEnd = System.currentTimeMillis();
			afp.getPatterns();
			mEnd = System.currentTimeMillis();
		}else{
			bStart = System.currentTimeMillis();
			afp.getCondFP2(infile, supp);
			bEnd = System.currentTimeMillis();
			afp.getPatterns2();
			mEnd = System.currentTimeMillis();
		}
		System.out.println((bEnd-bStart)+","+(mEnd-bEnd)+","+afp.numOfPatterns);
		//afp.printPatterns();
	}
	
	private void getCondFP(String infile, float supp) throws FileNotFoundException, IOException {
		String url = NewAFP.class.getResource("").getPath()+infile;
		
		// (1) get frequent items;
		scanDatabaseToDetermineFrequencyOfSingleItems(url, supp);
		
		// (2) read a dataset, and get unique transactions
		HashMap<Object, Integer> uniqSets = new HashMap<Object, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(url));
		String line;
		int item;
		Integer posOfItem;
		Integer freq;
		//BitSet transBS;
		BitSet transBS = new BitSet(numOfFreqItems);
		while( ((line = reader.readLine())!= null)){
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%'|| line.charAt(0) == '@') {// if the line is  a comment, is  empty or is a kind of metadata
				continue;
			}
			
			String[] lineSplited = line.split(" ");
			
			transBS.clear();
			for(String itemStr : lineSplited){
				item = Integer.parseInt(itemStr);
				posOfItem = freqItems.get(item);
				if(posOfItem != null){
					transBS.set(posOfItem);
				}
			}
			
			freq = uniqSets.get(transBS);
			if(freq != null){
				uniqSets.put(transBS, ++freq);
			}else{
				uniqSets.put(transBS.clone(), 1);
			}
		}
		reader.close();
		
		// (3) generate BitSetTable & FArray from unique transactions
		numOfUniqTrans = uniqSets.size();
		BitSetTable = new BitSet[numOfFreqItems];
		for(int i=0; i<numOfFreqItems; i++){
			BitSetTable[i] = new BitSet(numOfUniqTrans);
		}
		FArray = new int[numOfUniqTrans];
		Iterator<Integer> iter = uniqSets.values().iterator();
		Iterator<Entry<Object, Integer>> it = uniqSets.entrySet().iterator();
		int rowIndex=0;
		BitSet target;
		
		int[] tmpFreqs = new int[numOfFreqItems];
		int tmpFreq;
		
		int lastIndex = numOfFreqItems-1;
		while(it.hasNext()){
			target = (BitSet) it.next().getKey();
			tmpFreq = iter.next();
			
			if(target.get(lastIndex)){
				for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) {
				    BitSetTable[i].set(rowIndex);
				    tmpFreqs[i] += tmpFreq;
				}
			}else{
				for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) {
				    BitSetTable[i].set(rowIndex);
				}
			}
			FArray[rowIndex++] = tmpFreq;
		}
		
		// (4) get localFreqItems
		numOfLocalFreqItems=0;
		for(int i=0; i<lastIndex; i++){
			if(tmpFreqs[i] >= relativeMinSupp){
				localFreqItems[numOfLocalFreqItems++] = i;
			}
		}
		//System.out.println(numOfLocalFreqItems);
		
		/*
		// [DEBUG] Print BitSetTable & FArray
		System.out.println("========================================");
		for(int tmp=0; tmp<numOfFreqItems; tmp++){
			printBitSet("", BitSetTable[tmp]);
		}
		for(int tmp=0; tmp<numOfUniqTrans; tmp++){
			System.out.print(FArray[tmp] + " ");
		}System.out.println();
		*/
	}
	
	private void update(int numOfItems){	
		// (1) get new unique Transactions
		HashMap<Object, Integer> uniqSets = new HashMap<Object, Integer>();
		BitSet uniqTrans = new BitSet(numOfItems);
		Integer freq;

		for (int i = 0; i < numOfUniqTrans; i++) {
			uniqTrans.clear();
			for (int j = 0; j <= numOfItems; j++) {
				uniqTrans.set(j, BitSetTable[j].get(i));
			}
			freq = uniqSets.get(uniqTrans);

			if (freq != null) {
				uniqSets.put(uniqTrans, freq+FArray[i]);
			} else {
				uniqSets.put(uniqTrans.clone(), FArray[i]);
			}
		}
				
		// (2) Clear BitsetTable
		for(int i=0; i<numOfFreqItems; i++){
			BitSetTable[i].clear();
		}
		// (3) Update BitsetTable & FArray
		numOfUniqTrans = uniqSets.size();
		numOfFreqItems = numOfItems+1;
		
		Iterator<Integer> iter = uniqSets.values().iterator();
		Iterator<Entry<Object, Integer>> it = uniqSets.entrySet().iterator();
		int rowIndex=0;
		BitSet target;
		
		int[] tmpFreqs = new int[numOfFreqItems];
		int tmpFreq;
		while(it.hasNext()){
			target = (BitSet) it.next().getKey();
			tmpFreq = iter.next();
			if(target.get(numOfItems)){
				for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) {
				    BitSetTable[i].set(rowIndex);
				    tmpFreqs[i] += tmpFreq;
				}
			}else{
				for (int i = target.nextSetBit(0); i >= 0; i = target.nextSetBit(i+1)) {
				    BitSetTable[i].set(rowIndex);
				}
			}
			FArray[rowIndex++] = tmpFreq;
		}
		// (4) Update LocalFreqItems
		numOfLocalFreqItems=0;
		for(int i=0; i<numOfItems; i++){
			if(tmpFreqs[i] >= relativeMinSupp){
				localFreqItems[numOfLocalFreqItems++] = i;
			}
		}
		//System.out.println(numOfLocalFreqItems);

		/*
		//DEBUG print BitSetTable & FArray updated
		System.out.println("====================["+numOfItems+"]====================");
		for(int tmp=0; tmp<numOfFreqItems; tmp++){
			printBitSet("", BitSetTable[tmp]);
		}
		for(int tmp=0; tmp<numOfUniqTrans; tmp++){
			System.out.print(FArray[tmp]+" ");
		}System.out.println();*/
	}

	private void getPatterns(){//Generate patterns in depth-first search way
		// (0) Initialize Stack
		int[] ptnIndex = new int[numOfFreqItems];
		int[] len = new int[numOfFreqItems];
		BitSet[] checkLists = new BitSet[numOfFreqItems];
		
		for(int i=0; i<numOfFreqItems; i++){
			checkLists[i] = new BitSet(numOfUniqTrans);
		}
		int top = -1;
		
		int topLen;
		int freq;
		boolean isPoped;
		BitSet checkList;
		boolean isFirst = true;
		
		for(int index=numOfFreqItems-1; index>-1; index--){	
			if(isFirst){
				isFirst = false;
			}else{
				update(index);
			}
			//STACK
			top++;
			checkList = checkLists[top];
			checkList.clear();
			
			len[top] = numOfLocalFreqItems;
			ptnIndex[top] = index;
			checkList.or(BitSetTable[index]);

			isPoped = false;
			
			while(top > -1){
				if(isPoped){
					topLen = len[top]-2;
					if(topLen > -1){
						len[top]--;
						checkList = checkLists[top+1];
						checkList.clear();
						checkList.or(checkLists[top]);
						checkList.and(BitSetTable[localFreqItems[topLen]]);
						
						freq = 0;
						for (int i = checkList.nextSetBit(0); i > -1 ; i = checkList.nextSetBit(i+1)) {
						     freq += FArray[i];
						}
						
						if(freq >= relativeMinSupp){
							patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen], freq));
							//System.out.println(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen]);
							top++;
							ptnIndex[top] = numOfPatterns++;
							len[top] = topLen;
							isPoped = false;
						}else{
							isPoped = true;
						}
					}else{
						top--;
						isPoped = true;
					}
				}else{
					topLen = len[top]-1;
					
					if(topLen > -1){
						checkList = checkLists[top+1];
						checkList.clear();
						checkList.or(checkLists[top]);
						checkList.and(BitSetTable[localFreqItems[topLen]]);
						
						freq = 0;
						for (int i = checkList.nextSetBit(0); i > -1; i = checkList.nextSetBit(i+1)) {
						     freq += FArray[i];
						}
						if(freq >= relativeMinSupp){
							patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen], freq));
							//System.out.println(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen]);
							top++;
							ptnIndex[top] = numOfPatterns++;
							len[top] = topLen;
							isPoped = false;
						}else{
							isPoped = true;
						}
					}else{
						top--;
						isPoped = true;
					}
				}
			}
		}
	}
	
	private void getCondFP2(String infile, float supp) throws FileNotFoundException, IOException {
		String url = NewAFP.class.getResource("").getPath()+infile;
		// (1) get frequent items;
		scanDatabaseToDetermineFrequencyOfSingleItems(url, supp);
		
		// (2) read a dataset, and get unique transactions
		ArrayList<HashMap<Object, Integer>> uniqSets = new ArrayList<HashMap<Object, Integer>>(numOfFreqItems);
		for(int i=0; i<numOfFreqItems; i++){
			uniqSets.add(new HashMap<Object, Integer>());
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(url));
		String line;
		int item;
		Integer posOfItem;
		Integer freq;
		BitSet transBS = new BitSet(numOfFreqItems);
		
		int lastIndex = -1;
		HashMap<Object, Integer> uniqTrans = new HashMap<Object, Integer>();
		while( ((line = reader.readLine())!= null)){
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%'|| line.charAt(0) == '@') {// if the line is  a comment, is  empty or is a kind of metadata
				continue;
			}
			String[] lineSplited = line.split(" ");  //System.out.println("items="+lineSplited.length); //HEE JEONG			
			transBS.clear();
			lastIndex = -1;
			for(String itemStr : lineSplited){
				item = Integer.parseInt(itemStr);
				posOfItem = freqItems.get(item);
				if(posOfItem != null){
					if(posOfItem > lastIndex){
						lastIndex = posOfItem;
					}
					transBS.set(posOfItem);
				}
			}
			if(lastIndex != -1){
				uniqTrans= uniqSets.get(lastIndex);
				freq = uniqTrans.get(transBS);
				if(freq != null){
					uniqTrans.put(transBS, ++freq);
				}else{
					uniqTrans.put(transBS.clone(), 1);
					numOfUniqTrans++;
				}
			}
		}
		reader.close();
		
		// (3) generate BitSetTable & FArray
		BitSetTable = new BitSet[numOfUniqTrans];
		for(int i=0; i<numOfUniqTrans; i++){
			BitSetTable[i] = new BitSet();
		}
		FArray = new int[numOfUniqTrans];
		
		Iterator<Integer> iter;
		Iterator<Entry<Object, Integer>> it;
		int rowIndex=0;

		for(int i=numOfFreqItems-1; i>-1; i--){
			uniqTrans = uniqSets.get(i);
			iter = uniqTrans.values().iterator();
			it = uniqTrans.entrySet().iterator();
			BitSet target;
			while(it.hasNext()){
				target = (BitSet) it.next().getKey();
			
				for (int j = target.nextSetBit(0); j >= 0; j = target.nextSetBit(j+1)) {
			    	BitSetTable[j].set(rowIndex);
				}
				FArray[rowIndex++] = iter.next();
			}
		}
		/*
		// [DEBUG] Print BitSetTable & FArray
		System.out.println("========================================");
		for(int i=0; i<numOfFreqItems; i++){
			printBitSet("", BitSetTable[i]);
		}
		for(int i=0; i<numOfUniqTrans; i++){
			System.out.print(FArray[i]+" ");
		}System.out.println();
		*/
	}
	
	private void getPatterns2(){//Generate patterns in depth-first search way
		// (0) Initialize Stack
		int[] ptnIndex = new int[numOfFreqItems];
		int[] len = new int[numOfFreqItems];
		BitSet[] checkLists = new BitSet[numOfFreqItems];
		
		for(int i=0; i<numOfFreqItems; i++){
			checkLists[i] = new BitSet(numOfUniqTrans);
		}
		int top = -1; 
		int topLen;
		
		int freq;
		boolean isPoped;
		BitSet checkList;
		
		int[] tmpFreqs = new int[numOfFreqItems];
		BitSet tmpBS = new BitSet();
		//BitSet indexBS;
		
		for(int index=0; index<numOfFreqItems; index++){
			/*
			numOfLocalFreqItems = 0;
			indexBS = BitSetTable[index];
			
			for (int i = indexBS.nextSetBit(0); i > -1; i = indexBS.nextSetBit(i+1)) {
				for(int j=0; j<=index; j++){
					if(BitSetTable[j].get(i)){
						tmpFreqs[j] += FArray[i];
					}
				}
			}
			for(int j=0; j<=index; j++){
				if(tmpFreqs[j] >= relativeMinSupp){
					localFreqItems[numOfLocalFreqItems++] = j;
				}
				tmpFreqs[j] = 0;
			}*/
			
			
			numOfLocalFreqItems = 0;
			for(int j=0; j<index; j++){
				tmpBS.clear();
				tmpBS.or(BitSetTable[index]);
				tmpBS.and(BitSetTable[j]);
				tmpFreqs[j]=0;
				for (int i = tmpBS.nextSetBit(0); i > -1; i = tmpBS.nextSetBit(i+1)) {
					tmpFreqs[j] += FArray[i];
				}
				if(tmpFreqs[j] >= relativeMinSupp){
					localFreqItems[numOfLocalFreqItems++] = j;
				}
			}
			//System.out.println(numOfLocalFreqItems+"\t"+(numOfFreqItems-index));
			//System.out.println("numOfRefinedFreqItems="+numOfLocalFreqItems+"\t numOfFreqItems="+(numOfFreqItems-index));
			top++;
			checkList = checkLists[top];
			checkList.clear();
			
			len[top] = numOfLocalFreqItems;
			ptnIndex[top] = index;
			checkList.or(BitSetTable[index]);

			isPoped = false;
			
			while(top > -1){
				if(isPoped){
					topLen = len[top]-2;
					if(topLen > -1){
						len[top]--;
						checkList = checkLists[top+1];
						checkList.clear();
						checkList.or(checkLists[top]);
						//checkList.and(BitSetTable[topLen]);
						checkList.and(BitSetTable[localFreqItems[topLen]]);
						
						freq = 0;
						for (int i = checkList.nextSetBit(0); i > -1; i = checkList.nextSetBit(i+1)) {
						     freq += FArray[i];
						}
						
						if(freq >= relativeMinSupp){
							//patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+topLen, freq));
							patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen], freq));
							//System.out.println(patterns.get(ptnIndex[top]).itemset+" "+topLen);
							top++;
							ptnIndex[top] = numOfPatterns++;
							len[top] = topLen;
							isPoped = false;
						}else{
							isPoped = true;
						}
					}else{
						top--;
						isPoped = true;
					}
				}else{
					topLen = len[top]-1;
					
					if(topLen > -1){
						checkList = checkLists[top+1];
						checkList.clear();
						checkList.or(checkLists[top]);
						//checkList.and(BitSetTable[topLen]);
						checkList.and(BitSetTable[localFreqItems[topLen]]);
						
						freq = 0;
						for (int i = checkList.nextSetBit(0); i > -1; i = checkList.nextSetBit(i+1)) {
						     freq += FArray[i];
						}
						
						if(freq >= relativeMinSupp){
							//patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+topLen, freq));
							patterns.add(new Pattern(patterns.get(ptnIndex[top]).itemset+" "+localFreqItems[topLen], freq));
							//System.out.println(patterns.get(ptnIndex[top]).itemset+" "+topLen);
							top++;
							ptnIndex[top] = numOfPatterns++;
							len[top] = topLen;
							isPoped = false;
						}else{
							isPoped = true;
						}
					}else{
						top--;
						isPoped = true;
					}
				}
			}
		}
	}
	
	private void scanDatabaseToDetermineFrequencyOfSingleItems(String input, float minsupp) throws FileNotFoundException, IOException {
		HashMap<Integer, Integer> mapSupport = new HashMap<Integer, Integer>();
		
		//(1) Read a dataset, and count frequency of each item in a dataset
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line;
		int item;
		Integer count;
		
		while(((line = reader.readLine())!= null)){
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') { // if the line is  a comment, is  empty or is a kind of metadata
				continue;
			}
			String[] lineSplited = line.split(" ");
			for(String itemString : lineSplited){  
				item = Integer.parseInt(itemString);
				count = mapSupport.get(item);
				if(count == null){
					mapSupport.put(item, 1);
				}else{
					mapSupport.put(item, ++count);
				}
			}
			numOfTrans++;
		}
		reader.close();
		
		// (2) Sort frequent items
		relativeMinSupp = (int) Math.ceil(minsupp * numOfTrans); // relative minimum support
		
		ValueComparator bvc = new ValueComparator(mapSupport);
		TreeMap<Integer,Integer> sorted_items = new TreeMap<Integer,Integer>(bvc);
		
		Iterator<Entry<Integer, Integer>> it = mapSupport.entrySet().iterator();
		Map.Entry<Integer, Integer> pairs;
		while(it.hasNext()){
			pairs = (Map.Entry<Integer, Integer>) it.next();
			
			if(pairs.getValue() >= relativeMinSupp){
				sorted_items.put(pairs.getKey(), pairs.getValue());
				//System.out.println(pairs.getKey() + " = " + pairs.getValue());
			}
		}
		
		numOfFreqItems = sorted_items.size();
		localFreqItems = new int[numOfFreqItems];
		
		int index=0;
		it = sorted_items.entrySet().iterator();
		//System.out.println("[Index] = itemID, freq");
		while(it.hasNext()){
			pairs = (Map.Entry<Integer, Integer>) it.next();
			patterns.add(new Pattern(index+"", pairs.getValue()));
			//System.out.println("["+index + "] = " + pairs.getKey() +"," + pairs.getValue());
			freqItems.put(pairs.getKey(), index++);
			
		}
		numOfPatterns = numOfFreqItems;
		/*
		//DEBUG, Print all the freqItems
		System.out.println("=================");
		for(Map.Entry<Integer, Integer> pairs:freqItems.entrySet()){
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
		}*/
	}
	
	private void printLocalFreqItems(){
		for(int i=0; i<numOfLocalFreqItems; i++){
			System.out.print(localFreqItems[i]+" ");
		}System.out.println();
	}
	
	private void printBitSet(String name, BitSet base){ //DEBUG - base
		System.out.print(name+"=");
		for(int j=0; j<base.length(); j++){
			if(base.get(j)){ //base.get(j) == true
				System.out.print("1 ");
			}else{
				System.out.print("0 ");
			}
		}
		System.out.println();
	}
	
	private void printPatterns(){
		for(Pattern p : patterns){
			System.out.println(p.itemset+":"+p.freq);
		}
	}
}
