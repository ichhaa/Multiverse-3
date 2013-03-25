package com.mvplugin.core.command;

import com.dumptruckman.minecraft.pluginbase.command.CommandContext;
import com.dumptruckman.minecraft.pluginbase.command.CommandInfo;
import com.dumptruckman.minecraft.pluginbase.messages.Message;
import com.dumptruckman.minecraft.pluginbase.messages.Theme;
import com.dumptruckman.minecraft.pluginbase.minecraft.BasePlayer;
import com.dumptruckman.minecraft.pluginbase.permission.Perm;
import com.mvplugin.core.exceptions.WorldCreationException;
import com.mvplugin.core.minecraft.WorldEnvironment;
import com.mvplugin.core.plugin.MultiverseCore;
import com.mvplugin.core.util.Language;
import com.mvplugin.core.util.MVTheme;
import com.mvplugin.core.util.Perms;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@CommandInfo(
        primaryAlias = "import",
        desc = "Imports an existing world.",
        usage = "{NAME} {ENVIRONMENT}",
        prefixedAliases = "im",
        directlyPrefixedAliases = "import",
        flags = "g:n",
        min = 1,
        max = 2
)
public class ImportCommand extends MultiverseCommand {

    public static final Message IMPORT_HELP = Message.createMessage("command.import.help",
            Theme.HELP + "Imports a world into the server from a folder with the given name."
            + "\n" + Theme.HELP + "The folder must exist in the location where worlds are normally located and must contain Minecraft world data."
            + "\n" + Theme.HELP + "You must specify a world environment such as " + MVTheme.WORLD_NORMAL + "NORMAL " + Theme.HELP + "or " + MVTheme.WORLD_NORMAL + "NETHER" + Theme.HELP + "."
            + "\n" + Theme.HELP + "You may also specify a generator to use along with an optional generator ID."
            + "\n" + Theme.HELP + "The generator name is case sensitive!"
            + "\n" + Theme.HELP + "Flags:"
            + "\n" + Theme.CMD_FLAG + "  -g " + Theme.REQ_ARG + "{GENERATOR" + Theme.OPT_ARG + "[:ID]" + Theme.REQ_ARG + "}" + Theme.HELP + " Specify a generator."
            + "\n" + Theme.CMD_FLAG + "  -n " + Theme.HELP + "Do not adjust spawn"
            + "\n" + Theme.HELP + "Examples:"
                    + "\n  " + Theme.CMD_USAGE + "/mv import " + Theme.REQ_ARG + "gargamel " + MVTheme.WORLD_NORMAL + "normal"
                    + "\n  " + Theme.CMD_USAGE + "/mv import " + Theme.REQ_ARG + "\"hell world\" " + MVTheme.WORLD_NETHER + "nether"
                    + "\n  " + Theme.CMD_USAGE + "/mv import " + Theme.REQ_ARG + "space " + MVTheme.WORLD_NORMAL + "normal " + Theme.CMD_FLAG + "-g " + Theme.REQ_ARG + "CleanroomGenerator" + Theme.OPT_ARG + ":.");

    public static final Message POTENTIAL_WORLD_LIST = Message.createMessage("command.import.potential_world_list",
            Theme.INFO + "====[ These look like worlds ]====\n%s");

    public static final Message NO_POTENTIAL_WORLDS = Message.createMessage("command.import.no_potential_worlds",
            Theme.SORRY + "No potential worlds found. Sorry!");

    public static final Message STARTING_IMPORT = Message.createMessage("command.import.starting_import",
            Theme.PLEASE_WAIT + "Starting import of world '%s'...");

    public static final Message IMPORT_COMPLETE = Message.createMessage("command.import.import_complete",
            Theme.SUCCESS + "Import complete!");

    public static final Message IMPORT_FAILED = Message.createMessage("command.import.import_failed",
            Theme.FAILURE + "Import failed!");

    public static final Message NON_EXISTENT_FOLDER = Message.createMessage("command.import.non_existent_folder",
            Theme.FAILURE + "That world folder does not exist."
            + Theme.INFO + "These look like worlds to me: \n%s");

    protected ImportCommand(@NotNull final MultiverseCore plugin) {
        super(plugin);
    }

    @Override
    public Perm getPerm() {
        return Perms.CMD_IMPORT;
    }

    @NotNull
    @Override
    public Message getHelp() {
        return IMPORT_HELP;
    }

    @Override
    public boolean runCommand(@NotNull final BasePlayer sender, @NotNull final CommandContext context) {
        final String worldName = context.getString(0);

        if (worldName.toLowerCase().equals("--list") || worldName.toLowerCase().equals("-l")) {
            final String worldList = this.getPotentialWorlds();
            if (worldList.length() > 2) {
                getMessager().message(sender, POTENTIAL_WORLD_LIST, worldList);
            } else {
                getMessager().message(sender, NO_POTENTIAL_WORLDS);
            }
            return true;
        }
        // Since we made an exception for the list, we have to make sure they have at least 2 params:
        // Note the exception is --list, which is covered above.
        if (context.argsLength() == 1) {
            return false;
        }

        // Make sure we don't already know about this world.
        if (getPlugin().getWorldManager().isManaged(worldName)) {
            getMessager().message(sender, Language.WORLD_ALREADY_EXISTS, worldName);
            return true;
        }

        File worldFile = new File(getPlugin().getServerInterface().getWorldContainer(), worldName);

        String generator = context.getFlag('g');
        boolean useSpawnAdjust = !context.hasFlag('n');

        String env = context.getString(1);
        WorldEnvironment environment = WorldEnvironment.getFromString(env);
        if (environment == null) {
            getMessager().message(sender, IMPORT_FAILED);
            getMessager().message(sender, Language.INVALID_ENVIRONMENT, env);
            // TODO EnvironmentCommand.showEnvironments(sender);
            return true;
        }

        if (worldFile.exists() && env != null) {
            getMessager().messageAndLog(sender, STARTING_IMPORT, worldName);
            try {
                getPlugin().getWorldManager().addWorld(worldName, environment, null, null, null, generator, useSpawnAdjust);
                getMessager().messageAndLog(sender, IMPORT_COMPLETE);
            } catch (WorldCreationException e) {
                getMessager().message(sender, IMPORT_FAILED);
                e.sendException(getMessager(), sender);
            }
        } else {
            getMessager().message(sender, IMPORT_FAILED);
            String worldList = this.getPotentialWorlds();
            getMessager().message(sender, NON_EXISTENT_FOLDER, worldList);
        }
        return true;
    }

    @NotNull
    private String getPotentialWorlds() {
        final StringBuilder worldList = new StringBuilder();
        int i = 1;
        for (final String potentialWorld : getPlugin().getWorldManager().getPotentialWorlds()) {
            if (!getPlugin().getWorldManager().isManaged(potentialWorld)) {
                if (i % 2 == 0) {
                    worldList.append(Theme.LIST_EVEN);
                } else {
                    worldList.append(Theme.LIST_ODD);
                }
                worldList.append(potentialWorld).append(" ");
                i++;
            }
        }
        return worldList.toString();
    }
}
