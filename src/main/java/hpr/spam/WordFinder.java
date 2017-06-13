package hpr.spam;

import hpr.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordFinder {

	private static class WordChar {
		
		private String word_;
		private Map<Character, WordChar> wordChars_ = new HashMap<Character, WordChar>();
		
		public void createChild( final String word, int depth ) {
			if( word.length() == depth ) {
				word_ = word;
				return;
			}
			
			char ch = word.charAt(depth);
			WordChar wordChar = wordChars_.get(ch);
			if( null == wordChar ) {
				wordChar = new WordChar();
				wordChars_.put(ch, wordChar);
			}

			wordChar.createChild( word, ++depth);
		}
		
		public Pair<Integer, String> findWord( final String sentence, int depth, final boolean isFirst ) {
			if( sentence.length() == depth) {
				return new Pair<Integer, String>(depth, word_);
			}
			
			if( null == word_ ) {
				char ch = sentence.charAt(depth);
				WordChar wordChar = wordChars_.get(ch);
				if( null == wordChar ) {
					if( isFirst ) {
						++depth;
					}
					return new Pair<Integer, String>(depth, null);
				}
				else {
					return wordChar.findWord(sentence, ++depth, false);
				}
		
			}
			else {
				return new Pair<Integer, String>(depth, word_);
			}
		}

	};
	
	
	private WordChar root_ = new WordChar();
	
	public void append( final String word ) {
		
		root_.createChild( word, 0);
	}
	
	public List<Pair<Integer, String>> findAll ( final String sentence ) {
		
		List<Pair<Integer, String>> result = new ArrayList<Pair<Integer, String>>();
		
		int depth = 0;
		while( sentence.length() > depth ) {
			
			Pair<Integer, String> res = root_.findWord(sentence, depth, true);
			
			String word = res.getValue();

			if( null != word) {
				result.add(new Pair<Integer, String>(depth, word));
			}
			
			depth = res.getKey();
		}
		return result;
	}

	
	// public static void main(String[] args) {
	// 	WordFinder finder = new WordFinder();
		
	// 	finder.append("섹 수");
	// 	finder.append("바다");
	// 	finder.append("바다 라&섹수");
		
		
	// 	String str = "innopost         ezicomb_mms01    01023609146        1건:4월9일(a   b\t바 다 라cd섹 수\ne 섹수바다ㄴㅇㄹㄴㄹ\nddddddddddddddd";
	// 	for( String s: str.split("\\s+", 4)) {
	// 		System.out.println("s[" + s +"]");
	// 	}
		
	// 	List<Pair<Integer, String>> res = finder.findAll(str);
	// 	StringBuilder sb = new StringBuilder();
		
	// 	int pos = 0;
	// 	for( Pair<Integer, String> e: res ) {
			
	// 		System.out.println("e.getKey():" + e.getKey() + " " + e.getValue());
			
	// 		sb.append("[").append(str.substring(pos, e.getKey())).append(",").append(e.getValue()).append("]");
	// 		pos = e.getKey() + e.getValue().length();
			
	// 	}
	// 	if( pos < str.length()) {
	// 		sb.append("[").append(str.substring(pos)).append("]");
	// 	}
	// 	System.out.println("result:" + sb.toString());
		
		
	// }
}