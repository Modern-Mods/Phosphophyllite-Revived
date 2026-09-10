package modernmods.phosphophylliterevived.config.spec;

import modernmods.phosphophylliterevived.config.ConfigType;
import modernmods.phosphophylliterevived.config.ConfigValue;
import modernmods.phosphophylliterevived.util.NonnullDefault;

import javax.annotation.Nullable;

@NonnullDefault
public record ConfigOptionsDefaults(ConfigType type, boolean advanced, boolean hidden, boolean reloadable) {
    public ConfigOptionsDefaults() {
        this(ConfigType.NULL, false, false, false);
    }
    
    public ConfigOptionsDefaults transform(@Nullable ConfigValue annotation) {
        if (annotation == null) {
            return this;
        }
        return new ConfigOptionsDefaults(annotation.configType().from(type), annotation.advanced().from(advanced), annotation.hidden().from(hidden), annotation.reloadable().from(reloadable));
    }
}
