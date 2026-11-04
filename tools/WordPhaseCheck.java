import com.mechrobotix.aprendels.*;
import java.util.*;

/** Regresiones de Colores: la preparación, el ruido y un salto no son un recorrido. */
public final class WordPhaseCheck {
    private static WordPracticeEngine.Frame camera(WordPracticeEngine.Frame frame,long time,boolean noise,double scale,double dx,double dy) {
        var timed=new WordPracticeEngine.Frame(time,frame.hands);
        var sample=WordStagesCheck.camera(timed,frame.hands,1,noise,false);
        Map<String,JPracticeEngine.Sample> hands=new LinkedHashMap<>();
        for(var entry:sample.hands.entrySet()) {
            var h=entry.getValue();double[][] points=new double[21][2];
            for(int i=0;i<21;i++){points[i][0]=500+(h.points[i][0]-500)*scale+dx;points[i][1]=350+(h.points[i][1]-350)*scale+dy;}
            hands.put(entry.getKey(),new JPracticeEngine.Sample(points,h.flex,entry.getKey()));
        }
        return new WordPracticeEngine.Frame(time,hands);
    }
    private static void preparation(WordStagesCheck.Clip c,int variant) {
        var e=new WordPracticeEngine(c.keys,c.tolerance,true,c.options);int highest=0;
        for(int i=0;i<80;i++) {
            long time=1000+i*100;
            double progress=Math.min(1,i/30.0),scale=variant==2?.65+.35*progress:1;
            double dx=variant==2?80*progress:0,dy=variant==2?-100*progress:0;
            var f=camera(c.keys.get(0),time,variant==1,scale,dx,dy);
            e.update(time,f);highest=Math.max(highest,e.getProgress());
            if(i<3)WordEngineCheck.require(e.getProgress()==0,"Inicio sin confirmar: "+c.name);
        }
        WordEngineCheck.require(highest<=15 && !WordStagesCheck.success(e),"Preparación contó como recorrido: "+c.name+" caso="+variant+" progreso="+highest);
    }
    private static void jump(WordStagesCheck.Clip c,boolean noise) {
        var e=new WordPracticeEngine(c.keys,c.tolerance,true,c.options);long time=1000;
        for(int i=0;i<10;i++){e.update(time,camera(c.keys.get(0),time,false,1,0,0));time+=100;}
        int highest=e.getProgress();
        for(int i=0;i<80;i++) {
            e.update(time,camera(c.keys.get(c.keys.size()-1),time,noise,1,0,0));time+=100;
            highest=Math.max(highest,e.getProgress());
        }
        WordEngineCheck.require(highest<=15 && !WordStagesCheck.success(e),"Salto al final contó como recorrido: "+c.name+" ruido="+noise+" progreso="+highest);
    }
    static void check(WordStagesCheck.Clip c) {
        if(!c.options.phaseValidation)return;
        for(int variant=0;variant<3;variant++)preparation(c,variant);
        jump(c,false);jump(c,true);
        System.out.println("PHASE_PREPARATION\t"+c.name+"\t5/5\tprogressBound=15");
    }
    static void settings() {
        for(int stages=3;stages<=6;stages++) {
            var phase=new WordStageOptions();phase.phaseValidation=true;phase.stages=stages;
            boolean valid=true;try{phase.validate();}catch(IllegalArgumentException ex){valid=false;}
            WordEngineCheck.require(valid==(stages<=4),"Cantidad de fases inválida aceptada");
            var legacy=new WordStageOptions();legacy.stages=stages;legacy.validate();
        }
        for(long value:new long[]{0,119,1501}) {
            var phase=new WordStageOptions();phase.minMotionMs=value;
            boolean rejected=false;try{phase.validate();}catch(IllegalArgumentException ex){rejected=true;}
            WordEngineCheck.require(rejected,"Duración de movimiento inválida aceptada");
        }
        System.out.println("PASS: fases 3–4 y configuración de recorrido anterior 3–6");
    }
}
