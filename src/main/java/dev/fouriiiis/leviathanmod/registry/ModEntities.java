package dev.fouriiiis.leviathanmod.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import dev.fouriiiis.leviathanmod.LeviathanMod;
import dev.fouriiiis.leviathanmod.entity.leviathan.LeviathanEntity;

public class ModEntities {
    public static EntityType<LeviathanEntity> LEVIATHAN;

    public static void register() {
    LEVIATHAN = Registry.register(
                Registries.ENTITY_TYPE,
        Identifier.of(LeviathanMod.MOD_ID, "leviathan"),
        FabricEntityTypeBuilder
            .create(SpawnGroup.WATER_AMBIENT, LeviathanEntity::new)
            .dimensions(EntityDimensions.fixed(1.2f, 1.2f))
            .trackRangeBlocks(96)
            .trackedUpdateRate(2)
            .build()
        );

        FabricDefaultAttributeRegistry.register(LEVIATHAN, LeviathanEntity.createAttributes());
    }
}
