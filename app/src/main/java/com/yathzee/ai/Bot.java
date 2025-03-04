package com.yathzee.ai;

import com.yathzee.Dice;
import com.yathzee.Player;
import com.yathzee.Scores;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Bot extends Player
{
  private final Scores scores;
  private int[] scoresMax;


  private void setNums(List<Dice> ds,List<Integer> nums){
    if(nums.size() == 1)
      for(Dice d: ds)d.num=nums.get(0);
    else if(nums.size() == 5)//this is computational garbage, use itertorrs instead
      for(int i=0; i<nums.size();i++)ds.get(i).num=nums.get(i);
    else
      System.out.println("s");
  }
  private void setNums(List<Dice> ds,int n){
    for(Dice d: ds)d.num=n;
  }
  private void setNums(List<Dice> ds,int n1,int n2, int n3, int n4, int n5){
    Iterator<Dice> i = ds.iterator();
    i.next().num = n1;
    i.next().num = n2;
    i.next().num = n3;
    i.next().num = n4;
    i.next().num = n5;
  }
  public Bot(int scores){
    super("pc",scores);
    this.scores = new Scores();
    // cache abs max
    scoresMax = new int[this.scores.Names.length];
    List<Dice> ds = new ArrayList<>(5);
    for(int i=0;i<5;i++)ds.add(new Dice());
    int s = 0;
    for(;s<6;s++){setNums(ds,s+1);scoresMax[s] = this.scores.calcScore(s,ds);}
    s=6; setNums(ds,6);scoresMax[s] = this.scores.calcScore(s,ds);
    s=7; /*setNums(ds,6);*/scoresMax[s] = this.scores.calcScore(s,ds);
    s=8; setNums(ds,1,1,1,2,2);scoresMax[s] = this.scores.calcScore(s,ds);
    s=9; setNums(ds,1,2,3,4,5);scoresMax[s] = this.scores.calcScore(s,ds);
    s=10;/*setNums(ds,1,2,3,4,5);*/scoresMax[s] = this.scores.calcScore(s,ds);
    s=11;setNums(ds,1);scoresMax[s] = this.scores.calcScore(s,ds);
    s=12;setNums(ds,6);scoresMax[s] = this.scores.calcScore(s,ds);
  }
  private List<Integer> listMissingScore(){
    Set<String> psns = getScoreNames();
    List<Integer> ss = new ArrayList<>();
    for (int s=0;s< scores.Names.length;s++) if (!psns.contains(scores.Names[s])) ss.add(s);
    return ss;
  }

  private void keepDices(List<Dice> dices){
    //TODO: mark given dices as kept
  }
  //return -1: modifies dice state
  //returns>-1: scoreIdx tobe scored as
  public int decide(List<Dice> dices, int remRolls){
    //scores at t=0
    int[] ss = new int[this.scores.Names.length];
    for(int s=0;s<ss.length;s++)ss[s]=this.scores.calcScore(s,dices);

    //collect ready ones
    List<Integer> ok = new ArrayList<>();
    for(int s=0;s<ss.length;s++)if(ss[s]>0)ok.add(s);

    //choose max of those
    int takeS = -1;
    for(Integer s: ok)if(takeS == -1)takeS=s;else if(ss[takeS]<ss[s])takeS=s;

    //if improvable, reroll, otherwise take it
    if(remRolls == 0){
      if(takeS == -1){
        //just score the first uncredited
        return listMissingScore().get(0);
      }else return takeS;
    }else{
      if(takeS == -1){
        //TODO: manage dice
        return -1;
      }else {
        //if its max, dont reroll, otherwise reroll
        if(scoresMax[takeS] > ss[takeS]){
          //TODO: keep those ones

          return -1;
        }else return takeS;
      }
    }
  }

}
