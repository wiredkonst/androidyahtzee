package com.yathzee.pc;

import com.yathzee.Dice;
import com.yathzee.Player;
import com.yathzee.Scores;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PcPlayer extends Player
{
  private final int maxF,maxN,maxR; //max value for free, needed, rounds probabilities arg
  private final double[] probs; //systematic stored reachability probabilities
  /**
   * get reachability probability idx
   * @param r rounds remaining in [1,maxR]
   * @param f free dice in [1,maxF]
   * @param n needed correct dice of f [1,maxN]
   * @return [0,maxF*maxN*maxR)
   */
  private int getProp(int r,int f,int n){
    int ri=r-1;int ni=n-1;int fi=f-1;
    return ri*maxF*maxN+ni*maxF+fi;
  }
  private final Scores scores;
  private final int[] scoresMax; //each score idx mapped to its maximal possible score
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
  public PcPlayer(int scores){
    super("pc",scores);
    this.scores = new Scores();
    // 1. cache max scores
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

    // 2. cache probabilities
    maxF=4;maxN=4;maxR=2;s=maxF*maxN*maxR;
    int f=1,n=1,r=1;
    final double np = (double)5/6;
    probs = new double[s];
    for(int i=0;i<s;i++){
      final int t = r*f;
      probs[getProp(r,f,n)] = MathUtil.cumBinDist(t,np,1+t-n);
      if(f==maxF) {f=1;
        if(n==maxN){n=1;r++;}else n++;
      } else f++;
    }
  }

  public Decision lastDecision;
  private int acceptDecision(Decision decision,List<Dice> dices){
    lastDecision = decision;
    System.out.println("accepted: "+decision.toString());
    for (int d=0;d<5;d++){
      dices.get(d).isTaken = decision.ds.get(d).isTaken;
    }
    return decision.score;
  }
  /**
   * decides on the next action given the following state:
   * - current dice nos dices
   * - remaining rolls rollsLeft
   * - current scores
   * @return -1 if a reroll should be triggered, dice have been modified; else the score idx to be selected
   */
  public int decide(List<Dice> dices,int rollsLeft){
    //remaining scores:
    List<Decision> decisions = new ArrayList<>();
    for(int s=0; s<scores.Names.length;s++){
      if(getScoreNames().contains(scores.Names[s]))continue;
      decisions.add(new Decision(s,dices));
    }
    //completed and no more rolls:
    for(Decision d: decisions)if(d.needed<1 && d.free==0) return acceptDecision(d,dices);
    //non rolls:
    if(rollsLeft < 1){
      //accept first completed
      for(Decision d: decisions)if(d.needed<1)return acceptDecision(d,dices);
      //accept best score regarding relative maxscore
      Decision bestDecision = decisions.get(0);
      double scoreMax = 0;
      for(Decision d: decisions){
        double r = (double)scores.calcScore(d.score,d.ds)/scoresMax[d.score];
        if(r > scoreMax){
          bestDecision = d;
          scoreMax = r;
        }
      }
      return acceptDecision(bestDecision,dices);
    }else{
      //rolls left, completed with free or uncompleted with needed
      Decision bestDecision = decisions.get(0); double bestProb = 0;
      System.out.println("all r decisions:");
      for (Decision decision: decisions){
        double p = (double)(5-decision.free)/decision.neededTotal;
        //scale with max score value
        if(decision.score > 5 && decision.score < 8){
          int maxValue = 0;for(Dice d: decision.ds)maxValue+=d.num;
          p*=maxValue; //TODO: actually not quite the max value...
        }else p*=scoresMax[(decision.score < 6)?5:decision.score];
        if (decision.needed < 1){
          //TODO: if a decision completes a score and if its value depends on other free dice, some calculations could be done in this place...
        }else if(decision.free < 5 && decision.needed <5){
          p*=probs[getProp(rollsLeft,decision.free,decision.needed)];
        }else {
          //basically unreachable
          p*=0;
        }
        System.out.println(decision.toString()+"p:"+p);
        if(p>bestProb){
          bestDecision = decision;
          bestProb = p;
        }
      }
      acceptDecision(bestDecision,dices);
    }
    return -1;
  }

}
