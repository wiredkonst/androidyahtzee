package com.yathzee.pc;

import com.yathzee.Dice;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class DecisionTest extends TestCase {
  private void check(int score,int[] dice,boolean[] taken){
    List<Dice> ds = new ArrayList<>(5);
    for(int i=0;i<5;i++){
      Dice d = new Dice();d.num=dice[i];
      ds.add(d);
    }
    Decision d = new Decision(score,ds);
    for(int i=0;i<5;i++){
      assertTrue(taken[i]==d.ds.get(i).isTaken);
    }
  }
  public void testNumbers(){
    check(0, new int[]{1,2,3,1,4}, new boolean[]{true,false,false,true,false});
    check(5, new int[]{1,5,6,1,6}, new boolean[]{false,false,true,false,true});
  }
  public void testSame(){
    check(6, new int[]{3,3,1,3,6}, new boolean[]{true,true,false,true,true});
    check(6, new int[]{4,2,1,4,6}, new boolean[]{true,false,false,true,false});
    check(6, new int[]{4,3,3,3,3}, new boolean[]{true,true,true,true,false});
    check(7, new int[]{2,1,2,2,2}, new boolean[]{true,false,true,true,true});
    check(11, new int[]{1,1,2,2,1}, new boolean[]{true,true,false,false,true});
    check(11, new int[]{1,1,1,1,1}, new boolean[]{true,true,true,true,true});
    check(8, new int[]{1,1,1,1,1}, new boolean[]{true,true,true,false,false});
    check(8, new int[]{5,4,3,1,1}, new boolean[]{false,false,false,true,true});
    check(8, new int[]{5,1,1,1,5}, new boolean[]{true,true,true,true,true});
  }
  public void testSeq(){
    check(9, new int[]{1,2,1,3,6}, new boolean[]{true,true,false,true,false});
    check(9, new int[]{2,2,4,3,5}, new boolean[]{true,false,true,true,true});
    check(10, new int[]{1,2,3,3,5}, new boolean[]{true,true,true,false,true});
    check(10, new int[]{1,1,1,1,1}, new boolean[]{true,false,false,false,false});
  }

}