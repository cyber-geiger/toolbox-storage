package ch.fhnw.geiger.localstorage;

import ch.fhnw.geiger.totalcross.Random;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class TestRandom {

  @Test
  public void testSeeding() {
    Set<Integer> s = new HashSet<>();
    int iter = 1000;
    for (int i = 0; i < iter; i++) {
      s.add(new Random().nextInt());
    }
    double uniqueValues = 100.0 / (double) (iter) * s.size();
    System.out.println("The percentage of unique values when using random seed was " + uniqueValues);
    Assert.assertTrue("result set is too small " + iter + " iterations did result in " + s.size() + " unique values",
        uniqueValues > 95);
  }

  @Test
  public void testRandomSequence() {
    Set<Integer> s = new HashSet<>();
    int iter = 1000;
    Random r = new Random();
    for (int i = 0; i < iter; i++) {
      s.add(r.nextInt());
    }
    double uniqueValues = 100.0 / (double) (iter) * s.size();
    System.out.println("The percentage of unique values when using sequencial randoms was " + uniqueValues);
    Assert.assertTrue("result set is too small " + iter + " iterations did result in " + s.size() + " unique values",
        uniqueValues > 95);
  }


}
