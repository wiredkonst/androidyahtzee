package com.yathzee.pc;

public class MathUtil {
  /**
   * calculate faculty of n
   */
  public static long fac(int n){
    long res = 1;
    for(;n>1;n--)res*=n;
    return res;
  }
  /**
   * binomialcoeffient (n over k)
   * calculate amount of possible subsets of size k from a set of size n
   */
  public static int poss(int n,int k){
    if(k>n || k<0)return 0;
    return (int)(fac(n)/(fac(k)*fac(n-k)));
  }
  /**
   * binomial distribution
   * calculate total chance of having EXACTLY x successes out of n tries with a single probability of p
   */
  public static double binDist(int n,double p,int x){
    return poss(n,x) * Math.pow(p,x) * Math.pow(1-p,n-x);
  }

  /**
   * cumulative binomial distribution
   * calculate total chance of having AT MOST x successes out of n tries with a single probability of p
   */
  public static double cumBinDist(int n,double p,int x){
    double res=0;
    for(int i=0;i<x;i++)res+=binDist(n,p,i);
    return res;
  }

}
