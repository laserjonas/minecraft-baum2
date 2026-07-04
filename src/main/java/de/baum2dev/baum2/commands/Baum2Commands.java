package de.baum2dev.baum2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
}
