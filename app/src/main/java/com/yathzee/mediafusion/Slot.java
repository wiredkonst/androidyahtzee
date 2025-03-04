package com.yathzee.mediafusion;

public class Slot {
  public enum Type{Voice,Gesture,Touch};
  public final Type type;
  public final int actionNr;
  public final Object actionParam;
  public Slot(Type type, int actionNr,Object actionParam){
    this.type=type;this.actionNr=actionNr;this.actionParam=actionParam;
  }
}
