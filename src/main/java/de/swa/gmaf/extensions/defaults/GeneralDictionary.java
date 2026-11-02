package de.swa.gmaf.extensions.defaults;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General Dictionary to represent words and word-stems
 * 
 * @author stefan_wagenpfeil
 */

public class GeneralDictionary {
	private static final Logger log = LoggerFactory.getLogger(GeneralDictionary.class);

	/** private constructor, Singleton pattern **/
	private GeneralDictionary() {
		try {
			ObjectInputStream oin = new ObjectInputStream(new FileInputStream("temp/dictionary.ser"));
			words = (Hashtable<String, Word>)oin.readObject();
			wordIDs = (Hashtable<String, Word>)oin.readObject();
			oin.close();
		}
		catch (Exception x) {
			x.printStackTrace();
			log.info("No dictionary found. Recreating...");
		}
		if (words.size() == 0) init();
	}
	private static GeneralDictionary instance;
	/** Singleton Access to the dictionary **/
	public static synchronized GeneralDictionary getInstance() {
		if (instance == null) instance = new GeneralDictionary();
		return instance;
	}
	
	/** returns a vector of Dictionary-Words for a given label **/
	public Vector<Word> getWord(String label) {
		String stem = Stemmer.stemTerm(label);
		Vector<Word> result = new Vector<Word>();
		if (words.get(label) != null) result.add(words.get(label));
		if (words.get(stem) != null) result.add(words.get(stem));
		int idx = 1;
		while (words.get(label + "_" + idx) != null) {
			result.add(words.get(label + "_" + idx));
			idx ++;
		}
		for (Word w : result) {
			w.setWordStem(stem);
		}
		if (result.size() == 0) result.add(new Word("", label, Word.TYPE_UNKNOWN, null));
		return result;
	}
	
	public Word getFirstWord(String label) {
		Vector<Word> words = getWord(label);
		if (words != null && words.get(0) != null) return words.get(0);
		return new Word(null, null, 0, null);
	}
	
	/** returns the Dictionary-Word for a given Word-ID **/
	public Word getWordForId(String id) {
		return wordIDs.get(id);
	}

	private Hashtable<String, Word> words = new Hashtable<String, Word>();
	private Hashtable<String, Word> wordIDs = new Hashtable<String, Word>();
	
	private void init() {
		log.info("Initializing Dictionary");
		processFile("dictionary" + File.separatorChar + "data.noun", Word.TYPE_NOUN);
		log.info("-> nouns");
		processFile("dictionary" + File.separatorChar + "data.adj", Word.TYPE_ADJECTIVE);
		log.info("-> adjectives");
		processFile("dictionary" + File.separatorChar + "data.adv", Word.TYPE_ADVERB);
		log.info("-> adverbs");
		processFile("dictionary" + File.separatorChar + "data.verb", Word.TYPE_VERB);
		log.info("-> verbs");
		
		try {
			ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream("temp/dictionary.ser"));
			oout.writeObject(words);
			oout.writeObject(wordIDs);
			oout.flush();
			oout.close();
		}
		catch (Exception x) {
			x.printStackTrace();
		}
		log.info("Initializing Dictionary finished.");
	}
	
	/*
	 * Structure of the files: 

00001740 03 n 01 entity 0 003 ~ 00001930 n 0000 ~ 00002137 n 0000 ~ 04431553 n 0000 | that which is perceived or known or inferred to have its own distinct existence (living or nonliving)  
00001930 03 n 01 physical_entity 0 007 @ 00001740 n 0000 ~ 00002452 n 0000 ~ 00002684 n 0000 ~ 00007347 n 0000 ~ 00021007 n 0000 ~ 00029976 n 0000 ~ 14604577 n 0000 | an entity that has physical existence  
00002137 03 n 02 abstraction 0 abstract_entity 0 010 @ 00001740 n 0000 + 00694095 v 0101 ~ 00023280 n 0000 ~ 00024444 n 0000 ~ 00031563 n 0000 ~ 00032220 n 0000 ~ 00033319 n 0000 ~ 00033914 n 0000 ~ 05818169 n 0000 ~ 08016141 n 0000 | a general concept formed by extracting common features from specific examples  
02961438 06 n 02 captopril 0 Capoten 0 002 @ 02676491 n 0000 ;u 06864792 n 0201 | a drug (trade name Capoten) that blocks the formation of angiotensin in the kidneys resulting in vasodilation; used in the treatment of hypertension and congestive heart failure  
02961700 06 n 01 capuchin 0 001 @ 03049322 n 0000 | a hooded cloak for women  
02961779 06 n 05 car 0 auto 0 automobile 0 machine 1 motorcar 0 076 @ 03796768 n 0000 + 10298715 n 0401 + 10353814 n 0302 + 01934709 v 0301 -c 01834705 a 0000 -c 01113280 n 0000 %p 02673313 n 0000 %p 02688224 n 0000 -c 02702923 n 0000 ~ 02703861 n 0000 %p 02761758 n 0000 %p 02764562 n 0000 %p 02764839 n 0000 -c 02773835 n 0000 ~ 02817668 n 0000 %p 02914504 n 0000 %p 02921979 n 0000 ~ 02927938 n 0000 ~ 02934150 n 0000 %p 02967273 n 0000 %p 02969235 n 0000 %p 02974144 n 0000 %p 02977678 n 0000 ~ 03083140 n 0000 ~ 03104891 n 0000 ~ 03124047 n 0000 ~ 03145716 n 0000 ~ 03273701 n 0000 %p 03332786 n 0000 %p 03354857 n 0000 %p 03371567 n 0000 ~ 03426579 n 0000 %p 03429540 n 0000 %p 03446269 n 0000 %p 03464707 n 0000 ~ 03498320 n 0000 ~ 03503925 n 0000 %p 03523794 n 0000 %p 03536090 n 0000 ~ 03544283 n 0000 ~ 03548574 n 0000 ~ 03600138 n 0000 ~ 03675534 n 0000 ~ 03685838 n 0000 %p 03701391 n 0000 ~ 03775620 n 0000 ~ 03776212 n 0000 ~ 03783101 n 0000 ~ 03875783 n 0000 ~ 04043919 n 0000 %p 04067177 n 0000 %p 04092129 n 0000 -c 04104350 n 0000 ~ 04104467 n 0000 %p 04112532 n 0000 %p 04127433 n 0000 ~ 04173342 n 0000 -c 04216878 n 0000 -c 04276196 n 0000 ~ 04292146 n 0000 ~ 04293103 n 0000 %p 04301752 n 0000 ~ 04310126 n 0000 ~ 04330062 n 0000 ~ 04354197 n 0000 %p 04364208 n 0000 %p 04391493 n 0000 %p 04433112 n 0000 ~ 04466357 n 0000 -c 04505257 n 0000 ~ 04523649 n 0000 %p 04595668 n 0000 -c 10423621 n 0000 -c 11480680 n 0000 -c 13266745 n 0000 -c 01565408 v 0000 | a motor vehicle with four wheels; usually propelled by an internal combustion engine; "he needs a car to get to work"  
02963378 06 n 04 car 1 railcar 0 railway_car 0 railroad_car 0 014 @ 04583497 n 0000 #m 04475240 n 0000 ~ 02778044 n 0000 ~ 02935907 n 0000 ~ 03057961 n 0000 ~ 03398748 n 0000 ~ 03470252 n 0000 ~ 03489054 n 0000 ~ 03715833 n 0000 ~ 03901563 n 0000 ~ 04247704 n 0000 %p 04373203 n 0000 ~ 04416414 n 0000 ~ 04527775 n 0000 | a wheeled vehicle adapted to the rails of railroad; "three cars had jumped the rails"  
02963788 06 n 02 car 2 elevator_car 0 002 @ 03083745 n 0000 #p 03286056 n 0000 | where passengers ride up and down; "the car was on the top floor"  
02963937 06 n 02 car 3 gondola 3 002 @ 03083745 n 0000 #p 02695736 n 0000 | the compartment that is suspended from an airship and that carries personnel and the cargo and the power plant  
02964126 06 n 03 carabiner 0 karabiner 0 snap_ring 0 002 @ 03539152 n 0000 @ 03328648 n 0000 | an oblong metal ring with a spring clip; used in mountaineering to attach a rope to a piton or to connect two ropes  
02964339 06 n 02 carafe 0 decanter 0 002 @ 02879899 n 0000 + 02074224 v 0201 | a bottle with a stopper; for serving wine or water  

	 */
	private void processFile(String path, int type) {
		try {
			RandomAccessFile rf = new RandomAccessFile(path, "r");
			String line = "";
			while ((line = rf.readLine()) != null) {
				if (line.startsWith(" ")) continue;
				String relevant_section = line.substring(0, line.indexOf("|"));
				relevant_section = relevant_section.replace("@", "~");
				relevant_section = relevant_section.replace("#p", "~");
				relevant_section = relevant_section.replace("~i", "~");
				relevant_section = relevant_section.replace(";c", "~");
				relevant_section = relevant_section.replace("@", "~");
				relevant_section = relevant_section.replace("+", "~");
				
				String[] words = relevant_section.split("~");
				String mainWord = words[0].trim();
				String[] values = mainWord.split(" ");
				String id = values[0].trim();
				String label = values[4].trim();
				
				Vector<String> syn = new Vector<String>();
				for (int i = 1; i < words.length; i++) {
					String otherWord = words[i].trim();
					String[] otherValues = otherWord.split(" ");
					String synonymId = otherValues[0].trim();
					if (!synonymId.equals(""))
						syn.add(synonymId);
				}
				Word w = new Word(id, label, type, syn);
				this.wordIDs.put(id,  w);
				
				// a similar word with a different context already exists
				if (this.words.get(label) != null) {
					int idx = 1;
					while (this.words.get(label + "_" + idx) != null) {
						idx ++;
					}
					label = label + "_" + idx;
				}
				this.words.put(label, w);
			}
			rf.close();
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		GeneralDictionary d = GeneralDictionary.getInstance();
		Vector<Word> words = d.getWord("plant");
		for (Word w : words) {
			log.info("Result (1st level): {} [{}]", w.getWord(), w.getId());
			
			for (String s : w.getSynonymIDs()) {
				try {
//				System.out.println("  Synonym " + s);
					Word syn = d.getWordForId(s);
					log.info("\tSynonym (2nd level): {} [{}]", syn.getWord(), syn.getId());
				}
				catch (Exception x) {}
			}
		}
	}
}
