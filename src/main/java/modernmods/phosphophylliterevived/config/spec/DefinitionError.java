package modernmods.phosphophylliterevived.config.spec;

import modernmods.phosphophylliterevived.util.NonnullDefault;

@NonnullDefault
public class DefinitionError extends RuntimeException {
    public DefinitionError(String message) {
        super(message);
    }
}
