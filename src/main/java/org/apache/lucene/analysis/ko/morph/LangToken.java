package org.apache.lucene.analysis.ko.morph;

/**
 * store the result splited by LanguageSpliter.
 * @author SooMyung.Lee(smlee0818@argonet.co.kr)
 *
 */
public class LangToken {

	private int offset = 0;
	
	private char type;
	
	private String term;

	public LangToken(String term, int offset, char t) {
		this.term = term;
		this.offset = offset;
		this.type = t;
	}
	
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}
}
