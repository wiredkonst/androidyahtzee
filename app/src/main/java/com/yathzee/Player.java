package com.yathzee;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//handled as OOP class
public class Player {
  private final String name;
  private Map<String,Integer> scores;
  public Player(String name,int scoreSize){
    this.name = name;
    scores = new HashMap<>(scoreSize);
  }
  public void addScore(String scoreName,Integer score){
    scores.put(scoreName,score);
  }
  public Set<Map.Entry<String,Integer>> getScores(){return scores.entrySet();}
  public Set<String> getScoreNames(){return scores.keySet();}
  public Integer getScore(String score){
    if(!scores.containsKey(score))return -1;
    return scores.get(score);
  }
  public Integer getTotalScore(){
    int sum = 0;
    for(int score: scores.values())sum+=score;
    return sum;
  }
  public String getName(){
    return name;
  }
}
