package modernmods.phosphophylliterevived.multiblock.rectangular;

import modernmods.phosphophylliterevived.multiblock.validated.IValidatedMultiblockBlock;
import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public interface IRectangularMultiblockBlock extends IValidatedMultiblockBlock {
    boolean isGoodForInterior();
    
    boolean isGoodForExterior();
    
    default boolean isGoodForFrame() {
        return isGoodForExterior();
    }
    
    default boolean isGoodForCorner() {
        return isGoodForFrame();
    }
}
