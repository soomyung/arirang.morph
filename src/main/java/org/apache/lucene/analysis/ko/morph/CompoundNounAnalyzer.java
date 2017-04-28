package org.apache.lucene.analysis.ko.morph;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.ko.utils.DictionaryUtil;

/**
 * 복합명사를 분해한다.
 */
public class CompoundNounAnalyzer {
  
	private boolean exactMach = true;

	private boolean divisibleOne = true;

	private LanguageSpliter langSpliter;

	public CompoundNounAnalyzer() {
		langSpliter = new LanguageSpliter();
	}

	public boolean isExactMach() {
		return exactMach;
	}

	public void setExactMach(boolean exactMach) {
		this.exactMach = exactMach;
	}

	public void setDivisible(boolean is) {
		this.divisibleOne = is;
	}

	public List<CompoundEntry> analyze(String input) throws MorphException {

		WordEntry entry = DictionaryUtil.getAllNoun(input);
		if (entry != null && entry.getCompounds().size() > 0)
			return entry.getCompounds();

		List<CompoundEntry> outputs = analyzeCompoundNoun(input);
		return outputs;

	}
  
	public List<CompoundEntry> analyzeCompoundNoun(String input)
			throws MorphException {

		List<LangToken> tokenList = langSpliter.split(input);

		List<CompoundEntry> outputs = new ArrayList<CompoundEntry>();

		WordEntry entry = null;

		for (LangToken t : tokenList) {
			if (t.getType() != LanguageSpliter.TYPE_HAN) {
				boolean exist = true;

				outputs.add(new CompoundEntry(t.getTerm(), t.getOffset(), exist));
			} else if (tokenList.size() > 1
					&& (entry = DictionaryUtil.getAllNoun(t.getTerm())) != null) {

				if (entry.getCompounds().size() > 0)
					outputs.addAll(entry.getCompounds());
				else
					outputs.add(new CompoundEntry(t.getTerm(), t.getOffset(),
							true));

			} else {
				boolean success = analyze(t.getTerm(), outputs, true);

				if (!success)
					outputs.add(new CompoundEntry(t.getTerm(), t.getOffset(),
							false));
			}
			;
		}

		return outputs;

	}
    
	public boolean analyze(String input, List<CompoundEntry> outputs,
			boolean isFirst) throws MorphException {

		int len = input.length();
		boolean success = false;

		switch (len) {
		case 3:
			success = analyze3Word(input, outputs, isFirst);
			break;
		case 4:
			success = analyze4Word(input, outputs, isFirst);
			break;
		case 5:
			success = analyze5Word(input, outputs, isFirst);
			break;
		default:
			success = analyzeLongText(input, outputs, isFirst);
		}

		return success;
	}

	private boolean analyze3Word(String input, List<CompoundEntry> outputs,
			boolean isFirst) throws MorphException {

		if (!divisibleOne)
			return false;

		int[] units1 = { 2, 1 };
		CompoundEntry[] entries1 = analysisBySplited(units1, input, isFirst);
		if (entries1 != null && existAllWord(entries1)) {
			outputs.addAll(Arrays.asList(entries1));
			return true;
		}

		int[] units2 = { 1, 2 };
		CompoundEntry[] entries2 = analysisBySplited(units2, input, isFirst);
		if (entries2 != null && existAllWord(entries2)) {
			outputs.addAll(Arrays.asList(entries2));
			return true;
		}

		return false;
	}

	private boolean analyze4Word(String input, List<CompoundEntry> outputs,
			boolean isFirst) throws MorphException {

		if (!isFirst && divisibleOne) {
			int[] units0 = { 1, 3 };
			CompoundEntry[] entries0 = analysisBySplited(units0, input, isFirst);
			if (entries0 != null && existAllWord(entries0)) {
				outputs.addAll(Arrays.asList(entries0));
				return true;
			}
		}

		int[] units1 = { 2, 2 };
		CompoundEntry[] entries1 = analysisBySplited(units1, input, isFirst);
		if (entries1 != null && existAllWord(entries1)) {
			outputs.addAll(Arrays.asList(entries1));
			return true;
		}

		if (divisibleOne) {
			int[] units3 = { 3, 1 };
			CompoundEntry[] entries3 = analysisBySplited(units3, input, isFirst);
			if (entries3 != null && existAllWord(entries3)) {
				outputs.addAll(Arrays.asList(entries3));
				return true;
			}

			int[] units2 = { 1, 2, 1 };
			CompoundEntry[] entries2 = analysisBySplited(units2, input, isFirst);
			if (entries2 != null && existAllWord(entries2)) {
				outputs.addAll(Arrays.asList(entries2));
				return true;
			}
		}

		if (!exactMach && entries1 != null && existPartWord(entries1)) {
			outputs.addAll(Arrays.asList(entries1));
			return true;
		}

		return false;
	}

	private boolean analyze5Word(String input, List<CompoundEntry> outputs,
			boolean isFirst) throws MorphException {

		int[] units1 = { 2, 3 };
		CompoundEntry[] entries1 = analysisBySplited(units1, input, isFirst);
		if (entries1 != null && existAllWord(entries1)) {
			outputs.addAll(Arrays.asList(entries1));
			return true;
		}

		int[] units2 = { 3, 2 };
		CompoundEntry[] entries2 = analysisBySplited(units2, input, isFirst);
		if (entries2 != null && existAllWord(entries2)) {
			outputs.addAll(Arrays.asList(entries2));
			return true;
		}

		CompoundEntry[] entries_1 = null;
		CompoundEntry[] entries3 = null;
		CompoundEntry[] entries4 = null;

		if (divisibleOne) {
			int[] units_1 = { 4, 1 };
			entries_1 = analysisBySplited(units_1, input, isFirst);
			if (entries_1 != null && existAllWord(entries_1)) {
				outputs.addAll(Arrays.asList(entries_1));
				return true;
			}

			int[] units3 = { 2, 2, 1 };
			entries3 = analysisBySplited(units3, input, isFirst);
			if (entries3 != null && existAllWord(entries3)) {
				outputs.addAll(Arrays.asList(entries3));
				return true;
			}

			int[] units4 = { 2, 1, 2 };
			entries4 = analysisBySplited(units4, input, isFirst);
			if (entries4 != null && existAllWord(entries4)) {
				outputs.addAll(Arrays.asList(entries4));
				return true;
			}
		}

		if (!exactMach && entries1 != null && existPartWord(entries1)) {
			outputs.addAll(Arrays.asList(entries1));
			return true;
		}

		if (!exactMach && entries2 != null && existPartWord(entries2)) {
			outputs.addAll(Arrays.asList(entries2));
			return true;
		}

		boolean is = false;
		if (!exactMach && entries3 != null && existPartWord(entries3)) {
			outputs.addAll(Arrays.asList(entries3));
			is = true;
		}

		if (!exactMach && entries4 != null && existPartWord(entries4)) {
			outputs.addAll(Arrays.asList(entries4));
			is = true;
		}

		return is;
	}

	private boolean existAllWord(CompoundEntry[] entries) {
		for (CompoundEntry ce : entries) {
			if (!ce.isExist())
				return false;
		}
		return true;
	}

	private boolean existPartWord(CompoundEntry[] entries) {
		for (CompoundEntry ce : entries) {
			if (ce.getWord().length() == 1)
				continue;
			if (ce.isExist())
				return true;
		}
		return false;
	}

	/**
	 * segment the compound noun with more than 6 characters.
	 * 
	 * @param input
	 *            he compound noun which should be segmented.
	 * @param outputs
	 * @param isFirst
	 * @return
	 * @throws MorphException
	 */
	private boolean analyzeLongText(String input, List<CompoundEntry> outputs,
			boolean isFirst) throws MorphException {

		// list of the word which start from a specific position.
		List<TreeMap<Integer, String>> wordlist = new ArrayList<TreeMap<Integer, String>>();

		for (int i = 0; i < input.length(); i++) {
			TreeMap<Integer, String> words = findWords(i, input);
			wordlist.add(words);
		}

		if (wordlist.size() < 1)
			return false;

		Map<Integer, List<String>> posMap = new HashMap<Integer, List<String>>();
		List<String> entries = getBestCandidate(0, input, wordlist, posMap);
		mergConsecutiveOneWord(entries);

		int offset = 0;
		for (String entry : entries) {
			WordEntry word = DictionaryUtil.getAllNoun(entry);
			if (word != null) {
				List<CompoundEntry> list = word.getCompounds();
				if (list == null || list.size() < 2) {
					outputs.add(new CompoundEntry(entry, offset, true,
							PatternConstants.POS_NOUN));
				} else {
					outputs.addAll(list);
				}
			} else if (exactMach) {
				return false;
			} else {
				outputs.add(new CompoundEntry(entry, offset, false,
						PatternConstants.POS_NOUN));
			}
			offset += entry.length();
		}

		return outputs.size() > 0;
	}

	/**
	 * merge the words with one characters which is present consecutively
	 * 
	 * @param entries
	 */
	private void mergConsecutiveOneWord(List<String> entries) {

		int removed = 0;
		int end = entries.size();

		boolean wasOneWord = false;

		for (int pos = 0; pos < end; pos++) {
			int curpos = pos - removed;
			String word = entries.get(curpos);

			if (wasOneWord && word.length() == 1) {
				entries.set(curpos - 1, entries.get(curpos - 1) + word);
				entries.remove(curpos);
				removed += 1;
			}
			wasOneWord = (word.length() == 1);
		}

	}

	private List<String> getBestCandidate(int pos, String input,
			List<TreeMap<Integer, String>> wordlist,
			Map<Integer, List<String>> posMap) {

		if (wordlist.size() <= pos)
			return null;
		if (posMap.get(pos) != null)
			return posMap.get(pos);

		List<String> results = new ArrayList<String>();

		TreeMap<Integer, String> candidates = wordlist.get(pos);
		Iterator<Integer> indexes = candidates.descendingKeySet().iterator();

		int score = 0;
		List<String> bestcandidate = null;
		String bestterm = null;

		while (indexes.hasNext()) {
			Integer index = indexes.next();
			String term = candidates.get(index);

			int tempscore = 0;
			if (term.length() > 1)
				tempscore += term.length();

			List<String> terms = null;

			if (pos + term.length() < input.length()) {
				terms = getBestCandidate(pos + term.length(), input, wordlist,
						posMap);
				tempscore += getScore(terms);
			}

			if (bestterm == null || score < tempscore) {
				bestcandidate = terms;
				bestterm = term;
				score = tempscore;
			} else if (score == tempscore
					&& bestcandidate != null
					&& terms != null
					&& terms.size() == 1
					&& terms.get(terms.size() - 1).length() > bestcandidate
							.get(bestcandidate.size() - 1).length()) { // (정보,법,학회)
																		// >
																		// (정보,법,학회)
																		// / The
																		// larger
																		// length
																		// the
																		// last
																		// word
																		// has,
																		// the
																		// better
				bestcandidate = terms;
				bestterm = term;
				score = tempscore;
			}
		}

		if (bestterm != null)
			results.add(bestterm);
		if (bestcandidate != null)
			results.addAll(bestcandidate);
		posMap.put(pos, results);

		return results;
	}

	private int getScore(List<String> terms) {
		int score = 0;
		for (String term : terms) {
			if (term.length() > 1)
				score += term.length();
		}
		return score;
	}

	/**
	 * find the word with max length which start at the start position and then
	 * return the length of the word.
	 * 
	 * @param start
	 *            the start position
	 * @param input
	 *            the compound noun which should be segmented.
	 * @return the length of the word with the found word.
	 * @throws MorphException
	 *             throw exception
	 */
	private TreeMap<Integer, String> findWords(int start, String input)
			throws MorphException {

		TreeMap<Integer, String> wordMap = new TreeMap<Integer, String>();

		// every term with one character is a candidate.
		wordMap.put(1, input.substring(start, start + 1));

		for (int i = (start + 2); i <= input.length(); i++) {
			String text = input.substring(start, i);

			Iterator<WordEntry> iter = DictionaryUtil.findWithPrefix(text);
			if (iter == null || !iter.hasNext())
				break;

			WordEntry entry = DictionaryUtil.getAllNoun(text);
			if (entry != null) {
				wordMap.put(text.length(), text);

			}

		}

		return wordMap;
	}

	private CompoundEntry[] analysisBySplited(int[] units, String input,
			boolean isFirst) throws MorphException {

		List<CompoundEntry> entries = new ArrayList<CompoundEntry>();

		int pos = 0;
		String prev = null;

		for (int i = 0; i < units.length; i++) {

			String str = input.substring(pos, pos + units[i]);

			if (i != 0 && !validCompound(prev, str, isFirst && (i == 1), i))
				return null;

			analyzeSingle(str, entries); // CompoundEntry 로 변환

			pos += units[i];
			prev = str;
		}

		return (CompoundEntry[]) entries.toArray(new CompoundEntry[0]);
	}

	/**
	 * 입력된 String 을 CompoundEntry 로 변환
	 * 
	 * @param input
	 *            input
	 * @return compound entry
	 * @throws MorphException
	 *             exception
	 */
	private void analyzeSingle(String input, List<CompoundEntry> entries)
			throws MorphException {

		int score = AnalysisOutput.SCORE_ANALYSIS;
		// int ptn = PatternConstants.PTN_N;
		char pos = PatternConstants.POS_NOUN;
		if (input.length() == 1) {
			entries.add(new CompoundEntry(input, 0, true, pos));
			return;
		}

		WordEntry entry = DictionaryUtil.getWordExceptVerb(input);
		if (entry != null) {
			score = AnalysisOutput.SCORE_CORRECT;
			if (entry.getFeature(WordEntry.IDX_NOUN) != '1'
					&& entry.getFeature(WordEntry.IDX_NOUN) != '2') {
				// ptn = PatternConstants.PTN_AID;
				pos = PatternConstants.POS_AID;
			}
		}

		if (entry != null && entry.getFeature(WordEntry.IDX_NOUN) == '2') {
			entries.addAll(entry.getCompounds());
		} else {
			entries.add(new CompoundEntry(input, 0,
					score == AnalysisOutput.SCORE_CORRECT, pos));
		}

	}

	private boolean validCompound(String before, String after, boolean isFirst,
			int pos) throws MorphException {

		if (pos == 1 && before.length() == 1
				&& (!isFirst || !DictionaryUtil.existPrefix(before)))
			return false;

		if (after.length() == 1 && !isFirst
				&& !DictionaryUtil.existSuffix(after))
			return false;

		if (pos != 1 && before.length() == 1) {

			WordEntry entry1 = DictionaryUtil.getUncompound(before + after);
			if (entry1 != null) {
				List<CompoundEntry> compounds = entry1.getCompounds();
				if (before.equals(compounds.get(0).getWord())
						&& after.equals(compounds.get(1).getWord()))
					return false;
			}

		}

		WordEntry entry2 = after.length() == 1 ? null : DictionaryUtil
				.getUncompound(after);
		if (entry2 != null) {
			List<CompoundEntry> compounds = entry2.getCompounds();
			if ("*".equals(compounds.get(0).getWord())
					&& after.equals(compounds.get(1).getWord()))
				return false;
		}

		return true;
	}
}
