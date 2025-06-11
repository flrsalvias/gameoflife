import tkinter as tk
from tkinter import PhotoImage
import ctypes
from collections import defaultdict, deque

CELL_MIN = 5
CELL_MAX = 40
DELAY = 100
HISTORY_LIMIT = 30

PATTERNS = {
    "Planeur": [(1, 0), (2, 1), (0, 2), (1, 2), (2, 2)],
    "Canon": [
        (5, 1), (5, 2), (6, 1), (6, 2), (5, 11), (6, 11), (7, 11), (4, 12), (8, 12),
        (3, 13), (9, 13), (3, 14), (9, 14), (6, 15), (4, 16), (8, 16), (5, 17), (6, 17),
        (7, 17), (6, 18), (3, 21), (4, 21), (5, 21), (3, 22), (4, 22), (5, 22),
        (2, 23), (6, 23), (1, 25), (2, 25), (6, 25), (7, 25), (3, 35), (4, 35),
        (3, 36), (4, 36)
    ],
    "Puffer": [(i, i) for i in range(14)],
    "Lapin": [(1, 0), (2, 0), (0, 1), (3, 1), (0, 2), (3, 2), (1, 3), (2, 3)],
    "Diehard": [(7, 0), (1, 1), (2, 1), (2, 2), (6, 2), (7, 2), (8, 2)],
    "Acorn": [(1, 0), (3, 1), (0, 2), (1, 2), (4, 2), (5, 2), (6, 2)],
    "Beacon": [(1, 1), (1, 2), (2, 1), (4, 4), (4, 5), (5, 4)],
    "Pulsar": [(x+2, y+2) for x in [-6,-5,-4,4,5,6] for y in [-4,0,4]] + [(x+2, y+2) for x in [-4,0,4] for y in [-6,-5,-4,4,5,6]],
    "Pentadecathlon": [(1,0),(2,0),(3,0),(4,0),(5,0),(6,0),(3,-1),(3,1),(0,0),(7,0)],
    "LWSS": [(0,1),(0,3),(1,0),(2,0),(3,0),(4,0),(4,1),(4,2),(3,3)],
    "Tumbler": [(1,1),(1,2),(1,3),(2,1),(2,3),(3,1),(3,2),(3,3),(4,2),(5,1),(5,3),(6,1),(6,3),(7,1),(7,2),(7,3)],
    "Clock": [(1,0),(2,0),(2,1),(3,1),(0,2),(1,2)],
    "Snacker": [(0,1),(1,1),(2,1),(3,1),(4,1),(4,0),(2,2),(2,3),(2,4),(3,4),(4,4)]
}

class GameOfLife:
    def __init__(self, root):
        self.root = root
        root.title("Jeu de la Vie")
        root.iconphoto(False, PhotoImage(width=1, height=1))
        self.canvas = tk.Canvas(root, bg="white", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True)

        self.size = 20
        self.offset = [0, 0]
        self.cells = set()
        self.running = False
        self.generation = 0
        self.ctrl = False
        self.history = deque(maxlen=HISTORY_LIMIT)
        self.pattern = None
        self.draw_mode = 1
        self.drawing = False
        self.modified = set()
        self.fullscreen = False

        self.menu = tk.Menubutton(root, text="Models")
        menu = tk.Menu(self.menu, tearoff=0)
        self.menu["menu"] = menu
        for name in PATTERNS:
            menu.add_command(label=name, command=lambda n=name: self.select_pattern(n))
        self.menu.place(relx=1.0, rely=0.0, anchor='ne')

        ctrl = tk.Frame(root)
        ctrl.pack()
        self.btn = tk.Button(ctrl, text="Démarrer", command=self.toggle)
        self.btn.pack(side=tk.LEFT)
        self.label = tk.Label(ctrl, text="Génération : 0")
        self.label.pack(side=tk.LEFT)
        self.fs_btn = tk.Button(ctrl, text="Plein écran", command=self.toggle_fullscreen)
        self.fs_btn.pack(side=tk.LEFT)

        self.canvas.bind("<Button-1>", self.start_draw)
        self.canvas.bind("<B1-Motion>", self.drag_draw)
        self.canvas.bind("<ButtonRelease-1>", self.end_draw)
        self.canvas.bind("<MouseWheel>", self.zoom)
        self.canvas.bind("<Configure>", lambda e: self.draw())
        self.canvas.bind("<Button-3>", self.place_pattern)
        root.bind("<space>", lambda e: self.toggle())
        root.bind("<Control_L>", lambda e: self.set_ctrl(True))
        root.bind("<Control_R>", lambda e: self.set_ctrl(True))
        root.bind("<KeyRelease-Control_L>", lambda e: self.set_ctrl(False))
        root.bind("<KeyRelease-Control_R>", lambda e: self.set_ctrl(False))

    def toggle_fullscreen(self):
        self.fullscreen = not self.fullscreen
        self.root.attributes("-fullscreen", self.fullscreen)

    def set_ctrl(self, state):
        self.ctrl = state

    def toggle(self):
        self.running = not self.running
        self.btn.config(text="Arrêter" if self.running else "Démarrer")
        if self.running:
            self.run()

    def to_grid(self, x, y):
        return (x + self.offset[0]) // self.size, (y + self.offset[1]) // self.size

    def to_screen(self, gx, gy):
        return gx * self.size - self.offset[0], gy * self.size - self.offset[1]

    def start_draw(self, e):
        if self.pattern:
            return self.place_pattern(e)
        if self.running:
            return
        x, y = self.to_grid(e.x, e.y)
        self.draw_mode = 0 if (x, y) in self.cells else 1
        self.drawing = True
        self.modified = set()
        self.paint(x, y)

    def drag_draw(self, e):
        if self.drawing:
            x, y = self.to_grid(e.x, e.y)
            self.paint(x, y)

    def end_draw(self, e):
        self.drawing = False
        if not self.cells:
            self.generation = 0
            self.label.config(text="Génération : 0")

    def paint(self, x, y):
        if (x, y) in self.modified:
            return
        self.modified.add((x, y))
        (self.cells.add if self.draw_mode else self.cells.discard)((x, y))
        self.draw()

    def zoom(self, e):
        old = self.size
        self.size = max(CELL_MIN, min(CELL_MAX, old + (1 if e.delta > 0 else -1)))
        if self.size != old:
            self.draw()

    def run(self):
        if not self.running:
            return
        count = defaultdict(int)
        for x, y in self.cells:
            for dx in [-1, 0, 1]:
                for dy in [-1, 0, 1]:
                    if dx or dy:
                        count[(x+dx, y+dy)] += 1
        new_cells = {c for c, n in count.items() if n == 3 or (n == 2 and c in self.cells)}
        self.history.append(frozenset(new_cells))
        self.cells = new_cells
        self.generation += 1
        self.label.config(text=f"Génération : {self.generation}")
        self.draw()
        if not self.cells or any(self.history.count(state) > 1 for state in self.history):
            self.running = False
            self.btn.config(text="Démarrer")
            try:
                ctypes.windll.user32.FlashWindow(ctypes.windll.kernel32.GetConsoleWindow(), True)
            except Exception:
                pass
        else:
            self.root.after(DELAY // 15 if self.ctrl else DELAY, self.run)

    def draw(self):
        self.canvas.delete("all")
        w, h = self.canvas.winfo_width(), self.canvas.winfo_height()
        sx, sy = self.offset[0] // self.size, self.offset[1] // self.size
        cols, rows = w // self.size + 2, h // self.size + 2
        for y in range(sy, sy + rows):
            for x in range(sx, sx + cols):
                if (x, y) in self.cells:
                    px, py = self.to_screen(x, y)
                    self.canvas.create_rectangle(px, py, px + self.size, py + self.size, fill="black", width=0)

    def select_pattern(self, name):
        self.pattern = PATTERNS[name]

    def place_pattern(self, e):
        if not self.pattern:
            return
        gx, gy = self.to_grid(e.x, e.y)
        for dx, dy in self.pattern:
            self.cells.add((gx + dx, gy + dy))
        self.pattern = None
        self.draw()

if __name__ == "__main__":
    root = tk.Tk()
    GameOfLife(root)
    root.mainloop()
