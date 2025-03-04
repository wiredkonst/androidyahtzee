package com.yathzee;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.utilities.Score;
import com.yathzee.databinding.FragmentIngameBinding;
import com.yathzee.mediafusion.Slot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Fragment_InGame extends Fragment {


  //-- static
  private FragmentIngameBinding binding;
  private List<TableRow> trs; //score idx in names -> tablerow mapping
  private Scores scores; //util fnct for score stuff
  //- touch action numbers for multimodal fusion: (only if needed)
  public static final int btnDiceAction = 0; //roll dice btn, no param
  public static final int btnDiceNrAction = 1; //dice imgbtn, param is Integer of dice pos 0-based
  public static final int btnSetScoreAction = 2; //score btn, param is Integer of scorename idx
  //-- state
  private List<Player> players;
  private List<Dice> dices; //dices
  private int player; //player idx currently playing
  private int rolls; //rolls done already
  private int score_idx; //score to be credited
  //-- util
  private static final int max_rolls = 3;
  private Random myRandom;


  private TextView getScoreTVOfRow(TableRow tr){
    int c = tr.getChildCount();
    return (TextView)tr.getChildAt(c-1);
  }
  private ImageButton getDiceBtn(int idx){
    switch(idx){
      case 0: return binding.imgbtnDice1;
      case 1: return binding.imgbtnDice2;
      case 2: return binding.imgbtnDice3;
      case 3: return binding.imgbtnDice4;
      case 4: return binding.imgbtnDice5;
    }
    return null;
  }
  private void updateTableSum(int val,boolean valIsRelative){
    TextView tv = getScoreTVOfRow(binding.trSum);
    int _val = 0;
    if(valIsRelative){
      _val = Integer.parseInt(tv.getText().toString());
    }
    _val+=val;
    tv.setText(""+_val);
  }
  private void updateTable(){
    //reset table data & set all as not defined:
    for(TableRow tr: trs){
      getScoreTVOfRow(tr).setText("-");
    }
    //populate
    Player p = players.get(player);
    Set<Map.Entry<String, Integer>> scores = p.getScores();
    for(Map.Entry<String,Integer> score: scores){
      TableRow tr = trs.get(this.scores.getIdx(score.getKey()));
      getScoreTVOfRow(tr).setText(score.getValue().toString());
    }
    updateTableSum(p.getTotalScore(),false);
  }
  private void updateDiceBtn(){
    binding.btnRoll.setEnabled(true);
    String displayText;
    if(score_idx != -1){
      displayText = getString(R.string.btn_selected_txt,scores.Names[score_idx]);
    }else {
      if(max_rolls-rolls==0){
        displayText = getString(R.string.btn_select_txt);
        binding.btnRoll.setEnabled(false);
      }else {
        displayText = getString(R.string.btn_roll_txt, max_rolls - rolls);
      }
    }
    binding.btnRoll.setText(displayText);
  }
  private void updateDices(List<Integer> updatedIdxs){
    for(Integer idx: updatedIdxs){
      Dice dice = dices.get(idx);
      ImageButton btn = getDiceBtn(idx);
      int id = 42;
      switch(dice.num){
        case 1: id = R.drawable.dice_six_faces_one;break;
        case 2: id = R.drawable.dice_six_faces_two;break;
        case 3: id = R.drawable.dice_six_faces_three;break;
        case 4: id = R.drawable.dice_six_faces_four;break;
        case 5: id = R.drawable.dice_six_faces_five;break;
        case 6: id = R.drawable.dice_six_faces_six;break;
      }
      btn.setImageResource(id);
      if(dice.isTaken){
        btn.getDrawable().setColorFilter(ContextCompat.getColor(getContext(),R.color.dice_taken_tint), PorterDuff.Mode.SRC_ATOP);
      }else{
        btn.getDrawable().setColorFilter(null);
      }
      btn.invalidate();
    }
  }

  public List<Dice> getDices(){
    return dices;
  }
  public List<Player> getPlayers(){return players;}
  public int getCurrentPlayer(){return player;}
  public void restart(){
    NavHostFragment.findNavController(Fragment_InGame.this)
        .navigate(R.id.action_ingame_to_mainmenu);
  }
  private void endGame(){
    Bundle b = new Bundle();
    Player p1 = players.get(0);
    Player p2 = players.get(1);
    b.putString("player1_name", p1.getName());
    b.putString("player2_name", p2.getName());
    b.putInt("player1_score", p1.getTotalScore());
    b.putInt("player2_score", p2.getTotalScore());

    NavHostFragment.findNavController(Fragment_InGame.this)
        .navigate(R.id.action_ingame_to_fragment_Results,b);
  }

  public boolean isInLastTurn(){
    Player nextPlayer = players.get((player+1)%players.size());
    return  nextPlayer.getScores().size() == scores.Names.length;
  }
  public boolean isInScoringPhase(){
    return rolls == max_rolls;
  }
  public int getRemainingRolls(){return max_rolls - rolls;}
  private void switchTurn(boolean starting){
    //switch to next player
    if(!starting){
      player = (player + 1) % players.size();
    }
    Player p = players.get(player);
    //end the game if the next player already has every score
    if(p.getScores().size() == scores.Names.length){
      endGame();
      return; //?
    }
    String header = getString(R.string.current_player,p.getName());
    if(p.getScores().size() == scores.Names.length-1)
      header+="(last turn!)";
    binding.textviewPlayer.setText(header);
    //reset dices
    if(starting)rolls = 1;else rolls=0;
    List<Integer> rs = new ArrayList<>(5);
    for(int i=0;i<dices.size();i++){
      Dice d = dices.get(i);d.isTaken = false;
      rs.add(i);
    }
    score_idx = -1;

    //already roll it once, but not if just given
    if(!starting)
      diceBtnAction();
    updateDices(rs);
    updateDiceBtn();
    //update table and score
    updateTable();

  }
  // reset whole dice state & update
  private void resetDice(){
    List<Integer> r = new ArrayList<>(dices.size());
    for(int i=0;i<dices.size();i++){
      Dice d = dices.get(i);
      d.isTaken = false;
      d.markedAsReroll = false;
      r.add(i);
    }
    updateDices(r);
  }
  // -- reroll specific related fcts --
  // - usage: 1. setRerollState(); 2. toggleReroll()*;
  //  3.a: resetRerollState(); or
  //  3.b: diceBtnAction();
  // - updates textview over dices, roll btn + dices
  //go into reroll-setting ui display state, return if successful:
  public boolean setRerollState(){
    if(isInScoringPhase())return false;
    resetDice();
    binding.textviewDices.setText(getString(R.string.textview_dice_reroll));
    binding.btnRoll.setText(getString(R.string.btn_reroll_unselected_txt));
    binding.btnRoll.setEnabled(false);
    return true;
  }
  //go back to default state: (no, keeping state wont be restored)
  public void resetRerollState(){
    //reset dice:
    resetDice();
    binding.textviewDices.setText(getString(R.string.textview_dice_default));
    updateDiceBtn();
  }
  //toggle to-be-rerolled state of dice w idx, if idx < 0, reset all dice reroll state
  public void toggleReroll(int idx){
    if(isInScoringPhase())return;
    List<Integer> toggle = new ArrayList<>();
    if(idx < 0)
      for(int i=0;i<dices.size();i++)
      {if(dices.get(i).markedAsReroll)toggle.add(i);}
    else if(idx < 5)toggle.add(idx);
    for(Integer i: toggle){
      ImageButton btn = getDiceBtn(i);
      Dice dice = dices.get(i);
      dice.markedAsReroll = !dice.markedAsReroll;
      if(dice.markedAsReroll){
        btn.getDrawable().setColorFilter(ContextCompat.getColor(getContext(),R.color.dice_reroll_tint), PorterDuff.Mode.SRC_ATOP);
      }else{
        btn.getDrawable().setColorFilter(null);
      }
      btn.invalidate();
    }
    //update dice btn
    String display = getString(R.string.btn_reroll_unselected_txt);
    boolean enable = false;
    for(Dice d:dices)if(d.markedAsReroll){
      display = getString(R.string.btn_reroll_selected_txt);
      enable = true;
      break;
    }
    binding.btnRoll.setEnabled(enable);
    binding.btnRoll.setText(display);
  }
  // -- end

  //the action for:
  // - rolling dice
  // - rerolling dice (invers marking logic, ignores kept ones)
  // - scoring if theres a selected score
  public void diceBtnAction(){
    if(score_idx == -1) {
      if(isInScoringPhase())return;
      List<Integer> free_idxs = new ArrayList<>();
      //check if one is marked as reroll (and collect those idx)
      boolean reroll = false;
      for(int i = 0; i < dices.size(); i++)
        if(dices.get(i).markedAsReroll) {
          free_idxs.add(i);
          reroll = true;
        }
      //collect other idxs otherwise
      if(!reroll)for(int i=0;i<dices.size();i++)if(!dices.get(i).isTaken)free_idxs.add(i);
      //roll em:
      for (Integer idx : free_idxs) {
        int n = myRandom.nextInt(6) + 1;
        dices.get(idx).num = n;
      }
      rolls++;
      //reset whole dice state for reroll actions:
      if(reroll){
        free_idxs.clear();
        for(int i=0;i<dices.size();i++){
          free_idxs.add(i);
          Dice d = dices.get(i);
          d.markedAsReroll = false;
          d.isTaken = false;
        }
      }
      //update ui:
      updateDices(free_idxs);
      updateDiceBtn();
      binding.textviewDices.setText(getString(R.string.textview_dice_default));
    }else{

      Player p = players.get(player);
      p.addScore(scores.Names[score_idx],scores.calcScore(score_idx,dices));
      switchTurn(false);
    }
  }

  //one single dice btn action
  private void diceBtnAction(int nr){
    if(((MainActivity)getActivity()).getCmdController().tryFillSlot(Slot.Type.Touch,btnDiceNrAction,nr))return;
    toggleTaken(nr);
  }
  private boolean scoringPreCheck(int score_idx){
    return score_idx < 0 || score_idx > scores.Names.length-1 || rolls==0 || players.get(player).getScore(scores.Names[score_idx])!=-1;
  }
  //update a score for scoring, returns true if the update was successful, otherwise false
  public boolean setScore(int score_idx) {
    //dont set already set ones or if not rolled at least once + sanitize
    if(scoringPreCheck(score_idx))return false;
    //reset old, always non set before
    if(this.score_idx>-1){
      TextView tv = getScoreTVOfRow(trs.get(this.score_idx));
      updateTableSum(-Integer.parseInt(tv.getText().toString()),true);
      tv.setText("-");
    }
    //toggle the selection or show the score:
    if(score_idx == this.score_idx){
      this.score_idx = -1;
    } else{
      this.score_idx = score_idx;
      TextView tv = getScoreTVOfRow(trs.get(score_idx));
      int s = scores.calcScore(score_idx, dices);
      updateTableSum(s,true);
      tv.setText(""+s);

    }
    //consider ending turn:
    updateDiceBtn();
    return true;
  }

  public int getScore(int score_idx){
    //dont set already set ones or if not rolled at least once
    if(scoringPreCheck(score_idx))return -1;
    return scores.calcScore(score_idx, dices);
  }

  // complex use cases
  public void toggleTaken(List<Integer> nrs, boolean takeIt,boolean nrIsOrdinal){
    for(Integer nr: nrs) {
      //check inputs:
      if (nr < 1 || nr > 6 || nr > 5 && nrIsOrdinal)
        return;

      if (nrIsOrdinal) {
        //isTaken | takeIt
        //1 0 "release it"
        //1 1 "already took it"
        //0 1 "take it" ..
        if (dices.get(nr - 1).isTaken ^ takeIt) {
          toggleTaken(nr - 1);
        }
      } else {
        for (int d = 0; d < dices.size(); d++) {
          Dice dice = dices.get(d);
          if (dice.num.equals(nr) && dice.isTaken != takeIt)
            toggleTaken(d);
        }
      }
    }
  }
  // by dice idx
  public void toggleTaken(int idx){
    if(rolls==0)return;
    Dice d = dices.get(idx);
    d.isTaken = !d.isTaken;
    updateDices(Arrays.asList(idx));
  }
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  ) {

    binding = FragmentIngameBinding.inflate(inflater, container, false);
    dices = new ArrayList<>(5);
    Dice[] initarr = {new Dice(),new Dice(),new Dice(),new Dice(),new Dice()};
    dices.addAll(0, Arrays.asList(initarr));
    players = new ArrayList<>(2);
    scores = new Scores();
    player = 0; //first reg player always starts
    myRandom = new Random();
    return binding.getRoot();
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TableRow[] _trs = {binding.trOnes,binding.trTwos,binding.trThrees,binding.trFours,binding.trFives,binding.trSixes,
          binding.trThreeKind,binding.trFourKind,binding.trFullHouse,binding.trSmallStraight,binding.trBigStraight,binding.trYathzee,binding.trChance};
    trs = Arrays.asList(_trs);

    Bundle args = getArguments();
    String p1 = args.getString("player1","Player 1");
    String p2 = args.getString("player2","Player 2");
    boolean p2isPc = args.getBoolean("player2IsBot");
    dices.get(0).num = args.getInt("d1",5);
    dices.get(1).num = args.getInt("d2",5);
    dices.get(2).num = args.getInt("d3",5);
    dices.get(3).num = args.getInt("d4",5);
    dices.get(4).num = args.getInt("d5",5);

    Player _p1 = new Player(p1,scores.Names.length);
    Player _p2 = new Player(p2,scores.Names.length);
    //for(int i=1;i< scores.Names.length;i++){_p1.addScore(scores.Names[i],42);_p2.addScore(scores.Names[i],42);}
    //TODO: implement a bot
    players.add(_p1);
    players.add(_p2);
    switchTurn(true);

    binding.btnMenu.setOnClickListener(v->restart());
    binding.btnRoll.setOnClickListener(v->{
      if(((MainActivity)getActivity()).getCmdController().tryFillSlot(Slot.Type.Touch,btnDiceAction,null))return;
      diceBtnAction();
    });
    for(int i=0;i<dices.size();i++){
      int _i = i;
      getDiceBtn(i).setOnClickListener(v->diceBtnAction(_i));
    }
    for(int i=0;i<trs.size();i++){
      TableRow tr = trs.get(i);
      int _i = i;//effective final for lambda expr
      tr.setOnClickListener(v->{
        if(((MainActivity)getActivity()).getCmdController().tryFillSlot(Slot.Type.Touch,btnSetScoreAction,_i))return;
        setScore(_i);});
      tr.setOnHoverListener((v,event)->{
        switch(event.getAction()){
          case MotionEvent.ACTION_HOVER_ENTER:
            v.setPointerIcon(PointerIcon.getSystemIcon(getActivity(),PointerIcon.TYPE_HAND));
            break;
          default://MotionEvent.ACTION_HOVER_EXIT:
            v.setPointerIcon(PointerIcon.getSystemIcon(getActivity(),PointerIcon.TYPE_ARROW));
            break;
        }
        return false;
      });
    }
  }



  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

}