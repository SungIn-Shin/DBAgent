package hpr.util;

public class endian {

    public static short swap(short x) {
		return (short)((x << 8) |
					   ((x >> 8) & 0xff));
    }

    public static char swap(char x) {
		return (char)((x << 8) |
					  ((x >> 8) & 0xff));
    }

    public static int swap(int x) {
		return (int)((swap((short)x) << 16) |
					 (swap((short)(x >> 16)) & 0xffff));
    }

    public static long swap(long x) {
		return (long)(((long)swap((int)(x)) << 32) |
					  ((long)swap((int)(x >> 32)) & 0xffffffffL));
    }

    public static float swap(float x) {
		return Float.intBitsToFloat(swap(Float.floatToRawIntBits(x)));
    }

    public static double swap(double x) {
		return Double.longBitsToDouble(swap(Double.doubleToRawLongBits(x)));
    }
}
