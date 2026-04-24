package net.arsija;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Rays Farms — by ArSi
public class RaysMod implements ModInitializer {
        public static final String MOD_ID = "rays-mod";
        public static final String MOD_NAME = "Rays Farms";
        public static final String MOD_AUTHOR = "ArSi";

        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod id as the logger's name.
        // That way, it's clear which mod wrote info, warnings, and errors.
        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                // Rays Farms initialized. Made with care by ArSi.
                LOGGER.info("[{}] loaded — by {}", MOD_NAME, MOD_AUTHOR);
        }
}