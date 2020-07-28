package me.settingdust.multicurrencies.command.multicurrencies;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;
import me.settingdust.multicurrencies.AbstractCommand;
import me.settingdust.multicurrencies.locale.LocaleService;
import org.spongepowered.api.Sponge;
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

public class CommandSet extends AbstractCommand implements CommandExecutor {
    @Inject
    private PluginContainer pluginContainer;

    private final LocaleService localeService;
    private final ServiceManager serviceManager;

    @Inject
    public CommandSet(LocaleService localeService, ServiceManager serviceManager) {
        this.localeService = localeService;
        this.serviceManager = serviceManager;

        alias("set", "s");

        serviceManager
            .provide(EconomyService.class)
            .ifPresent(
                economyService ->
                    commandSpec(
                        CommandSpec
                            .builder()
                            .permission("multicurrencies.command.set")
                            .description(localeService.getTextWithFallback("command.multicurrencies.subcommand.set.description"))
                            .executor(this)
                            .arguments(
                                GenericArguments.onlyOne(GenericArguments.userOrSource(Text.of("user"))),
                                GenericArguments.choices(
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
                                GenericArguments.bigDecimal(Text.of("amount")),
                                GenericArguments.flags().flag("m").buildWith(GenericArguments.none())
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

                    if (
                        account.setBalance(currency, amount, Cause.of(EventContext.empty(), pluginContainer)).getResult().equals(ResultType.SUCCESS)
                    ) {
                        Text currentBalanceMessage = localeService.getTextWithFallback(
                            "info.currentBalance",
                            Tuple.of("currency", currency.getDisplayName()),
                            Tuple.of("amount", account.getBalance(currency)),
                            Tuple.of("symbol", currency.getSymbol())
                        );
                        src.sendMessage(currentBalanceMessage);
                        user
                            .getPlayer()
                            .ifPresent(
                                player -> {
                                    if (args.hasAny("m")) {
                                        player.sendMessage(currentBalanceMessage);
                                    }
                                }
                            );
                    } else {
                        src.sendMessage(localeService.getTextWithFallback("error.commandFailed"));
                    }
                }
            );
        return CommandResult.success();
    }
}
