package com.yathzee.pc;

import junit.framework.TestCase;

public class MathUtilTest extends TestCase {

  public void testFac() {
    assertEquals(1,MathUtil.fac(0));
    assertEquals(1,MathUtil.fac(1));
    assertEquals(40320,MathUtil.fac(8));
  }

  public void testPoss() {
    assertEquals(5,MathUtil.poss(5,1));
    assertEquals(0,MathUtil.poss(5,6));
    assertEquals(10,MathUtil.poss(5,3));
  }

  public void testBinDist() {
    assertEquals(0.375,MathUtil.binDist(3,0.5,2));
    assertEquals(0.24609375,MathUtil.binDist(10,0.5,5));
  }

  public void testCumBinDist() {
    assertEquals(0.0546875,MathUtil.cumBinDist(10,0.5,3));
  }
}