# Mon travail sur GeoPeople

## 1. Objet et périmètre

Dans ce document, je présente les principales contributions que j’ai réalisées sur le projet `GeoPeople` à travers mes différents commits. Mon travail couvre plusieurs aspects du développement : la mise en place de l’application Android, la création du backend, la synchronisation des données entre le client et le serveur, ainsi que l’intégration de mini-jeux et diverses améliorations fonctionnelles.

Au cours du projet, j’ai participé à la construction progressive d’une application complète reposant sur une architecture client-serveur. Les développements réalisés concernent aussi bien l’interface utilisateur que la logique métier et les mécanismes de communication entre les différentes couches de l’application.

---

## 2. Résumé des contributions

Mon travail s’est organisé en plusieurs étapes.

Dans un premier temps, j’ai développé le socle de l’application Android en mettant en place la carte, l’inventaire, les écrans de capture et les mécanismes de navigation. J’ai ensuite structuré le projet autour d’une architecture plus propre en séparant le frontend et le backend.

Par la suite, j’ai développé les modèles de données, les routes API et les services nécessaires au fonctionnement du serveur. Une fois cette base établie, j’ai connecté l’application mobile au backend afin de synchroniser la position GPS du joueur, son état et les informations liées aux captures.

Enfin, j’ai intégré plusieurs mini-jeux, ajouté de nouvelles ressources graphiques et effectué différents correctifs ainsi que des améliorations d’interface avant la phase de finalisation du projet.

---

# 3. Chronologie des contributions

## 3.1 Mise en place du socle mobile

La première phase du développement a consisté à construire la structure principale de l’application Android.

J’ai développé :

- l’intégration de la carte OpenStreetMap ;
- l’affichage des marqueurs représentant le joueur et les cartes disponibles ;
- l’écran de capture ;
- l’inventaire des cartes capturées ;
- l’écran principal du jeu ;
- la navigation entre les différentes vues ;
- le `GameViewModel` chargé de centraliser l’état du jeu ;
- la gestion des permissions Android.

Cette étape a permis de disposer d’une première version fonctionnelle de l’application dans laquelle un utilisateur peut visualiser son environnement, interagir avec les éléments présents sur la carte et consulter les cartes obtenues.

### Principaux commits

- `46f7b6e` : intégration d’OpenStreetMap et des marqueurs.
- `3d30d5e` : ajout de l’overlay de capture.
- `bbbaabd` : création de l’inventaire.
- `cd195a0` : création de l’écran principal du jeu.
- `88f9abf` : ajout de la navigation.
- `a17181b` : implémentation du `GameViewModel`.
- `ced95dc` : gestion des permissions et intégration dans `MainActivity`.

---

## 3.2 Séparation du frontend et du backend

Une fois la première version de l’interface réalisée, j’ai restructuré le projet afin de distinguer clairement les responsabilités entre la partie mobile et la partie serveur.

Cette séparation a permis :

- d’isoler la logique métier ;
- de faciliter les échanges entre les composants ;
- d’améliorer la maintenabilité du projet ;
- de préparer l’intégration d’une API dédiée.

Les commits de fusion réalisés durant cette période montrent également l’intégration progressive de branches fonctionnelles dédiées au backend Express.

---

## 3.3 Développement du backend

J’ai ensuite développé les premières briques du backend.

Cette phase comprend :

- la création du modèle `Player` ;
- la création du modèle `Capture` ;
- l’implémentation des routes HTTP ;
- la création des services métier ;
- la mise en place des endpoints nécessaires aux interactions avec l’application mobile.

Les principales fonctionnalités serveur permettent :

- de gérer les joueurs ;
- d’enregistrer les captures ;
- de récupérer les cartes situées à proximité ;
- de fournir les données nécessaires au fonctionnement du jeu.

L’architecture adoptée repose sur une séparation entre modèles, routes et services afin de limiter le couplage entre les différentes couches applicatives.

### Principaux commits

- `6a6afb5` : création des modèles `Player` et `Capture`.
- `80dd9fc` : implémentation des routes et services backend.

---

## 3.4 Synchronisation entre l’application et le serveur

Une étape importante du projet a consisté à connecter l’application Android au backend.

J’ai implémenté :

- la communication avec l’API ;
- l’envoi de la position GPS ;
- la récupération de l’état du joueur ;
- la synchronisation des données de capture ;
- les adaptations nécessaires au niveau du manifeste Android.

Les principaux composants concernés sont :

- `CardRepository.kt`
- `GameViewModel.kt`
- le manifeste Android

Cette intégration a permis de passer d’une application principalement locale à une application connectée capable d’échanger des données avec un serveur distant.

### Principal commit

- `f2861f0` : synchronisation GPS et état du joueur avec le backend.

---

## 3.5 Intégration des mini-jeux

Après la mise en place des fonctionnalités principales, j’ai ajouté plusieurs mini-jeux afin d’étendre les mécaniques de gameplay du projet.

Cette phase comprend :

- l’intégration de nouveaux modules Android ;
- l’ajout de ressources graphiques ;
- le raccordement des mini-jeux au système de capture ;
- la création de nouveaux écrans et composants.

Les commits associés représentent une part importante du volume de code ajouté au dépôt. Ils introduisent de nombreux fichiers, ressources et composants nécessaires à l’exécution des mini-jeux.

### Principaux commits

- `285b03a` : ajout des mini-jeux.
- `d908c38` : intégration d’un mini-jeu dans le flux de capture.
- `0c180cc` : ajout de ressources graphiques.
- `ed58b27` : fusion de la branche liée aux mini-jeux.

---

## 3.6 Corrections et finalisation

La dernière phase du projet a été consacrée à la stabilisation de l’application.

Les modifications réalisées concernent principalement :

- les corrections visuelles ;
- les ajustements d’interface ;
- la documentation ;
- la résolution de problèmes détectés lors des tests.

Cette étape a permis d’améliorer la qualité globale du projet avant sa livraison finale.

### Principaux commits

- `a886ce1` : correction du style des boutons.
- `10623b2` : corrections diverses.
- `c37db8b` : mise à jour du README.

---

# 4. Analyse technique

## 4.1 Architecture applicative

L’application repose sur une architecture client-serveur composée :

- d’un frontend Android développé avec Kotlin et Jetpack Compose ;
- d’un backend Node.js basé sur Express.

Côté serveur, les responsabilités sont réparties entre :

- les modèles ;
- les routes ;
- les services.

Côté Android, l’état du jeu est centralisé dans un `GameViewModel`, ce qui limite la duplication de logique entre les écrans et facilite la gestion du cycle de vie de l’application.

---

## 4.2 Fonctionnalités de géolocalisation

La géolocalisation constitue un élément central du projet.

L’application exploite :

- les permissions Android de localisation ;
- les coordonnées GPS du joueur ;
- la synchronisation avec le backend ;
- la mise à jour dynamique des éléments affichés sur la carte.

Cette fonctionnalité permet de relier directement l’environnement réel du joueur à l’état du jeu.

---

## 4.3 Mini-jeu TP4 (Adam_memory)

Le mini-jeu `TP4` est un jeu de mémoire reposant sur l’association de cartes.

Les principaux composants développés sont :

- `MemoryGame`
- `MemoryBoardManager`
- `BoardViewModel`
- `MemoryCard`
- `FishDisplayer`
- `Chronometer`
- `LoadBar`

La logique implémentée comprend :

- la génération des cartes ;
- la gestion des états visibles et cachés ;
- la comparaison des paires ;
- la progression de la partie ;
- la conservation de l’état via un `ViewModel`.

L’interface utilise Jetpack Compose afin de fournir un affichage réactif et cohérent avec le reste du projet.

---

## 4.4 Mini-jeu Pokeball (Adam_pokeball)

Le mini-jeu `Pokeball` repose sur un moteur de jeu utilisant les capteurs du téléphone et une simulation physique simplifiée.

Les principaux composants sont :

- `GameEngine`
- `GameScreen`
- `Pokeball`
- `AccelerometerManager`
- `ClapDetector`

Les fonctionnalités implémentées comprennent :

- la gestion de la gravité ;
- les collisions ;
- les rebonds ;
- les déplacements ;
- la rotation de l’objet principal ;
- le suivi de caméra ;
- l’interaction avec les capteurs matériels.

L’architecture sépare clairement le moteur de jeu de la couche d’affichage afin de faciliter l’évolution du module.

---

# 5. Évaluation des contributions

## Points forts

- progression fonctionnelle cohérente ;
- architecture client-serveur clairement identifiée ;
- séparation des responsabilités côté backend ;
- centralisation de l’état côté Android ;
- intégration progressive de nouvelles fonctionnalités ;
- ajout de plusieurs mécaniques de gameplay complémentaires.

## Points d’amélioration

- certains messages de commit sont peu descriptifs ;
- certains commits regroupent un volume important de modifications ;
- l’historique pourrait être davantage structuré avec une convention de nommage uniforme (`feat`, `fix`, `docs`, `refactor`).

---

# 6. Conclusion

Mon travail sur GeoPeople couvre l’ensemble des principales couches du projet : interface Android, architecture applicative, backend, communication réseau, géolocalisation et mini-jeux.

Les contributions réalisées ont permis de faire évoluer le projet depuis une structure mobile initiale vers une application complète reposant sur une architecture client-serveur et intégrant plusieurs mécanismes de gameplay. L’ensemble des développements s’inscrit dans une progression cohérente qui aboutit à une application fonctionnelle, connectée et extensible.