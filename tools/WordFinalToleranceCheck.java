import com.mechrobotix.aprendels.*;
import java.util.*;

/** Comprueba el margen final con una ejecución completa y posturas deliberadamente distintas. */
public final class WordFinalToleranceCheck {
    static WordPracticeEngine.Frame frame(long time,double x,double flex) {
        double[][] points=new double[21][2];
        for(int i=0;i<21;i++){points[i][0]=300+x+(i%4)*15;points[i][1]=400-(i/4)*20;}
        double[] angles=new double[10];Arrays.fill(angles,flex);
        return new WordPracticeEngine.Frame(time,Map.of("Right",new JPracticeEngine.Sample(points,angles,"Right")));
    }
    static WordPracticeEngine replay(double scale,boolean wrongFingers,boolean jump) {
        return replay(scale,wrongFingers,jump,0);
    }
    static WordPracticeEngine replay(double scale,boolean wrongFingers,boolean jump,double finalMargin) {
        var keys=List.of(frame(0,0,35),frame(300,30,35),frame(600,60,35),frame(900,90,35));
        var tolerance=new WordTolerance();tolerance.meanFlex=10;tolerance.maxFlex=20;tolerance.endHoldMs=140;tolerance.startHoldMs=100;tolerance.minAttemptMs=300;
        var options=new WordStageOptions();options.finalPoseScale=scale;options.pathTolerance=.4;options.maxFingerMismatches=0;options.finalPositionTolerance=finalMargin;
        var engine=new WordPracticeEngine(keys,tolerance,false,options);long time=1000;
        for(int i=0;i<5;i++){engine.update(time,frame(time,0,35));time+=100;}
        if(!jump)for(int x=5;x<=90;x+=5){engine.update(time,frame(time,x,wrongFingers?100:46));time+=100;}
        for(int i=0;i<20;i++){engine.update(time,frame(time,90,wrongFingers?100:46));time+=100;}
        return engine;
    }
    public static void main(String[] args) {
        WordEngineCheck.require(replay(1,false,false).getStage()==WordPracticeEngine.Stage.END,"El cierre base debía rechazar una diferencia de 11 con margen 10");
        WordEngineCheck.require(WordStagesCheck.success(replay(1.15,false,false)),"El margen 1.15 no flexibilizó el cierre");
        WordEngineCheck.require(!WordStagesCheck.success(replay(1.15,true,false)),"Aceptó dedos claramente distintos");
        WordEngineCheck.require(!WordStagesCheck.success(replay(1.15,false,true)),"Aceptó saltarse el recorrido");
        for(double margin:new double[]{0,.85}) {
            var engine=replay(1,false,false,margin); // Recorrido hecho, cierre pendiente por flexión.
            for(long time=5300;time<6300;time+=100)engine.update(time,frame(time,130,35));
            WordEngineCheck.require(WordStagesCheck.success(engine)==(margin>0),"No separó zona final y recorrido");
        }
        WordEngineCheck.require(!WordStagesCheck.success(replay(1.15,false,true,.85)),"La zona final amplia permitió saltar el recorrido");
        for(double invalid:new double[]{.24,1.51,Double.NaN,Double.POSITIVE_INFINITY}) {
            var o=new WordStageOptions();o.finalPoseScale=invalid;
            boolean rejected=false;try{o.validate();}catch(IllegalArgumentException expected){rejected=true;}
            WordEngineCheck.require(rejected,"Aceptó finalPoseScale inválido");
        }
        for(double invalid:new double[]{-.01,2.01,Double.NaN,Double.POSITIVE_INFINITY}) {
            var o=new WordStageOptions();o.finalPositionTolerance=invalid;
            boolean rejected=false;try{o.validate();}catch(IllegalArgumentException expected){rejected=true;}
            WordEngineCheck.require(rejected,"Aceptó finalPositionTolerance inválido");
        }
        System.out.println("PASS: margen final, dedos distintos, salto al final y límites de configuración");
    }
}
