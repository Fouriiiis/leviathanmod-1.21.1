package dev.fouriiiis.leviathanmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import dev.fouriiiis.leviathanmod.registry.ModEntities;
import dev.fouriiiis.leviathanmod.entity.leviathan.client.LeviathanRenderer;

public class LeviathanModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.LEVIATHAN, (ctx) -> new LeviathanRenderer(ctx));
	}
}