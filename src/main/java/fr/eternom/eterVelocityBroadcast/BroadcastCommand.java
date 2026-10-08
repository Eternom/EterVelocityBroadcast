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

    BroadcastCommand(ProxyServer proxy, Messages messages) {
        this.proxy = proxy;
        this.messages = messages;
    }

    @Override
    public void execute(Invocation invocation) {
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

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(EterVelocityBroadcast.PERMISSION);
    }
}
