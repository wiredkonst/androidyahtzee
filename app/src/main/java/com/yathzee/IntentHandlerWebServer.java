package com.yathzee;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Path;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.color.utilities.Score;
import com.yathzee.mediafusion.Slot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fi.iki.elonen.NanoHTTPD;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.functions.Function4;

public class IntentHandlerWebServer {

  private NanoHTTPD server;
  private final int port = 8080;
  private final Activity activity; // mainactivity

  //--- api:
  private final List<String> ctxs; //list of available ctxs
  private class Intent{

    public final int id; //unique identifier if being lazy
    public final String name,desc; //intent name (uri), desc is valid ssml formed help-response
    public final List<Integer> ctxs; //idxs into ctxs for which ctxs the intent is available
    public final boolean needsResponse; //true if intent doesnt make sense w/o a response
    public Intent(int id, String name, String desc, List<Integer> ctxs,boolean needsResponse){
      this.id = id;this.name = name;this.desc = desc;this.ctxs=ctxs;this.needsResponse=needsResponse;
    }
  };
  private Intent emptyIntent = new Intent(-1,"","",Collections.EMPTY_LIST,false);
  private final List<Intent> intents;
  //scorenames = scores names lowercase'd
  private static final String[] ords = {"first","second","third","fourth","fifth"};
  //collective numbers = <number>s
  private final Map<String,String> responses; //ctx_idx -> ssml formed response
  //some vars for tracking the dialog state and configs
  private int dialogIdx = 0; //context derivable
  private String currCtx = ""; //current ctx
  private Intent currIntent = emptyIntent; //current served intent

  private boolean vuiOnly = true;
  //---
  private final Scores scores;
  private Intent getIntent(String uri){
    String intent = uri.replace("/","");
    for(Intent i: intents)
      if(i.name.equals(intent))return i;
    return emptyIntent;
  }

  private int getScoreNameIdx(String scorename){
    for(int i=0;i<scores.Names.length;i++){
      if(scores.Names[i].toLowerCase().equals(scorename))
        return i;
    }
    return -1;
  }
  private String getStrParam(Map<String,List<String>>params,String param){
    if(!params.containsKey(param))return "";
    List<String> ps = params.get(param);
    if(ps == null || ps.isEmpty())return "";
    return ps.get(0);
  }
  private List<Integer> getIntArrParam(Map<String,List<String>>params,String param){
    if(!params.containsKey(param))return Collections.EMPTY_LIST;
    List<String> ps = params.get(param);
    if(ps == null || ps.isEmpty())return Collections.EMPTY_LIST;
    List<Integer> arr = new ArrayList<>();
    for(String p: ps) {
      int n = -1;
      try {
        n = Integer.parseInt(p);
      } catch (Exception e) {
        System.out.println("cannot parse param "+p);
        return arr;
      }
      arr.add(n);
    }
    return arr;
  }
  private class Response{
    private final List<String> msgs = new ArrayList<>();
    private final AtomicBoolean responded = new AtomicBoolean(false);
    public void addRes(String res){msgs.add(res);}
    public void setRes(){responded.set(true);}
    public String get(){
      if(msgs.isEmpty())return "";
      StringBuilder sb = new StringBuilder();
      sb.append("<speak>");
      for(String s: msgs)sb.append("<p>"+s+"</p>");
      sb.append("</speak>");
      return sb.toString();
    }
    public void waitForRes(){while(!responded.get());}
  };
  //add/complete response
  // only write if intent requires one or vui enforced
  private void writeResponse(Optional<String> addMsg,Response res){
    if(!currIntent.needsResponse && !vuiOnly)return;
    if(addMsg.isPresent())res.addRes(addMsg.get());
    else res.setRes();
  }

  private String getArrStr(List<String> arr){
    StringBuilder sb = new StringBuilder();
    for(int i=0;i<arr.size();i++){
      sb.append(arr.get(i));
      if(i != arr.size()-1)if(i<arr.size()-2)sb.append(", "); else sb.append(" and ");
    }
    return sb.toString();
  }
  private String getHelpStr(String ctx){
    int ctxIdx = ctxs.indexOf(ctx);
    List<String> s = new ArrayList<>();
    for(Intent i: intents)if(i.ctxs.contains(ctxIdx))s.add(i.desc);
    return String.join(" ",s);
  }
  private String getDiceStr(List<Dice> dices){
    List<String> ds = new ArrayList<>();
    for(Dice d: dices)ds.add(d.num.toString());
    return getArrStr(ds);
  }
  private String getNrsStr(List<Integer> nrs,boolean nrOrdinals){
    List<String> s = new ArrayList<>();
    for(Integer nr: nrs)if(nrOrdinals)s.add(ords[nr-1]);else s.add(nr+"s");
    return getArrStr(s);
  }

  private String readAssetFile(String fn){
    try{
      InputStream is = this.activity.getAssets().open(fn);
      BufferedReader bR = new BufferedReader(new InputStreamReader(is));
      StringBuilder sB = new StringBuilder();
      String line;while((line = bR.readLine()) != null)sB.append(line);
      bR.close();
      return sB.toString();
    }catch (Exception e){
      System.out.println("reading asset "+fn+" failed");
      return "";
    }

  }
  private String applySubs(String s, JSONObject subs){
    Iterator<String> kI = subs.keys();
    String _s = s;
    try {
      while(kI.hasNext()){
        String k = kI.next();
        _s = _s.replace(k,subs.getString(k));
      }
    }catch (Exception e){
      System.out.println("applying substitutions failed for: "+s);
    }
    return _s;
  }


  public IntentHandlerWebServer(MainActivity activity){
    //main init shit
    this.activity = activity;
    //external shit
    this.scores = new Scores();
    JSONObject intentsObj = null,responseObj = null,shortcutsObj = new JSONObject();
    try {
      intentsObj = new JSONObject(readAssetFile("intents.json"));
      responseObj = new JSONObject(readAssetFile("responses.json"));
    }catch (Exception e){
      System.out.println("parsing json files failed");
    }
    //init responses
    this.responses = new HashMap<>();
    try{
      shortcutsObj =  responseObj.getJSONObject("shortcuts");
      JSONObject responsesObj = responseObj.getJSONObject("responses");
      Iterator<String> kI = responsesObj.keys();
      while(kI.hasNext()){
        String k = kI.next();
        responses.put(k,applySubs(responsesObj.getString(k),shortcutsObj));
      }
    }catch (Exception e){
      System.out.println("responses: accessing json members failed");
    }
    //init intents
    this.ctxs = new ArrayList<>();
    this.intents = new ArrayList<>();
    try {
      //get available ctxs
      JSONArray jsonCtxs = intentsObj.getJSONArray("ctxs");
      for(int i=0; i<jsonCtxs.length();i++)ctxs.add(jsonCtxs.getString(i));
      //get available intents
      JSONArray jsonIntents = intentsObj.getJSONArray("intents");
      for(int i=0;i<jsonIntents.length();i++){
        JSONObject jsonIntent = jsonIntents.getJSONObject(i);
        JSONArray jsonIntentCtxs = jsonIntent.getJSONArray("c");
        List<Integer> intentCtxs = new ArrayList<>(jsonIntentCtxs.length());
        //get idx of those ctxs
        for(int ci = 0;ci<jsonIntentCtxs.length();ci++) {
          String c = jsonIntentCtxs.getString(ci);
          if(ctxs.contains(c))
            intentCtxs.add(ctxs.indexOf(c));
          else
            throw new RuntimeException("intents ctx doesnt exist");
        }
        intents.add(new Intent(jsonIntent.getInt("id"),
            jsonIntent.getString("n"),
            applySubs(jsonIntent.getString("d"),shortcutsObj),
            intentCtxs,
            jsonIntent.has("r") && jsonIntent.getBoolean("r")));
      }
    }catch (Exception e){
      System.out.println("intents: accessing json members failed");
    }

    //server
    server = new NanoHTTPD(port) {
      @Override
      public Response serve(IHTTPSession session) {
        //logging:
        //String clientIP = session.getRemoteIpAddress();
        System.out.println(session.getRemoteIpAddress());
        System.out.println(session.getUri());
        System.out.println(session.getParameters());

        //if(!clientIP.equals(lambda_server) && !clientIP.equals(dev_server))
        //  return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "Forbidden");

        currIntent = getIntent(session.getUri());
        IntentHandlerWebServer.Response res = new IntentHandlerWebServer.Response();

        //(game)state independent request handling:
        if(currIntent.id == 20){ //toggle vui
          vuiOnly = !vuiOnly;
          if(vuiOnly)
            writeResponse(Optional.of(responses.get("vui_0")),res);
          else
            writeResponse(Optional.of(responses.get("vui_1")),res);
          writeResponse(Optional.empty(),res);
        } else if (currIntent.id == 27 || currIntent.id == 28){ //close cmd
          activity.runOnUiThread(()->
            activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, null)
          );
          writeResponse(Optional.empty(),res);
        }
        else {

          //state depending request handling:
          Fragment currFragment = activity.getCurrentFragment();
          if (currFragment instanceof Fragment_MainMenu) {
            currCtx = "mm";
            Fragment_MainMenu fmain = (Fragment_MainMenu) currFragment;
            activity.runOnUiThread(() -> {
              Runnable ctxSwitch = () -> {
                writeResponse(Optional.of(String.format(responses.get("mm_3"), fmain.getNamePlayer1(), fmain.getNamePlayer2())), res);
                writeResponse(Optional.of(String.format(responses.get("g_18"), fmain.getNamePlayer1())), res);
                writeResponse(Optional.of(responses.get("g_0")), res);
                writeResponse(Optional.of(String.format(responses.get("g_1"), getDiceStr(fmain.setAndGetDices()))), res);
                writeResponse(Optional.of(responses.get("g_2")), res);
                fmain.start();
                dialogIdx = 1; //dialog in game is state-driven
              };
              switch (currIntent.id) {
                case 0: fmain.setNamePlayer1(getStrParam(session.getParameters(), "name"));
                  writeResponse(Optional.of(responses.get("mm_4")), res);
                  break;
                case 1: fmain.setNamePlayer2(getStrParam(session.getParameters(), "name"));
                  writeResponse(Optional.of(responses.get("mm_4")), res);
                  break;
                case 2: ctxSwitch.run();break;
                case 19:
                { String name = getStrParam(session.getParameters(), "name");
                  if (dialogIdx < 0) break;
                  dialogIdx++;
                  switch (dialogIdx) {
                    case 1:
                      fmain.setNamePlayer1(name);
                      writeResponse(Optional.of(responses.get("mm_2")), res);
                      break;
                    case 2:
                      fmain.setNamePlayer2(name);
                      ctxSwitch.run();
                      break;
                  }
                  break;}
                case 21: //help intent
                  writeResponse(Optional.of(getHelpStr("mm")), res);break;
                case 22: //open game
                  dialogIdx = 0;
                  writeResponse(Optional.of(responses.get("mm_0")), res);
                  writeResponse(Optional.of(responses.get("mm_1")), res);
                  break;
                case 20: case 27: case 28: break; //handled above
                default: System.out.println(session.getUri() + " not available in main menu");
              }
              writeResponse(Optional.empty(), res);
            });
          } else if (currFragment instanceof Fragment_InGame) {
            currCtx = "g";
            Fragment_InGame fig = (Fragment_InGame) currFragment;
            activity.runOnUiThread(() -> {
              int cP = fig.getCurrentPlayer();
              List<Player> ps = fig.getPlayers();
              //modify dice taken-state depending on given
              Function4<String, String, Boolean, Boolean, Object> handleDice = (String pn, String a, Boolean takeIt, Boolean nrOrdinals) -> {
                List<Integer> params = getIntArrParam(session.getParameters(), pn);
                if(activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, params))return null;
                fig.toggleTaken(params, takeIt, nrOrdinals);
                writeResponse(Optional.of(String.format(responses.get(a), getNrsStr(params, nrOrdinals))), res);
                return null;
              };
              //list dice state
              Runnable listDice = () -> {
                List<Dice> ds = fig.getDices();
                List<String> dps = new ArrayList<>();
                for (int i = 0; i < ds.size(); i++) if (ds.get(i).isTaken) dps.add(ords[i]);
                String keepStr = "no";
                if (!dps.isEmpty()) keepStr = "the " + getArrStr(dps);
                writeResponse(Optional.of(String.format(responses.get("g_9"), ps.get(fig.getCurrentPlayer()).getName(),getDiceStr(ds), keepStr, fig.getRemainingRolls())), res);
              };
              //list missing scores
              Runnable listMissingScores = () -> {
                Set<String> psns = ps.get(fig.getCurrentPlayer()).getScoreNames();
                List<String> s = new ArrayList<>();
                for (String sname : scores.Names) if (!psns.contains(sname)) s.add(sname);
                writeResponse(Optional.of(String.format(responses.get("g_3"), getArrStr(s))), res);
              };
              //advance in the in-game dialog
              //execution independent (finite state)
              //if boolAnswer is null, dialog stays in states which need an answer,
              //(however if they dont need, the dialog still continues)
              Consumer<Boolean> dialog = (Boolean boolAnswer) -> {
                if (dialogIdx < 0) return;
                switch (dialogIdx) {
                  case 0:
                    writeResponse(Optional.of(responses.get("g_2")), res);
                    dialogIdx++;
                    break;
                  case 1: if(boolAnswer == null)break;
                    if (boolAnswer) listMissingScores.run();
                    dialogIdx++;
                    writeResponse(Optional.of(responses.get("g_4")), res);
                    break;
                  case 2: if(boolAnswer == null)break;
                    if (boolAnswer) listDice.run();
                    dialogIdx++;
                    writeResponse(Optional.of(responses.get("g_5")), res);
                    break;
                  case 3:
                    if (fig.isInScoringPhase()) writeResponse(Optional.of(responses.get("g_6")), res);
                    break;
                }
              };
              //reroll all depending on dice state
              Consumer<Boolean> rollDice = (Boolean rerolling) -> {
                List<Dice> ds = fig.getDices();
                List<Dice> rr = new ArrayList<>();
                if(rerolling != null && rerolling) for (Dice d : ds){ if (d.markedAsReroll) rr.add(d);}
                else for (Dice d : ds) if (!d.isTaken) rr.add(d);
                fig.diceBtnAction();
                writeResponse(Optional.of(String.format(responses.get("g_17"), rr.size())), res);
                writeResponse(Optional.of(String.format(responses.get("g_1"), getDiceStr(rr))), res);
                listDice.run();
                dialog.accept(null);
              };
              //reroll only given
              Function2<String, Boolean, Object> rerollDice = (String pn, Boolean nrOrdinals) -> {
                List<Integer> params = getIntArrParam(session.getParameters(), pn);
                if(activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, params))return null;
                if (fig.isInScoringPhase()) return null;
                List<Dice> ds = fig.getDices();
                for (int i = 0; i < ds.size(); i++) {
                  Dice d = ds.get(i);
                  //create new depending on reroll params
                  if(!d.markedAsReroll)
                    if (nrOrdinals) {if (params.contains(i + 1)) d.markedAsReroll = true;}
                    else if (params.contains(d.num)) d.markedAsReroll = true;
                }
                rollDice.accept(true);
                return null;
              };

              switch (currIntent.id) {
                case 3: handleDice.invoke("numbers", "g_10", true, false);break;
                case 4: handleDice.invoke("onumbers", "g_11", true, true);break;
                case 5: handleDice.invoke("numbers", "g_12", false, false);break;
                case 6: handleDice.invoke("onumbers", "g_13", false, true);break;
                case 7:
                  if(activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, null))break;
                  if(fig.isInScoringPhase()) break;rollDice.accept(false);break;
                case 8: {
                  String arg = getStrParam(session.getParameters(), "sname");
                  int s = fig.getScore(getScoreNameIdx(arg));
                  if (s == -1)
                    writeResponse(Optional.of(String.format(responses.get("g_14_a"), arg)), res);
                  else
                    writeResponse(Optional.of(String.format(responses.get("g_14"), arg, s)), res);
                  break;}
                case 9: {
                  String arg = getStrParam(session.getParameters(), "sname");
                  int sidx = getScoreNameIdx(arg);
                  if(activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, sidx))break;
                  int s = fig.getScore(sidx);
                  if (fig.setScore(getScoreNameIdx(arg))) {
                    writeResponse(Optional.of(String.format(responses.get("g_15"), s, arg)), res);
                    if (fig.isInLastTurn()) {
                      //game over, using workaround as well
                      int[] ss = {ps.get(0).getTotalScore(), ps.get(1).getTotalScore()};
                      ss[cP] += s;
                      writeResponse(Optional.of(String.format(responses.get("r_0"), ps.get(0).getName(), ss[0], ps.get(1).getName(), ss[1])), res);
                      String w = "";
                      if (ss[0] > ss[1]) w = ps.get(0).getName();if (ss[0] < ss[1]) w = ps.get(1).getName();
                      if (w.isEmpty()) writeResponse(Optional.of(responses.get("r_1")), res);
                      else writeResponse(Optional.of(String.format(responses.get("r_2"), w)), res);
                      writeResponse(Optional.of(responses.get("r_3")), res);
                      fig.diceBtnAction();
                      dialogIdx = 0;
                    } else {
                      //switch turn
                      fig.diceBtnAction();
                      cP = fig.getCurrentPlayer();
                      writeResponse(Optional.of(String.format(responses.get("g_18"), ps.get(cP).getName())), res);
                      writeResponse(Optional.of(responses.get("g_0")), res);
                      writeResponse(Optional.of(String.format(responses.get("g_1"), getDiceStr(fig.getDices()))), res);
                      writeResponse(Optional.of(responses.get("g_2")), res);
                      dialogIdx = 1;
                    }
                  } else writeResponse(Optional.of(String.format(responses.get("g_14_a"), arg)), res);
                  break;}
                case 10:
                  writeResponse(Optional.of(responses.get("g_16")), res);
                  writeResponse(Optional.of(responses.get("mm_1")), res);
                  dialogIdx = 0;
                  fig.restart();
                  break;
                case 11: //list completed scores
                { Set<Map.Entry<String, Integer>> scores = ps.get(cP).getScores();
                  if (scores.isEmpty()) writeResponse(Optional.of(responses.get("g_7")), res);
                  else {
                    List<String> s = new ArrayList<>();
                    for (Map.Entry<String, Integer> e : scores) s.add(e.getValue().toString() + " points in " + e.getKey());
                    writeResponse(Optional.of(String.format(responses.get("g_8"), getArrStr(s))), res);
                  }
                  break;}
                case 12: listMissingScores.run();break;
                case 13:
                  if(activity.getCmdController().tryFillSlot(Slot.Type.Voice,currIntent.id,null))break;
                  listDice.run();break;
                case 14: //help score
                { String arg = getStrParam(session.getParameters(), "sname");
                  int scoreIdx = getScoreNameIdx(arg);
                  StringBuilder sb = new StringBuilder();
                  if (scoreIdx < 0)break;else
                  if (scoreIdx < 6) sb.append(String.format(responses.get("s_0"), "" + (scoreIdx + 1)));else
                  if (scoreIdx < 8) sb.append(String.format(responses.get("s_6"), "" + (scoreIdx - 3)));else
                  if (scoreIdx < 9) sb.append(responses.get("s_8"));else
                  if (scoreIdx < 11) sb.append(String.format(responses.get("s_9"), arg, scoreIdx - 5,30 + (scoreIdx - 9) * 10));else
                  if (scoreIdx < 12) sb.append(responses.get("s_11"));else
                  if (scoreIdx < 13) sb.append(responses.get("s_12"));else break;
                  writeResponse(Optional.of(sb.toString()),res);
                  writeResponse(Optional.of(String.format(responses.get("s_ex"), arg, responses.get("s_" + scoreIdx + "_ex"))),res);
                  break;}
                case 15: rerollDice.invoke("numbers", false);break;
                case 16: rerollDice.invoke("onumbers", true);break;
                case 17: dialog.accept(true);break;
                case 18: dialog.accept(false);break;
                case 21: writeResponse(Optional.of(getHelpStr("g")), res);break;
                //cmd based voice interaction only:
                case 23: case 25://cmd triggers w/o arg ctx depending
                  activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, null);  break;
                case 24:
                { List<Integer> args = getIntArrParam(session.getParameters(),"onumber");
                  if(args.isEmpty())break;
                  activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, args.get(0)); break;}
                case 26:
                { int scoreIdx = getScoreNameIdx(getStrParam(session.getParameters(),"sname"));
                  if(scoreIdx < 0)break;
                  activity.getCmdController().tryFillSlot(Slot.Type.Voice, currIntent.id, scoreIdx);
                  break;}
                case 20:  case 27: case 28: break; //handled above
                default: System.out.println(session.getUri() + " not available in game");
              }
              writeResponse(Optional.empty(), res);
            });
          } else if (currFragment instanceof Fragment_Results) {
            currCtx="r";
            Fragment_Results fr = (Fragment_Results) currFragment;
            activity.runOnUiThread(() -> {
              switch (currIntent.id) {
                case 10:
                  writeResponse(Optional.of(responses.get("g_16")), res);
                  writeResponse(Optional.of(responses.get("mm_1")), res);
                  dialogIdx = 0;
                  fr.restart();
                  break;
                case 21: writeResponse(Optional.of(getHelpStr("g")), res);break;
                case 20: case 27: case 28: break; //handled above
                default: System.out.println(session.getUri() + " not available in results");
              }
              writeResponse(Optional.empty(), res);
            });
          } else {
            System.out.println("unclear game state");
          }
        }

        //sync for response if required:
        if(currIntent.needsResponse || vuiOnly){
          res.waitForRes();
        }
        return newFixedLengthResponse(res.get());
      }
    };
  }

  public void startService(){
    System.out.println("Server started on port "+port);
    try {
      server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false); //nonblocking
    } catch (IOException e) {
      System.out.println("smth went wrong when starting the server");
      throw new RuntimeException(e);
    }
  }
  public void stopService(){
    System.out.println("close server");
    server.closeAllConnections();
    server.stop(); //thread will run to finish if server is closed
  }
}
