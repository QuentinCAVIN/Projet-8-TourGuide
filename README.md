# Société TourGuide - Amélioration de la Performance de l'Application

## Introduction
TourGuide est une entreprise innovante qui a développé une application facilitant la planification de voyages. Suite à une croissance rapide et exponentielle, le nombre d'utilisateurs est passé de quelques centaines à plus de 30 000, entraînant une lenteur significative de l'application. Ce projet a pour objectif d'améliorer les performances de l'application en intégrant la programmation asynchrone.

## Travail Réalisé
Pour résoudre les problèmes de performance et améliorer l'expérience utilisateur, plusieurs actions ont été entreprises :

### Résolution des Dysfonctionnements
- **ConcurentModificationException** : Correction de l'exception générée lors du lancement de certains tests.
- **Tests de Performances** : Réduction du temps d'exécution des tests de performances, auparavant trop longs.
- **Affichage des Attractions** : Correction des défauts dans la fonctionnalité d'affichage des attractions aux utilisateurs.

### Amélioration de la Performance
- **Bugs des Tests Unitaires** : Identification et correction des bugs dans les tests unitaires.
- **Programmation Asynchrone** : Amélioration de la performance de l'application en intégrant la programmation asynchrone pour les appels aux classes externes (gpsUtil et RewardsCentral) en utilisant `CompletableFuture`.
- **Alignement des Tests de Performances** : Mise à jour des tests de performances pour refléter les améliorations apportées.

### Intégration Continue
- **Pipeline d'Intégration Continue** : Mise en place d'un pipeline d'intégration continue avec GitHub Actions pour compiler, tester et construire le jar exécutable de l'application.

### Documentation
- **Documentation Fonctionnelle et Technique** : Création d'une documentation couvrant les étapes de conception, de développement et de test, ainsi que les solutions apportées aux dysfonctionnements identifiés.
- **Document de Mesure de Performance** : Élaboration d'un document détaillant les améliorations de performance et la rapidité de l'application.

## Technologies Utilisées
- **Pipeline d'Intégration Continue** : GitHub Actions
- **Programmation Asynchrone** : `ExecutorService` et `CompletableFuture`

## Conclusion
Grâce à ces améliorations, l'application TourGuide est désormais plus rapide et capable de gérer efficacement un nombre croissant d'utilisateurs. Les optimisations apportées garantissent une meilleure expérience utilisateur et une plus grande fiabilité de l'application.
![Diagramme de classe TourGuide](https://github.com/QuentinCAVIN/Projet-8-TourGuide/assets/117484688/edc3946c-36db-4a47-903b-30e38a1392bc)
