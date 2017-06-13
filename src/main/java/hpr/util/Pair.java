package hpr.util;

public class Pair<A, B> {
	private A key;
	private B value;

	public Pair(A key, B value) {
		super();
		this.key = key;
		this.value = value;
	}

	public int hashCode() {
		int hashFirst = key != null ? key.hashCode() : 0;
		int hashSecond = value != null ? value.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	public boolean equals(Object other) {
		if (other instanceof Pair) {
			Pair<A,B> otherPair = (Pair<A,B>) other;
			return 
					((  this.key == otherPair.key ||
					( this.key != null && otherPair.key != null &&
					this.key.equals(otherPair.key))) &&
					(	this.value == otherPair.value ||
					( this.value != null && otherPair.value != null &&
					this.value.equals(otherPair.value))) );
		}

		return false;
	}

	public String toString()
	{ 
		return "(" + key + ", " + value + ")"; 
	}

	public A getKey() {
		return key;
	}

	public A first() {
		return key;
	}

	public void setKey(A key) {
		this.key = key;
	}

	public B getValue() {
		return value;
	}

	public B second() {
		return value;
	}

	public void setValue(B value) {
		this.value = value;
  }
}