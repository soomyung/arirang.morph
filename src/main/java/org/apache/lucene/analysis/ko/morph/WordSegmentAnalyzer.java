package org.apache.lucene.analysis.ko.morph;

import org.apache.lucene.analysis.ko.utils.DictionaryUtil;

import java.util.*;


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

public class WordSegmentAnalyzer {

  private final MorphAnalyzer morphAnal = new MorphAnalyzer();

    private static final int maxCandidate = 64;

    private static final int adjustNoOfCandidate = 40;
    
    private static final String possibleWordStartJosa = "의은가나며아야에엔여와요이";

    @SuppressWarnings("unused")
	public List<List<AnalysisOutput>> analyze(String inputText) throws MorphException {
        int[] nounPos = new int[inputText.length()];
        for(int i=0;i<inputText.length();i++) nounPos[i]=-1; // initialization

        StringBuffer sb = new StringBuffer();
        for(int i=0;i<inputText.length();i++) {
            int lastIndex = findLongestNoun(i, inputText);
            if(lastIndex!=-1 && nounPos[lastIndex]==-1) {
                nounPos[lastIndex] = i; // store the start position of a noun at the iastIndex position to which the noun spans.
            }
        }

        int[] oneJosa = new int[inputText.length()];
        List<String> segList = splitByNoun(inputText, nounPos,oneJosa);
        List<List<AnalysisOutput>> result = new ArrayList<List<AnalysisOutput>>();
        
        // less than 4 length word without noun can be a unknown noun. in many case it is a person's name.
//        if(segList.size()==1 && inputText.length()<=4) return result;
        
        int offset = 0;
        for(int i=0;i<segList.size();i++) {
        	int length = segList.get(i).length();
        	boolean containOneJosa = isContainOneJosa(offset, length,oneJosa);
            analyze(segList.get(i), result, containOneJosa);
            offset += length;
        }

        return result;
    }

    private boolean isContainOneJosa(int offset, int length, int[] oneJosa) {
    	for(int i=offset;i<length;i++) {
    		if(oneJosa[i]==1) return true;
    	}
    	return false;
    }
    
    /**
     * find the longest noun with more than 2 length.
     * @param start
     * @param inputText
     * @return
     */
    public int findLongestNoun(int start, String inputText) throws MorphException {
        StringBuffer sb = new StringBuffer();
        sb.append(inputText.charAt(start));

        int lastIndex = -1;
        for(int i=start+1;i<inputText.length();i++) {
            sb.append(inputText.charAt(i));
            if(!DictionaryUtil.findWithPrefix(sb.toString()).hasNext()) {
                return lastIndex;
            }
            if(DictionaryUtil.getAllNoun(sb.toString())!=null) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    public List<String> splitByNoun(String inputText, int[] nounPos, int[] oneJosa) throws MorphException {
        List<Integer> positions = new LinkedList<Integer>();

        for(int i=1;i<nounPos.length;i++) {
        	if(nounPos[i]!=-1) continue;
            int endWithMe = -1;
            for(int j=i+1;j<nounPos.length;j++) {
                if(nounPos[j]==i) {endWithMe=j;break;}
                if(nounPos[j]>i) {break;}
            }
            if(endWithMe==-1) continue; // there is no word starting at i position

            if(possibleWordStartJosa.indexOf(inputText.charAt(i))!=-1)
            	oneJosa[i] = 1;
            
            boolean possibleWord = true;
            for(int j=endWithMe+1;j<nounPos.length;j++) {
                if(nounPos[j]!=-1&&(nounPos[j]<endWithMe ||
                        (nounPos[j]==endWithMe && (endWithMe-i)<=(j-nounPos[j]))) // [명사+명사]인 경우, 예)도서관어린이사서의
                        ) 
                {
                	possibleWord = false;
                }
                if(!possibleWord || nounPos[j]>endWithMe) {break;}
            }
            if(possibleWord) {
                positions.add(i);
            }
        }

        List<String> fragments = new ArrayList<String>();
        int start = 0;
        for(int i=0;i<positions.size();i++) {
            fragments.add(inputText.substring(start,positions.get(i)));
            start = positions.get(i);
        }
        fragments.add(inputText.substring(start));

        return fragments;
    }

  /**
   * segment unsegmented sentence into words
   * a input text which has less than 3 characters or more than 10 character is not analyzed, 
   * because It seems to be no word segmentation error.
   * @param inputText unsegmented sentence
   * @return  segmented sentence into words
 * @throws MorphException 
   */
  public void analyze(String inputText, List<List<AnalysisOutput>> result, boolean containOneJosa) throws MorphException {

    List<WordListCandidate> candiateList = new ArrayList<WordListCandidate>();

    List<AnalysisOutput> aoList = morphAnal.analyze(inputText);
    if(aoList.get(0).getScore()==AnalysisOutput.SCORE_CORRECT && !containOneJosa) { // valid morpheme
        result.add(aoList);
        return;
    }

      int length = inputText.length();
    // add last character as the first candidate
    WordListCandidate listCandidate = new WordListCandidate(
        morphAnal.analyze(inputText.substring(length-1,length)));
    
    candiateList.add(listCandidate);
    
    boolean divided = false;
    
    Map<String,List<AnalysisOutput>> analyzedSet = new HashMap<String,List<AnalysisOutput>>();
    
    // from last position, check whether if each position can be a dividing point.
    for(int start=inputText.length()-2;start >=0 ; start--) {
      
      String thisChar = Character.toString(inputText.charAt(start));
      List<WordListCandidate> newCandidates = null;
      
//      if(!divided) {
        // newly created candidates
        newCandidates = new ArrayList<WordListCandidate>();
        
        for(WordListCandidate candidate : candiateList) {
          
          String fragment = thisChar + candidate.getFirstFragment();
          // build the position key with the start position and the end position
          String posKey = new StringBuffer()
              .append(start)
              .append(",")
              .append(start+candidate.getFirstFragment().length())
              .toString();
                    
          WordListCandidate newCandidate = candidate.newCopy();
          List<AnalysisOutput> outputs = analyzedSet.get(posKey);

          // check whether if already analyzed.
          if(outputs == null) {
            outputs = morphAnal.analyze(fragment);
            newCandidate.replaceFirst(outputs);
            analyzedSet.put(posKey, outputs);
          } else {
            newCandidate.replaceFirst(outputs);
          }
          
          newCandidates.add(newCandidate);
        }
//      }
      
      List<AnalysisOutput> outputs = morphAnal.analyze(thisChar);

      String posKey = new StringBuffer()
          .append(start)
          .append(",")
          .append(start+1)
          .toString();

      analyzedSet.put(posKey, outputs);
      
      for(WordListCandidate candidate : candiateList) {
        candidate.addWord(outputs);
      }      
      
      if(newCandidates!=null) candiateList.addAll(newCandidates);
      if(candiateList.size()>=maxCandidate) {
          Collections.sort(candiateList, new WordListComparator());
          removeLast(candiateList,adjustNoOfCandidate);
      }
      
//      int newStart = validation(candiateList, thisChar, start, inputText);
//
//      divided = (newStart==start);
//      if(divided) start = newStart;
    }
    
     Collections.sort(candiateList, new WordListComparator());
//    List<AnalysisOutput> result = new ArrayList<AnalysisOutput>();
    for(WordListCandidate candidate : candiateList) {
      
      if(candiateList.indexOf(candidate)!=candiateList.size()-1 && 
          hasConsecutiveOneWord(candidate))
        continue;
      
      for(List<AnalysisOutput> outputs : candidate.getWordList()) {
        result.add(outputs);
      }      
      break;
    }
    
  }
  
  private boolean hasConsecutiveOneWord(WordListCandidate candidate) {
    
    int size = candidate.getWordList().size();
    for(int i=1;i<size;i++) {
      List<AnalysisOutput> outputs1 = candidate.getWordList().get(i-1);
      List<AnalysisOutput> outputs2 = candidate.getWordList().get(i);
      if(outputs1.get(0).getStem().length()==1 
          && outputs2.get(0).getStem().length()==1)
        return true;
    }
    
    return false;
  }
  
  private int validation(List<WordListCandidate> candiateList, String thisChar, 
      int start, String inputText) {
    
    int newStart = -1;
    AnalysisOutput dividedOutput = null;
    
    boolean lastPos = true;
    for(int i=candiateList.size()-1; i>=0;i--) {
      
      WordListCandidate candidate = candiateList.get(i);
      AnalysisOutput output = candidate.getWordList().get(0).get(0);
      
      int tempStart = validWord(output, start, inputText, lastPos);
      lastPos = false;
      
      if(tempStart<=start) {
        newStart = tempStart;
        dividedOutput = output;
        break;
      }     
    }
    
    // if here is a dividing point.
    if(newStart==start) 
      removeInvalidCandidate(candiateList, dividedOutput);
    
    return newStart;
  }
  
  
  /**
   * 
   * @param candiateList  all candidate list
   * @param dividedOutput the dividing analysis output
   */
  private void removeInvalidCandidate(List<WordListCandidate> candiateList, AnalysisOutput dividedOutput) {
    
    List<WordListCandidate> removes = new ArrayList<WordListCandidate>();
    for(int i=0;i<candiateList.size();i++) {
      
      WordListCandidate candidate = candiateList.get(i);
      AnalysisOutput output = candidate.getWordList().get(0).get(0);
 
      if(!output.getSource().equals(dividedOutput.getSource()) &&
          !includeNoun(candidate, dividedOutput, i)) 
        removes.add(candidate);        
    }
    
    candiateList.removeAll(removes);
  }
  
  /**
   * when the fragment can be analyzed as a verb, check whether if noun is included in the fragment.
   * prevent from being divided such as "전복사고==>전^복사고"
   * @param candidate all candidate list
   * @param dividedOutput  this analysis output
   * @return  check result
   */
  private boolean includeNoun(WordListCandidate candidate, AnalysisOutput dividedOutput, int pos) {
    
    if(candidate.getWordList().size()>1) {
      AnalysisOutput nextOutput = candidate.getWordList().get(1).get(0);
      if(nextOutput.getSource().length()>1 &&
          nextOutput.getPatn() == PatternConstants.PTN_N 
          && nextOutput.getScore()==AnalysisOutput.SCORE_CORRECT)
        return true;
    }
    
    return false;
  }
  
  /**
   * return the start position of the longest valid noun before the start position
   * @param output  analysis output
   * @param start start position
   * @param inputText input text
   * @param isLast  whether if this is the last word.
   * @return  the start position of the longest valid noun
   */
  private int validWord(AnalysisOutput output, int start, String inputText, boolean isLast) {
    
    int newStart = -1;
    if(output.getScore()!=AnalysisOutput.SCORE_CORRECT || 
        start==0 || output.getSource().length()<2) return newStart;
    
    if(!isLast && output.getJosa()==null && output.getEomi()==null) return newStart;
    
    if(output.getScore()==AnalysisOutput.SCORE_CORRECT)  newStart = start;
    
    // the word with greater than 6 length doesn't exist
    int minPos = start - 6;
    if(minPos<0) minPos = 0;
    
    for(int i=start-1; i>=minPos; i--) {
      String word = inputText.substring(i,start) + output.getStem();
      if(DictionaryUtil.getWord(word)!=null) {
        newStart = i;
      }
    }
    
    return newStart;
  }

  /**
   * calculate the score which is the worst score of the derived word scores
   * @param list  input
   * @return  calculated score
   */
  public int getOutputScore(List<AnalysisOutput> list) {
    int score = 100;
    for (AnalysisOutput o : list) {
      score = Math.min(score, o.getScore());
    }
    return score;
  }
  
  private void removeLast(List<WordListCandidate> list, int start) {
  	List<WordListCandidate> removed = new ArrayList<WordListCandidate>();
  	for(int i=start;i<list.size();i++) {
  		removed.add(list.get(i));
  	}
  	
  	for(Object o : removed) {
  		list.remove(o);
  	}
  	
  	removed=null;
  }
  
}
