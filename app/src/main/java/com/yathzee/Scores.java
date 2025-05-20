package com.yathzee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scores {

  public final String Ones = "Ones";//0
  public final String Twos = "Twos";//1
  public final String Threes = "Threes";//2
  public final String Fours = "Fours";//3
  public final String Fives = "Fives";//4
  public final String Sixes = "Sixes";//5
  public final String ThreeOfAKind = "Three of a kind";//6 threeKind
  public final String FourOfAKind = "Four of a kind";//7 fourKind
  public final String FullHouse = "Full house";//8
  public final String SmallStraight = "Small straight";//9
  public final String LargeStraight = "Large straight";//10
  public final String Yahtzee = "Yahtzee";//11
  public final String Chance = "Chance";//12
  public final String[] Names = {Ones,Twos,Threes,Fours,Fives,Sixes,ThreeOfAKind,FourOfAKind,FullHouse,SmallStraight, LargeStraight,Yahtzee,Chance};
  public int getIdx(String name){
    for(int i=0;i<Names.length;i++){
      if(name.equals(Names[i]))return i;
    }
    return -1;
  }
  public static Map<Integer,Integer> getApps(List<Dice> dices){ //APPearance S
    Map<Integer,Integer> as = new HashMap<>(6);
    for(Dice d: dices){
      if(as.containsKey(d.num)){
        Integer n = as.get(d.num);
        as.put(d.num,n+1);
      }else{
        as.put(d.num,1);
      }
    }
    return as;
  }
  public static final int[][] fourSeqAllPoss = {{1,2,3,4},{2,3,4,5},{3,4,5,6}};
  public static final int[][] fiveSeqAllPoss = {{1,2,3,4,5},{2,3,4,5,6}};//kinda dumb cuz its always just 5 dices but why not?
  private boolean seqCheck(int[][] seqs, Set<Integer> nrs){
    for(int[] seq: seqs){
      boolean ex = true;
      for(int nr: seq)
        if(!nrs.contains(nr)){
          ex = false;break;
        }
      if(ex)return true;
    }
    return false;
  }

  public List<Integer> getScoreAffDice(int idx, List<Dice> dices){
    List<Integer> ads = new ArrayList<>();
    if(idx<6){
      for(int d=0;d<dices.size();d++)if(dices.get(d).num == idx+1)ads.add(d);
      return ads;
    }else if(idx<8){
      int app=(idx==6)?3:4;
      boolean exists = false;
      for(Integer a: getApps(dices).values())if(a>=app)
      {  exists = true; break;}
      if(!exists)return ads;
    }else if(idx == 8){
      if(calcScore(idx,dices)==0)return ads;
    }
    //TODO: continue
    //default: all dices
    for(int i=0;i<dices.size();i++)ads.add(i);
    return ads;
  }
  public int calcScore(int idx, List<Dice> dices){
    if(idx < 0 || idx > Names.length-1){
      return -6969;
    }
    //TODO: performance improvement for Apps
    if(idx < 6){
      // ones,..sixes
      int nr = idx+1;int app=0;
      for(Dice d: dices){
        if(d.num==nr)app++;
      }
      return app*nr;
    }else if(idx < 8){
      // three,four -kind
      int app=(idx==6)?3:4;
      Map<Integer,Integer> as = getApps(dices);
      int maxNr = 0;
      for(Map.Entry<Integer,Integer> a:as.entrySet()){
        if(a.getValue() >= app && maxNr < a.getKey())maxNr = a.getKey();
      }
      if(maxNr != 0) {int s = 0;for (Dice d : dices) s += d.num;return s;}
    }else if(idx == 8){
      // full house
      Map<Integer,Integer> as = getApps(dices);
      int threeNr=0,twoNr=0;
      for(Map.Entry<Integer,Integer> a:as.entrySet()){
        if(a.getValue() == 3)threeNr = a.getKey();
        if(a.getValue() == 2)twoNr = a.getKey();
      }
      if(threeNr == 0 || twoNr == 0 || threeNr == twoNr)return 0;
      return 25;
    }else if(idx < 11){
      // small,big straight
      if(seqCheck(((idx==9)?fourSeqAllPoss:fiveSeqAllPoss),getApps(dices).keySet()))return ((idx==9)?30:40);
    }else if(idx == 11){
      // yathzee
      if(getApps(dices).entrySet().size() == 1)return 50;
    }else{
      // chance
      int s = 0;for(Dice d: dices)s+=d.num;return s;
    }
    return 0;
  }
}
