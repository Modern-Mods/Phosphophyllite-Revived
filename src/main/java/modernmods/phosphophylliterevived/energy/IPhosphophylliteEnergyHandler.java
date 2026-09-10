package modernmods.phosphophylliterevived.energy;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;

public interface IPhosphophylliteEnergyHandler {
    
    BlockCapability<IPhosphophylliteEnergyHandler, Direction> CAPABILITY =
            BlockCapability.createSided(Identifier.fromNamespaceAndPath("phosphophyllite", "energy_handler"), IPhosphophylliteEnergyHandler.class);
    
    long insertEnergy(long maxInsert, boolean simulate);
    
    long extractEnergy(long maxExtract, boolean simulate);
    
    long energyStored();
    
    long maxEnergyStored();
}
