package modernmods.phosphophylliterevived.fluids;

import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.recipes.RotaryRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import modernmods.phosphophylliterevived.registry.OnModLoad;
import modernmods.phosphophylliterevived.threading.WorkQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MekanismGasWrappers {

    public static IChemicalHandler wrap(IPhosphophylliteFluidHandler fluidHandler) {
        return new MekanismGasWrappers.FluidToGasWrapper(fluidHandler);
    }

    public static IPhosphophylliteFluidHandler wrap(IChemicalHandler gasHandler) {
        return new MekanismGasWrappers.GasToFluidWrapper(gasHandler);
    }

    public static IChemicalHandler EMPTY_TANK;

    static {
        EMPTY_TANK = new IChemicalHandler() {
            @Override
            public int getChemicalTanks() {
                return 0;
            }

            @Override
            public ChemicalStack getChemicalInTank(int tank) {
                return ChemicalStack.EMPTY;
            }

            @Override
            public void setChemicalInTank(int tank, ChemicalStack stack) {
            }

            @Override
            public long getChemicalTankCapacity(int tank) {
                return 0;
            }

            @Override
            public boolean isValid(int tank, ChemicalStack stack) {
                return false;
            }

            @Override
            public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
                return stack;
            }

            @Override
            public ChemicalStack extractChemical(int tank, long amount, Action action) {
                return ChemicalStack.EMPTY;
            }
        };
    }

    private static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/MekanismIntegration");

    private static class Mapping {
        final List<Chemical> gases = new ArrayList<>();
        long gasToFluidGasUnits = -1;
        long gasToFluidFluidUnits = -1;
        final List<Fluid> fluids = new ArrayList<>();
        long fluidToGasGasUnits = -1;
        long fluidToGasFluidUnits = -1;
    }

    private static final Map<Chemical, Mapping> gasToFluidMap = new HashMap<>();
    private static final Map<Fluid, Mapping> fluidToGasMap = new HashMap<>();

    @OnModLoad(required = false)
    private static void onModLoad() {
        NeoForge.EVENT_BUS.addListener(MekanismGasWrappers::addReloadEventListener);
        NeoForge.EVENT_BUS.addListener(MekanismGasWrappers::serverAboutToStart);
        NeoForge.EVENT_BUS.addListener(MekanismGasWrappers::serverStopped);
    }

    private static void addReloadEventListener(AddReloadListenerEvent event) {
        reloadQueue.enqueue(MekanismGasWrappers::reloadMappings);
        if (server != null) {
            reloadQueue.runAll();
        }
    }

    @Nullable
    private static MinecraftServer server;

    private static final WorkQueue reloadQueue = new WorkQueue();

    private static void serverAboutToStart(ServerAboutToStartEvent event) {
        server = event.getServer();
        reloadQueue.runAll();
    }

    private static void serverStopped(ServerStoppedEvent event) {
        server = null;
        reloadQueue.runAll();
    }

    private static long GCD(long A, long B) {
        long A2 = A >> 1;
        long B2 = B >> 1;

        long GCD = 1;
        for (long i = 2; i < A2 && i < B2; i++) {
            long Ai = (A / i) * i;
            long Bi = (B / i) * i;
            if (Ai == A && Bi == B) {
                GCD = i;
            }
        }

        return GCD;
    }

    private static void reloadMappings() {

        // forces the wrappers to update their mappings
        gasToFluidMap.forEach((k, v) -> {
            v.fluids.clear();
            v.gases.clear();
        });
        fluidToGasMap.forEach((k, v) -> {
            v.fluids.clear();
            v.gases.clear();
        });
        gasToFluidMap.clear();
        fluidToGasMap.clear();

        @SuppressWarnings("unchecked")
        RecipeType<RotaryRecipe> type = (RecipeType<RotaryRecipe>) BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.fromNamespaceAndPath("mekanism", "rotary"));
        if (type == null || server == null) {
            return;
        }
        final var recipes = server.getRecipeManager().getAllRecipesFor(type);

        for (final var recipeHolder : recipes) {
            final var recipe = recipeHolder.value();
            Mapping mapping = new Mapping();
            if (recipe.hasChemicalToFluid()) {

                List<ChemicalStack> inputs = recipe.getChemicalInput().getRepresentations();
                for (ChemicalStack input : inputs) {
                    Chemical gas = input.getChemical();
                    long amount = input.getAmount();
                    if (mapping.gasToFluidGasUnits != -1 && mapping.gasToFluidGasUnits != amount) {
                        LOGGER.warn("Input amount discrepancy in rotary recipe " + recipeHolder.id() + " with gas " + gas.getRegistryName() + " wanting " + amount + " input while a different gas wants " + mapping.gasToFluidGasUnits);
                        continue;
                    }
                    if (!mapping.gases.contains(gas)) {
                        mapping.gases.add(gas);
                    }
                    mapping.gasToFluidGasUnits = amount;
                }

                FluidStack output = recipe.getFluidOutputDefinition().get(0);
                if (!mapping.fluids.contains(output.getFluid())) {
                    mapping.fluids.add(output.getFluid());
                }
                mapping.gasToFluidFluidUnits = output.getAmount();

                if (mapping.gasToFluidGasUnits <= 0 || mapping.gasToFluidFluidUnits <= 0) {
                    mapping.gasToFluidGasUnits = -1;
                    mapping.gasToFluidFluidUnits = -1;
                }

                long GCD = GCD(mapping.gasToFluidGasUnits, mapping.gasToFluidFluidUnits);
                mapping.gasToFluidGasUnits /= GCD;
                mapping.gasToFluidFluidUnits /= GCD;
            }
            if (recipe.hasFluidToChemical()) {

                List<FluidStack> inputs = recipe.getFluidInput().getRepresentations();
                for (FluidStack input : inputs) {
                    Fluid fluid = input.getFluid();
                    long amount = input.getAmount();
                    if (mapping.gasToFluidGasUnits != -1 && mapping.gasToFluidGasUnits != amount) {
                        LOGGER.warn("Input amount discrepancy in rotary recipe " + recipeHolder.id() + " with fluid " + BuiltInRegistries.FLUID.getKey(fluid) + " wanting " + amount + " input while a different gas wants " + mapping.fluidToGasFluidUnits);
                        continue;
                    }
                    if (!mapping.fluids.contains(fluid)) {
                        mapping.fluids.add(fluid);
                    }
                    mapping.fluidToGasFluidUnits = amount;
                }

                ChemicalStack output = recipe.getChemicalOutputDefinition().get(0);
                if (!mapping.gases.contains(output.getChemical())) {
                    mapping.gases.add(output.getChemical());
                }
                mapping.fluidToGasGasUnits = output.getAmount();

                if (mapping.fluidToGasGasUnits <= 0 || mapping.fluidToGasFluidUnits <= 0) {
                    mapping.fluidToGasGasUnits = -1;
                    mapping.fluidToGasFluidUnits = -1;
                }

                long GCD = GCD(mapping.fluidToGasGasUnits, mapping.fluidToGasFluidUnits);
                mapping.fluidToGasGasUnits /= GCD;
                mapping.fluidToGasFluidUnits /= GCD;
            }

            for (Chemical gas : mapping.gases) {
                Mapping oldMapping = gasToFluidMap.put(gas, mapping);
                if (oldMapping != null) {
                    LOGGER.warn("Duplicate gas entry for gas " + gas.getRegistryName());
                }
            }
            for (Fluid fluid : mapping.fluids) {
                Mapping oldMapping = fluidToGasMap.put(fluid, mapping);
                if (oldMapping != null) {
                    LOGGER.warn("Duplicate fluid entry for fluid " + BuiltInRegistries.FLUID.getKey(fluid));
                }
            }
        }
    }

    private static void removeMapping(Mapping mapping) {
        for (Chemical gas : mapping.gases) {
            gasToFluidMap.remove(gas);
        }
        for (Fluid fluid : mapping.fluids) {
            fluidToGasMap.remove(fluid);
        }
        mapping.gases.clear();
        mapping.fluids.clear();
    }

    private static class GasToFluidWrapper implements IPhosphophylliteFluidHandler {
        final IChemicalHandler gasHandler;
        @Nullable
        Mapping lastMapping;

        GasToFluidWrapper(IChemicalHandler handler) {
            gasHandler = handler;
        }

        @Override
        public int tankCount() {
            return gasHandler.getChemicalTanks();
        }

        @Override
        public long tankCapacity(int tank) {
            // this isn't 100% accurate, but i cant do much better
            // in most cases its 1:1 anyway, so, shouldn't matter
            return gasHandler.getChemicalTankCapacity(tank);
        }

        @Override
        public Fluid fluidTypeInTank(int tank) {
            ChemicalStack gasStack = gasHandler.getChemicalInTank(tank);
            if (lastMapping == null || !lastMapping.gases.contains(gasStack.getChemical())) {
                Mapping map = gasToFluidMap.get(gasStack.getChemical());
                if (map == null) {
                    return Fluids.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + gasStack.getChemical().getRegistryName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return Fluids.EMPTY;
            }
            return lastMapping.fluids.get(0);
        }

        @Override
        public long fluidAmountInTank(int tank) {
            if (tank > tankCount()) {
                return 0;
            }
            ChemicalStack gasStack = gasHandler.getChemicalInTank(tank);
            if (lastMapping == null || !lastMapping.gases.contains(gasStack.getChemical())) {
                Mapping map = gasToFluidMap.get(gasStack.getChemical());
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            long amount = gasStack.getAmount();
            amount *= lastMapping.gasToFluidFluidUnits;
            amount /= lastMapping.gasToFluidGasUnits;
            return amount;
        }

        @Override
        public boolean fluidValidForTank(int tank, Fluid fluid) {
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return false;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + BuiltInRegistries.FLUID.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return false;
            }
            // ok, *technically* i should check against all of them, but chances are, i dont need to
            return gasHandler.isValid(tank, lastMapping.gases.get(0).getStack(1));
        }

        @Override
        public long fill(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
            if (!components.isEmpty() || fluid == Fluids.EMPTY) {
                return 0;
            }
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + BuiltInRegistries.FLUID.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return 0;
            }
            if (lastMapping.fluidToGasGasUnits <= 0) {
                return 0;
            }
            long gasAmount = amount;
            gasAmount *= lastMapping.fluidToGasGasUnits;
            gasAmount /= lastMapping.fluidToGasFluidUnits;
            if (gasAmount <= 0) {
                return 0;
            }
            ChemicalStack stack = gasHandler.insertChemical(lastMapping.gases.get(0).getStack(gasAmount), Action.get(!simulate));
            long remainingFluid = stack.getAmount();
            remainingFluid *= lastMapping.fluidToGasFluidUnits;
            remainingFluid /= lastMapping.fluidToGasGasUnits;
            return amount - remainingFluid;
        }

        @Override
        public long drain(Fluid fluid, DataComponentPatch components, long amount, boolean simulate) {
            if (!components.isEmpty() || fluid == Fluids.EMPTY) {
                return 0;
            }
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return 0;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + BuiltInRegistries.FLUID.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return 0;
            }
            if (lastMapping.gasToFluidFluidUnits <= 0) {
                return 0;
            }
            long gasAmount = amount;
            gasAmount *= lastMapping.gasToFluidFluidUnits;
            gasAmount /= lastMapping.gasToFluidGasUnits;
            if (gasAmount <= 0) {
                return 0;
            }
            ChemicalStack stack = gasHandler.extractChemical(lastMapping.gases.get(0).getStack(gasAmount), Action.get(!simulate));
            long drained = stack.getAmount();
            drained *= lastMapping.gasToFluidGasUnits;
            drained /= lastMapping.gasToFluidFluidUnits;
            return drained;
        }
    }

    private static class FluidToGasWrapper implements IChemicalHandler {

        final IPhosphophylliteFluidHandler fluidHandler;
        @Nullable
        Mapping lastMapping;

        private FluidToGasWrapper(IPhosphophylliteFluidHandler handler) {
            fluidHandler = handler;
        }

        @Override
        public int getChemicalTanks() {
            return fluidHandler.tankCount();
        }

        @Override
        public ChemicalStack getChemicalInTank(int tank) {
            Fluid fluid = fluidHandler.fluidTypeInTank(tank);
            long amount = fluidHandler.fluidAmountInTank(tank);
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return ChemicalStack.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + BuiltInRegistries.FLUID.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return ChemicalStack.EMPTY;
            }
            amount *= lastMapping.fluidToGasGasUnits;
            amount /= lastMapping.fluidToGasFluidUnits;
            if (amount <= 0) {
                return ChemicalStack.EMPTY;
            }
            return lastMapping.gases.get(0).getStack(amount);
        }

        @Override
        public void setChemicalInTank(int tank, ChemicalStack stack) {
            throw new RuntimeException("Not implemented for this handler");
        }

        @Override
        public long getChemicalTankCapacity(int tank) {
            return fluidHandler.tankCapacity(tank);
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {
            if (lastMapping == null || !lastMapping.gases.contains(stack.getChemical())) {
                Mapping map = gasToFluidMap.get(stack.getChemical());
                if (map == null) {
                    return false;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + stack.getChemical().getRegistryName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return false;
            }
            // ok, *technically* i should check against all of them, but chances are, i dont need to
            return fluidHandler.fluidValidForTank(tank, lastMapping.fluids.get(0));
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            if (lastMapping == null || !lastMapping.gases.contains(stack.getChemical())) {
                Mapping map = gasToFluidMap.get(stack.getChemical());
                if (map == null) {
                    return stack;
                }
                lastMapping = map;
            }
            if (lastMapping.fluids.isEmpty()) {
                LOGGER.error("Gas mapping for " + stack.getChemical().getRegistryName() + " has zero fluid elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return stack;
            }
            if (lastMapping.gasToFluidFluidUnits <= 0) {
                return stack;
            }
            Fluid fluid = lastMapping.fluids.get(0);
            long amount = stack.getAmount();
            amount *= lastMapping.gasToFluidFluidUnits;
            amount /= lastMapping.gasToFluidGasUnits;
            long filled = fluidHandler.fill(fluid, DataComponentPatch.EMPTY, amount, action.simulate());
            if (filled == 0) {
                return stack;
            }
            if (filled == amount) {
                return ChemicalStack.EMPTY;
            }
            long remaining = amount - filled;
            remaining *= lastMapping.gasToFluidGasUnits;
            remaining /= lastMapping.gasToFluidFluidUnits;
            if (remaining <= 0) {
                return ChemicalStack.EMPTY;
            }
            return stack.copyWithAmount(remaining);
        }

        @Override
        public ChemicalStack extractChemical(int tank, long gasAmount, Action action) {
            Fluid fluid = fluidHandler.fluidTypeInTank(tank);
            if (lastMapping == null || !lastMapping.fluids.contains(fluid)) {
                Mapping map = fluidToGasMap.get(fluid);
                if (map == null) {
                    return ChemicalStack.EMPTY;
                }
                lastMapping = map;
            }
            if (lastMapping.gases.isEmpty()) {
                LOGGER.error("Fluid mapping for " + BuiltInRegistries.FLUID.getKey(fluid) + " has zero gas elements, removing");
                removeMapping(lastMapping);
                lastMapping = null;
                return ChemicalStack.EMPTY;
            }
            if (lastMapping.fluidToGasFluidUnits <= 0) {
                return ChemicalStack.EMPTY;
            }
            long amount = gasAmount;
            amount *= lastMapping.fluidToGasFluidUnits;
            amount /= lastMapping.fluidToGasGasUnits;
            long drained = fluidHandler.drain(fluid, DataComponentPatch.EMPTY, amount, action.simulate());
            if (drained == 0) {
                return ChemicalStack.EMPTY;
            }
            long gasDrained = drained;
            gasDrained *= lastMapping.fluidToGasGasUnits;
            gasDrained /= lastMapping.fluidToGasFluidUnits;
            if (gasDrained <= 0) {
                return ChemicalStack.EMPTY;
            }
            return lastMapping.gases.get(0).getStack(gasDrained);
        }
    }

}
