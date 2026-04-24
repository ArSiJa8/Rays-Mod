package net.arsija.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class RaysModClient implements ClientModInitializer {
    public static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rays-mod.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.rays-mod"
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
