package ch.fhnw.geiger.totalcross;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper class to work with java and Totalcross Random class.
 */
public class Random {

  private static Class rcls = null;
  private static Object robj = null;
  private long seed;

  public Random() {
    this(System.currentTimeMillis());
  }

  public Random(long seed) {
    this.seed = seed;
  }

  /**
   * Get next random int.
   *
   * @param border the upper border for the random int
   * @return a random int
   */
  public static int nextInt(int border) {
    try {
      if (robj == null) {
        if (Detector.isTotalCross()) {
          rcls = Class.forName("totalcross.util.Random");
        } else {
          rcls = Class.forName("java.util.Random");
        }
        Constructor ct = rcls.getConstructor(new Class[0]);
        robj = (ct.newInstance(new Object[0]));
      }
      Method m = rcls.getMethod("nextInt", new Class[]{int.class});
      return (int) (m.invoke(robj, new Object[]{border}));
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
        | InvocationTargetException | InstantiationException e) {
      throw new RuntimeException("OOPS! That is bad", e);
    }
  }

  public int nextInt() {
    return nextInt(Integer.MAX_VALUE);
  }

  protected synchronized int next(int bits) {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    return (int) (seed >>> (48 - bits));
  }

}
