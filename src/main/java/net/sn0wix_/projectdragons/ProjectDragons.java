package net.sn0wix_.projectdragons;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.sn0wix_.projectdragons.entity.ModEntities;
import net.sn0wix_.projectdragons.entity.custom.ShellSmasherEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDragons implements ModInitializer {
	public static final String MOD_ID = "project-dragons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ModEntities.registerModEntities();

        FabricDefaultAttributeRegistry.register(ModEntities.SHELL_SMASHER, ShellSmasherEntity.createAttributes());
	}
}