package me.settingdust.multicurrencies.command.multicurrencies;

import static org.spongepowered.api.command.args.GenericArguments.bigDecimal;
import static org.spongepowered.api.command.args.GenericArguments.choices;
import static org.spongepowered.api.command.args.GenericArguments.flags;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.userOrSource;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;
import me.settingdust.multicurrencies.AbstractCommand;
import me.settingdust.multicurrencies.locale.LocaleService;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;

public class CommandAdd extends AbstractCommand implements CommandExecutor {
    @Inject
    private PluginContainer pluginContainer;

    private final LocaleService localeService;
    private final ServiceManager serviceManager;

    @Inject
    public CommandAdd(LocaleService localeService, ServiceManager serviceManager) {
        this.localeService = localeService;
        this.serviceManager = serviceManager;

        alias("add", "a");

        serviceManager
            .provide(EconomyService.class)
            .ifPresent(
                economyService ->
                    commandSpec(
                        CommandSpec
                            .builder()
                            .permission("multicurrencies.command.add")
                            .description(localeService.getTextWithFallback("command.multicurrencies.subcommand.add.description"))
                            .executor(this)
                            .arguments(
                                optional(onlyOne(userOrSource(Text.of("user")))),
                                choices(
                                    Text.of("currency"),
                                    () ->
                                        economyService
                                            .getCurrencies()
                                            .stream()
                                            .map(Currency::getDisplayName)
                                            .map(Text::toPlain)
                                            .collect(Collectors.toList()),
                                    name ->
                                        economyService
                                            .getCurrencies()
                                            .stream()
                                            .filter(currency -> currency.getDisplayName().toPlain().equals(name))
                                            .findFirst()
                                            .orElse(null)
                                ),
                                bigDecimal(Text.of("amount")),
                                flags().flag("m").buildWith(GenericArguments.none())
                            )
                            .build()
                    )
            );
    }

    @Override
    @SuppressWarnings({ "DuplicatedCode", "NullableProblems" })
    public CommandResult execute(CommandSource src, CommandContext args) {
        Optional<Player> userOptional = args.getOne("user");
        if (!userOptional.isPresent()) {
            src.sendMessage(localeService.getTextWithFallback("error.missingPlayer"));
            return CommandResult.empty();
        }

        Optional<Currency> currencyOptional = args.getOne("currency");
        if (!currencyOptional.isPresent()) {
            src.sendMessage(localeService.getTextWithFallback("error.missingCurrency"));
            return CommandResult.empty();
        }

        Optional<BigDecimal> amountOptional = args.getOne("amount");
        if (!amountOptional.isPresent()) {
            src.sendMessage(localeService.getTextWithFallback("error.missingArgument", Tuple.of("name", "amount")));
            return CommandResult.empty();
        }

        User user = userOptional.get();

        serviceManager
            .provide(EconomyService.class)
            .ifPresent(
                economyService -> {
                    Optional<UniqueAccount> accountOptional = economyService.getOrCreateAccount(user.getUniqueId());
                    if (!accountOptional.isPresent()) {
                        src.sendMessage(localeService.getTextWithFallback("error.missingAccount"));
                        return;
                    }

                    Currency currency = currencyOptional.get();
                    BigDecimal amount = amountOptional.get();
                    UniqueAccount account = accountOptional.get();

                    if (account.deposit(currency, amount, Cause.of(EventContext.empty(), pluginContainer)).getResult().equals(ResultType.SUCCESS)) {
                        Text addMessage = localeService.getTextWithFallback(
                            "info.addBalance",
                            Tuple.of("currency", currency.getDisplayName()),
                            Tuple.of("amount", amount),
                            Tuple.of("symbol", currency.getSymbol())
                        );
                        src.sendMessage(addMessage);
                        src.sendMessage(
                            localeService.getTextWithFallback(
                                "info.currentBalance",
                                Tuple.of("currency", currency.getDisplayName()),
                                Tuple.of("amount", account.getBalance(currency)),
                                Tuple.of("symbol", currency.getSymbol())
                            )
                        );
                        if (args.hasAny("m")) {
                            user.getPlayer().ifPresent(player -> player.sendMessage(addMessage));
                        }
                    } else {
                        src.sendMessage(localeService.getTextWithFallback("error.commandFailed"));
                    }
                }
            );
        return CommandResult.success();
    }
}
