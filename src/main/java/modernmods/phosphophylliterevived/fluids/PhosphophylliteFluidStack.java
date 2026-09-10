package modernmods.phosphophylliterevived.fluids;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import modernmods.phosphophylliterevived.util.NonnullDefault;

import javax.annotation.Nullable;

/**
 * Re-writable long capable version of a FluidStack
 */
@NonnullDefault
public class PhosphophylliteFluidStack {
    
    public static final PhosphophylliteFluidStack EMPTY = new PhosphophylliteFluidStack();
    
    public static final Codec<PhosphophylliteFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(PhosphophylliteFluidStack::getFluid),
            Codec.LONG.fieldOf("amount").forGetter(PhosphophylliteFluidStack::getLongAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(PhosphophylliteFluidStack::getComponents)
    ).apply(instance, PhosphophylliteFluidStack::new));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, PhosphophylliteFluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(BuiltInRegistries.FLUID.key()), PhosphophylliteFluidStack::getFluid,
            ByteBufCodecs.VAR_LONG, PhosphophylliteFluidStack::getLongAmount,
            DataComponentPatch.STREAM_CODEC, PhosphophylliteFluidStack::getComponents,
            PhosphophylliteFluidStack::new
    );
    
    private Fluid fluid;
    private long amount;
    private DataComponentPatch components;
    
    public PhosphophylliteFluidStack() {
        this(Fluids.EMPTY, 0, DataComponentPatch.EMPTY);
    }
    
    public PhosphophylliteFluidStack(Fluid fluid, long amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }
    
    public PhosphophylliteFluidStack(Fluid fluid, long amount, DataComponentPatch components) {
        this.fluid = fluid;
        this.amount = amount;
        this.components = components;
    }
    
    public PhosphophylliteFluidStack(FluidStack stack) {
        this(stack.getFluid(), stack.getAmount(), stack.getComponentsPatch());
    }
    
    public PhosphophylliteFluidStack(FluidStack stack, long amount) {
        this(stack.getFluid(), amount, stack.getComponentsPatch());
    }
    
    public PhosphophylliteFluidStack(PhosphophylliteFluidStack stack) {
        this(stack.fluid, stack.amount, stack.components);
    }
    
    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }
    
    public Fluid getRawFluid() {
        return fluid;
    }
    
    public void setFluid(Fluid fluid) {
        this.fluid = fluid;
    }
    
    public DataComponentPatch getComponents() {
        return components;
    }
    
    public void setComponents(DataComponentPatch components) {
        this.components = components;
    }
    
    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }
    
    public int getAmount() {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }
    
    public long getLongAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        setAmount((long) amount);
    }
    
    public void setAmount(long amount) {
        this.amount = amount;
    }
    
    public void grow(long amount) {
        setAmount(this.amount + amount);
    }
    
    public void shrink(long amount) {
        setAmount(this.amount - amount);
    }
    
    public FluidStack toFluidStack() {
        if (isEmpty()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), getAmount(), components);
    }
    
    public FluidStack toFluidStack(int amount) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), amount, components);
    }
    
    public boolean isFluidEqual(PhosphophylliteFluidStack other) {
        return getFluid() == other.getFluid() && components.equals(other.components);
    }
    
    public boolean isFluidEqual(FluidStack other) {
        return getFluid() == other.getFluid() && components.equals(other.getComponentsPatch());
    }
    
    public boolean isFluidStackIdentical(PhosphophylliteFluidStack other) {
        return isFluidEqual(other) && amount == other.amount;
    }
    
    public boolean containsFluid(PhosphophylliteFluidStack other) {
        return isFluidEqual(other) && amount >= other.amount;
    }
    
    public PhosphophylliteFluidStack copy() {
        return new PhosphophylliteFluidStack(this);
    }
    
    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putString("FluidName", BuiltInRegistries.FLUID.getKey(getFluid()).toString());
        nbt.putInt("Amount", getAmount());
        nbt.putLong("LongAmount", amount);
        return nbt;
    }
    
    public static PhosphophylliteFluidStack loadFromNBT(@Nullable CompoundTag nbt) {
        if (nbt == null || !nbt.contains("FluidName")) {
            return new PhosphophylliteFluidStack();
        }
        final var fluid = BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(nbt.getStringOr("FluidName", "")));
        long amount = nbt.contains("LongAmount") ? nbt.getLongOr("LongAmount", 0L) : nbt.getIntOr("Amount", 0);
        return new PhosphophylliteFluidStack(fluid, amount);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PhosphophylliteFluidStack other)) {
            return false;
        }
        return isFluidStackIdentical(other);
    }
    
    @Override
    public int hashCode() {
        return 31 * getFluid().hashCode() + Long.hashCode(amount);
    }
}
