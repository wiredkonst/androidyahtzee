package com.yathzee.pc;

import com.yathzee.Dice;
import com.yathzee.Scores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Decision {
  public List<Dice> ds;
  public int score;
  public int free,needed,neededTotal; //free: non taken, needed: amount missing,
  // neededTotal: max total amount of specific dice needed

  public void calcFree(){
    free=5;
    for(Dice d:ds)if(d.isTaken)free--;
  }
  /**
   * pick at most maxDice dice which have a number > 3
   * returns true if maxDice dice have been taken, otherwise false
   */
  private boolean pickMediumBest(int maxDice){
    int taken=0;
    for(Dice d: ds)if(!d.isTaken && d.num>3 && taken < maxDice){
      d.isTaken=true;
      taken++;
    }
    if(taken == maxDice)return true;
    return false;
  }
  /**
   * take the dice ds with same numbers
   * - if sum is relevant, choose the number where appearances * number is max,
   * - otherwise with max appearances
   * and take at most maxDice
   * doesnt consider numbers already taken
   * return true if maxDice amount has been taken, else false
   */
  private boolean pickBestSame(boolean sumRelevant, int maxDice){
    //get already taken numbers
    Set<Integer> took = new HashSet<>();
    for(Dice d: ds)if(d.isTaken && !took.contains(d.num))took.add(d.num);
    //get appearances regarding if already number taken
    Map<Integer,Integer> as = new HashMap<>(6);
    for(Dice d: ds){
      if(took.contains(d.num))continue;
      if(as.containsKey(d.num)){
        Integer n = as.get(d.num);
        as.put(d.num,n+1);
      }else as.put(d.num,1);
    }
    //get best dice nr regarding sum or appearance amount
    Map.Entry<Integer,Integer> best = null;int prevBest = 0;
    for(Map.Entry<Integer,Integer> a:as.entrySet()) {
      if(sumRelevant){
        int nxtBest = a.getKey()*a.getValue();
        if(prevBest < nxtBest){
          prevBest = nxtBest;
          best = a;
        }
      }else if(best == null || best.getValue() < a.getValue())best = a;
    }
    //mark the choosen best dice if existing, and only as many as maxDice
    if(best == null)return false;
    int taken=0;
    for(Dice d: ds)if(d.num == best.getKey().intValue() &&
        (maxDice == -1 || taken<maxDice)){
      d.isTaken = true;
      taken++;
    }
    if(taken == maxDice)return true;
    return false;
  }
  public Decision(int score,List<Dice> dices){
    this.score = score;
    ds = new ArrayList<>(5);
    for(int i=0;i<5;i++){
      Dice nd = dices.get(i).clone();
      nd.isTaken = false; nd.markedAsReroll=false;
      ds.add(nd);
    }
    if(score< 6){
      ds.stream().filter(d -> d.num == score+1).forEach(d -> d.isTaken = true);
      neededTotal = 5;
      calcFree();needed = (neededTotal-2) - (5-free); // heuristic: 3 needed
    }else if(score<8){
      int app=(score==6)?3:4;
      if(pickBestSame(true,app)){
        pickMediumBest(6);
      }
      neededTotal = app;
      calcFree();needed = neededTotal - (5-free);
    }else if(score==8){
      if(pickBestSame(false,3)) pickBestSame(false,2);
      neededTotal = 5;
      calcFree();needed = free; //all needed
    }else if(score<11){
      Map<Integer,Integer> as = Scores.getApps(ds);
      int seqSize = (score==9)?4:5;
      int[][] seqs = (seqSize==4)?Scores.fourSeqAllPoss:Scores.fiveSeqAllPoss;
      //search for best reachable sequence
      int[] bestSeq = {};int bestSeqDiff=5;
      for(int s=0; s<seqs.length;s++){
        int[] seq = seqs[s];int diff=0;
        for(int n:seq)if(!as.containsKey(n))diff++;
        if(bestSeqDiff > diff){
          bestSeq=seq;
          bestSeqDiff=diff;
        }
      }
      //select distinct
      for(int n: bestSeq) for(Dice d:ds)if(d.num == n){d.isTaken=true; break;}
      neededTotal = seqSize;
      calcFree();needed = neededTotal - (5-free);//depends on size of sequence
    }else if(score==11){
      pickBestSame(false,5);
      neededTotal = 5;
      calcFree();needed = free; //all needed
    }else{
      pickMediumBest(3);
      neededTotal = 5;
      calcFree();needed = neededTotal - (5-free);
    }
  }
  public String toString(){
    String s=" s:"+score+" [";
    for (Dice d: ds)s+="("+d.num+":"+d.isTaken+"),";
    s=s.substring(0,s.length()-1)+ "] n:"+needed;
    return s;
  }
}

