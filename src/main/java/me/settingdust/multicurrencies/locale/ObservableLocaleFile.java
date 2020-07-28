package me.settingdust.multicurrencies.locale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import me.settingdust.multicurrencies.ObservableFile;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

/**
 * @author The EpicBanItem Team
 */
public class ObservableLocaleFile implements ObservableFile {
    private final FileConsumer<ConfigurationNode> updateConsumer;
    private final Path path;
    private final HoconConfigurationLoader configurationLoader;
    private boolean closed;

    private ObservableLocaleFile(Builder builder) throws IOException {
        this.updateConsumer = builder.updateConsumer;
        this.path = builder.path;
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        configurationLoader = HoconConfigurationLoader.builder().setPath(path).build();

        this.next(StandardWatchEventKinds.ENTRY_MODIFY, path);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void next(Kind<Path> kind, Path path) throws IOException {
        if (closed) {
            throw new IOException("File closed.");
        }
        if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
            Files.createFile(path);
        } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind) || StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
            this.updateConsumer.accept(configurationLoader.load());
        }
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    public static final class Builder {
        private FileConsumer<ConfigurationNode> updateConsumer;
        private Path path;

        public Builder updateConsumer(final FileConsumer<ConfigurationNode> updateConsumer) {
            this.updateConsumer = updateConsumer;
            return this;
        }

        public Builder path(final Path path) {
            this.path = path;
            return this;
        }

        public ObservableLocaleFile build() throws IOException {
            return new ObservableLocaleFile(this);
        }
    }
}
