package net.sn0wix_.projectdragons.entity.custom;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

public enum ShellSmasherVariants implements StringIdentifiable {
    FORREST(0, "forrest"),
    TROPIC(1, "tropic"),
    SWAMP(2, "swamp");

    public static final Codec<ShellSmasherVariants> CODEC;
    private static final IntFunction<ShellSmasherVariants> BY_ID;
    private final int id;
    private final String name;

    ShellSmasherVariants(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public static ShellSmasherVariants byId(int id) {
        return BY_ID.apply(id);
    }

    @Override
    public String asString() {
        return this.name;
    }

    static {
        CODEC = StringIdentifiable.createCodec(ShellSmasherVariants::values);
        BY_ID = ValueLists.createIdToValueFunction(ShellSmasherVariants::getId, ShellSmasherVariants.values(), ValueLists.OutOfBoundsHandling.WRAP);
    }
}
