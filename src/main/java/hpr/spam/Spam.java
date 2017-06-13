package hpr.spam;

import hpr.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class Spam {
	
	private static final int AND_CONDITION_MAX_VALUE = 100;
	
	private static class IndexQuot {
		public int	index_;		// 구분자 (-1은 빈노드, 0은 독립 인덱스, 1이상은 그룹 지정자
		public int	quot_;		// 몫
		
		public IndexQuot( int index, int quot ) {
			index_ = index;
			quot_= quot;
		}
	};

	private static class SpamData {
		
		private String spamWord_;
		private ArrayList<IndexQuot> indexQuots_ = new ArrayList<IndexQuot>() ;
		
		public void add( int index, int quot ) {
			if( 0 > index ) 
				return;
			
			indexQuots_.add(new IndexQuot(index, quot ));
		}
		
		public boolean isEmpty() {
			return indexQuots_.isEmpty();
		}

		public void setSpamWord( String spamWord ) {
			 spamWord_ = spamWord;
		}
		
		public final String getSpamWord() {
			return spamWord_;
		}

		public final ArrayList<IndexQuot> getIndexQuots() {
			return indexQuots_;
		}
		
		public void display(StringBuilder sb, final int level) {
			for(int i = 0; i < indexQuots_.size(); ++i ) {
				sb.append("[").append(level).append("]")
					.append(indexQuots_.get(i).index_).append(",")
					.append(indexQuots_.get(i).index_).append(":")
					.append(spamWord_).append(System.getProperty("line.separator"));
			}
		}
	};

	private static class SpamChar {
		
		private SpamData spamData_ = new SpamData();;
		private Map<Character, SpamChar> spamChars_ = new HashMap<Character, SpamChar>();
		
		public void createChild( final int wordId, final int quot, final String spamWord, int depth ) {
			if( spamWord.length() == depth ) {
				
				spamData_.setSpamWord(spamWord);
				spamData_.add(wordId, quot);
				
				return;
			}
			
			char ch = spamWord.charAt(depth);
			SpamChar spamChar = spamChars_.get(ch);
			if( null == spamChar ) {
				spamChar = new SpamChar();
				spamChars_.put(ch, spamChar);
			}

			spamChar.createChild(wordId, quot, spamWord, ++depth);
		}
		
		public Pair<Integer, SpamData> findSpam( final String sentence, int depth, final boolean isFirst ) {
			if( sentence.length() == depth) {
				return new Pair<Integer, SpamData>(depth, spamData_);
			}
			
			if( spamData_.isEmpty() ) {
				char ch = sentence.charAt(depth);
				SpamChar spamChar = spamChars_.get(ch);
				if( null == spamChar ) {
					if( isFirst ) {
						++depth;
					}
					return new Pair<Integer, SpamData>(depth, spamData_);
				}
				else {
					return spamChar.findSpam(sentence, ++depth, false);
				}
		
			}
			else {
				return new Pair<Integer, SpamData>(depth, spamData_);
			}
		}

		public void display(StringBuilder sb, final int level) {
			spamData_.display(sb, level);

			for( Map.Entry<Character, SpamChar> entry : spamChars_. entrySet() ) {
				entry.getValue().display(sb, level+1);
			}
		}
			

	};
	
	
	private int maxWordIndex_ = 0;
	private SpamChar spamCharRoot_ = new SpamChar();
	private Map<Integer, String[]> originalSpamWords_ = new HashMap<Integer, String[]>();
	
	public void append( final String spamWord ) {
		
		spamCharRoot_.createChild( 0, AND_CONDITION_MAX_VALUE, spamWord, 0);
	}
	
	public void append( final String[] spamWords ) {
		
		if( null == spamWords || 0 == spamWords.length) {
			return;
		}
		else if( 1 == spamWords.length ) {
			append( spamWords[0] );
		}
		else {
			int quot = AND_CONDITION_MAX_VALUE / spamWords.length;
			int rem = AND_CONDITION_MAX_VALUE % spamWords.length;
			
			++maxWordIndex_;
			for( int i = 0; i < spamWords.length; ++i ) {
				spamCharRoot_.createChild( maxWordIndex_, quot + (0 == i ? rem : 0), spamWords[i], 0);
			}
			
			originalSpamWords_.put( maxWordIndex_, spamWords);
		}
	}
	
	public List<String> findSpam( final String sentence ) {
		List<String> result = new ArrayList<String>();

		Map<Integer, Integer> collect = new HashMap<Integer, Integer>();
		
		int depth = 0;
		while( sentence.length() > depth ) {
			
			Pair<Integer, SpamData> res = spamCharRoot_.findSpam(sentence, depth, true);
			
			depth = res.getKey();
			SpamData spamData = res.getValue();
			
			if( spamData.isEmpty() ) {
				continue;
			}
			
			final ArrayList<IndexQuot> indexQuots = spamData.getIndexQuots();
			
			for( int i = 0; i < indexQuots.size(); ++i ) {
				IndexQuot indexQuot = indexQuots.get(i);
				
				if( 0 == indexQuot.index_) {
					result.add(spamData.getSpamWord());
		
					return result;
				}
				
				if( 0 < indexQuot.index_ ) {
					
					Integer quot = collect.get(indexQuot.index_);
					if( quot == null ) {
						collect.put( indexQuot.index_, indexQuot.quot_);
						continue;
					}
					else {
						quot += indexQuot.quot_;
					}
					
					if( AND_CONDITION_MAX_VALUE == quot ) {
						for( String s: originalSpamWords_.get(indexQuot.index_)) {
							result.add(s);
						}
						return result;
					}
				}
			}
		}
		return result;
	}

	public StringBuilder display() {
		 StringBuilder sb = new StringBuilder();
		 
		 spamCharRoot_.display(sb, 0);
		 
		 return sb;
	}
	
	
	// public static void main(String[] args) {
	// 	Spam spam = new Spam();
		
	// 	spam.append("섹  수");
	// 	spam.append("바 다");
	// 	spam.append("바다 라&섹수".split("&"));
		
		
	// 	List<String> res = spam.findSpam("ab바다 라cd섹 수e 섹수");
	// 	StringBuilder sb = new StringBuilder();
	// 	for( String s: res ) {
	// 		if( 0 < sb.length() )
	// 			sb.append("&");
	// 		sb.append(s);
	// 	}
	// 	System.out.println("result:" + sb.toString());
	// }

}
