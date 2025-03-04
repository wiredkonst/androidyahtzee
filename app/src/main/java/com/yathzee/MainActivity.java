package com.yathzee;

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.yathzee.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import fi.iki.elonen.NanoHTTPD;


public class MainActivity extends AppCompatActivity {

  private AppBarConfiguration appBarConfiguration;
  private ActivityMainBinding binding;

  private IntentHandlerWebServer webServer;

  private GestureController gestureController;
  private Fragment lastFragment = null;

  private CmdController cmdController;
  public CmdController getCmdController(){
    return cmdController;
  }

  public Fragment getCurrentFragment(){
    List<Fragment> fs = getSupportFragmentManager().getFragments();
    NavHostFragment n =  (NavHostFragment)fs.get(0);
    return n.getChildFragmentManager().getPrimaryNavigationFragment();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //Fragment f = getSupportFragmentManager().findFragmentById(R.id.frag_menu);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    setSupportActionBar(binding.toolbar);

    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

    appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    //voice interaction:
    webServer = new IntentHandlerWebServer(this);
    webServer.startService();
    //gesture interaction:
    gestureController = new GestureController(this);
    //combined interaction slot based:
    cmdController = new CmdController(this);

    /*binding.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab)
            .setAction("Action", null).show();
      }
    });*/
  }
  public void stopApp(){
    Timer timer = new Timer();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    //System.out.println("m x: "+ev.getX()+" y:"+ev.getY());
    //not sure why, but same method on views doesnt include movement events
    //reinit only after fragment switch
    Fragment nxtFragment = getCurrentFragment();
    if(nxtFragment != lastFragment){
      lastFragment = nxtFragment;
      gestureController.init();
    }
    try{gestureController.update(ev);}catch (Exception e){};
    return super.dispatchTouchEvent(ev);
  }

  @Override
  protected void onDestroy() {
    webServer.stopService();
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onSupportNavigateUp() {
    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    return NavigationUI.navigateUp(navController, appBarConfiguration)
        || super.onSupportNavigateUp();
  }
}