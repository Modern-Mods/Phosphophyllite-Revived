package modernmods.phosphophylliterevived.capabilities;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

import javax.annotation.Nullable;

public interface IPhosphophylliteCapabilityProvider {

    @Nullable
    <T> T getCapability(BlockCapability<T, Direction> cap, @Nullable Direction side);
}
