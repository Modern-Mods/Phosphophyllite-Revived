package modernmods.phosphophylliterevived.capabilities;

import net.neoforged.fml.ModList;

public final class MekanismPresence {

    private static Boolean loaded;

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded("mekanism");
        }
        return loaded;
    }
}
