package de.baum2dev.baum2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import de.baum2dev.baum2.classes.ClassDefinition;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.classes.ClassRegistry;
import de.baum2dev.baum2.classes.PlayerClass;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;

public class Baum2Commands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(Baum2Commands::registerBaum2Commands);
    }

    private static void registerBaum2Commands(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("baum2")
                .then(CommandManager.literal("addxp")
                    .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                        .executes(context -> addXpCommand(
                            context.getSource(),
                            context.getSource().getPlayer(),
                            LongArgumentType.getLong(context, "amount")
                        ))
                    )
                )
                .then(CommandManager.literal("level")
                    .executes(context -> levelCommand(
                        context.getSource(),
                        context.getSource().getPlayer()
                    ))
                )
                .then(CommandManager.literal("class")
                    .then(CommandManager.literal("list")
                        .executes(context -> classListCommand(context.getSource()))
                    )
                    .then(CommandManager.literal("info")
                        .executes(context -> classInfoCommand(
                            context.getSource(),
                            context.getSource().getPlayer()
                        ))
                        .then(CommandManager.argument("class", StringArgumentType.word())
                            .suggests(Baum2Commands::suggestClassNames)
                            .executes(context -> classInfoCommand(
                                context.getSource(),
                                StringArgumentType.getString(context, "class")
                            ))
                        )
                    )
                    .then(CommandManager.literal("select")
                        .then(CommandManager.argument("class", StringArgumentType.word())
                            .suggests(Baum2Commands::suggestClassNames)
                            .executes(context -> classSelectCommand(
                                context.getSource(),
                                context.getSource().getPlayer(),
                                StringArgumentType.getString(context, "class")
                            ))
                        )
                    )
                )
        );
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestClassNames(
        com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
        com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        return CommandSource.suggestMatching(
            Arrays.stream(PlayerClass.values()).map(playerClass -> playerClass.name().toLowerCase(Locale.ROOT)),
            builder
        );
    }

    private static int addXpCommand(ServerCommandSource source, ServerPlayerEntity player, long amount) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        PlayerLevelSystem.addExperience(player, amount);
        PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
        source.sendFeedback(
            () -> Text.literal(String.format("Added %d XP. Level: %d, XP: %d/%d",
                amount,
                progress.getLevel(),
                progress.getExperience(),
                progress.getExperienceForNextLevel()
            )),
            false
        );
        return 1;
    }

    private static int levelCommand(ServerCommandSource source, ServerPlayerEntity player) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
        float progress_pct = progress.getExperienceProgress() * 100;
        source.sendFeedback(
            () -> Text.literal(String.format("Level: %d | XP: %d / %d (%.1f%%)",
                progress.getLevel(),
                progress.getExperience(),
                progress.getExperienceForNextLevel(),
                progress_pct
            )),
            false
        );
        return 1;
    }

    private static int classListCommand(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Available classes:"), false);
        for (ClassDefinition definition : ClassRegistry.all()) {
            source.sendFeedback(() -> Text.literal(String.format(" - %s: %s (Bonus: %s)",
                definition.displayName(),
                definition.description(),
                describeBonus(definition)
            )), false);
        }
        return 1;
    }

    private static int classInfoCommand(ServerCommandSource source, ServerPlayerEntity player) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        Optional<PlayerClass> selected = ClassManager.getSelectedClass(player);
        if (selected.isEmpty()) {
            source.sendError(Text.literal("You have not selected a class yet. Use /baum2 class select <class>"));
            return 0;
        }

        return sendClassInfo(source, ClassRegistry.get(selected.get()));
    }

    private static int classInfoCommand(ServerCommandSource source, String rawClassName) {
        PlayerClass playerClass = parsePlayerClass(source, rawClassName);
        if (playerClass == null) {
            return 0;
        }

        return sendClassInfo(source, ClassRegistry.get(playerClass));
    }

    private static int classSelectCommand(ServerCommandSource source, ServerPlayerEntity player, String rawClassName) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        PlayerClass playerClass = parsePlayerClass(source, rawClassName);
        if (playerClass == null) {
            return 0;
        }

        ClassManager.selectClass(player, playerClass);
        ClassDefinition definition = ClassRegistry.get(playerClass);
        source.sendFeedback(() -> Text.literal(String.format("You are now a %s. Bonus: %s",
            definition.displayName(),
            describeBonus(definition)
        )), false);
        return 1;
    }

    private static PlayerClass parsePlayerClass(ServerCommandSource source, String rawClassName) {
        try {
            return PlayerClass.valueOf(rawClassName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Unknown class: " + rawClassName));
            return null;
        }
    }

    private static int sendClassInfo(ServerCommandSource source, ClassDefinition definition) {
        source.sendFeedback(() -> Text.literal(String.format("%s: %s | Bonus: %s",
            definition.displayName(),
            definition.description(),
            describeBonus(definition)
        )), false);
        return 1;
    }

    private static String describeBonus(ClassDefinition definition) {
        String amountText = switch (definition.bonusOperation()) {
            case ADD_VALUE -> String.format("+%.1f", definition.bonusAmount());
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> String.format("+%.0f%%", definition.bonusAmount() * 100);
        };
        return amountText + " " + attributeName(definition.bonusAttribute());
    }

    private static String attributeName(RegistryEntry<EntityAttribute> attribute) {
        return attribute.getKey().map(key -> key.getValue().getPath()).orElse("bonus");
    }
}
