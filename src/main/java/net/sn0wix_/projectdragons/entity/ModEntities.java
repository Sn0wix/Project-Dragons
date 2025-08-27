package net.sn0wix_.projectdragons.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
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

    public static class Attributes {
        public static final RegistryEntry.Reference<EntityAttribute> GENERIC_FLYING_SPEED = Registry.registerReference(Registries.ATTRIBUTE,
                Identifier.of(ProjectDragons.MOD_ID, "generic.flying_speed"),
                new ClampedEntityAttribute("attribute.name.generic.flying_speed", 0.7, 0.0, 1024.0).setTracked(true));
    }
}
