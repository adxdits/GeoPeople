# Contributions personnelles - Ihssane Bennani

## Ce que j'ai fait dans le projet

Dans ce projet, j'ai participé à plusieurs parties importantes de GeoPeople. J'ai d'abord travaillé sur le backend en Express TypeScript : mise en place de l'API, premières routes de cartes, routes de capture, routes liées aux joueurs, inventaire, score et leaderboard.

Dans la partie backend, mon objectif était de centraliser les règles importantes côté serveur. Par exemple, la capture d'une carte ne dépend pas seulement du résultat du mini-jeu : le backend vérifie aussi que la carte existe, qu'elle n'est pas déjà capturée et que le joueur est bien à proximité. Cela évite de laisser toute la logique dans l'application Android.

J'ai ensuite amélioré la logique de capture. J'ai ajouté la validation côté backend, l'historique de capture et un verrouillage progressif lorsqu'un joueur perd un mini-jeu. Si le joueur échoue, il doit attendre avant de retenter la même carte.

Le verrouillage progressif permet de rendre l'échec plus significatif. Plus le joueur échoue sur une même carte, plus le délai avant une nouvelle tentative augmente. Côté interface, le joueur voit le temps restant, ce qui rend la règle compréhensible pendant le test.

J'ai aussi intégré les mini-jeux dans le parcours de capture. J'ai déclaré les activités des mini-jeux, relié leur lancement au bouton de capture et récupéré leur résultat pour savoir si la carte devait être gagnée ou non. J'ai aussi ajouté un bouton de secours dans le mini-jeu Pokeball pour pouvoir tester si le clap au micro ne fonctionne pas.

Pour cette intégration, j'ai utilisé le mécanisme de lancement d'activité Android. Le jeu principal lance un mini-jeu, attend son résultat, puis reprend le flux normal de GeoPeople. Cela permet d'ajouter plusieurs mini-jeux tout en gardant une seule logique de capture dans l'application principale.

J'ai travaillé sur l'affichage des cartes capturées. J'ai ajouté un écran de détail pour consulter une carte de l'inventaire, puis j'ai ajouté la récupération d'informations biographiques depuis Wikidata et Wikipedia, sans utiliser de WebView. J'ai aussi ajouté l'historique de possession pour voir la chronologie d'une carte.

L'écran de détail permet de donner plus de valeur aux cartes capturées. Le joueur ne voit pas seulement un nom dans son inventaire, il peut consulter des informations sur la personnalité, un résumé biographique et les sources associées. Cela rapproche la fonctionnalité de ce qui était demandé dans le sujet.

J'ai ajouté une aide à la capture sur la carte : distance jusqu'à la carte, direction à suivre et indication si le joueur se rapproche ou s'éloigne. Cette fonctionnalité aide le joueur à trouver une carte avant de lancer le mini-jeu.

Cette aide utilise la position GPS du joueur et la position de la carte. Elle permet de transformer la recherche en vraie phase de jeu : le joueur choisit une carte, avance vers elle et reçoit un retour visuel sur sa progression.

J'ai aussi contribué aux échanges entre joueurs. J'ai ajouté une règle qui oblige les joueurs à être proches pour pouvoir échanger des cartes. Cela garde une logique cohérente avec le principe de jeu géolocalisé.

Enfin, j'ai ajouté des statistiques personnelles dans le profil du joueur et j'ai intégré le fichier `people-places.jsonl` fourni par le professeur. Comme ce fichier est très volumineux, il est ignoré par Git et lu en streaming côté backend.

Pour le fichier du professeur, j'ai fait en sorte que le backend puisse générer des cartes à partir des personnalités et des lieux associés. J'ai aussi gardé des cartes de démonstration autour de la position du joueur pour faciliter les tests sur émulateur, car la position simulée ne correspond pas toujours à des lieux réels du fichier.

## Mon mini-jeu

Mon mini-jeu est Ballrun / Treasure Run. Le joueur contrôle une boule sur une route sinueuse en inclinant le téléphone avec l'accéléromètre. Le but est de rester dans les limites de la route tout en récupérant tous les indices placés sur le parcours. Si le joueur sort de la route, la partie est perdue et la capture échoue. Si le joueur arrive à la fin sans avoir récupéré tous les indices, la carte n'est pas encore débloquée et il doit continuer.

J'ai organisé mon mini-jeu en plusieurs fichiers pour séparer les responsabilités. L'écran principal gère l'état du jeu, les capteurs et la fin de partie. Le Canvas s'occupe de dessiner la route, la boule, les indices, la progression et la carte finale. Une autre partie contient les calculs de la route, notamment son centre et sa largeur selon l'avancement du joueur.

La route est générée avec des courbes pour éviter un parcours trop simple. Elle peut aussi se rétrécir à certains endroits, ce qui oblige le joueur à mieux contrôler la boule. J'ai ajouté une courte protection au début de la partie pour éviter que le joueur perde immédiatement pendant l'initialisation du jeu ou des capteurs.

Quand tous les indices sont récupérés et que le joueur atteint la fin du parcours, il passe à une phase de découverte de la carte. Il doit d'abord secouer le téléphone pour nettoyer la carte, puis utiliser le microphone pour souffler et enlever la poussière restante. Après ces actions, le mini-jeu renvoie une victoire à GeoPeople et la capture peut être validée. J'ai aussi ajouté des boutons gauche/droite pour pouvoir tester le jeu sur émulateur si les capteurs ne fonctionnent pas bien.

Ce mini-jeu utilise donc plusieurs interactions physiques : incliner le téléphone pour se déplacer, secouer pour révéler la carte et souffler dans le micro pour finir la capture. J'ai choisi cette logique pour que le mini-jeu soit lié au téléphone et pas seulement à des boutons classiques. Les boutons de secours restent utiles pour les tests, mais l'expérience principale repose sur les capteurs.
