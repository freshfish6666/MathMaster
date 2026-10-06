import com.freshfish.mathmaster.compat.jade.BossTooltipLayout;
import java.util.UUID;

/** Standalone layout checks; no game/world or Jade runtime required. */
public final class BossTooltipLayoutCheck {
    private static int checks;
    private static void expect(int actual, int expected, String description) {
        checks++;
        if (actual != expected) throw new AssertionError(description + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        var layout = new BossTooltipLayout();
        var boss = UUID.randomUUID();
        var other = UUID.randomUUID();
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 6, "No visible bar");
        layout.addBar(110, 3, 400, 40, boss);
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 46, "Default top panel below medallion");
        expect(layout.tooltipY(other, 150, 6, 180, 45), 6, "Unrelated target unchanged");
        expect(layout.tooltipY(boss, 0, 6, 100, 45), 6, "Left panel unchanged");
        expect(layout.tooltipY(boss, 400, 6, 100, 45), 6, "Right edge does not overlap");
        expect(layout.tooltipY(boss, 10, 6, 100, 45), 6, "Left edge does not overlap");
        expect(layout.tooltipY(boss, 150, 46, 180, 45), 46, "Already has gap");
        expect(layout.tooltipY(boss, 150, 130, 180, 45), 130, "Lower user position unchanged");
        expect(layout.tooltipY(boss, 150, 41, 180, 45), 46, "Ensure gap below bar");
        expect(layout.tooltipY(boss, 150, -20, 180, 23), -20, "Above title without overlap");
        layout.addBar(150, 43, 350, 57, null);
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 63, "Mixed vanilla bar below custom bar");
        layout.addBar(110, 62, 400, 99, other);
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 105, "Multiple holder bars");
        expect(layout.tooltipY(other, 150, 6, 180, 45), 105, "Other visible holder");
        expect(layout.tooltipY(boss, 150, 6, 0, 45), 6, "Zero width");
        expect(layout.tooltipY(boss, 150, 6, 180, 0), 6, "Zero height");
        layout.clear();
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 6, "Death/hidden HUD/next frame clears stale bounds");
        layout.addBar(160, 3, 342, 17, null);
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 6, "Vanilla only does not identify holder");
        layout.clear();
        layout.addBar(9, 3, 139, 40, boss);
        expect(layout.tooltipY(boss, 15, 6, 110, 90), 46, "Narrow GUI and large scaled tooltip");
        layout.clear();
        layout.addBar(110, 3, 400, 40, boss);
        expect(layout.tooltipY(boss, 150, 6, 180, 45), 46, "Resize uses only new frame");
        System.out.println("PASS: " + checks + " boss tooltip layout checks");
    }
}
