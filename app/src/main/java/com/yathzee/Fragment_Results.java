package com.yathzee;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yathzee.databinding.FragmentResultsBinding;

public class Fragment_Results extends Fragment {
  private FragmentResultsBinding binding;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  ) {

    binding = FragmentResultsBinding.inflate(inflater, container, false);
    return binding.getRoot();

  }
  public void restart(){
    NavHostFragment.findNavController(Fragment_Results.this)
        .navigate(R.id.action_fragment_Results_to_mainmenu);
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    String p1_name = getArguments().getString("player1_name");
    String p2_name = getArguments().getString("player2_name");
    int p1_score = getArguments().getInt("player1_score");
    int p2_score = getArguments().getInt("player2_score");

    String winner = "",display="";
    if(p1_score>p2_score){
      winner = p1_name;
    }
    if(p1_score<p2_score){
      winner = p2_name;
    }
    if(p1_score == p2_score){
      display = getString(R.string.player_draw);
    }else{
      display = getString(R.string.player_won,winner);
    }
    binding.tvP1.setText(getString(R.string.player_score,p1_name,p1_score));
    binding.tvP2.setText(getString(R.string.player_score,p2_name,p2_score));
    binding.tvWinner.setText(display);

    binding.btnDone.setOnClickListener(v -> {restart();});
  }
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}