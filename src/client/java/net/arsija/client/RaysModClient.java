package net.arsija.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

// Rays Farms by ArSi
public class RaysModClient implements ClientModInitializer {
    public static final KeyMapping.Category RAYS_FARMS_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rays-mod", "rays_farms"));

    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.rays-mod.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                RAYS_FARMS_CATEGORY
        ));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> RaysDataLoader.reload());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new RaysMenuScreen());
                }
            }
        });
    }
}
