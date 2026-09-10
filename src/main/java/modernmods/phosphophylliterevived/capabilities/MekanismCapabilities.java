package modernmods.phosphophylliterevived.capabilities;

import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public final class MekanismCapabilities {
    
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);
    
    public static final BlockCapability<IStrictEnergyHandler, Direction> STRICT_ENERGY =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath("mekanism", "strict_energy_handler"), IStrictEnergyHandler.class);
}
