import com.freshfish.mathmaster.client.GeometryHolderBarStyle;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shares the actual HUD rectangles; preview only, never launches the user's world. */
public final class GeometryHolderBarPreview {
    public static void main(String[] args) throws Exception {
        StringBuilder svg=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"960\" height=\"280\" viewBox=\"0 0 320 93.333\">\n");
        svg.append("<rect x=\"0\" y=\"0\" width=\"320\" height=\"94\" fill=\"#101722\"/>\n");
        for(int stage=0;stage<2;stage++) {
            int y=20+stage*43;
            GeometryHolderBarStyle.draw((x1,y1,x2,y2,color) -> svg.append(String.format(java.util.Locale.ROOT,
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"#%06x\" fill-opacity=\"%.4f\"/>\n",
                    x1,y1,x2-x1,y2-y1,color&0xFFFFFF,(color>>>24)/255.0)),30,y,260,stage==0 ? .82F : .32F,stage);
            svg.append("<text x=\"160\" y=\"").append(y-4).append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#e2f4f8\">几何持有者</text>\n");
        }
        svg.append("</svg>");
        Files.writeString(Path.of("build/geometry-holder-bar-preview.svg"),svg);
    }
}
