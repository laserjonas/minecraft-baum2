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
import de.baum2dev.baum2.classes.ClassSubspec;
import de.baum2dev.baum2.classes.PlayerClass;
import de.baum2dev.baum2.classes.SubspecDefinition;
import de.baum2dev.baum2.classes.SubspecRegistry;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;
import de.baum2dev.baum2.skills.Spell;
import de.baum2dev.baum2.skills.SpellCaster;

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
                    .then(CommandManager.literal("subspec")
                        .then(CommandManager.literal("list")
                            .executes(context -> subspecListCommand(
                                context.getSource(),
                                context.getSource().getPlayer()
                            ))
                        )
                        .then(CommandManager.literal("select")
                            .then(CommandManager.argument("subspec", StringArgumentType.word())
                                .suggests(Baum2Commands::suggestSubspecNames)
                                .executes(context -> subspecSelectCommand(
                                    context.getSource(),
                                    context.getSource().getPlayer(),
                                    StringArgumentType.getString(context, "subspec")
                                ))
                            )
                        )
                    )
                )
                .then(CommandManager.literal("cast")
                    .then(CommandManager.argument("spell", StringArgumentType.word())
                        .suggests(Baum2Commands::suggestSpellNames)
                        .executes(context -> castCommand(
                            context.getSource(),
                            context.getSource().getPlayer(),
                            StringArgumentType.getString(context, "spell")
                        ))
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

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSubspecNames(
        com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
        com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        Optional<PlayerClass> currentClass = context.getSource().getPlayer() != null
            ? ClassManager.getSelectedClass(context.getSource().getPlayer())
            : Optional.empty();
        return CommandSource.suggestMatching(
            Arrays.stream(ClassSubspec.values())
                .filter(subspec -> currentClass.isEmpty() || subspec.parentClass() == currentClass.get())
                .map(subspec -> subspec.name().toLowerCase(Locale.ROOT)),
            builder
        );
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSpellNames(
        com.mojang.brigadier.context.CommandContext<ServerCommandSource> context,
        com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        Optional<PlayerClass> currentClass = context.getSource().getPlayer() != null
            ? ClassManager.getSelectedClass(context.getSource().getPlayer())
            : Optional.empty();
        return CommandSource.suggestMatching(
            Arrays.stream(Spell.values())
                .filter(spell -> currentClass.isEmpty() || spell.requiredClass() == currentClass.get())
                .map(spell -> spell.name().toLowerCase(Locale.ROOT)),
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

    private static int subspecListCommand(ServerCommandSource source, ServerPlayerEntity player) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        Optional<PlayerClass> currentClass = ClassManager.getSelectedClass(player);
        if (currentClass.isEmpty()) {
            source.sendError(Text.literal("You have not selected a class yet. Use /baum2 class select <class>"));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Sub-specializations for " + ClassRegistry.get(currentClass.get()).displayName() + ":"), false);
        for (SubspecDefinition definition : SubspecRegistry.forClass(currentClass.get())) {
            source.sendFeedback(() -> Text.literal(String.format(" - %s: %s (Bonus: %s)",
                definition.displayName(),
                definition.description(),
                describeBonus(definition)
            )), false);
        }
        return 1;
    }

    private static int subspecSelectCommand(ServerCommandSource source, ServerPlayerEntity player, String rawSubspecName) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        ClassSubspec subspec = parseSubspec(source, rawSubspecName);
        if (subspec == null) {
            return 0;
        }

        if (!ClassManager.selectSubspec(player, subspec)) {
            source.sendError(Text.literal(subspec.name() + " belongs to a different class. Select that class first with /baum2 class select <class>."));
            return 0;
        }

        SubspecDefinition definition = SubspecRegistry.get(subspec);
        source.sendFeedback(() -> Text.literal(String.format("You are now a %s. Bonus: %s",
            definition.displayName(),
            describeBonus(definition)
        )), false);
        return 1;
    }

    private static ClassSubspec parseSubspec(ServerCommandSource source, String rawSubspecName) {
        try {
            return ClassSubspec.valueOf(rawSubspecName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Unknown sub-specialization: " + rawSubspecName));
            return null;
        }
    }

    private static int castCommand(ServerCommandSource source, ServerPlayerEntity player, String rawSpellName) {
        if (player == null) {
            source.sendError(Text.literal("Must be run by a player"));
            return 0;
        }

        Spell spell;
        try {
            spell = Spell.valueOf(rawSpellName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Unknown spell: " + rawSpellName));
            return 0;
        }

        SpellCaster.CastAttempt attempt = SpellCaster.attemptCast(player, spell);
        switch (attempt.result()) {
            case WRONG_CLASS -> {
                source.sendError(Text.literal(spell.displayName() + " requires the " + ClassRegistry.get(spell.requiredClass()).displayName() + " class."));
                return 0;
            }
            case ON_COOLDOWN -> {
                double remainingSeconds = attempt.remainingCooldownTicks() / 20.0;
                source.sendError(Text.literal(String.format("%s is on cooldown (%.1fs remaining).", spell.displayName(), remainingSeconds)));
                return 0;
            }
            case SUCCESS -> {
                source.sendFeedback(() -> Text.literal("You cast " + spell.displayName() + "."), false);
                return 1;
            }
        }
        return 0;
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
        return describeBonus(definition.bonusOperation(), definition.bonusAmount(), definition.bonusAttribute());
    }

    private static String describeBonus(SubspecDefinition definition) {
        return describeBonus(definition.bonusOperation(), definition.bonusAmount(), definition.bonusAttribute());
    }

    private static String describeBonus(
        net.minecraft.entity.attribute.EntityAttributeModifier.Operation operation,
        double amount,
        RegistryEntry<EntityAttribute> attribute
    ) {
        String amountText = switch (operation) {
            case ADD_VALUE -> String.format("+%.1f", amount);
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> String.format("+%.0f%%", amount * 100);
        };
        return amountText + " " + attributeName(attribute);
    }

    private static String attributeName(RegistryEntry<EntityAttribute> attribute) {
        return attribute.getKey().map(key -> key.getValue().getPath()).orElse("bonus");
    }
}
