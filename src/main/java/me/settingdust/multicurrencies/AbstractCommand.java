package me.settingdust.multicurrencies;

import java.util.Arrays;
import java.util.List;
import org.spongepowered.api.command.spec.CommandSpec;

public abstract class AbstractCommand {
    private CommandSpec commandSpec;
    private List<String> alias;

    public AbstractCommand commandSpec(CommandSpec commandSpec) {
        this.commandSpec = commandSpec;
        return this;
    }

    public CommandSpec commandSpec() {
        return commandSpec;
    }

    public List<String> alias() {
        return alias;
    }

    public AbstractCommand alias(List<String> alias) {
        this.alias = alias;
        return this;
    }

    public AbstractCommand alias(String... alias) {
        this.alias = Arrays.asList(alias);
        return this;
    }
}
