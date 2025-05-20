package com.yathzee;
//handled like a c struct
public class Dice {
  public Integer num = 5; //display nr
  public boolean isTaken = false; //true if taken
  public boolean markedAsReroll = false; //true if the die is marked as to be rerolled

  public Dice clone(){
    Dice d = new Dice();
    d.num = num;
    d.isTaken = isTaken;
    d.markedAsReroll = markedAsReroll;
    return d;
  }
}
