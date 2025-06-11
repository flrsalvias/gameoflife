# Conway's game of life - jeu de la vie

Un **Jeu de la Vie** de John Conway, codé **entièrement en Python** **sans module à installer**.


* Interface graphique dynamique et redimensionnable
* Zoom par molette
* Plein écran
* Dessin à la souris
* Placement de motifs prédéfinis
* Avancement automatique avec pause/reprise
* Historique pour détection d'états stables ou cycliques
* Aucune dépendance externe requise (utilise uniquement la bibliothèque standard de Python)

---

## Comment ça fonctionne ?

Le **Jeu de la Vie** est un automate cellulaire : chaque cellule vit ou meurt selon ses voisines sur une grille infinie en 2D.

Règles :

* Une cellule vivante avec 2 ou 3 voisines reste vivante.
* Une cellule morte avec exactement 3 voisines devient vivante.
* Toutes les autres meurent ou restent mortes.

L'évolution se fait à chaque **génération**.

---

## Comment l'utiliser ?

### 1. Lancer le programme

```bash
python gameoflife.py
```

### 2. Interface graphique

* **Clique gauche** : dessiner ou effacer une cellule.
* **Glisser** : dessiner ou effacer plusieurs cellules.
* **Clic droit** : insérer un motif (si sélectionné).
* **Molette** : zoom avant/arrière.
* **Espace** : démarrer ou arrêter la simulation.
* **Ctrl** (gauche ou droite) : accélérer temporairement la simulation (x15)
* **Bouton "Plein écran"** : bascule en mode plein écran.
* **Menu "Modèles"** : sélectionner un motif à placer.

---

## Les motifs disponibles

Le menu **"Modèles"** vous permet de placer des motifs classiques du Jeu de la Vie :

* Planeur
* Canon
* Puffer
* Lapin
* Diehard
* Acorn
* Beacon
* Pulsar
* Pentadecathlon
* LWSS (Lightweight Spaceship)
* Tumbler
* Clock
* Snacker

Chaque motif est défini par des coordonnées relatives, insérées au clic droit sur la grille.

---

## Options et fonctionnalités implémentées

### Sans modules à installer

* Utilise seulement : `tkinter`, `collections`, `ctypes` — tous inclus avec Python standard.

### Dessin à la souris

* Activation automatique du **mode dessin** au clic gauche.
* Mode **ajout** ou **suppression** de cellules détecté dynamiquement.

### Zoom

* Molette de souris pour ajuster dynamiquement la taille des cellules (`CELL_MIN` à `CELL_MAX`).

### Accélération

* Maintenir **Ctrl** pour accélérer la simulation (`DELAY` divisé par 15).

### Historique

* Un historique limité à `HISTORY_LIMIT` générations permet de :

  * Détecter la répétition d’un état (oscillation ou stabilisation).
  * Stopper la simulation automatiquement dans ce cas.

### Redimensionnement dynamique

* La grille s’adapte automatiquement à la taille de la fenêtre.

### Plein écran

* Via bouton "Plein écran", la fenêtre se maximise et supprime les bordures.

### Ajout de motifs dynamiquement

* Menu de motifs dans l’interface (`tk.Menu`).
* Insertion relative au clic (clic droit).
* Prend en charge motifs complexes.

---

## Structure du code

### `PATTERNS`

Dictionnaire de motifs. Chacun est une liste de positions `(x, y)` relatives.

### `GameOfLife`

Classe principale :

* Gère l’UI (`Canvas`, `Menu`, `Buttons`, etc.)
* Gestion des cellules vivantes (`set`)
* Dessin (`draw()`), logique (`run()`), zoom, historique
* Placement de motifs et dessin à la volée

---

## Comment j'ai fait ?

* **Aucune dépendance** : uniquement la bibliothèque standard
* **Tkinter** pour l’interface
* **`set()`** pour stocker les cellules vivantes (rapide et efficace)
* **`defaultdict(int)`** pour compter les voisines facilement
* **`deque(maxlen=30)`** pour détecter les répétitions de configuration
* **`ctypes.windll`** pour faire clignoter la fenêtre sur Windows si la simulation s’arrête (facultatif mais sympa !)

Fait par Fleurs - flrsalvias
