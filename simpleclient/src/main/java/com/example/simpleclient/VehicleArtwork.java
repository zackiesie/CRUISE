package com.example.simpleclient;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;

/** Original scalable automotive illustration, independent of external assets. */
public final class VehicleArtwork extends Region {
 private final Canvas canvas = new Canvas();
 public VehicleArtwork() {
  getChildren().add(canvas); setMinSize(120,70); setPrefSize(440,205);
  setMouseTransparent(true); setAccessibleText("Illustration of a touring sedan");
 }
 @Override protected void layoutChildren() {
  double w=getWidth(),h=getHeight(); canvas.setWidth(w);canvas.setHeight(h);
  GraphicsContext g=canvas.getGraphicsContext2D(); g.clearRect(0,0,w,h);g.save();
  double s=Math.min(w/480,h/230);g.translate((w-480*s)/2,(h-230*s)/2);g.scale(s,s);
  g.setStroke(Color.web("#859472",0.22));g.setLineWidth(1);
  g.strokeOval(100,-18,280,280);g.strokeOval(132,14,216,216);g.strokeLine(4,195,476,195);
  g.setFill(Color.web("#101a11",0.45));g.fillOval(40,168,404,30);
  g.setFill(new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#e1e5d9")),new Stop(.52,Color.web("#aebba0")),new Stop(1,Color.web("#657559"))));
  g.beginPath();g.moveTo(25,149);g.bezierCurveTo(30,127,54,121,102,114);
  g.bezierCurveTo(124,88,155,64,196,64);g.lineTo(270,64);g.bezierCurveTo(310,66,335,90,357,108);
  g.bezierCurveTo(386,112,429,116,448,133);g.lineTo(456,159);g.bezierCurveTo(455,170,445,177,426,179);
  g.lineTo(60,179);g.bezierCurveTo(36,176,25,168,25,149);g.closePath();g.fill();
  g.setFill(Color.web("#243329"));
  g.beginPath();g.moveTo(121,111);g.bezierCurveTo(139,89,164,76,196,76);g.lineTo(211,76);g.lineTo(211,109);g.closePath();g.fill();
  g.beginPath();g.moveTo(219,76);g.lineTo(266,76);g.bezierCurveTo(296,77,317,94,337,110);g.lineTo(219,109);g.closePath();g.fill();
  g.setStroke(Color.web("#f1f2e9",.65));g.setLineWidth(1.5);g.strokeLine(63,126,399,124);
  g.setStroke(Color.web("#4f6048"));g.setLineWidth(1);g.strokeLine(213,114,213,166);g.strokeLine(332,116,344,166);g.strokeLine(132,116,123,166);g.strokeLine(115,165,351,165);
  g.setStroke(Color.web("#56654c"));g.setLineWidth(3);g.strokeLine(183,126,201,126);g.strokeLine(295,126,313,126);
  wheel(g,100,173);wheel(g,382,173);
  g.setFill(Color.web("#f5e8c6"));g.fillRoundRect(421,133,28,7,3,3);
  g.setFill(Color.web("#bc8767"));g.fillRoundRect(28,133,17,6,2,2);
  g.setStroke(Color.web("#34432f"));g.setLineWidth(4);g.strokeLine(430,155,449,155);
  g.setStroke(Color.web("#d5b78e",.7));g.setLineWidth(1);g.strokeLine(25,214,109,214);g.strokeLine(371,214,455,214);
  g.restore();
 }
 private void wheel(GraphicsContext g,double x,double y) {
  g.setFill(Color.web("#182018"));g.fillOval(x-25,y-25,50,50);
  g.setFill(Color.web("#72816a"));g.fillOval(x-17,y-17,34,34);
  g.setStroke(Color.web("#cbd4bd"));g.setLineWidth(2);g.strokeOval(x-14,y-14,28,28);
  for(int i=0;i<5;i++){double a=i*Math.PI*2/5;g.strokeLine(x,y,x+12*Math.cos(a),y+12*Math.sin(a));}
  g.setFill(Color.web("#d5b78e"));g.fillOval(x-4,y-4,8,8);
 }
}
