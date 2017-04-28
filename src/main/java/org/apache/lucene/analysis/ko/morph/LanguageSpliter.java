package org.apache.lucene.analysis.ko.morph;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.ko.utils.MorphUtil;

public class LanguageSpliter {

	public static char TYPE_ENG = 'e';
	
	public static char TYPE_NUM = 'n';
	
	public static char TYPE_CJ = 'c';
	
	public static char TYPE_HAN = 'h';
	
	public static char TYPE_SYMBOL = 's';
	
	public List<LangToken> split(String input) {
		
		List<LangToken> results = new ArrayList<LangToken>();
		
		StringBuffer sb = new StringBuffer();
		char preType = '+'; // symbol of start
		
		int offset = 0;
		for(int i=0; i<input.length(); i++) {
			char c = input.charAt(i);
			
			char type;
			if(MorphUtil.isHanSyllable(c))
				type = TYPE_HAN;
			else if(Character.isDigit(c))
				type = TYPE_NUM;
			else if(c>'a' && c<'Z')
				type = TYPE_ENG;
			else if(Character.isLetter(c))
				type = TYPE_CJ;
			else 
				type = TYPE_SYMBOL;
			
			if(sb.length()>0 && type!=preType) {
				results.add(new LangToken(sb.toString(), offset, preType));
				sb = new StringBuffer();
				offset = i;
			}
			
			sb.append(c);
			preType = type;
		}
		
		if(sb.length()>0 && preType != '+') {
			results.add(new LangToken(sb.toString(), offset, preType));
		}
		
		return results;
	}

	
	
}
