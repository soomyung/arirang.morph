package org.apache.lucene.analysis.ko.morph;

import java.util.ArrayList;
import java.util.List;


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

/**
 * store the word list into which an unsegmented sentence can be divide.
 */
public class WordListCandidate implements Cloneable {
  
  // segmented word list
  private List<List<AnalysisOutput>> wordList = new ArrayList<List<AnalysisOutput>>();

  private int correctLength = -1;
  
  private int verbCount = -1;
  
  private int unknownCount = -1;
  
  public WordListCandidate() {

  }
  
  public WordListCandidate(List<AnalysisOutput> analysisOutputs) {
    this.addWord(analysisOutputs);
  }
  
    /**
   * 
   * @param analysisOutputs analysis outputs for each word
   */
  public void addWord(List<AnalysisOutput> analysisOutputs) {
    wordList.add(0, analysisOutputs);
  }
  
  public void replaceFirst(List<AnalysisOutput> analysisOutputs) {
    wordList.set(0, analysisOutputs);
  }
   
  public List<List<AnalysisOutput>> getWordList() {
    return this.wordList;
  }
  
  public void setWordList(List<List<AnalysisOutput>> wordList) {
    this.wordList = wordList;
  }
  
  public String getFirstFragment() {
    return this.wordList.get(0).get(0).getSource();
  }
  
  public int getCorrectLength() {
    
    if(this.correctLength!=-1) return correctLength;
    
    correctLength = 0;
    for(List<AnalysisOutput> outputs : wordList) {
      // words with one character is not counted
      if(outputs.get(0).getSource().length()==1) continue;
      if(outputs.get(0).getScore()==AnalysisOutput.SCORE_CORRECT)
        correctLength += outputs.get(0).getSource().length();
    }
    
    return correctLength;
  }
  
  public void setCorrectLength(int l) {
    this.correctLength = l;
  }
  
  public int getVerbCount() {
    
    if(this.verbCount!=-1) return verbCount;
    verbCount = 0;
    
    for(List<AnalysisOutput> outputs : wordList) {
      // words with one character is not counted
      if(outputs.get(0).getSource().length()==1) continue;
      int ptn = outputs.get(0).getPatn();
      
      if((ptn>=PatternConstants.PTN_VM && ptn <= PatternConstants.PTN_VMXMJ) ||
          outputs.get(0).getEomi()!=null) {
        verbCount++;
      }
    }
    
    return verbCount;
    
  }
  
  public void setVerbCount(int c) {
    this.verbCount = c;
  }
  
  public int getUnknownCount() {
    
    if(this.unknownCount!=-1) return unknownCount;
    unknownCount = 0;
    
    for(List<AnalysisOutput> outputs : wordList) {
      int score = outputs.get(0).getScore();
      if(score<=AnalysisOutput.SCORE_ANALYSIS) unknownCount++;
    }
    
    return unknownCount;
    
  }
  
  public void setUnknownCount(int c) {
    this.unknownCount = c;
  }
  
  /**
   * create a clone
   */
  public WordListCandidate newCopy() {   
    
    WordListCandidate c = new WordListCandidate();
    c.wordList.addAll(wordList);
    
    return c;
  }
  
}
