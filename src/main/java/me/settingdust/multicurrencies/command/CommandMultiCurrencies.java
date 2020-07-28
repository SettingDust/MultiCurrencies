package me.settingdust.multicurrencies.command;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import me.settingdust.multicurrencies.AbstractCommand;
import me.settingdust.multicurrencies.command.multicurrencies.CommandAdd;
import me.settingdust.multicurrencies.command.multicurrencies.CommandBalance;
import me.settingdust.multicurrencies.command.multicurrencies.CommandRemove;
import me.settingdust.multicurrencies.command.multicurrencies.CommandSet;
import me.settingdust.multicurrencies.command.multicurrencies.CommandSetAll;
import me.settingdust.multicurrencies.locale.LocaleService;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.PluginContainer;

public class CommandMultiCurrencies extends AbstractCommand implements CommandExecutor {
    public static final String COMMAND_LABEL = "multicurrencies";

    @Inject
    private CommandManager commandManager;

    private final PluginContainer pluginContainer;

    @Inject
    public CommandMultiCurrencies(
        EventManager eventManager,
        PluginContainer pluginContainer,
        LocaleService localeService,
        CommandSet commandSet,
        CommandAdd commandAdd,
        CommandBalance commandBalance,
        CommandRemove commandRemove,
        CommandSetAll commandSetAll
    ) {
        this.pluginContainer = pluginContainer;
        Builder builder = CommandSpec.builder();
        builder
            .description(localeService.getTextWithFallback("command.multicurrencies.description"))
            .permission("multicurrencies.command")
            .executor(this);

        Sets
            .newHashSet(commandSet, commandAdd, commandBalance, commandRemove, commandSetAll)
            .forEach(command -> builder.child(command.commandSpec(), command.alias()));

        commandSpec(builder.build());

        eventManager.registerListener(pluginContainer, GameStartingServerEvent.class, this::onServerStarting);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public CommandResult execute(CommandSource src, CommandContext args) {
        commandManager.process(src, "help " + COMMAND_LABEL);
        return CommandResult.success();
    }

    private void onServerStarting(GameStartingServerEvent event) {
        commandManager.register(pluginContainer, commandSpec(), COMMAND_LABEL, "meco");
    }
}
