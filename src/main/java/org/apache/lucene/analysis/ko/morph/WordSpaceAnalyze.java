package org.apache.lucene.analysis.ko.morph;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.ko.utils.MorphUtil;
import org.apache.lucene.analysis.ko.utils.SyllableUtil;

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

public class WordSpaceAnalyze {
  
  private MorphAnalyzer morphAnalyzer = null;
  
  public WordSpaceAnalyze() {
    morphAnalyzer = new MorphAnalyzer();
  }
  
  public List analyze(String input) throws MorphException {
    
    int len = input.length();
    List<AnalysisOutput>[][] repositories = new ArrayList[len][len];
    
    for(int s=0; s < len ; s++) {
      
      int xend = -1;
      boolean josaFlag = true; boolean eomiFlag=true; boolean etcFlag = true;
           
      int score = 0;
      
      for(int e=s; e<len; e++) {
      
        String text = input.substring(s, e+1);
        
//        char[] features = SyllableUtil.getFeature(input.charAt(e));
//        if(josaFlag && (features[SyllableUtil.IDX_JOSA1]=='1' || 
//            features[SyllableUtil.IDX_JOSA2]=='1')) {
//          xend = e;
//        }else if(josaFlag && features[SyllableUtil.IDX_JOSA1]=='0' &&
//            features[SyllableUtil.IDX_JOSA2]=='0') {
//          josaFlag = false;
//        }
//        
//        if(eomiFlag && (isNLMBEomi(features) || 
//            features[SyllableUtil.IDX_EOMI1]=='1' || 
//            features[SyllableUtil.IDX_EOMI2]=='1')) {
//          xend = e;
//        } else if(eomiFlag && !isNLMBEomi(features) &&
//            features[SyllableUtil.IDX_EOMI1]=='0' &&
//            features[SyllableUtil.IDX_EOMI2]=='0') {
//          eomiFlag = false;
//        }
        
        repositories[s][e] = morphAnalyzer.analyze(text);
      
//        if(repositories[s][e].get(0).getScore()<AnalysisOutput.SCORE_COMPOUNDS &&
//            score > repositories[s][e].get(0).getScore()) break;
        
        if(score<repositories[s][e].get(0).getScore()) 
        	score = repositories[s][e].get(0).getScore();
        if(xend==-1 || repositories[s][e].get(0).getScore()>=AnalysisOutput.SCORE_CORRECT) xend = e;
        if(repositories[s][e].get(0).getScore()>=AnalysisOutput.SCORE_CORRECT) 
        	System.out.println(repositories[s][e].get(0));
        
      }
      
      if(xend!=-1) s = xend;
      System.out.println("next");
    }
    
    List sentence = chooseBest(repositories);
    
    return sentence;
  }
  
  private List chooseBest(List<AnalysisOutput>[][] repositories) {
	  
	  List<List> candidates = new ArrayList();
	  List<AnalysisOutput> sentence = new ArrayList();
	  candidates.add(sentence);
	  
	  moveE(repositories, 0, 0,sentence,candidates);
	
	  int bestScore = 0;
	  List<AnalysisOutput> bestSentence = null;
	  
	  for(List<AnalysisOutput> c : candidates) {
		
		int totalScore = 0;
		for(AnalysisOutput o : c) {
			totalScore += o.getScore();
		}
		
		int score = totalScore/c.size();
		if(score>bestScore) {
			bestScore = score;
			bestSentence = c;
		}
	  }
	  
	  return bestSentence;
  }

  private boolean moveE(List<AnalysisOutput>[][] repositories, int s, int e, 
		  List<AnalysisOutput> sentence,  List<List> candidates) {

	  boolean has = false;
	  List<AnalysisOutput> c_sentence = new ArrayList(sentence);
	  
	  for(int i=e;i<repositories[s].length;i++) 
	  {
		if(repositories[s][i]==null) return false; 
		
		List<AnalysisOutput> t_sentence = null;
		if(has) {
			t_sentence = new ArrayList(c_sentence);
			t_sentence.add(repositories[s][i].get(0));
			candidates.add(t_sentence);			
		} else {
			sentence.add(repositories[s][i].get(0));
			t_sentence = sentence;
		}
		
		
		System.out.println("moveE-1");
		if(!moveS(repositories,s,i,t_sentence,candidates)) {
			candidates.remove(t_sentence);
		}
		
		System.out.println("moveE-2");
		has = true;  
	  }
	  
	  return true;
  }
  
  private boolean moveS(List<AnalysisOutput>[][] repositories, int s, int e,
		  List<AnalysisOutput> sentence,  List<List> candidates) 
  {	
	  int me = e+1;
	  System.out.println(repositories[s][e].get(0).getSource());
	  int ms = s+repositories[s][e].get(0).getSource().length();
	  if(ms==repositories.length) return true;
	  
	  moveE(repositories,ms,me,sentence,candidates);
	  
	  return false;
  }
  
  private boolean isNLMBEomi(char[] features) {
    return (features[SyllableUtil.IDX_YNPLA]=='1' || features[SyllableUtil.IDX_YNPMA]=='1' || 
        features[SyllableUtil.IDX_YNPNA]=='1' || features[SyllableUtil.IDX_YNPBA]=='1');
  }
  
}
