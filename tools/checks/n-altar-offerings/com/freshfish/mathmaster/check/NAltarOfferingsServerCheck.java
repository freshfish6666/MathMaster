package com.freshfish.mathmaster.check;

import com.freshfish.mathmaster.ritual.NAltarOfferingsCheck;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = "mathmaster")
public final class NAltarOfferingsServerCheck {
    @SubscribeEvent
    public static void check(ServerStartedEvent event) {
        String result;
        try {
            result = "PASS: " + NAltarOfferingsCheck.run(event.getServer().overworld()) + " offering checks";
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL: " + error;
        }
        try { Files.writeString(Path.of("offerings-result.txt"), result); }
        catch (Exception error) { throw new RuntimeException(error); }
        System.out.println("N_ALTAR_OFFERINGS_CHECK " + result);
        event.getServer().halt(false);
    }
}
