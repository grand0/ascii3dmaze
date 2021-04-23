package ru.zeroapps.ascii3dmaze;

import ru.zeroapps.ascii3dmaze.utils.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Comparator;

import static java.lang.Math.PI;

public class Main {
    int screenWidth = 120;
    int screenHeight = 40;

    JTextArea screen;

    volatile boolean aPressed = false;
    volatile boolean dPressed = false;
    volatile boolean wPressed = false;
    volatile boolean sPressed = false;

    public static void main(String[] args) {
        Main main = new Main();
        main.setupGui();
        System.out.println("Starting...");
        main.runGame();
    }

    void setupGui() {
        JFrame frame = new JFrame("3DMaze");
        screen = new JTextArea();
        screen.setEditable(false);
        screen.setFont(new Font("Consolas", Font.PLAIN, 14));
        screen.setBackground(Color.BLACK);
        screen.setForeground(Color.WHITE);
        screen.setColumns(screenWidth);
        screen.setRows(screenHeight);
        screen.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A:
//                        playerAngle -= 0.1f;
                        aPressed = true;
                        break;
                    case KeyEvent.VK_D:
//                        playerAngle += 0.1f;
                        dPressed = true;
                        break;
                    case KeyEvent.VK_W:
//                        playerX += Math.sin(playerAngle) * 1.0f;
//                        playerY += Math.cos(playerAngle) * 1.0f;
                        wPressed = true;
                        break;
                    case KeyEvent.VK_S:
//                        playerX -= Math.sin(playerAngle) * 1.0f;
//                        playerY -= Math.cos(playerAngle) * 1.0f;
                        sPressed = true;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        System.exit(0);
                        break;
                }
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A -> aPressed = false;
                    case KeyEvent.VK_D -> dPressed = false;
                    case KeyEvent.VK_W -> wPressed = false;
                    case KeyEvent.VK_S -> sPressed = false;
                }
                e.consume();
            }
        });
        screen.requestFocus();
        frame.getContentPane().add(screen);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    float playerX = 8.0f;
    float playerY = 8.0f;
    float playerAngle = 0.0f;
    float fov = (float) PI / 4.0f;

    float depth = 16.0f;

    int mapHeight = 16;
    int mapWidth = 16;

    void runGame() {
        String map = "";
        map += "################";
        map += "#..............#";
        map += "#..............#";
        map += "#..........##..#";
        map += "#..........##..#";
        map += "#..............#";
        map += "#..............#";
        map += "#..............#";
        map += "#..............#";
        map += "#..............#";
        map += "#..............#";
        map += "#######........#";
        map += "#.....#........#";
        map += "#.....#........#";
        map += "#..............#";
        map += "################";

        long tp1 = System.currentTimeMillis();
        long tp2 = System.currentTimeMillis();

        while (true) {
            tp2 = System.currentTimeMillis();
            float elapsedTime = (float) (tp2 - tp1) / 1000.0f;
            tp1 = tp2;

            if (aPressed) {
                playerAngle -= 0.8f * elapsedTime;
            }
            if (dPressed) {
                playerAngle += 0.8f * elapsedTime;
            }
            if (wPressed) {
                playerX += Math.sin(playerAngle) * 5.0f * elapsedTime;
                playerY += Math.cos(playerAngle) * 5.0f * elapsedTime;

                if (map.charAt((int) playerY * mapWidth + (int) playerX) == '#') {
                    playerX -= Math.sin(playerAngle) * 5.0f * elapsedTime;
                    playerY -= Math.cos(playerAngle) * 5.0f * elapsedTime;
                }
            }
            if (sPressed) {
                playerX -= Math.sin(playerAngle) * 5.0f * elapsedTime;
                playerY -= Math.cos(playerAngle) * 5.0f * elapsedTime;

                if (map.charAt((int) playerY * mapWidth + (int) playerX) == '#') {
                    playerX += Math.sin(playerAngle) * 5.0f * elapsedTime;
                    playerY += Math.cos(playerAngle) * 5.0f * elapsedTime;
                }
            }

            StringBuilder buf = new StringBuilder();
            buf.append((" ".repeat(screenWidth) + '\n').repeat(screenHeight));

            for (int x = 0; x < screenWidth; x++) {
                float rayAngle = (playerAngle - fov / 2.0f) + ((float) x / (float) screenWidth) * fov;
                float distanceToWall = 0.0f;
                boolean hitWall = false;
                boolean boundary = false;
                float eyeX = (float) Math.sin(rayAngle);
                float eyeY = (float) Math.cos(rayAngle);
                while (!hitWall && distanceToWall < depth) {
                    distanceToWall += 0.1f;

                    int testX = (int) (playerX + eyeX * distanceToWall);
                    int testY = (int) (playerY + eyeY * distanceToWall);

                    if (testX < 0 || testX >= mapWidth || testY < 0 || testY >= mapHeight) {
                        hitWall = true;
                        distanceToWall = depth;
                    } else {
                        if (map.charAt(testY * mapWidth + testX) == '#') {
                            hitWall = true;

                            ArrayList<Pair<Float, Float>> p = new ArrayList<>(); // distance, dot
                            for (int tx = 0; tx < 2; tx++) {
                                for (int ty = 0; ty < 2; ty++) {
                                    float vy = (float) testY + ty - playerY;
                                    float vx = (float) testX + tx - playerX;
                                    float d = (float) Math.sqrt(vx*vx + vy*vy);
                                    float dot = (eyeX * vx / d) + (eyeY * vy / d);
                                    p.add(new Pair<>(d, dot));
                                }
                            }
                            p.sort(Comparator.comparing(Pair::getFirst));

                            float bound = 0.01f;
                            if (Math.acos(p.get(0).getSecond()) < bound) boundary = true;
                            if (Math.acos(p.get(1).getSecond()) < bound) boundary = true;
                        }
                    }
                }
                int ceiling = (int) (((float) screenHeight / 2.0f) - (float) screenHeight / distanceToWall);
                int floor = screenHeight - ceiling;

                char shade = ' ';
                if (distanceToWall <= depth / 4.0f) shade = '@';      // █
                else if (distanceToWall < depth / 3.0f) shade = '$';  // ▓
                else if (distanceToWall < depth / 2.0f) shade = '*';  // ▒
                else if (distanceToWall < depth) shade = '\'';         // ░

                if (boundary) shade = ' ';

                for (int y = 0; y < screenHeight; y++) {
                    if (y < ceiling){
                        buf.setCharAt(y * screenWidth + x + y, ' ');
                    }
                    else if (y > ceiling && y <= floor) {
                        buf.setCharAt(y * screenWidth + x + y, shade);
                    }
                    else {
                        float b = 1.0f - (((float) y - screenHeight / 2.0f) / ((float) screenHeight / 2.0f));
                        char floorShade = ' ';
                        if (b < 0.25) floorShade = '#';
                        else if (b < 0.5) floorShade = 'x';
                        else if (b < 0.75) floorShade = '.';
                        else if (b < 0.9) floorShade = '-';
                        buf.setCharAt(y * screenWidth + x + y, floorShade);
                    }
                }

            }
            String stats = "X=%5.2f Y=%5.2f A=%5.2f FPS=%8.2f".formatted(playerX, playerY, playerAngle, 1.0f/elapsedTime);
            buf.replace(screenWidth-stats.length()-1, screenWidth-1, stats);

            for (int i = 0; i < mapHeight; i++) {
                StringBuilder line = new StringBuilder(map.substring(i * mapWidth, i * mapWidth + mapWidth));
                if ((int) playerY == i) {
                    char direction = 'P';
                    boolean neg = playerAngle < 0;
                    if (neg) playerAngle = -playerAngle;
                    float ang = playerAngle % (2.0f*(float)PI);
                    if (neg) playerAngle = -playerAngle;
                    // from start player is looking down
                    if ((ang <= PI/4.0 && ang >= 0) || (ang > 7.0*PI/4.0 && ang < 2.0*PI)) direction = '↓';
                    else if (ang > PI/4.0 && ang <= 3.0*PI/4.0) direction = '→';
                    else if (ang > 3.0*PI/4.0 && ang <= 5.0*PI/4.0) direction = '↑';
                    else if (ang > 5.0*PI/4.0 && ang <= 7.0*PI/4.0) direction = '←';
                    if (neg && direction == '→') direction = '←';
                    else if (neg && direction == '←') direction = '→';
                    line.setCharAt((int) playerX, direction);
                }
                buf.replace(i * screenWidth + i, i * screenWidth + mapWidth + i, line.toString());
            }

            screen.setText(buf.toString());
        }
    }
}
