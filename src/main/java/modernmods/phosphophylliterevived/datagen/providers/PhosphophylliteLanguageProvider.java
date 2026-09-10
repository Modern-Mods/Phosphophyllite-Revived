package modernmods.phosphophylliterevived.datagen.providers;

import modernmods.phosphophylliterevived.Phosphophyllite;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class PhosphophylliteLanguageProvider extends LanguageProvider {

    public static final String[] LOCALES = {"en_us", "es_es", "es_mx", "es_ar", "zh_cn"};

    private final String locale;

    public PhosphophylliteLanguageProvider(PackOutput output, String locale) {
        super(output, Phosphophyllite.modid, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        switch (locale) {
            case "es_es", "es_mx", "es_ar" -> spanish();
            case "zh_cn" -> chinese();
            default -> english();
        }
    }

    private void english() {
        add("item_group.phosphophyllite", "Phosphophyllite");
        add("multiblock.error.phosphophyllite.invalid_block.frame", "Invalid block (%s) for frame at %s");
        add("multiblock.error.phosphophyllite.invalid_block.exterior", "Invalid block (%s) for exterior at %s");
        add("multiblock.error.phosphophyllite.invalid_block.interior", "Invalid block (%s) for interior at %s");
        add("multiblock.error.phosphophyllite.invalid_block.corner", "Invalid block (%s) for corner at %s");
        add("multiblock.error.phosphophyllite.invalid_block.generic", "Invalid block (%s) at %s");
        add("multiblock.error.phosphophyllite.unknown", "An unknown multiblock error has occurred, report to mod author");
        add("multiblock.error.phosphophyllite.null_controller", "The multiblock system has experienced an unknown internal error, report to Phosphophyllite");
        add("multiblock.error.phosphophyllite.dimensions", "Dimensions not allowed \nActual: %d, %d, %d, \nMin allowed: %d, %d, %d, \nMax allowed: %d, %d, %d,");

        add("block.phosphophyllite.item_black_hole", "Item Black Hole");
        add("block.phosphophyllite.item_white_hole", "Item White Hole");
        add("block.phosphophyllite.fluid_black_hole", "Fluid Black Hole");
        add("block.phosphophyllite.fluid_white_hole", "Fluid White Hole");
        add("block.phosphophyllite.power_black_hole", "Power Black Hole");
        add("block.phosphophyllite.power_white_hole", "Power White Hole");

        add("block.phosphophyllite.phosphophyllite_ore", "Phosphophyllite Ore");

        add("item.phosphophyllite.debug_tool", "Debeefer");
    }

    private void spanish() {
        add("item_group.phosphophyllite", "Phosphophyllite");

        add("block.phosphophyllite.phosphophyllite_ore", "Mena de Fosfofilita");
        add("item.phosphophyllite.debug_tool", "Debeefer");

        add("block.phosphophyllite.fluid_black_hole", "Agujero Negro de Fluidos");
        add("block.phosphophyllite.fluid_white_hole", "Agujero Blanco de Fluidos");
        add("block.phosphophyllite.item_black_hole", "Agujero Negro de Ítems");
        add("block.phosphophyllite.item_white_hole", "Agujero Blanco de Ítems");
        add("block.phosphophyllite.power_black_hole", "Agujero Negro de Energía");
        add("block.phosphophyllite.power_white_hole", "Agujero Blanco de Energía");

        add("multiblock.error.phosphophyllite.dimensions", "Dimensiones no permitidas \nActual: %d, %d, %d, \nMínimo permitido: %d, %d, %d, \nMáximo permitido: %d, %d, %d,");
        add("multiblock.error.phosphophyllite.invalid_block.corner", "Bloque inválido (%s) para una esquina en %s");
        add("multiblock.error.phosphophyllite.invalid_block.exterior", "Bloque inválido (%s) para el exterior en %s");
        add("multiblock.error.phosphophyllite.invalid_block.frame", "Bloque inválido (%s) para el marco en %s");
        add("multiblock.error.phosphophyllite.invalid_block.generic", "Bloque inválido (%s) en %s");
        add("multiblock.error.phosphophyllite.invalid_block.interior", "Bloque inválido (%s) para el interior en %s");
        add("multiblock.error.phosphophyllite.null_controller", "El sistema de multibloques tuvo un error interno desconocido, reportalo a Phosphophyllite");
        add("multiblock.error.phosphophyllite.unknown", "Ocurrió un error de multibloque desconocido, reportalo al autor del mod");
    }

    private void chinese() {
        add("block.phosphophyllite.fluid_black_hole", "液体黑洞");
        add("block.phosphophyllite.fluid_white_hole", "液体白洞");
        add("block.phosphophyllite.item_black_hole", "物品黑洞");
        add("block.phosphophyllite.item_white_hole", "物品白洞");
        add("block.phosphophyllite.phosphophyllite_ore", "磷叶石矿石");
        add("block.phosphophyllite.power_black_hole", "能量黑洞");
        add("block.phosphophyllite.power_white_hole", "能量白洞");
        add("item.phosphophyllite.debug_tool", "调试工具");
        add("itemGroup.modernmods.phosphophylliterevived.Phosphophyllite", "磷叶石");
        add("multiblock.error.phosphophyllite.dimensions", "无法在该维度使用 \n实际：%d, %d, %d, \n允许最小值：%d, %d, %d, \n允许最大值：%d, %d, %d,");
        add("multiblock.error.phosphophyllite.invalid_block.corner", "无效角落方块 (%s)，位于%s");
        add("multiblock.error.phosphophyllite.invalid_block.exterior", "无效外部方块 (%s)，位于%s");
        add("multiblock.error.phosphophyllite.invalid_block.frame", "无效方块框架 (%s)，位于%s");
        add("multiblock.error.phosphophyllite.invalid_block.generic", "无效方块 (%s)，位于%s");
        add("multiblock.error.phosphophyllite.invalid_block.interior", "无效内部方块 (%s)，位于%s");
        add("multiblock.error.phosphophyllite.unknown", "出现了未知的多方块结构错误，请联系作者");
        add("random.bullshit.that.doesnt.actually.get.used.anywhere", "这只是个结尾");
    }
}
