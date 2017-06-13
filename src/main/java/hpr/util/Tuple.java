package hpr.util;

public class Tuple<A, B, C> {
	private A value1_;
	private B value2_;
	private C value3_;

	public Tuple(A value1, B value2, C value3) {
		super();
		this.value1_ = value1;
		this.value2_ = value2;
		this.value3_ = value3;
	}

	public int hashCode() {
		int hashFirst = value1_ != null ? value1_.hashCode() : 0;
		int hashSecond = value2_ != null ? value2_.hashCode() : 0;
		int hashThird = value3_ != null ? value3_.hashCode() : 0;

		return ((hashFirst + hashSecond) * hashSecond + hashFirst) * hashThird + hashThird;
	}

	public boolean equals(Object other) {
		if (other instanceof Tuple) {
			Tuple<A,B,C> otherPair = (Tuple<A,B,C>) other;
			return 
					((  this.value1_ == otherPair.value1_ ||
					( this.value1_ != null && otherPair.value1_ != null &&
					this.value1_.equals(otherPair.value1_))) &&
					(	this.value2_ == otherPair.value2_ ||
					( this.value2_ != null && otherPair.value2_ != null &&
					this.value2_.equals(otherPair.value2_))) &&
					(	this.value3_ == otherPair.value3_ ||
					( this.value3_ != null && otherPair.value3_ != null &&
					this.value3_.equals(otherPair.value3_))) );
		}

		return false;
	}

	public String toString()
	{ 
		return "(" + value1_ + ", " + value2_ + ", " + value3_ + ")"; 
	}

	public A get1() {
		return value1_;
	}

	public void set1(A value1) {
		this.value1_ = value1;
	}

	public B get2() {
		return value2_;
	}

	public void set2(B value2) {
		this.value2_ = value2;
    }

	public C get3() {
		return value3_;
	}

	public void set3(C value3) {
		this.value3_ = value3;
    }
}