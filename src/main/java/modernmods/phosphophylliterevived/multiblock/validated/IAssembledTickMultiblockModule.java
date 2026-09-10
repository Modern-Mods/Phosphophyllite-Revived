package modernmods.phosphophylliterevived.multiblock.validated;

import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public interface IAssembledTickMultiblockModule {
    
    default void preTick() {
    }
    
    default void postTick() {
    }
    
    default void preDisassembledTick() {
    }
    
    default void postDisassembledTick() {
    }
}
