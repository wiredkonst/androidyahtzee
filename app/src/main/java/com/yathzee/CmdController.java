package com.yathzee;

import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.yathzee.mediafusion.Cmd;
import com.yathzee.mediafusion.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CmdController {

  private final MainActivity activity;

  //the current cmd will be accessed by the timer thread and the ui thread,
  private Cmd currCmd;
  private List<Cmd> cmds;
  private static final long cmdTimeout = 10000; //in ms, 10s due to vui
  private Timer timeoutTimer;
  private TimerTask timerTask;

  public CmdController(MainActivity activity){
    this.activity = activity;
    cmds = new ArrayList<>();
    timeoutTimer = new Timer();
    currCmd = null;
    registerCmds();
  }
  private void registerCmds(){
    //reroll cmd
    cmds.add(new Cmd(){
      //true if success otherwise false
      private boolean setReroll(Fragment_InGame fig, int idx){
        if(filled.isEmpty() || complete)return false;
        fig.toggleReroll(idx);
        return true;
      }
      // true if success otherwise false
      private boolean reroll(Fragment_InGame fig){
        if(filled.size()<2 || complete)return false;
        fig.diceBtnAction();
        complete=true;
        return true;
      }
      @Override
      public boolean tryFill(Slot slot){
        Fragment f = activity.getCurrentFragment();
        if(!(f instanceof Fragment_InGame))return false;
        Fragment_InGame fig = (Fragment_InGame)f;
        if(fig.isInScoringPhase())return false;
        switch (slot.type){
          case Voice:
            switch (slot.actionNr){
              case 23: //start -> only as first slot accepted
                if(filled.isEmpty())fig.setRerollState();else return false;
                break;
              case 24: //mark as reroll -> only if start exists & non completed
                if(!setReroll(fig,((Integer)slot.actionParam)-1))return false;
                break;
              case 13: //end -> only accept this cmd after start and at least one marked one
                if(!reroll(fig))return false;
                break;
              default: return true; //other actions which are interceptable shouldnt work in this cmd
            }break;
          case Gesture:
            switch (slot.actionNr){
              case 1: //end -> see above
                if(!reroll(fig)) return false;
                break;
              case 2: case 3: case 4: case 5: case 6: //mark as reroll -> see above
                if(!setReroll(fig,slot.actionNr-2)) return false;
                break;
              default: return true; //theres no 6.
            }break;
          case Touch:
            switch (slot.actionNr){
              case Fragment_InGame.btnDiceAction:
                if(!reroll(fig)) return false;
                break;
              case Fragment_InGame.btnDiceNrAction: //mark as reroll -> see above
                if(!setReroll(fig,(Integer)slot.actionParam))return false;
                break;
              default: return true; //see comment above
            }break;
        }
        filled.add(slot);
        return true;
      }
      @Override
      public boolean isStartSlot(Slot slot){
        return slot.type == Slot.Type.Voice && slot.actionNr == 23;
      }
      @Override
      public void cancel(){
        Fragment f = activity.getCurrentFragment();
        if(!(f instanceof Fragment_InGame) || filled.isEmpty())return;
        Fragment_InGame fig = (Fragment_InGame)f;
        fig.resetRerollState();
      }
    });
    //scoring cmd
    cmds.add(new Cmd() {
      //do the scoring, return true if it was
      private boolean tryScore(Fragment_InGame fig, int scoreIdx){
        if(!filled.isEmpty() && !complete){
          if(!fig.setScore(scoreIdx))return false;
          //NOTE: only working if all cmds which potentially set the score and do not actually score are captured by this cmd
          fig.diceBtnAction();complete=true;
          return true;
        }else return false;
      }
      @Override
      public boolean tryFill(Slot slot) {
        Fragment f = activity.getCurrentFragment();
        if(!(f instanceof Fragment_InGame))return false;
        Fragment_InGame fig = (Fragment_InGame)f;
        //if(!fig.isInScoringPhase())return false;
        switch (slot.type){
          case Voice:
            switch (slot.actionNr){
              case 25: //start -> only as first slot accepted
                if(!filled.isEmpty()) return false;
                break;
              case 26: //end -> only after start accepted
                if(!tryScore(fig,(Integer)slot.actionParam))return false;
                break;
              default: return true; //see comment above + this cmd is actually even less restrictive,
              // e.g. "score as X" intent could count as filling the second slot
            }break;
          case Gesture:
            switch (slot.actionNr){
              case 2: case 3: case 4: case 5: case 6: case 7: //end -> see above
                if(!tryScore(fig,slot.actionNr-2))return false;
                break;
              default: return false;
            }break;
          case Touch:
            switch (slot.actionNr){ //end -> see above
              case Fragment_InGame.btnSetScoreAction:
                if(!tryScore(fig,(Integer) slot.actionParam))return false;
                break;
              default: return true; //see comment above +
              // "false" wouldnt break anything (toggling is irreleant to scring, rollbtn is disabled)
            }break;
        }
        filled.add(slot);
        return true;
      }
      @Override
      public boolean isStartSlot(Slot slot) {
        return slot.type == Slot.Type.Voice && slot.actionNr == 25;
      }
    });
    // close cmd:
    cmds.add(new Cmd() {
      @Override
      public boolean tryFill(Slot slot) {
        switch (slot.type){
          case Voice:
            switch (slot.actionNr){
              case 27: //start -> only as first slot accepted
                if(!filled.isEmpty()) return false;
                break;
              case 28: //end -> only after start accepted
                if(filled.isEmpty())return false;
                complete = true;
                activity.finish();
                //assuming nanohttpd waits for threadcompletion before closing
                // (seems to be the case when having a response for this intend,
                // not 100% sure but dont want to waste my time again)
                break;
              default: return false; //game ctx independent
            }break;
          default: return false; //game ctx independent
        }
        filled.add(slot);
        return true;
      }

      @Override
      public boolean isStartSlot(Slot slot) {
        return slot.type == Slot.Type.Voice && slot.actionNr == 27;
      }
    });
  }
  private void stopTimer(){
    if(timerTask != null)timerTask.cancel();
    if(timeoutTimer != null)timeoutTimer.cancel();
  }
  /**
   * called from the ui thread when an action which can fill a slot has been triggered
   * @param type its action type
   * @param actionNumber some nr for identifying the triggered action
   * @param actionParam obj containing action parameters
   * @return true if the slot completion was successful, otherwise false
   */
  public boolean tryFillSlot(Slot.Type type, int actionNumber, Object actionParam){
    Slot slot = new Slot(type,actionNumber,actionParam);
    boolean res = false;
    if(currCmd == null) {
      for (Cmd c : cmds)
        if (c.isStartSlot(slot)) {
          currCmd = c;
          currCmd.init();
          break;
        }
    }
    if(currCmd != null) {
      res = currCmd.tryFill(slot);
      //reset timer if slot filled:
      if (res) {
        stopTimer();
        timeoutTimer = new Timer(); //no rescheduling possible...
        timerTask = new TimerTask() {
          @Override
          public void run() {
            activity.runOnUiThread(() -> {
              currCmd.cancel();
              Toast.makeText(activity, "command canceled", Toast.LENGTH_LONG).show();
              currCmd = null;
            });
          }
        };
        timeoutTimer.schedule(timerTask, cmdTimeout);
      }
      if (currCmd.isComplete()) {
        stopTimer();
        currCmd = null;
      }
    }


    return res;
  }

}
