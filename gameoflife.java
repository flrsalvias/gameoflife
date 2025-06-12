import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;

public class GameOfLife extends JPanel implements ActionListener {
    static final int CELL_MIN = 5, CELL_MAX = 40, DELAY = 100, HISTORY_LIMIT = 30;
    int size = 20, offsetX = 0, offsetY = 0, generation = 0;
    boolean running = false, ctrl = false, drawing = false, fullscreen = false;
    int drawMode = 1;
    Set<Point> cells = new HashSet<>(), modified = new HashSet<>();
    Deque<Set<Point>> history = new ArrayDeque<>();
    java.util.List<Point> pattern = null;
    javax.swing.Timer timer = new javax.swing.Timer(DELAY, this);
    JFrame frame;
    JLabel label = new JLabel("Génération : 0");
    Point lastMouse = new Point();

    static final Map<String, java.util.List<Point>> PATTERNS = Map.ofEntries(
        Map.entry("Planeur", java.util.List.of(new Point(1, 0), new Point(2, 1), new Point(0, 2), new Point(1, 2), new Point(2, 2))),
        Map.entry("Canon", java.util.List.of(
            new Point(1, 5), new Point(1, 6), new Point(2, 5), new Point(2, 6),
            new Point(11, 5), new Point(11, 6), new Point(11, 7),
            new Point(12, 4), new Point(12, 8), new Point(13, 3), new Point(13, 9),
            new Point(14, 3), new Point(14, 9), new Point(15, 6),
            new Point(16, 4), new Point(16, 8), new Point(17, 5), new Point(17, 6), new Point(17, 7),
            new Point(18, 6),
            new Point(21, 3), new Point(21, 4), new Point(21, 5),
            new Point(22, 3), new Point(22, 4), new Point(22, 5),
            new Point(23, 2), new Point(23, 6),
            new Point(25, 1), new Point(25, 2), new Point(25, 6), new Point(25, 7),
            new Point(35, 3), new Point(35, 4), new Point(36, 3), new Point(36, 4)
        )),
        Map.entry("Lapin", java.util.List.of(new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(3, 1), new Point(0, 2), new Point(3, 2), new Point(1, 3), new Point(2, 3))),
        Map.entry("Diehard", java.util.List.of(new Point(7, 0), new Point(1, 1), new Point(2, 1), new Point(2, 2), new Point(6, 2), new Point(7, 2), new Point(8, 2))),
        Map.entry("Acorn", java.util.List.of(new Point(1, 0), new Point(3, 1), new Point(0, 2), new Point(1, 2), new Point(4, 2), new Point(5, 2), new Point(6, 2))),
        Map.entry("Beacon", java.util.List.of(new Point(1, 1), new Point(1, 2), new Point(2, 1), new Point(3, 4), new Point(4, 3), new Point(4, 4))),
        Map.entry("Pentadecathlon", java.util.List.of(new Point(1,0), new Point(2,0), new Point(3,0), new Point(4,0), new Point(5,0), new Point(6,0),
                                                      new Point(2,-1), new Point(2,1), new Point(5,-1), new Point(5,1))),
        Map.entry("LWSS", java.util.List.of(new Point(0,1), new Point(0,3), new Point(1,0), new Point(2,0), new Point(3,0), new Point(4,0), new Point(4,1), new Point(4,2), new Point(3,3))),
        Map.entry("Tumbler", java.util.List.of(new Point(1,1), new Point(1,2), new Point(1,3), new Point(2,1), new Point(2,3),
                                               new Point(3,2), new Point(4,1), new Point(4,3), new Point(5,1), new Point(5,3), new Point(6,1), new Point(6,2), new Point(6,3))),
        Map.entry("Clock", java.util.List.of(new Point(1,0), new Point(2,0), new Point(2,1), new Point(3,1), new Point(3,2), new Point(4,2), new Point(4,3), new Point(1,3))),
        Map.entry("Snacker", java.util.List.of(new Point(0,1), new Point(1,1), new Point(2,1), new Point(3,1), new Point(4,1), new Point(5,1), new Point(6,1),
                                               new Point(2,0), new Point(2,2), new Point(4,0), new Point(4,2)))
    );

    public GameOfLife(JFrame f) {
        frame = f;
        setBackground(Color.WHITE);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastMouse = e.getPoint();
                if (SwingUtilities.isMiddleMouseButton(e)) return;
                startDraw(e);
            }

            public void mouseReleased(MouseEvent e) {
                drawing = false;
                if (cells.isEmpty()) reset();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    Point p = e.getPoint();
                    offsetX += p.x - lastMouse.x;
                    offsetY += p.y - lastMouse.y;
                    lastMouse = p;
                    repaint();
                } else if (drawing) {
                    paintCell(toGrid(e.getX(), e.getY()));
                }
            }
        });

        addMouseWheelListener(e -> {
            int oldSize = size;
            int rotation = e.getWheelRotation();
            int newSize = Math.max(CELL_MIN, Math.min(CELL_MAX, size + (rotation < 0 ? 1 : -1)));

            if (newSize != oldSize) {
                double scale = (double)newSize / oldSize;
                int mouseX = e.getX();
                int mouseY = e.getY();

                offsetX = (int)((offsetX + mouseX) * scale - mouseX);
                offsetY = (int)((offsetY + mouseY) * scale - mouseY);

                size = newSize;
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) toggle();
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) ctrl = true;
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) ctrl = false;
            }
        });

        JPanel controls = new JPanel();
        JButton btn = new JButton("Démarrer");
        btn.addActionListener(e -> toggle());
        controls.add(btn);
        controls.add(label);
        JButton fsBtn = new JButton("Plein écran");
        fsBtn.addActionListener(e -> toggleFullscreen());
        controls.add(fsBtn);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Modèles");
        PATTERNS.forEach((k, v) -> {
            JMenuItem item = new JMenuItem(k);
            item.addActionListener(e -> pattern = v);
            menu.add(item);
        });
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);
        frame.add(this);
        frame.add(controls, BorderLayout.SOUTH);
        timer.start();
    }

    void reset() {
        generation = 0;
        label.setText("Génération : 0");
    }

    Point toGrid(int x, int y) {
        return new Point((int)Math.floor((x + offsetX) / (double)size), (int)Math.floor((y + offsetY) / (double)size));
    }

    void paintCell(Point p) {
        if (modified.contains(p)) return;
        modified.add(p);
        if (drawMode == 1) cells.add(p); else cells.remove(p);
        repaint();
    }

    void startDraw(MouseEvent e) {
        if (pattern != null) {
            placePattern(e.getX(), e.getY());
            return;
        }
        if (running) return;
        Point p = toGrid(e.getX(), e.getY());
        drawMode = cells.contains(p) ? 0 : 1;
        drawing = true;
        modified.clear();
        paintCell(p);
    }

    void placePattern(int x, int y) {
        if (pattern == null) return;
        Point base = toGrid(x, y);
        for (Point d : pattern) cells.add(new Point(base.x + d.x, base.y + d.y));
        pattern = null;
        repaint();
    }

    void toggle() {
        running = !running;
        label.setText("Génération : " + generation);
    }

    void toggleFullscreen() {
        fullscreen = !fullscreen;
        frame.dispose();
        frame.setUndecorated(fullscreen);
        frame.setExtendedState(fullscreen ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (!running) return;

        int steps = ctrl ? 15 : 1;
        for (int i = 0; i < steps; i++) {
            Map<Point, Integer> count = new HashMap<>();
            for (Point p : cells)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0)
                            count.merge(new Point(p.x + dx, p.y + dy), 1, Integer::sum);

            Set<Point> newCells = new HashSet<>();
            for (Map.Entry<Point, Integer> e1 : count.entrySet()) {
                Point p = e1.getKey();
                int n = e1.getValue();
                if (n == 3 || (n == 2 && cells.contains(p))) newCells.add(p);
            }

            history.addLast(new HashSet<>(newCells));
            if (history.size() > HISTORY_LIMIT) history.removeFirst();
            cells = newCells;
            generation++;
        }

        label.setText("Génération : " + generation);
        repaint();

        if (cells.isEmpty() || history.stream().distinct().count() < history.size()) running = false;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (Point p : cells) {
            int x = p.x * size - offsetX;
            int y = p.y * size - offsetY;
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, size, size);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Jeu de la Vie");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1000, 800);
        new GameOfLife(f);
        f.setVisible(true);
    }
}
