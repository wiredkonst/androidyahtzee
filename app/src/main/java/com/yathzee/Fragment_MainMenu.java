package com.yathzee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.yathzee.databinding.FragmentMainmenuBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Fragment_MainMenu extends Fragment {

  private FragmentMainmenuBinding binding;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  ) {

    binding = FragmentMainmenuBinding.inflate(inflater, container, false);
    return binding.getRoot();

  }

  public void setNamePlayer1(String name){
    binding.txtinputPlayer1.setText(name);
  }
  public void setNamePlayer2(String name){
    binding.txtinputPlayer2.setText(name);
  }

  public String getNamePlayer1(){String n = binding.txtinputPlayer1.getText().toString(); if(n.isEmpty()) return "Player1"; else return n;}
  public String getNamePlayer2(){String n = binding.txtinputPlayer2.getText().toString(); if(n.isEmpty()) return "Player2"; else return n;}
  //workaround for fragmentswitch for intenthandler: already compute dices
  private Random myRandom = new Random();
  private List<Dice> initDices;
  public List<Dice> setAndGetDices(){
    initDices = new ArrayList<>(5);
    for (int i=0;i<5;i++)initDices.add(new Dice());
    for (Dice d: initDices) d.num = myRandom.nextInt(6) + 1;
    return initDices;
  }
  public void start(){
    Bundle bundle = new Bundle();
    String player1 = getNamePlayer1();
    String player2 = getNamePlayer2();

    if(!player1.isEmpty())bundle.putString("player1",player1);
    if(!player2.isEmpty())bundle.putString("player2",player2);
    bundle.putInt("d1",initDices.get(0).num);
    bundle.putInt("d2",initDices.get(1).num);
    bundle.putInt("d3",initDices.get(2).num);
    bundle.putInt("d4",initDices.get(3).num);
    bundle.putInt("d5",initDices.get(4).num);

    bundle.putBoolean("player2IsBot",binding.checkboxPc.isChecked());

    NavHostFragment.findNavController(Fragment_MainMenu.this)
        .navigate(R.id.action_mainmenu_to_ingame,bundle);
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.btnStart.setOnClickListener(v -> {setAndGetDices();start();});
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

}