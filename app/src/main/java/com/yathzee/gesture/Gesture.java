package com.yathzee.gesture;

import com.timeseries.TimeSeries;
import com.util.DistanceFunction;
import com.util.DistanceFunctionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Gesture{
  public int id; //id for registered gestures
  public List<Point> pts;
  public TimeSeries ts;
  public void createTS(List<Integer> degs){
    double[] tspts = new double[degs.size()];
    Iterator<Integer> it = degs.iterator();
    for(int i=0;i<degs.size();i++)tspts[i]=it.next();
    ts = new TimeSeries(tspts);
  }
  //used some gesture experiments for getting those numbers:
  public static int dt = 30; //ms, timeseries stepping time, due to testing not static
  public static int v = 2; //assumed gesture velocity px/ms, due to testing not static
  public final static double distThreshold = 450;
  //some algo constants:
  public final static DistanceFunction distFunc = DistanceFunctionFactory.getDistFnByName("EuclideanDistance");
  private static double cta = 180/Math.PI;//cos to angle factor
  private static double getNorm(Point pt){
    return Math.sqrt(pt.x*pt.x+pt.y*pt.y);
  }
  public static List<Integer> getDegrees(List<Point> pts){
    List<Integer> as = new ArrayList<>();
    if(pts.size() < 3) return as;
    Iterator<Point> it = pts.iterator();
    Point f = it.next(),p = it.next(),c,pf,pc; //first, previous, current pt and p to f and p to c displacement
    long dp = 0; //dotproduct
    double r = 0; //eq parts
    while(it.hasNext()){
      c = it.next();
      //calc angle between three pts f,p,c where p is the intersection
      pf = new Point(f.x-p.x,f.y-p.y);
      pc = new Point(c.x-p.x,c.y-p.y);
      dp = pf.x*pc.x+pf.y*pc.y;
      r = getNorm(pf)*getNorm(pc);
      if(r == 0)r=0;else r=Math.acos(((double)dp)/r)*cta;
      as.add((int)r);
      p=c;
    }
    return as;
  }
  //get points of the path fitting to the gesture dt
  public static List<Point> getTSPoints(List<PointEvent> path){
    List<Point> _path = new ArrayList<>(); //new path
    if(path.size() < 3)return _path;
    PointEvent c,p = null; //current pair and previous pair
    Iterator<PointEvent> it = path.iterator();
    c = it.next(); p = c;
    Long ts = c.ts+dt;
    _path.add(c.pt);
    while(it.hasNext()){
      c=it.next();
      if(c.ts >= ts){
        if(c.ts-ts < ts-p.ts){
          //current is nearest to time step
          _path.add(c.pt);
        }else{
          //previous is nearest to time step
          _path.add(p.pt);
        }
        ts+=Gesture.dt;
      }
      p = c;
    }
    return _path;
  }

  private int getAbsPos(int p,int dimension){
    return (int)Math.round((float)p*0.01*dimension);
  }
  /**
   * called on init
   * @param pts
   */
  public Gesture(int[][] pts,int d, int id){
    this.pts = new ArrayList<>();
    //convert given abs pt in 2d space in gesture pts
    // since rads only used in the end, no need for relatives, anything fine
    // conversion heavily based on line equation
    double remD = 0,remS=0,dist=0,stepS=0; //remaining (distance, s),total distance, stepping s
    int[] pt,npt;//current point and next point
    int stepD = dt*v,xD,yD;//stepping distance, x and y diffs
    for(int p = 0; p<pts.length-1;p++){
      pt = pts[p];
      npt = pts[p+1];
      xD = getAbsPos(npt[0]-pt[0],d);
      yD = getAbsPos(npt[1]-pt[1],d);
      dist = Math.sqrt(xD*xD + yD*yD);
      if(remD == 0)remS=0; else remS = remD/dist; //remaining distance needs to be rescaled to current s
      stepS = stepD/dist;
      for(;remS<=1.0;remS+=stepS){
        int x = getAbsPos(pt[0],d) + (int)Math.round(remS*(getAbsPos(npt[0]-pt[0],d)));
        int y = getAbsPos(pt[1],d) + (int)Math.round(remS*(getAbsPos(npt[1]-pt[1],d)));
        this.pts.add(new Point(x,y));
      }
      remS -= 1.0;
      remD = remS*dist;
    }
    createTS(getDegrees(this.pts));
    this.id=id;
  }

  /**
   * called within touch event
   * @param pts
   */
  public Gesture(List<PointEvent> pts){
    this.pts = getTSPoints(pts);
    createTS(getDegrees(this.pts));
  }
}
