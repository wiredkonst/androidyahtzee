package com.yathzee;


import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.dtw.FastDTW;
import com.util.DistanceFunction;
import com.util.DistanceFunctionFactory;
import com.yathzee.gesture.*;
import com.yathzee.mediafusion.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GestureController {
  private MainActivity activity;
  private TouchVisView v;
  private Point visOffs;
  private boolean recognitionActive;

  private List<PointEvent> path; //current touch/mouse-path



  private List<Gesture> gestures; //registered gestures

  public GestureController(MainActivity activity){
    path = new ArrayList<>();
    gestures = new ArrayList<>();
    this.activity = activity;
    v = null;
    recognitionActive = false;
    visOffs = new Point(0,0);
    registerGestures();
  }
  private void registerGestures(){
    //gesture should be drawn on a rect(one sized) which always fits on screen
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    int d=height;
    visOffs.y=0;
    visOffs.x=(int)Math.round((width-height)*0.5);
    if(width<height){
      d=width;
      visOffs.x=0;
      visOffs.y=(int)Math.round((height-width)*0.5);
    }
    int[] tl={20,20},tr={80,20},bl={20,80},br={80,80};
    //rectangle-gesture clockwise: (dir doesnt matter)
    gestures.add(new Gesture(new int[][]{tr,tl,bl,br,tr},d,1));
    //numbers:
    //1:
    gestures.add(new Gesture(new int[][]{{30,40},{60,10},{60,90}},d,2));
    //2:
    gestures.add(new Gesture(new int[][]{{30,40},{50,10},{70,40},{30,90},{70,90}},d,3));
    //3:
    gestures.add(new Gesture(new int[][]{{30,20},{50,10},{70,20},{50,50},{70,80},{50,90},{30,80}},d,4));
    //4:
    gestures.add(new Gesture(new int[][]{{50,10},{30,60},{60,60},{60,90}},d,5));
    //5:
    gestures.add(new Gesture(new int[][]{{70,10},{30,10},{30,50},{50,50},{70,60},{70,80},{50,90},{30,90}},d,6));
    //6:
    gestures.add(new Gesture(new int[][]{{60,10},{40,40},{30,60},{30,80},{50,90},{70,80},{70,70},{50,60},{40,60}},d,7));

  }

  public void init(){
    Fragment f = activity.getCurrentFragment();
    TouchVisView newVis = f.getView().findViewById(R.id.touch_vis);
    Button btn = f.getView().findViewById(R.id.btn_gestures);
    Button btn2 = f.getView().findViewById(R.id.btn_toggle_gestures);

    if(newVis != null && btn != null){
      v = newVis;
      //visualizer-view uses diff pts
      List<List<android.graphics.Point>> gs = new ArrayList<>(gestures.size());
      for(Gesture g: gestures){
        List<android.graphics.Point> pts = new ArrayList<>(g.pts.size());
        for(Point pt: g.pts)pts.add(new android.graphics.Point(pt.x+visOffs.x,pt.y+visOffs.y));
        gs.add(pts);
      }
      v.setGestures(gs);
      btn.setOnClickListener(v1 -> v.showGesture());
      btn2.setOnClickListener(v1 -> recognitionActive=!recognitionActive);
    }else{
      v=null;
    }
  }



  public void update(MotionEvent event){
    if(v == null || !recognitionActive) return;
    //update event
    Point pt = new Point((int)event.getX(),(int)event.getY());
    //get event
    int action = event.getAction();
    boolean add = false;
    switch(action){
      case MotionEvent.ACTION_MOVE:
      case MotionEvent.ACTION_BUTTON_PRESS:
      case MotionEvent.ACTION_POINTER_DOWN:
      case MotionEvent.ACTION_DOWN: add = true; break;
    };
    //update
    //for some reason the up-event doesnt always work -> always delete all tocuhes on new path
    if(action == MotionEvent.ACTION_DOWN){
      path.clear();
      v.update(Collections.EMPTY_LIST);
    }
    //using a timer doesnt work for whatever reason
    if(add)path.add(new PointEvent(pt,event.getEventTime()));
    else{
      //2. create gesture/timeseries
      Gesture g = new Gesture(path);
      //3. check if its actually a gesture
      if(!g.pts.isEmpty()) {
        //4. calc distances
        Gesture gBest = null;
        double dBest = Double.MAX_VALUE;
        for (Gesture _g : gestures) {
          try {
            double d = FastDTW.getWarpDistBetween(g.ts, _g.ts, 3, Gesture.distFunc);
            if (d < dBest) {gBest = _g;dBest = d;}
          } catch (Exception e) {
            System.out.println("failed calc gesture distance");
          }
        }
        //5. execute ctx depending action (already in ui thread)
        System.out.println(" d: " + dBest + " for gesture id" + gBest.id);
        if (gBest != null && dBest <= Gesture.distThreshold) {
          Fragment f = activity.getCurrentFragment();
          if (f instanceof Fragment_InGame) {
            Fragment_InGame fig = (Fragment_InGame) f;
            switch (gBest.id) {
              case 1:
                fig.diceBtnAction();
                System.out.println("triggered gesture 1: roll dice");
                break;
              case 2: case 3: case 4: case 5: case 6: case 7:
                if(activity.getCmdController().tryFillSlot(Slot.Type.Gesture, gBest.id, null))break;
                fig.setScore(gBest.id - 2);
                System.out.println("triggered gesture " + gBest.id + ": score as " + (gBest.id - 1) + "s");
                break;
              default:
                System.out.println("gesture not available in game");
            }
          }
        }
      }
      path.clear();
    }

    //visualization:
    if(add)v.update(new android.graphics.Point(pt.x,pt.y));else v.update(Collections.EMPTY_LIST);
  }
}
