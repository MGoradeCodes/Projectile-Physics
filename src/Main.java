import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    // ===================== GLOBAL STATE =====================
    private double angle = 0;
    private double speed = 0;

    double gravity = 0.5;
    double e = 0.73;

    private final List<Bullet> bullets = new ArrayList<>();

    // ===================== ENTRY =====================
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        // ===================== ROOT =====================
        Group root = new Group();
        Scene scene = new Scene(root, 800, 600);

        // ===================== WORLD OBJECTS =====================
        Rectangle floor = createFloor();
        Rectangle player = createPlayer();
        QuadCurve curve = createCurve();

        root.getChildren().add(curve);

        root.getChildren().addAll(floor, player);

        // ===================== INPUT =====================
        setupMouseControls(scene, player, root);

        // ===================== GAME LOOP =====================
        startGameLoop(floor, curve);

        // ===================== STAGE =====================
        stage.setTitle("My 2D Game");
        stage.setScene(scene);
        stage.show();
    }

    // =========================================================
    // GAME LOOP
    // =========================================================

    private void startGameLoop(Rectangle floor, QuadCurve curve) {

        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateBullets(floor, curve);
            }
        };

        gameLoop.start();
    }

    // =========================================================
    // OBJECT CREATION
    // =========================================================

    private Rectangle createFloor() {
        Rectangle floor = new Rectangle(800, 30);
        floor.setY(570);
        floor.setFill(Color.GRAY);
        return floor;
    }

    private QuadCurve createCurve(){
        QuadCurve curve = new QuadCurve();
        curve.setStartX(600);
        curve.setStartY(250);

        curve.setEndX(800);
        curve.setEndY(250);

        curve.setControlX(600);
        curve.setControlY(550);

        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(Color.DODGERBLUE);
        curve.setStrokeWidth(20);
        curve.setStrokeLineCap(StrokeLineCap.ROUND);

        return curve;
    }

    private Rectangle createPlayer() {
        Rectangle player = new Rectangle(50, 50);
        player.setFill(Color.DODGERBLUE);

        player.setX(800 / 2.0 - player.getWidth() / 2);
        player.setY(600 / 2.0 - player.getHeight() / 2);

        return player;
    }

    // =========================================================
    // INPUT
    // =========================================================

    private void setupMouseControls(Scene scene,
                                    Rectangle player,
                                    Group root) {

        scene.setOnMouseMoved(event ->
                handleMouseMove(event.getX(), event.getY(), player)
        );

        scene.setOnMouseClicked(event ->
                spawnBullet(player, root)
        );
    }

    private void handleMouseMove(double mouseX, double mouseY, Rectangle player) {

        double centerX = player.getX() + player.getWidth() / 2;
        double centerY = player.getY() + player.getHeight() / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        speed = Math.sqrt(dx * dx + dy * dy);
        angle = Math.atan2(dy, dx);

        player.setRotate(Math.toDegrees(angle));
    }

    // =========================================================
    // BULLET SYSTEM
    // =========================================================

    private void spawnBullet(Rectangle player, Group root) {

        Circle bulletShape = createBullet(player);
        root.getChildren().add(bulletShape);

        double vx = Math.cos(angle) * speed / 20;
        double vy = Math.sin(angle) * speed / 20;

        bullets.add(new Bullet(bulletShape, vx, vy));
    }

    private Circle createBullet(Rectangle player) {

        Circle bullet = new Circle(15);
        bullet.setFill(Color.BLACK);

        double centerX = player.getX() + player.getWidth() / 2;
        double centerY = player.getY() + player.getHeight() / 2;

        double offset = 55;

        bullet.setCenterX(centerX + Math.cos(angle) * offset);
        bullet.setCenterY(centerY + Math.sin(angle) * offset);

        return bullet;
    }

    // =========================================================
    // PHYSICS (ALL IN ONE LOOP)
    // =========================================================

    private void updateBullets(Rectangle floor, QuadCurve curve) {

        for (Bullet b : bullets) {

            // X movement
            b.shape.setCenterX(b.shape.getCenterX() + b.vx);

            // gravity
            b.vy += gravity;

            // Y movement
            b.shape.setCenterY(b.shape.getCenterY() + b.vy);

            for (double t = 0; t <= 1; t += 0.01) {

                double point1X = lerp(
                        curve.getStartX(),
                        curve.getControlX(),
                        t);

                double point1Y = lerp(
                        curve.getStartY(),
                        curve.getControlY(),
                        t);

                double point2X = lerp(
                        curve.getControlX(),
                        curve.getEndX(),
                        t);

                double point2Y = lerp(
                        curve.getControlY(),
                        curve.getEndY(),
                        t);

                double x = lerp(point1X, point2X, t);
                double y = lerp(point1Y, point2Y, t);

                double dx = b.shape.getCenterX() - x;
                double dy = b.shape.getCenterY() - y;

                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist <= b.shape.getRadius()) {
                    curveCollision(b, curve, t);
                    break;
                }
            }

            // floor collision
            if (b.shape.getBoundsInParent().intersects(
                    floor.getBoundsInParent())
                    && b.vy > 0) {

                b.shape.setCenterY(floor.getY() - b.shape.getRadius());
                b.vy = -b.vy * e;
            }
        }
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // =========================================================
    // STATIC BULLET CLASS
    // =========================================================

    public static class Bullet {
        Circle shape;
        double vx;
        double vy;

        Bullet(Circle shape, double vx, double vy) {
            this.shape = shape;
            this.vx = vx;
            this.vy = vy;
        }
    }

    public void curveCollision(Bullet b, QuadCurve curve, double t) {
        double point1X = lerp(
                curve.getStartX(),
                curve.getControlX(),
                t);

        double point1Y = lerp(
                curve.getStartY(),
                curve.getControlY(),
                t);

        double point2X = lerp(
                curve.getControlX(),
                curve.getEndX(),
                t);

        double point2Y = lerp(
                curve.getControlY(),
                curve.getEndY(),
                t);

        double x = lerp(point1X, point2X, t);
        double y = lerp(point1Y, point2Y, t);
        System.out.println("HIT at: " + x + ' ' + y );
    }
}