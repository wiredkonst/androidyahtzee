package com.yathzee;

import static org.junit.Assert.assertEquals;

import com.yathzee.gesture.Gesture;
import com.yathzee.gesture.PointEvent;
import com.yathzee.gesture.Point;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GestureUnitTest {
  private void assertPtList(int[] arr,int[] exp){
    List<PointEvent> pts = new ArrayList<>();
    for(int a:arr) pts.add(new PointEvent(new Point(a,0),(long)a));
    List<Point> r = Gesture.getTSPoints(pts);
    assertEquals(exp.length,r.size());
    for(int i=0;i<r.size();i++)
      assertEquals(exp[i],r.get(i).x);

  }
  private void assertDegList(int[][] arr){
    List<Point> pts = new ArrayList<>();
    for(int[] a: arr)pts.add(new Point(a[0],a[1]));
    List<Integer> degs = Gesture.getDegrees(pts);
    for(int i=0;i<degs.size();i++)
      assertEquals(arr[i+2][2],degs.get(i).intValue());
  }
  @Test
  public void getTSPoints(){
    Gesture.dt = 3;
    assertPtList(new int[]{0,1,2,4,6,7,10},new int[]{0,2,6,10});
  }
  @Test
  public void getDegrees(){
    assertDegList(new int[][]{
        {100,100},{150,100},
        {200,100,180},
        {200,150,90},
        {200,200,90+26},
        {150,200,45},
        {100,200,45+18}});
  }
}
