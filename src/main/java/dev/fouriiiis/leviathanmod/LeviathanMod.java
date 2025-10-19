package dev.fouriiiis.leviathanmod;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;
import dev.fouriiiis.leviathanmod.registry.ModEntities;

public class LeviathanMod implements ModInitializer {
	public static final String MOD_ID = "leviathanmod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		LOGGER.info("LeviathanMod initialized");
	}
}