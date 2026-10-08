# EterVelocityBroadcast

Les **annonces du réseau**, côté **proxy Velocity**. Document développeur, à tenir à jour avec le code.
Sur le proxy plutôt que sur Paper : il voit tous les joueurs de tous les serveurs, donc une seule config et jamais
d'annonce envoyée deux fois (par deux serveurs).

## Fonctionnement

- **Annonces automatiques** (`Announcements`) : astuces, Discord, événements, nouveautés… Une toutes les
  `announcements.interval-minutes` (5 ; 0 = aucune), rien si personne n'est connecté.
  - La liste est dans `config.yml` (`announcements.list`, identifiant → serveurs visés), le **texte** dans `lang/`
    (`announcements.<identifiant>`), dans la langue de chaque joueur, avec le préfixe du réseau. MiniMessage :
    commandes cliquables (`<click:run_command:'/lobby'>`), survol, liens.
  - `servers` : débuts de noms de serveurs qui la reçoivent (`lobby` vise `lobby-1`, `lobby-2`… de l'orchestrateur) ;
    absent = tout le réseau.
  - Ordre mélangé, chaque annonce une fois par tour complet, jamais deux fois de suite (même d'un tour à l'autre).
  - Les joueurs ne peuvent pas les couper (choix du serveur) : d'où un intervalle large.
- **`/broadcast <message>`** (alias `/bc`, permission `etervelocitybroadcast.broadcast`, console du proxy comprise) :
  à tout le réseau, dans le format de chaque langue (`broadcast.format`), avec un petit son. Le texte du staff est lu
  en MiniMessage. Remplace l'ancien `/broadcast` d'EterEssential (qui passait par Redis et chaque serveur).

## Technique

- Dépend d'**EterVelocityLib** (`@Dependency`) : config, langues (repli sur les textes communs), préfixe et palette.
- La version est aussi écrite dans `@Plugin` : à garder identique à `gradle.properties`.
- Compilation : `gradlew build` → `build/libs/EterVelocityBroadcast-<version>.jar`, copié dans `<eterPluginsDir>/velocity`.
