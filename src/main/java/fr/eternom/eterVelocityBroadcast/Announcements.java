package fr.eternom.eterVelocityBroadcast;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.eternom.eterVelocityLib.core.Config;
import fr.eternom.eterVelocityLib.helper.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Annonces automatiques (config.yml > announcements.list : astuces, Discord, événements...) : une à chaque tour, à tous
 * les joueurs concernés, dans leur langue (lang/ > announcements.<annonce>). Ordre mélangé, chaque annonce une fois par
 * tour complet, jamais deux fois de suite. Une annonce peut viser certains serveurs : ceux dont le nom commence par un des préfixes de servers (les lobbys créés
 * par l'orchestrateur s'appellent lobby-1, lobby-2...).
 */
final class Announcements {

    /** Une annonce : sa clé dans lang/ (announcements.<id>), et les débuts de noms de serveurs visés (vide = partout). */
    private record Announcement(String id, List<String> servers) {

        boolean concerns(Player player) {
            if (servers.isEmpty()) {
                return true;
            }
            String server = player.getCurrentServer().map(ServerConnection::getServerInfo)
                    .map(info -> info.getName().toLowerCase(Locale.ROOT)).orElse("");
            return servers.stream().anyMatch(server::startsWith);
        }
    }

    private final List<Announcement> all;
    /** Tour en cours : les annonces pas encore faites, dans l'ordre (tâche du proxy : un seul fil à la fois). */
    private final List<Announcement> round = new ArrayList<>();
    private Announcement last;

    private Announcements(List<Announcement> all) {
        this.all = all;
    }

    static Announcements load(Config config, Logger logger) {
        List<Announcement> list = new ArrayList<>();
        for (String id : config.getKeys("announcements.list")) {
            if (!id.matches("[a-z0-9_-]+")) {
                logger.warn("announcements.list.{} : identifiant invalide (minuscules, chiffres, - et _), ignoré", id);
                continue;
            }
            List<String> servers = config.getStringList("announcements.list." + id + ".servers").stream()
                    .map(server -> server.toLowerCase(Locale.ROOT)).toList();
            list.add(new Announcement(id, servers));
        }
        return new Announcements(List.copyOf(list));
    }

    boolean isEmpty() {
        return all.isEmpty();
    }

    int size() {
        return all.size();
    }

    /** L'annonce suivante, à chaque joueur qu'elle concerne ; rien si personne n'est connecté. */
    void announce(ProxyServer proxy, Messages messages) {
        if (proxy.getPlayerCount() == 0) {
            return;
        }
        Announcement announcement = next();
        for (Player player : proxy.getAllPlayers()) {
            if (announcement.concerns(player)) {
                messages.send(player, "announcements." + announcement.id(), TagResolver.empty());
            }
        }
    }

    private Announcement next() {
        if (round.isEmpty()) {
            round.addAll(all);
            Collections.shuffle(round);
            // Nouveau tour : pas la même annonce que la dernière du tour précédent
            if (round.size() > 1 && round.getFirst().equals(last)) {
                Collections.swap(round, 0, round.size() - 1);
            }
        }
        last = round.removeFirst();
        return last;
    }
}
