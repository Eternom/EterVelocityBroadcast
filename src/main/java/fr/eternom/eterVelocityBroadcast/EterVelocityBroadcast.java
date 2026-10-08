package fr.eternom.eterVelocityBroadcast;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.eternom.eterVelocityLib.EterVelocityLib;
import fr.eternom.eterVelocityLib.core.Config;
import fr.eternom.eterVelocityLib.helper.Messages;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * EterVelocityBroadcast : les annonces de tout le réseau, depuis le proxy (qui voit tous les joueurs de tous les
 * serveurs : une seule config, jamais d'annonce en double). Annonces automatiques dans la langue de chaque joueur
 * (astuces, Discord, événements...), et /broadcast pour le staff.
 */
@Plugin(id = "etervelocitybroadcast", name = "EterVelocityBroadcast", version = "1.0.0", authors = {"NadTum"},
        description = "Annonces du réseau : annonces automatiques et /broadcast",
        dependencies = {@Dependency(id = "etervelocitylib")})
public final class EterVelocityBroadcast {

    public static final String PERMISSION = "etervelocitybroadcast.broadcast";

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public EterVelocityBroadcast(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        Config config;
        Messages messages;
        try {
            config = new Config(EterVelocityBroadcast.class, dataDirectory);
            messages = EterVelocityLib.get().messages(EterVelocityBroadcast.class, dataDirectory, logger);
        } catch (IOException | RuntimeException e) {
            logger.error("config.yml ou lang/ illisible (vérifie la syntaxe YAML), EterVelocityBroadcast désactivé");
            return;
        }
        CommandManager commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder("broadcast").aliases("bc").plugin(this).build(),
                new BroadcastCommand(proxy, messages));

        Announcements announcements = Announcements.load(config, logger);
        int minutes = config.getInt("announcements.interval-minutes", 5);
        if (minutes > 0 && !announcements.isEmpty()) {
            Duration interval = Duration.ofMinutes(minutes);
            proxy.getScheduler().buildTask(this, () -> announcements.announce(proxy, messages))
                    .delay(interval).repeat(interval).schedule();
            logger.info("{} annonces, une toutes les {} min", announcements.size(), minutes);
        }
    }
}
