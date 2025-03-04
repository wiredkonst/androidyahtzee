package com.yathzee.mediafusion;

import java.util.ArrayList;
import java.util.List;

public abstract class Cmd {
  public List<Slot> filled;//save already filled slots/actions
  protected boolean complete;//save if last slot completed the cmd
  public Cmd(){
    this.filled = new ArrayList<>();
  }
  // called if the cmd has become active
  public void init(){
    this.filled.clear();
    this.complete = false;
  }
  // called from ui thread, tries to complete the cmd,
  // returns true if it has been successful and the default behavior of the action needs to be canceled, false otherwise
  public abstract boolean tryFill(Slot slot);
  // returns true if given slot is the first action
  public abstract boolean isStartSlot(Slot slot);
  // returns true if cmd has been completed by last action
  public boolean isComplete(){return complete;}
  // run from ui thread for restoring the state before this cmd has been triggered
  public void cancel(){}
}
