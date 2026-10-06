package com.freshfish.mathmaster.client;

/** Pixel geometry shared by the live HUD and the standalone preview. No textures or game state. */
public final class GeometryHolderBarStyle {
    private GeometryHolderBarStyle() {}
    @FunctionalInterface public interface Canvas { void fill(int x1,int y1,int x2,int y2,int color); }
    public static void draw(Canvas canvas,int x,int y,int width,float progress,float phase) {
        int rim=blend(0xFF789CA8,0xFF76404B,phase);
        int glow=blend(0xFF7EEFFF,0xFFFF4258,phase);
        int deep=blend(0xFF17516A,0xFF64182B,phase);
        canvas.fill(x-2,y,width+x+2,y+15,0xD509101C);
        canvas.fill(x,y+1,x+width,y+14,0xFF24343E);
        canvas.fill(x+1,y+2,x+width-1,y+13,0xFF0B1420);
        canvas.fill(x+2,y+1,x+width-2,y+2,rim);
        canvas.fill(x+2,y+13,x+width-2,y+14,rim);
        canvas.fill(x,y+3,x+1,y+12,rim);
        canvas.fill(x+width-1,y+3,x+width,y+12,rim);
        int fill=Math.round(Math.max(0,Math.min(1,progress))*(width-8));
        if(fill>0) {
            canvas.fill(x+4,y+4,x+4+fill,y+11,deep);
            canvas.fill(x+4,y+4,x+4+fill,y+6,glow);
            canvas.fill(x+4,y+6,x+4+fill,y+9,blend(glow,deep,.35F));
            canvas.fill(x+4,y+11,x+4+fill,y+12,blend(0x553BCDEB,0x55ED354A,phase));
            canvas.fill(x+3+fill,y+4,x+4+fill,y+11,0xFFDFF7FF);
        }
        for(int i=1;i<10;i++) {
            int mark=x+4+(width-8)*i/10;
            canvas.fill(mark,y+3,mark+1,y+5,0x99445660);
            canvas.fill(mark,y+10,mark+1,y+12,0x99445660);
        }
        // Broken angular wings and small suspended shards echo the boss's square halo.
        for(int side:new int[]{-1,1}) {
            int end=side<0 ? x : x+width;
            for(int i=0;i<5;i++) {
                int px=end+side*(4+i);
                canvas.fill(px,y+2+i,px+2,y+4+i,rim);
                canvas.fill(px,y+11-i,px+2,y+13-i,rim);
            }
            int shard=end+side*13;
            canvas.fill(shard,y+5,shard+2,y+10,glow);
        }
        // Hollow cube medallion below the center; its ribs remain crisp at GUI scale.
        int cx=x+width/2,cy=y+19;
        for(int i=0;i<=6;i++) {
            int step=Math.round(i*.5F);
            canvas.fill(cx-i,cy-5+step,cx-i+1,cy-4+step,rim);
            canvas.fill(cx+i,cy-5+step,cx+i+1,cy-4+step,rim);
            canvas.fill(cx-i,cy+1-step,cx-i+1,cy+2-step,glow);
            canvas.fill(cx+i,cy+1-step,cx+i+1,cy+2-step,glow);
            canvas.fill(cx-i,cy+8-step,cx-i+1,cy+9-step,glow);
            canvas.fill(cx+i,cy+8-step,cx+i+1,cy+9-step,glow);
        }
        canvas.fill(cx-6,cy-2,cx-5,cy+6,rim);
        canvas.fill(cx+6,cy-2,cx+7,cy+6,rim);
        canvas.fill(cx,cy+1, cx+1,cy+8,glow);
        canvas.fill(x+8,y+18,cx-12,y+19,0x556995A4);
        canvas.fill(cx+13,y+18,x+width-8,y+19,0x556995A4);
    }
    private static int blend(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));int color=0;
        for(int shift:new int[]{0,8,16,24}) color|=Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;
        return color;
    }
}
