package me.settingdust.multicurrencies;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Objects;
import me.settingdust.multicurrencies.locale.LocaleService;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

@Plugin(
    id = "multicurrencies",
    name = "Multi Currencies",
    description = "A plugin to manage the other currencies",
    authors = {"SettingDust"},
    version = "1.0.0"
)
public class MultiCurrencies {

    @Inject
    private Injector injector;

    @Inject
    public MultiCurrencies(LocaleService localeService, EventManager eventManager, PluginContainer pluginContainer) {
        Objects.requireNonNull(localeService);

        eventManager.registerListener(pluginContainer, GamePostInitializationEvent.class, this::onGamePostInitialization);
    }

    private void onGamePostInitialization(GamePostInitializationEvent event) {
        Objects.requireNonNull(injector.getInstance(CommandModule.class));
    }
}
