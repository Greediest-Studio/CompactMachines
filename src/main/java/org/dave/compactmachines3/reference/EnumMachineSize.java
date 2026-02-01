package org.dave.compactmachines3.reference;

import net.minecraft.util.IStringSerializable;

public enum EnumMachineSize implements IStringSerializable {
    TINY    (0, "tiny", 4),
    SMALL   (1, "small", 6),
    NORMAL  (2, "normal", 8),
    LARGE   (3, "large", 10),
    GIANT   (4, "giant", 12),
    MAXIMUM (5, "maximum", 14),
    EXTRA1  (6, "extra1", 18),
    EXTRA2  (7, "extra2", 34),
    EXTRA3  (8, "extra3", 50),
    EXTRA4  (9, "extra4", 64),
    EXTRA5  ( 10, "extra5", 128),
    EXTRA0  (11, "extra0", 2);

    private int meta;
    private String name;
    private int dimension;

    EnumMachineSize(int meta, String name, int dimension) {
        this.meta = meta;
        this.name = name;
        this.dimension = dimension;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public int getDimension() {
        return this.dimension;
    }

    public int getMeta() {
        return this.meta;
    }

    public static EnumMachineSize getFromMeta(int meta) {
        switch (meta) {
            case 0: return TINY;
            case 1: return SMALL;
            case 2: return NORMAL;
            case 3: return LARGE;
            case 4: return GIANT;
            case 5: return MAXIMUM;
            case 6: return EXTRA1;
            case 7: return EXTRA2;
            case 8: return EXTRA3;
            case 9: return EXTRA4;
            case 10: return EXTRA5;
            case 11: return EXTRA0;
        }
        return TINY;
    }
}
