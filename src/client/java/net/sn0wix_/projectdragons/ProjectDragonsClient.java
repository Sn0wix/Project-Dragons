package net.sn0wix_.projectdragons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.sn0wix_.projectdragons.entity.ModEntities;
import net.sn0wix_.projectdragons.entity.shellsmasher.ShellSmasherRenderer;

public class ProjectDragonsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHELL_SMASHER, ShellSmasherRenderer::new);
	}
}