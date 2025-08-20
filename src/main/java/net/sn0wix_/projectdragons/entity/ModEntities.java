package net.sn0wix_.projectdragons.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sn0wix_.projectdragons.ProjectDragons;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;

public class ModEntities {
    public static final EntityType<ShellSmasherEntity> SHELL_SMASHER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(ProjectDragons.MOD_ID, "shell_smasher"),
            EntityType.Builder.create(ShellSmasherEntity::new, SpawnGroup.CREATURE)
                    .dimensions(2.5f, 1.8f).build());

    public static void registerModEntities() {
        ProjectDragons.LOGGER.info("Registering entities...");
    }
}
