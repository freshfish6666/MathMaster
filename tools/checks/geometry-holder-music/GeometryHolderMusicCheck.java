import com.freshfish.mathmaster.ritual.GeometryHolderMusicSequence;
import java.util.ArrayList;
import java.util.List;

public final class GeometryHolderMusicCheck {
    static int checks;
    static final class Sound implements GeometryHolderMusicSequence.Audio {
        final int track;
        boolean started=true,playing=true,canceled;
        double elapsed;
        float gain;
        Sound(int track) {this.track=track;}
        public boolean playing(){return playing&&!canceled;}
        public boolean started(){return started;}
        public double seconds(){return elapsed;}
        public void advance(double dt){if(started)elapsed+=dt;if(elapsed>=GeometryHolderMusicSequence.duration(track))playing=false;}
        public void volume(float v){require(Float.isFinite(v)&&v>=0&&v<=.80001,"bounded gain");gain=v;}
        public void cancel(){canceled=true;}
    }
    static final class Backend implements GeometryHolderMusicSequence.Player {
        final List<Sound> sounds=new ArrayList<>();
        boolean delayed;
        public Sound play(int track){var s=new Sound(track);s.started=!delayed;sounds.add(s);return s;}
        Sound latest(){return sounds.get(sounds.size()-1);}
    }
    static void require(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static void tick(GeometryHolderMusicSequence s,int phase,boolean dead,boolean range,int n){for(int i=0;i<n;i++)s.tick(phase,dead,range,.05);}
    public static void main(String[] args){
        var b=new Backend();var s=new GeometryHolderMusicSequence(b);
        tick(s,0,false,false,10);require(b.sounds.isEmpty(),"no phantom music");
        tick(s,1,false,true,45);var first=b.latest();require(first.track==1&&Math.abs(first.gain-.8)<.0001,"phase one fades in");
        b.delayed=true;tick(s,2,false,true,10);var second=b.latest();require(second.track==2&&second.gain==0&&!first.canceled&&first.gain>.79,"decode wait retains first stage");
        second.started=true;tick(s,2,false,true,20);
        require(first.gain>.5&&second.gain>.5,"equal-power midpoint avoids a volume hole");
        tick(s,2,false,true,22);require(first.canceled&&second.gain>.79,"phase transition completes");
        int count=b.sounds.size();tick(s,2,false,true,20);require(b.sounds.size()==count,"no duplicate restart each tick");
        b.delayed=false;tick(s,0,true,true,1);var outro=b.latest();require(outro.track==4,"stage two defeat chooses its own ending");
        tick(s,0,true,true,26);require(second.canceled&&outro.gain>.79,"defeat dissolves combat into ending");
        tick(s,0,true,true,150);require(!s.running()&&outro.canceled&&b.sounds.size()==count+1,"outro finishes once without loop");
        tick(s,2,false,true,45);require(b.latest().track==2,"late tracking begins directly in stage two");
        var leaving=b.latest();tick(s,0,false,false,35);require(!s.running()&&leaving.canceled,"unload and range exit fade, no victory");
        tick(s,1,false,true,45);count=b.sounds.size();tick(s,1,false,true,1550);
        require(b.sounds.size()>count&&s.running()&&b.latest().track==1,"combat loops before natural ending");
        s.reset();require(b.sounds.stream().allMatch(v->v.canceled),"reset cancels all voices");
        tick(s,1,false,true,45);tick(s,2,false,true,10);tick(s,0,true,true,1);
        require(b.latest().track==4,"death during phase fade uses second ending");
        tick(s,0,true,true,150);require(!s.running(),"interrupted transition fully clears");
        tick(s,1,false,true,45);tick(s,0,true,true,1);require(b.latest().track==3,"first-stage defeat ending");
        tick(s,2,false,true,45);require(b.latest().track==2&&s.running(),"another boss takes over ending");
        s.reset();tick(s,1,false,true,45);var fallback=b.latest();b.delayed=true;tick(s,2,false,true,1);
        b.latest().playing=false;tick(s,2,false,true,105);require(!fallback.canceled&&s.running(),"failed stream retains audible fallback");
        b.delayed=false;tick(s,2,false,true,90);require(b.latest().track==2&&b.latest().started&&b.latest().gain>.79,"failed phase transition retries");
        s.reset();require(!s.running(),"disconnect / dimension / player death / mute reset");
        b.delayed=false;tick(s,1,false,true,45);b.delayed=true;tick(s,0,true,true,300);
        require(!s.running(),"missing defeat stream has bounded lifetime");
        s.reset();b.delayed=false;tick(s,1,false,true,45);var held=b.latest();b.delayed=true;
        tick(s,2,false,true,110);require(!held.canceled&&s.running(),"hung decoder also falls back after five seconds");
        b.delayed=false;tick(s,2,false,true,95);require(b.latest().track==2&&b.latest().gain>.79,"hung decoder retries without an infinite wait");
        s.reset();tick(s,1,false,true,45);b.latest().elapsed=78;
        count=b.sounds.size();s.tick(1,false,true,1.3);
        require(b.sounds.size()==count+1,"lagged frame uses real playback time for loop boundary");
        s.reset();
        System.out.println("PASS: "+checks+" boss music assertions");
    }
}
