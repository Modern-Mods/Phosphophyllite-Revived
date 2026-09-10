package modernmods.phosphophylliterevived.config;

import modernmods.phosphophylliterevived.parsers.Element;
import modernmods.phosphophylliterevived.parsers.JSON5;
import modernmods.phosphophylliterevived.parsers.TOML;

public enum ConfigFormat {
    JSON5,
    TOML,
    ;
    
    public Element parse(String string) {
        return switch (this) {
            case JSON5 -> modernmods.phosphophylliterevived.parsers.JSON5.parseString(string);
            case TOML -> modernmods.phosphophylliterevived.parsers.TOML.parseString(string);
        };
    }
    
    public String parse(Element element) {
        return switch (this) {
            case JSON5 -> modernmods.phosphophylliterevived.parsers.JSON5.parseElement(element);
            case TOML -> modernmods.phosphophylliterevived.parsers.TOML.parseElement(element);
        };
    }
}
