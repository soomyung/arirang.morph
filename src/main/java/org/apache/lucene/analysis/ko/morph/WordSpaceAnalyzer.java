package org.apache.lucene.analysis.ko.morph;

import java.util.ArrayList;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.ko.utils.DictionaryUtil;


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

public class WordSpaceAnalyzer {

  private final MorphAnalyzer morphAnal = new MorphAnalyzer();
  
  private static List<AnalysisOutput> EMPTY_LIST = new ArrayList<AnalysisOutput>();
  
  /**
   * segment unsegmented sentence into words
   * a input text which has less than 3 characters or more than 10 character is not analyzed, 
   * because It seems to be no word segmentation error.
   * @param inputText unsegmented sentence
   * @return  segmented sentence into words
 * @throws MorphException 
   */
  public List<AnalysisOutput> analyze(String inputText) throws MorphException {
    
    int length = inputText.length();
    if(length<3 || length>15) return WordSpaceAnalyzer.EMPTY_LIST;
    
    List<WordListCandidate> candiateList = new ArrayList<WordListCandidate>();
    
    // add last character as the first candidate
    WordListCandidate listCandidate = new WordListCandidate(
        morphAnal.analyze(inputText.substring(length-1,length)));
    
    candiateList.add(listCandidate);
    
    boolean divided = false;
    
    Map<String,List<AnalysisOutput>> analyzedSet = new HashMap<String,List<AnalysisOutput>>();
    
    // from last position, check whether if each position can be a dividing point.
    for(int start=inputText.length()-2;start >=0 ; start--) {
      
      String thisChar = inputText.substring(start, start+1);
      List<WordListCandidate> newCandidates = null;
      
      if(!divided) {
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
      }
      
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
      
      int newStart = validation(candiateList, thisChar, start, inputText);

      divided = (newStart==start);
      if(divided) start = newStart;
    }
    
    Collections.sort(candiateList, new WordListComparator());
    
    List<AnalysisOutput> result = new ArrayList<AnalysisOutput>(); 
    for(WordListCandidate candidate : candiateList) {
      
      if(candiateList.indexOf(candidate)!=candiateList.size()-1 && 
          hasConsecutiveOneWord(candidate))
        continue;
      
      for(List<AnalysisOutput> outputs : candidate.getWordList()) {
        result.add(outputs.get(0));
      }      
      break;
    }
 
    return result;
    
  }
  
  private boolean hasConsecutiveOneWord(WordListCandidate candidate) {
    
    int size = candidate.getWordList().size();
    for(int i=1;i<size;i++) {
      List<AnalysisOutput> outputs1 = candidate.getWordList().get(i-1);
      List<AnalysisOutput> outputs2 = candidate.getWordList().get(i);
      if(outputs1.get(0).getSource().length()==1 
          && outputs2.get(0).getSource().length()==1)
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
   * 
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
    
    // the word with greater length than 6 doesn't exist
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
  
}
