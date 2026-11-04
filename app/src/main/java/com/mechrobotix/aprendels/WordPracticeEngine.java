package com.mechrobotix.aprendels;

import java.util.*;

/** Palabras: fases amplias 3–4 opcionales o recorrido ordenado 3–6 con el perfil anterior. */
public final class WordPracticeEngine {
    public static final class Frame {
        public final long timeMs;
        public final Map<String,JPracticeEngine.Sample> hands;
        public Frame(long time,Map<String,JPracticeEngine.Sample> hands) {
            timeMs=time;this.hands=Collections.unmodifiableMap(new LinkedHashMap<>(hands));
        }
        public boolean valid(){return !hands.isEmpty() && hands.size()<=2 && hands.values().stream().allMatch(JPracticeEngine.Sample::valid);}
    }
    public enum Stage { START, MOVE, POSE, END, SUCCESS }
    private final WordTolerance tolerance;
    private final WordStageOptions options;
    private final WordStagePlan plan;
    private final boolean mirror;
    private final List<Frame> route;
    private Frame routeLastMatch;
    private final Map<String,JPracticeEngine.Sample> routeReferenceStarts=new HashMap<>(),routeActualStarts=new HashMap<>();
    private int routeTarget;
    private Stage stage=Stage.START;
    private String message;
    private int target=0,motionSamples=0;
    private boolean swap=false;
    private long last=-1,began=-1,lost=-1,lastMotionTime=-1,lastGood=-1,held=0,lastMatch=-1;
    private Frame anchor,referenceAnchor,motionPrevious,stable;
    private double excursion=0;
    private long phaseMotionMs=0;
    private double phaseRequiredMovement=0;
    private boolean phaseMotionConfirmed=false;
    private final Deque<Frame> phaseWindow=new ArrayDeque<>();
    private long phaseLastEvidence=-1;
    private boolean phasePoseSeen=false;
    private int phasePoseSamples=0;
    private long phasePoseChecked=-1,phasePoseLastGood=-1;
    private boolean phaseMoveGoalSeen=false;
    private Frame phaseMoveGoalReference;

    public WordPracticeEngine(List<Frame> keys,WordTolerance tolerance,boolean mirror) {
        this(keys,tolerance,mirror,new WordStageOptions());
    }
    public WordPracticeEngine(List<Frame> keys,WordTolerance tolerance,boolean mirror,WordStageOptions options) {
        if(keys==null || keys.size()<4)throw new IllegalArgumentException("Referencia incompleta");
        for(Frame f:keys)if(f==null || !f.valid())throw new IllegalArgumentException("Referencia sin manos");
        tolerance.validate();options.validate();this.tolerance=tolerance;this.options=options;this.mirror=mirror;
        plan=new WordStagePlan(keys,options.stages,options.phaseValidation);route=Collections.unmodifiableList(new ArrayList<>(keys));reset();
    }
    public static double palmSize(JPracticeEngine.Sample s) {
        double size=0;
        for(int[] pair:new int[][]{{0,5},{0,9},{0,13},{0,17},{5,17}})
            size=Math.max(size,JPracticeEngine.distance(s.points[pair[0]],s.points[pair[1]]));
        return Math.max(2,size);
    }
    public static double change(Frame a,Frame b) {
        double max=0;
        for(String side:a.hands.keySet()) {
            JPracticeEngine.Sample p=a.hands.get(side),q=b.hands.get(side);if(q==null)continue;
            double sum=0;for(int i=0;i<21;i++)sum+=JPracticeEngine.distance(p.points[i],q.points[i])/palmSize(p);
            max=Math.max(max,sum/21);
        }return max;
    }
    public Stage getStage(){return stage;}
    public String getMessage(){return message;}
    public int getProgress(){
        if(stage==Stage.SUCCESS)return 100;
        if(options.phaseValidation)return stage==Stage.START?0:stage==Stage.MOVE?15:stage==Stage.POSE?50:plan.goals.size()==3?75:80;
        return target*100/plan.goals.size();
    }
    public int getTarget(){return target;}
    public int getKeyCount(){return plan.goals.size();}
    public void reset() {
        stage=Stage.START;target=0;swap=false;
        routeLastMatch=null;routeTarget=1;routeReferenceStarts.clear();routeActualStarts.clear();
        last=began=lost=lastMotionTime=lastGood=lastMatch=-1;
        held=0;motionSamples=0;excursion=0;anchor=referenceAnchor=motionPrevious=stable=null;
        phaseMotionMs=0;phaseRequiredMovement=0;phaseMotionConfirmed=false;
        phaseWindow.clear();phaseLastEvidence=-1;
        phasePoseSeen=false;phasePoseSamples=0;phasePoseChecked=phasePoseLastGood=-1;
        phaseMoveGoalSeen=false;phaseMoveGoalReference=null;
        message=label()+"Imita la posición inicial";
    }
    private String label(){return (target+1)+"/"+plan.goals.size()+" · ";}
    private String side(String ref){return swap?(ref.equals("Left")?"Right":"Left"):ref;}
    private JPracticeEngine.Sample hand(Frame f,String ref){return f==null?null:f.hands.get(side(ref));}
    private double direction(){return mirror?-1:1;}
    private boolean hasHands(Frame actual,Frame ref){for(String s:ref.hands.keySet())if(hand(actual,s)==null)return false;return true;}
    private void clearHold(){held=0;lastGood=lastMatch=-1;stable=null;}
    private void mismatch(long time) {
        lastGood=-1;
        if(lastMatch<0 || time-lastMatch>options.matchGraceMs)clearHold();
    }
    private boolean hold(long time,Frame sample,long required) {
        if(stable!=null && !(options.phaseValidation && stage==Stage.POSE) && change(stable,sample)>tolerance.stability)clearHold();
        if(stable==null)stable=sample;
        if(lastGood>=0)held+=Math.min(150,time-lastGood); // Solo intervalos con postura válida.
        lastGood=lastMatch=time;
        return held>=required;
    }
    public void missing(long time,String reason) {
        if(stage==Stage.SUCCESS)return;
        if(last>=0 && time<=last)return;
        last=time;
        if(lost<0)lost=time;
        clearHold();motionPrevious=null;lastMotionTime=-1; // No contar un salto tras la oclusión.
        if(options.phaseValidation && !phaseMotionConfirmed){motionSamples=0;phaseMotionMs=0;excursion=0;phaseWindow.clear();phaseLastEvidence=-1;}
        if(time-lost>tolerance.lostGraceMs){reset();last=time;message="Se perdió la mano. Repite desde el inicio";}
        else message=label()+reason+"; conserva la etapa y recupera la mano";
    }
    private double posture(Frame actual,Frame ref) {
        if(!hasHands(actual,ref))return Double.POSITIVE_INFINITY;
        double score=0;
        for(var entry:ref.hands.entrySet()) {
            JPracticeEngine.Sample a=hand(actual,entry.getKey()),r=entry.getValue();
            double shape=0,flex=0,worst=0;
            // Distinguir una mano abierta de varios dedos cerrados sin exigir ángulos exactos.
            // Una zona neutral amplia admite variación; el pulgar no se clasifica.
            int mismatches=0;
            for(int finger=1;finger<5;finger++) {
                double rf=(r.flex[finger*2]+r.flex[finger*2+1])/2;
                double af=(a.flex[finger*2]+a.flex[finger*2+1])/2;
                if((rf<25 && af>60) || (rf>50 && af<20))mismatches++;
            }
            if(mismatches>options.maxFingerMismatches)return Double.POSITIVE_INFINITY;
            for(int i=1;i<21;i++)shape+=Math.hypot(
                (a.points[i][0]-a.points[0][0])/palmSize(a)-direction()*(r.points[i][0]-r.points[0][0])/palmSize(r),
                (a.points[i][1]-a.points[0][1])/palmSize(a)-(r.points[i][1]-r.points[0][1])/palmSize(r));
            for(int i=0;i<10;i++){double d=Math.abs(a.flex[i]-r.flex[i]);flex+=d;worst=Math.max(worst,d);}
            score=Math.max(score,Math.max(shape/20/tolerance.shape,Math.max(flex/10/tolerance.meanFlex,worst/tolerance.maxFlex)));
        }
        // Posición relativa entre manos; no impone una coordenada fija en pantalla.
        if(ref.hands.size()==2) {
            String first=ref.hands.keySet().iterator().next();
            JPracticeEngine.Sample a0=hand(actual,first),r0=ref.hands.get(first);
            for(String s:ref.hands.keySet()) {
                JPracticeEngine.Sample a=hand(actual,s),r=ref.hands.get(s);
                double dx=(a.points[0][0]-a0.points[0][0])/palmSize(a0)-direction()*(r.points[0][0]-r0.points[0][0])/palmSize(r0);
                double dy=(a.points[0][1]-a0.points[0][1])/palmSize(a0)-(r.points[0][1]-r0.points[0][1])/palmSize(r0);
                score=Math.max(score,Math.hypot(dx,dy)/(tolerance.travel+1));
            }
        }return score;
    }
    private void observeMotion(long time,Frame sample) {
        if(options.phaseValidation){observePhaseMotion(time,sample);return;}
        if(anchor==null)return;
        excursion=Math.max(excursion,change(anchor,sample));
        if(motionPrevious==null){motionPrevious=sample;lastMotionTime=time;return;}
        if(time-lastMotionTime<60)return;
        if(change(motionPrevious,sample)>=Math.max(options.motionNoise,tolerance.minMotion)) {
            motionSamples++;motionPrevious=sample;lastMotionTime=time;
        }
    }
    /** Cambio de gesto: elimina traslación y escala comunes de la entrada en cámara.
     * Conserva cambios de dedos/orientación y de la separación relativa entre manos.
     */
    public static double phaseChange(Frame a,Frame b) {
        double change=0;
        int commonHands=0;String onlyCommonHand=null;
        for(String side:a.hands.keySet()) {
            var p=a.hands.get(side);var q=b.hands.get(side);if(q==null)continue;
            commonHands++;onlyCommonHand=side;
            double shape=0,flex=0,ps=palmSize(p),qs=palmSize(q);
            for(int i=1;i<21;i++)shape+=Math.hypot(
                (p.points[i][0]-p.points[0][0])/ps-(q.points[i][0]-q.points[0][0])/qs,
                (p.points[i][1]-p.points[0][1])/ps-(q.points[i][1]-q.points[0][1])/qs);
            for(int i=0;i<10;i++)flex+=Math.abs(p.flex[i]-q.flex[i]);
            change=Math.max(change,Math.max(shape/20,flex/1200));
        }
        if(a.hands.size()==2 && b.hands.size()==2 && a.hands.keySet().equals(b.hands.keySet())) {
            var sides=new ArrayList<>(a.hands.keySet());Collections.sort(sides);
            var a0=a.hands.get(sides.get(0));var a1=a.hands.get(sides.get(1));
            var b0=b.hands.get(sides.get(0));var b1=b.hands.get(sides.get(1));
            double sa=(palmSize(a0)+palmSize(a1))/2,sb=(palmSize(b0)+palmSize(b1))/2;
            change=Math.max(change,Math.hypot(
                (a1.points[0][0]-a0.points[0][0])/sa-(b1.points[0][0]-b0.points[0][0])/sb,
                (a1.points[0][1]-a0.points[0][1])/sa-(b1.points[0][1]-b0.points[0][1])/sb));
        }
        // Si solo hay una mano común, su desplazamiento de muñeca también forma
        // parte del gesto. Con dos manos se conserva la comparación relativa para
        // no contar como seña que ambas se trasladen juntas al encuadrarse.
        if(commonHands==1) {
            var p=a.hands.get(onlyCommonHand);var q=b.hands.get(onlyCommonHand);
            double scale=(palmSize(p)+palmSize(q))/2;
            change=Math.max(change,Math.hypot(q.points[0][0]-p.points[0][0],q.points[0][1]-p.points[0][1])/scale);
        }
        return change;
    }
    private void observePhaseMotion(long time,Frame sample) {
        if(anchor==null || stage!=Stage.MOVE || phaseMotionConfirmed)return;
        Frame previous=phaseWindow.peekLast();
        if(previous!=null && time-previous.timeMs<60)return;
        phaseWindow.addLast(new Frame(time,sample.hands));
        while(phaseWindow.size()>16 || time-phaseWindow.peekFirst().timeMs>1500)phaseWindow.removeFirst();
        if(previous==null || phaseWindow.size()<3 || phaseChange(previous,sample)<options.motionNoise*.5)return;
        var window=new ArrayList<>(phaseWindow);
        long span=0;double evidence=0;
        // Buscar un cambio distribuido entre observaciones reales. Un salto grande
        // seguido de temblor no cuenta: ninguna transición puede explicar todo el gesto.
        for(int first=0;first+2<window.size();first++) {
            double net=phaseChange(window.get(first),sample),path=0,largest=0;
            if(net<phaseRequiredMovement)continue;
            for(int i=first+1;i<window.size();i++) {
                double step=phaseChange(window.get(i-1),window.get(i));path+=step;largest=Math.max(largest,step);
            }
            if(path>0 && net/path>=.5 && largest<=net*.6) {
                span=time-window.get(first).timeMs;evidence=net;break;
            }
        }
        if(span<120)return;
        if(phaseLastEvidence<0 || time-phaseLastEvidence>Math.max(400,options.matchGraceMs)) {
            motionSamples=0;phaseMotionMs=Math.min(150,span);
        }else phaseMotionMs+=Math.min(150,time-phaseLastEvidence);
        phaseLastEvidence=time;motionSamples++;excursion=Math.max(excursion,evidence);
    }
    /** Región de la postura, no trayectoria fotograma por fotograma. */
    private boolean phaseRegion(Frame actual,Frame ref) {
        if(!hasHands(actual,ref))return false;
        if(ref.hands.size()<2)return true;
        var sides=new ArrayList<>(ref.hands.keySet());Collections.sort(sides);
        var r0=ref.hands.get(sides.get(0));var r1=ref.hands.get(sides.get(1));
        var a0=hand(actual,sides.get(0));var a1=hand(actual,sides.get(1));
        double rs=(palmSize(r0)+palmSize(r1))/2,as=(palmSize(a0)+palmSize(a1))/2;
        return Math.hypot(
            (a1.points[0][0]-a0.points[0][0])/as-direction()*(r1.points[0][0]-r0.points[0][0])/rs,
            (a1.points[0][1]-a0.points[0][1])/as-(r1.points[0][1]-r0.points[0][1])/rs)<=options.phasePositionTolerance;
    }
    /** El cambio clave puede verse durante el recorrido, antes de que se confirme
     * toda su evidencia. Recordarlo evita exigir que la persona vuelva a una pose pasada.
     */
    private void observePhasePose() {
        if(!options.phaseValidation || plan.goals.size()!=4 || stage!=Stage.MOVE ||
           (motionSamples==0 && !(options.pathTolerance>0 && routeTarget>1)) || phasePoseSeen)return;
        var goal=plan.goals.get(2);
        for(Frame actual:phaseWindow) {
            if(actual.timeMs<=phasePoseChecked)continue;
            phasePoseChecked=actual.timeMs;boolean matches=false;
            for(Frame ref:goal.alternatives) {
                Frame[] common=commonHands(actual,ref);if(common==null)continue;
                double minimum=Math.max(.06,Math.min(.15,phaseChange(referenceAnchor,common[1])*.3));
                if(phaseChange(anchor,common[0])>=minimum && posture(common[0],common[1])<=1 &&
                   phaseRegion(common[0],common[1]) && movement(common[0],common[1],false)){matches=true;break;}
            }
            if(!matches)continue;
            if(phasePoseLastGood<0 || actual.timeMs-phasePoseLastGood>options.matchGraceMs)phasePoseSamples=0;
            phasePoseLastGood=actual.timeMs;phasePoseSamples++;
            if(phasePoseSamples>=2){phasePoseSeen=true;return;}
        }
    }
    private Frame[] commonHands(Frame actual,Frame ref) {
        Map<String,JPracticeEngine.Sample> actualHands=new LinkedHashMap<>(),referenceHands=new LinkedHashMap<>();
        for(String side:ref.hands.keySet()) {
            var a=hand(actual,side);if(a==null)continue;
            actualHands.put(side,a);referenceHands.put(side,ref.hands.get(side));
        }
        if(actualHands.isEmpty())return null;
        return new Frame[]{new Frame(actual.timeMs,actualHands),new Frame(ref.timeMs,referenceHands)};
    }
    /** Solo magnitud/dirección y una región amplia; NO forma de dedos durante desplazamiento. */
    private boolean movement(Frame actual,Frame ref,boolean requireSamples) {
        if(!hasHands(actual,ref))return false;
        double expected=change(referenceAnchor,ref);
        double required=Math.max(options.minDisplacement,Math.min(1.2,expected*tolerance.minMotionRatio));
        if(requireSamples && (motionSamples<options.motionSamples || excursion<required))return false;
        for(String s:ref.hands.keySet()) {
            JPracticeEngine.Sample r0=referenceAnchor.hands.get(s),r=ref.hands.get(s),a0=hand(anchor,s),a=hand(actual,s);
            if(r0==null || a0==null)continue;
            double rx=direction()*(r.points[0][0]-r0.points[0][0])/palmSize(r0),ry=(r.points[0][1]-r0.points[0][1])/palmSize(r0);
            double ax=(a.points[0][0]-a0.points[0][0])/palmSize(a0),ay=(a.points[0][1]-a0.points[0][1])/palmSize(a0);
            double rn=Math.hypot(rx,ry),an=Math.hypot(ax,ay);
            if(rn>.6) {
                if(an<.08 || (rx*ax+ry*ay)/(rn*an)<tolerance.directionCosine)return false;
                if((rx*ax+ry*ay)/(rn*rn)<tolerance.minTravelProgress)return false;
            }
            if(Math.hypot(ax-rx,ay-ry)>tolerance.travel+rn*tolerance.travelRelative)return false;
        }return true;
    }
    /** Recorrido ordenado independiente de las anclas flexibles de las etapas visibles.
     * Normaliza respecto al inicio para que aprobar una etapa antes no acorte la siguiente.
     * Solo se usa cuando pathTolerance > 0; no sigue el reloj del video.
     */
    private void startRoute(Frame actual,Frame reference) {
        routeLastMatch=actual;
        Frame routeOrigin=options.phaseValidation?reference:route.get(0);
        for(String side:routeOrigin.hands.keySet()) {
            routeReferenceStarts.put(side,routeOrigin.hands.get(side));
            routeActualStarts.put(side,hand(actual,side));
        }
    }
    private boolean routeReached(Frame goal) {
        return routeTarget>=route.size() || route.get(routeTarget).timeMs>goal.timeMs;
    }
    private void observeRoute(Frame actual) {
        if(options.pathTolerance<=0 || routeLastMatch==null)return;
        while(routeTarget<route.size()) {
            Frame ref=route.get(routeTarget);
            if(options.phaseValidation?!hasAnyHand(actual,ref):!hasHands(actual,ref))return;
            // Una mano nueva obtiene su propia ancla al entrar en el recorrido.
            for(String side:ref.hands.keySet())if(!routeReferenceStarts.containsKey(side)) {
                routeReferenceStarts.put(side,ref.hands.get(side));
                routeActualStarts.put(side,hand(actual,side));
            }
            for(String side:ref.hands.keySet())if(routeActualStarts.get(side)==null && hand(actual,side)!=null)
                routeActualStarts.put(side,hand(actual,side));
            if(!routeMatches(actual,ref))return;
            // Un mismo cuadro inmóvil no puede consumir varios hitos distintos.
            // Los hitos casi iguales de una postura sostenida sí pueden agruparse.
            if(change(route.get(routeTarget-1),ref)>=options.minDisplacement &&
               change(routeLastMatch,actual)<Math.max(options.motionNoise,tolerance.minMotion))return;
            routeLastMatch=actual;routeTarget++;
        }
    }
    private boolean hasAnyHand(Frame actual,Frame ref) {
        for(String side:ref.hands.keySet())if(hand(actual,side)!=null)return true;
        return false;
    }
    private boolean phaseRouteComplete(){return options.pathTolerance<=0 || routeTarget>=route.size();}
    private boolean routeMatches(Frame actual,Frame ref) {
        return routeMatches(actual,ref,options.pathTolerance);
    }
    private boolean routeMatches(Frame actual,Frame ref,double margin) {
        if(options.phaseValidation)return phaseRouteMatches(actual,ref,margin);
        if(!hasHands(actual,ref))return false;
        for(String side:ref.hands.keySet()) {
            // Perfil opcional: todas las manos usan el origen inicial de la mano principal.
            // Evita anclar una mano nueva a una fase anterior del movimiento.
            String origin=options.sharedRouteOrigin?route.get(0).hands.keySet().iterator().next():side;
            var r0=routeReferenceStarts.get(origin);var a0=routeActualStarts.get(origin);
            var r=ref.hands.get(side);var a=hand(actual,side);
            if(r0==null || a0==null)return false;
            double rx=direction()*(r.points[0][0]-r0.points[0][0])/palmSize(r0);
            double ry=(r.points[0][1]-r0.points[0][1])/palmSize(r0);
            double ax=(a.points[0][0]-a0.points[0][0])/palmSize(a0);
            double ay=(a.points[0][1]-a0.points[0][1])/palmSize(a0);
            if(Math.hypot(ax-rx,ay-ry)>margin)return false;
        }
        return true;
    }
    /** En Colores una mano de apoyo puede perderse durante algunos cuadros.
     * Se comparan las manos visibles y se exige que al menos una corresponda al
     * hito; el inicio y el cierre mantienen sus comprobaciones completas.
     */
    private boolean phaseRouteMatches(Frame actual,Frame ref,double margin) {
        String origin=options.sharedRouteOrigin?route.get(0).hands.keySet().iterator().next():null;
        boolean matched=false;
        for(String side:ref.hands.keySet()) {
            var a=hand(actual,side);if(a==null)continue;
            String base=origin==null?side:origin;
            var r0=routeReferenceStarts.get(base);var a0=routeActualStarts.get(base);
            var r=ref.hands.get(side);
            if(r0==null || a0==null)continue;
            double rx=direction()*(r.points[0][0]-r0.points[0][0])/palmSize(r0);
            double ry=(r.points[0][1]-r0.points[0][1])/palmSize(r0);
            double ax=(a.points[0][0]-a0.points[0][0])/palmSize(a0);
            double ay=(a.points[0][1]-a0.points[0][1])/palmSize(a0);
            if(Math.hypot(ax-rx,ay-ry)<=margin)matched=true;
        }
        return matched;
    }
    private void advance(long time,Frame actual,Frame ref) {
        if(!options.phaseValidation || anchor==null) {
            anchor=actual;referenceAnchor=ref;
            if(options.phaseValidation){
                double expected=0;for(Frame key:route)expected=Math.max(expected,phaseChange(ref,key));
                phaseRequiredMovement=Math.max(options.minDisplacement,Math.min(.45,expected*tolerance.minMotionRatio));
            }
        }
        motionPrevious=actual;lastMotionTime=time;motionSamples=0;phaseMotionMs=0;excursion=0;clearHold();
        if(options.phaseValidation){phaseWindow.clear();phaseWindow.addLast(new Frame(time,actual.hands));phaseLastEvidence=-1;}
        target++;lost=-1;
        if(target==plan.goals.size()){stage=Stage.SUCCESS;message="¡Palabra completada!";return;}
        var kind=plan.goals.get(target).kind;
        stage=kind==WordStagePlan.Kind.END?Stage.END:kind==WordStagePlan.Kind.POSE?Stage.POSE:Stage.MOVE;
        message=label()+(stage==Stage.END?"Llega a la posición final":stage==Stage.POSE?"Cambia la posición de dedos y manos":"Continúa el desplazamiento del video");
    }
    public void update(long time,Frame sample) {
        if(stage==Stage.SUCCESS || (last>=0 && time<=last))return;
        if(sample==null || !sample.valid()){missing(time,"Coloca las manos en la cámara");return;}
        if(last>=0 && time-last>tolerance.lostGraceMs){reset();last=time;message="Se perdió la continuidad. Repite desde el inicio";return;}
        if(lost>=0 && time-lost>tolerance.lostGraceMs){reset();last=time;message="Se perdió la mano. Repite desde el inicio";return;}
        var goal=plan.goals.get(target);
        if(stage==Stage.START) {
            boolean previousSwap=swap,chosen=false;double best=Double.POSITIVE_INFINITY;Frame reference=goal.center;
            for(boolean assignment:new boolean[]{false,true}) {
                swap=assignment;
                for(Frame ref:goal.alternatives){double s=posture(sample,ref);if(s<best && (!options.phaseValidation || phaseRegion(sample,ref))){best=s;reference=ref;chosen=assignment;}}
            }
            swap=chosen;last=time;
            if(best>1){mismatch(time);message=label()+"Ajusta la posición inicial de dedos y manos";return;}
            lost=-1;if(previousSwap!=swap)clearHold();
            message=label()+"Bien, mantén un instante el inicio";
            if(hold(time,sample,tolerance.startHoldMs)){if(options.pathTolerance>0)startRoute(sample,reference);began=time;advance(time,sample,reference);}
            return;
        }
        if(time-began>tolerance.maxAttemptMs){reset();last=time;message="Vamos otra vez desde el inicio";return;}
        if(options.pathTolerance>0)observeRoute(sample);
        if(options.phaseValidation && stage==Stage.POSE && phasePoseSeen) {
            if(options.pathTolerance>0 && !phaseRouteComplete()) {
                last=time;lost=-1;message=label()+"Continúa el recorrido antes de la posición final";return;
            }
            last=time;lost=-1;advance(time,sample,goal.center);return;
        }
        // El recorrido se registra aunque el siguiente control visible aún no tenga
        // todas sus manos. En Colores basta una mano visible que coincida con el hito;
        // el inicio y el cierre sí conservan las manos que exige cada postura.
        if(!hasHands(sample,goal.center)) {
            boolean missingEstablished=false;
            for(String s:goal.center.hands.keySet())if(hand(sample,s)==null && referenceAnchor.hands.containsKey(s) && hand(anchor,s)!=null)missingEstablished=true;
            if(missingEstablished && options.phaseValidation) {
                // Conservar observaciones reales de la mano visible durante una oclusión
                // breve. Para aprobar la postura siguen siendo necesarias sus dos manos.
                last=time;if(lost<0)lost=time;clearHold();observeMotion(time,sample);observePhasePose();
                message=label()+"Recupera la mano que estaba participando; conserva la fase";
            }
            else if(missingEstablished)missing(time,"Recupera la mano que estaba participando");
            else {
                last=time;clearHold();
                // No iniciar la cuenta de pérdida para una mano que aún no entró en esta fase.
                message=label()+"Introduce la otra mano cuando lo indique el video";
                if(sample.valid()){observeMotion(time,sample);observePhasePose();}
            }
            return;
        }
        last=time;lost=-1;observeMotion(time,sample);observePhasePose();
        if(stage==Stage.END && options.pathTolerance>0 && !options.phaseValidation) {
            if(!routeReached(goal.center)) {
                clearHold();message=label()+"Completa el último recorrido antes de mantener el final";return;
            }
            double finalMargin=options.finalPositionTolerance>0?options.finalPositionTolerance:options.pathTolerance;
            if(!routeMatches(sample,goal.center,finalMargin)) {
                clearHold();message=label()+"Acerca las manos a la zona final del video";return;
            }
        }
        if(stage==Stage.MOVE) {
            boolean routeMilestone=options.phaseValidation && options.pathTolerance>0 &&
                (plan.goals.size()==3?phaseRouteComplete():routeReached(goal.center));
            boolean measuredMotion=motionSamples>=options.motionSamples && phaseMotionMs>=options.minMotionMs &&
                excursion>=phaseRequiredMovement;
            if(options.phaseValidation && !measuredMotion && !routeMilestone) {
                message=label()+"Inicio reconocido. Realiza el movimiento de la seña";return;
            }
            if(options.phaseValidation) {
                for(Frame ref:goal.alternatives)if(movement(sample,ref,false) && phaseRegion(sample,ref) && posture(sample,ref)<=1) {
                    phaseMoveGoalSeen=true;phaseMoveGoalReference=ref;break;
                }
                boolean movementSeen=phaseMoveGoalSeen || routeMilestone;
                if(movementSeen) {
                    boolean routeReady=options.pathTolerance<=0 ||
                        (plan.goals.size()==3?phaseRouteComplete():routeMilestone);
                    if(!routeReady) {
                        message=label()+"Completa el recorrido de la seña";return;
                    }
                    phaseMotionConfirmed=true;
                    advance(time,sample,phaseMoveGoalReference==null?goal.center:phaseMoveGoalReference);return;
                }
                message=label()+"Continúa el movimiento hacia la siguiente posición";return;
            }
            for(Frame ref:goal.alternatives)if(movement(sample,ref,true)){advance(time,sample,ref);return;}
            message=label()+(motionSamples<options.motionSamples?"Haz un desplazamiento continuo; puedes cambiar los dedos":
                excursion<options.minDisplacement?"Desplaza un poco más la mano":"Lleva la mano hacia la siguiente posición del video");
            return;
        }
        Frame best=null;double score=Double.POSITIVE_INFINITY;
        for(Frame ref:(stage==Stage.END && options.pathTolerance>0 && !options.phaseValidation?Collections.singletonList(goal.center):goal.alternatives)) {
            double s=posture(sample,ref);
            if(s<score && movement(sample,ref,false) && (!options.phaseValidation || phaseRegion(sample,ref))){score=s;best=ref;}
        }
        if(best==null || score>(stage==Stage.END?options.finalPoseScale:1)) {
            mismatch(time);
            message=label()+(best==null?(stage==Stage.END?"Lleva la mano a la zona final del video":"Acerca la mano a la siguiente posición"):
                (stage==Stage.END?"Ajusta la posición final de dedos y manos":"Haz el cambio de dedos y orientación del video"));
            return;
        }
        if(options.phaseValidation && stage==Stage.END && options.pathTolerance>0 && !phaseRouteComplete()) {
            clearHold();message=label()+"Completa el recorrido antes de validar la posición final";return;
        }
        long required=stage==Stage.END?tolerance.endHoldMs:options.poseHoldMs;
        message=label()+(stage==Stage.END?"Bien, mantén un instante el final":"Bien, cambio de posición reconocido");
        if(hold(time,sample,required) && (stage!=Stage.END || time-began>=tolerance.minAttemptMs)) {
            if(options.phaseValidation && stage==Stage.POSE && options.pathTolerance>0 && !phaseRouteComplete()) {
                phasePoseSeen=true;message=label()+"Continúa el recorrido antes de la posición final";return;
            }
            advance(time,sample,best);
        }
    }
}
