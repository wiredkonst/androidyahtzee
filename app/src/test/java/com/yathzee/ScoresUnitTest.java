package com.yathzee;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * unit tests for scores
 * runs on the host machine
 */
public class ScoresUnitTest {
  private Scores s;
  private List<Dice> dices; //calc score param
  public ScoresUnitTest(){
    s = new Scores();
    dices = Arrays.asList(new Dice(), new Dice(), new Dice(), new Dice(),new Dice());
  }
  private void setDices(int[] nums){
    for(int i =0; i <nums.length;i++){
      dices.get(i).num = nums[i];
    }
  }

  @Test
  public void calc_score() {
    Scores s = new Scores();
    //X's
    setDices(new int[]{1,1,3,3,2});
    assertEquals(2,s.calcScore(s.getIdx(s.Ones),dices));
    assertEquals(0,s.calcScore(s.getIdx(s.Fours),dices));
    setDices(new int[]{2,1,2,3,2});
    assertEquals(6,s.calcScore(s.getIdx(s.Twos),dices));
    //3/4-kind's
    setDices(new int[]{3,3,3,3,2});
    assertEquals(14,s.calcScore(s.getIdx(s.ThreeOfAKind),dices));
    assertEquals(14,s.calcScore(s.getIdx(s.FourOfAKind),dices));
    setDices(new int[]{4,3,4,3,2});
    assertEquals(0,s.calcScore(s.getIdx(s.ThreeOfAKind),dices));
    //fullhouse:3kind+2kind
    setDices(new int[]{6,6,3,3,3});
    assertEquals(25,s.calcScore(s.getIdx(s.FullHouse),dices));
    setDices(new int[]{3,3,3,3,3});
    assertEquals(0,s.calcScore(s.getIdx(s.FullHouse),dices));
    //smallstraight: 4 seq
    setDices(new int[]{1,2,3,4,3});
    assertEquals(30,s.calcScore(s.getIdx(s.SmallStraight),dices));
    setDices(new int[]{2,5,4,3,2});
    assertEquals(30,s.calcScore(s.getIdx(s.SmallStraight),dices));
    setDices(new int[]{1,6,3,6,3});
    assertEquals(0,s.calcScore(s.getIdx(s.SmallStraight),dices));
    //bigstraight: 5 seq
    setDices(new int[]{2,4,3,5,1});
    assertEquals(40,s.calcScore(s.getIdx(s.LargeStraight),dices));
    setDices(new int[]{2,4,6,5,1});
    assertEquals(0,s.calcScore(s.getIdx(s.LargeStraight),dices));
    //yahtzee: 5kind
    setDices(new int[]{1,1,1,1,1});
    assertEquals(50,s.calcScore(s.getIdx(s.Yahtzee),dices));
    setDices(new int[]{3,3,4,3,3});
    assertEquals(0,s.calcScore(s.getIdx(s.Yahtzee),dices));
    //chance: sum
    setDices(new int[]{5,4,4,3,3});
    assertEquals(19,s.calcScore(s.getIdx(s.Chance),dices));
  }
}