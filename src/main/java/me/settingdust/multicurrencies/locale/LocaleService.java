package me.settingdust.multicurrencies.locale;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import me.settingdust.multicurrencies.ObservableFileService;
import me.settingdust.multicurrencies.TextUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetManager;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tuple;

/**
 * @author The EpicBanItem Team
 */
@Singleton
public class LocaleService {
    private static final String MISSING_MESSAGE_KEY = "error.missingMessage";
    private static final String MESSAGE_FILE_NAME = "message.lang";

    private ConfigurationNode root;

    private final Map<String, TextTemplate> cache;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    private ObservableFileService fileService;

    @Inject
    private Logger logger;

    @Inject
    public LocaleService(AssetManager assetManager, PluginContainer pluginContainer, EventManager eventManager)
        throws IOException {
        this.cache = Maps.newConcurrentMap();

        Asset fallbackAsset = assetManager
            .getAsset(pluginContainer, "lang/" + Locale.getDefault().toString().toLowerCase() + ".lang")
            .orElse(
                assetManager
                    .getAsset(pluginContainer, "lang/en_us.lang")
                    .orElseThrow(() -> new NoSuchElementException("Can't find the language files"))
            );

        HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().setURL(fallbackAsset.getUrl()).build();
        root = configurationLoader.load();

        cache.put(
            MISSING_MESSAGE_KEY,
            TextUtil.parseTextTemplate(
                getString(MISSING_MESSAGE_KEY).orElse("Missing language key: {message_key}"),
                Collections.singleton("message_key")
            )
        );

        eventManager.registerListener(pluginContainer, GamePreInitializationEvent.class, this::onPreInit);
    }

    @SafeVarargs
    public final String getStringWithFallback(String path, Tuple<String, ?>... tuples) {
        return getTextWithFallback(path, tuples).toPlain();
    }

    @SafeVarargs
    public final Optional<String> getString(String path, Tuple<String, ?>... tuples) {
        return getText(path, tuples).map(Text::toPlain);
    }

    public Optional<String> getString(String path) {
        Optional<String> stringOptional = Optional.empty();
        try {
            stringOptional =
                Optional.ofNullable(Arrays.stream(path.split("\\.")).reduce(root, ConfigurationNode::getNode, (prev, curr) -> curr).getString());
        } catch (MissingResourceException ignore) {}
        return stringOptional;
    }

    @SuppressWarnings("unchecked")
    public final Text getTextWithFallback(String path, Iterable<Tuple<String, ?>> tuples) {
        return this.getTextWithFallback(path, Iterables.toArray(tuples, Tuple.class));
    }

    @SafeVarargs
    public final Text getTextWithFallback(String path, Tuple<String, ?>... tuples) {
        return getText(path, tuples)
            .orElseGet(() -> getTextWithFallback(MISSING_MESSAGE_KEY, Tuple.of("message_key", path)).toBuilder().color(TextColors.RED).build());
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public final Optional<Text> getText(String path, Tuple<String, ?>... tuples) {
        return getText(path, Arrays.stream(tuples).collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond)));
    }

    public Optional<Text> getText(String path, Map<String, ?> params) {
        Optional<Text> textOptional = Optional.empty();
        if (!cache.containsKey(path)) {
            getString(path).ifPresent(value -> cache.put(path, TextUtil.parseTextTemplate(value, params.keySet())));
        }
        if (cache.containsKey(path)) {
            textOptional = Optional.of(cache.get(path).apply(params).build());
        }
        return textOptional;
    }

    private void onPreInit(GamePreInitializationEvent event) throws IOException {
        fileService.register(
            ObservableLocaleFile
                .builder()
                .path(this.configDir.resolve(MESSAGE_FILE_NAME))
                .updateConsumer(
                    node -> {
                        node.mergeValuesFrom(root);
                        this.root = node;
                        this.cache.clear();
                        cache.put(
                            MISSING_MESSAGE_KEY,
                            TextUtil.parseTextTemplate(
                                getString(MISSING_MESSAGE_KEY).orElse("Missing language key: {message_key}"),
                                Collections.singleton("message_key")
                            )
                        );
                        getString("info.reload", Tuple.of("name", MESSAGE_FILE_NAME)).ifPresent(logger::info);
                    }
                )
                .build()
        );
    }
}
