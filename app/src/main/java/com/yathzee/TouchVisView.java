package com.yathzee;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TouchVisView extends View {
  private List<Point> path; //current touch path
  private List<List<Point>> gestures; //gesture paths
  private int gesture; //current showing gesture
  private Paint paintT,paintG;
  public TouchVisView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }
  public TouchVisView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }
  public TouchVisView(Context context) {
    super(context);
    init();
  }
  private void init(){
    path = new ArrayList<>();
    paintT = new Paint();
    paintT.setARGB(255,255,0,0);
    paintT.setStrokeWidth(5);
    paintG = new Paint();
    paintG.setARGB(255,0,0,255);
    paintG.setStrokeWidth(8);
    gesture = -1;
  }

  public void setGestures(List<List<Point>> gestures) {
    this.gestures = gestures;
  }

  //show available gestures via toggling through them
  public void showGesture(){
    if(gesture<0)gesture=0;
    else {
      gesture++;
      if(gesture > gestures.size()-1)gesture=-1;
    }
    invalidate();
  }

  public void update(List<Point> newPath){
    path.clear();
    path.addAll(newPath);
    invalidate();
  }
  public void update(Point pt){
    path.add(pt);
    invalidate();
  }
  private void drawPath(Canvas canvas, List<Point> path,Paint paint){
    Point prev = null;
    for(Point p: path){
      if(prev != null) canvas.drawLine(prev.x,prev.y,p.x,p.y,paint);
      prev = p;
    }
  }
  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    //always cleared before :)
    if(gesture>-1)drawPath(canvas, gestures.get(gesture),paintG);
    drawPath(canvas,path,paintT);
  }
}
