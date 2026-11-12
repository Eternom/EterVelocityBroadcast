package fr.eternom.eterVelocityBroadcast;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.eternom.eterVelocityLib.helper.Messages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * /broadcast <message> (alias /bc) : annonce à tout le réseau, dans le format de chaque langue (broadcast.format), avec
 * un petit son. Le texte du staff est lu en MiniMessage (couleurs, liens...) : commande réservée au staff (et à la
 * console du proxy).
 */
final class BroadcastCommand implements SimpleCommand {

    private static final Sound CHIME = Sound.sound(Key.key("block.note_block.chime"), Sound.Source.MASTER, 0.6f, 1.2f);

    private final ProxyServer proxy;
    private final Messages messages;

    private final Object plugin;

    BroadcastCommand(Object plugin, ProxyServer proxy, Messages messages) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.messages = messages;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 2 && args[0].equalsIgnoreCase("restart")) {
            restart(invocation, args[1]);
            return;
        }
        if (invocation.arguments().length == 0) {
            messages.send(invocation.source(), "broadcast.usage", TagResolver.empty());
            return;
        }
        Component text = MiniMessage.miniMessage().deserialize(String.join(" ", invocation.arguments()));
        for (Player player : proxy.getAllPlayers()) {
            player.sendMessage(messages.get(player, "broadcast.format", TagResolver.resolver(Placeholder.component("message", text))));
            player.playSound(CHIME);
        }
        proxy.getConsoleCommandSource().sendMessage(messages.get(proxy.getConsoleCommandSource(), "broadcast.format",
                TagResolver.resolver(Placeholder.component("message", text))));
    }

    /**
     * /broadcast restart <minutes> : annonce un redémarrage du réseau tout de suite, puis à 10, 5 et 1 min (celles qui
     * restent). Seulement une annonce : le redémarrage lui-même reste celui du panel (tâche planifiée, qui peut lancer
     * cette commande sur la console du proxy quelques minutes avant).
     */
    private void restart(Invocation invocation, String value) {
        int minutes;
        try {
            minutes = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            messages.send(invocation.source(), "broadcast.usage", TagResolver.empty());
            return;
        }
        if (minutes < 1 || minutes > 120) {
            messages.send(invocation.source(), "broadcast.usage", TagResolver.empty());
            return;
        }
        announceRestart(minutes);
        for (int left : new int[]{10, 5, 1}) {
            if (left < minutes) {
                proxy.getScheduler().buildTask(plugin, () -> announceRestart(left)).delay(minutes - left, java.util.concurrent.TimeUnit.MINUTES).schedule();
            }
        }
        messages.send(invocation.source(), "broadcast.restart-scheduled", TagResolver.resolver(Placeholder.unparsed("minutes", String.valueOf(minutes))));
    }

    private void announceRestart(int minutes) {
        TagResolver left = TagResolver.resolver(Placeholder.unparsed("minutes", String.valueOf(minutes)));
        for (Player player : proxy.getAllPlayers()) {
            player.sendMessage(messages.get(player, "broadcast.restart", left));
            player.playSound(CHIME);
            if (minutes <= 1) {
                player.showTitle(net.kyori.adventure.title.Title.title(messages.get(player, "broadcast.restart-title", left),
                        messages.get(player, "broadcast.restart", left)));
            }
        }
        proxy.getConsoleCommandSource().sendMessage(messages.get(proxy.getConsoleCommandSource(), "broadcast.restart", left));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(EterVelocityBroadcast.PERMISSION);
    }
}
