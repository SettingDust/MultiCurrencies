package me.settingdust.multicurrencies;

import com.google.inject.Inject;
import java.util.Objects;
import me.settingdust.multicurrencies.command.CommandMultiCurrencies;

public class CommandModule {

    @Inject
    public CommandModule(CommandMultiCurrencies multiCurrencies) {
        Objects.requireNonNull(multiCurrencies);
    }
}
