package lando.systems.ld47.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import lando.systems.ld47.Game;

public class GameBoard {
    public static int TILESWIDE = 10;
    public static int TILESHIGH = 20;

    public Game game;
    private Array<Tetrad> tetrads;
    private Tetrad activeTetrad;
    public Rectangle gameBounds;
    private OrthographicCamera camera;
    float fallInterval;
    float timeToFall;

    public GameBoard(Game game, OrthographicCamera camera) {
        this.game = game;
        this.camera = camera;
        this.tetrads = new Array<>();
        float width = TILESWIDE * Tetrad.POINT_WIDTH;
        float height = TILESHIGH * Tetrad.POINT_WIDTH;
        gameBounds = new Rectangle((camera.viewportWidth - width)/2f, (camera.viewportHeight - height)/2f, width, height);
        fallInterval = 1f;
        timeToFall = fallInterval;
    }

    public void update(float dt) {
        if (activeTetrad == null){
            activeTetrad = new Tetrad(this);
            activeTetrad.insertIntoBoard();
            if (collidesAt(activeTetrad, Vector2.Zero)){
                //GAME OVER
                //TODO something else
                tetrads.clear();
            }
            timeToFall = fallInterval; // TODO make this faster
        }

        if (activeTetrad != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                if (!collidesAt(activeTetrad, new Vector2(-1, 0))){
                    activeTetrad.origin.x -= 1;
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                if (!collidesAt(activeTetrad, new Vector2(1, 0))){
                    activeTetrad.origin.x += 1;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)){
                activeTetrad.rotate(-1);
                if (collidesAt(activeTetrad, Vector2.Zero)){
                    activeTetrad.rotate(1);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)){
                activeTetrad.rotate(1);
                if (collidesAt(activeTetrad, Vector2.Zero)){
                    activeTetrad.rotate(-1);
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                moveDown(activeTetrad);
            }
        }


        timeToFall -= dt;
        if (timeToFall < 0){
            moveDown(activeTetrad);

        }

        if (activeTetrad != null) {
            activeTetrad.update(dt);
        }
    }


    public void render(SpriteBatch batch) {
        batch.setColor(Color.DARK_GRAY);
        batch.draw(game.assets.whitePixel, gameBounds.x, gameBounds.y, gameBounds.width, gameBounds.height);
        batch.setColor(Color.WHITE);

        for (Tetrad tetrad : tetrads){
            tetrad.render(batch);
        }
        if (activeTetrad != null) {
            activeTetrad.render(batch);
        }
    }

    Vector2 testOrigin = new Vector2();
    public boolean collidesAt(Tetrad tetrad, Vector2 dir) {
        if (tetrad.origin == null) return false;
        testOrigin.set(tetrad.origin.x + dir.x, tetrad.origin.y + dir.y);
        for (Vector2 point : tetrad.points){
            for (Tetrad placedPiece : tetrads) {
                for (Vector2 placedPoint: placedPiece.points){
                    if (placedPiece.origin == null) continue;
                    if (point.x + testOrigin.x == placedPoint.x + placedPiece.origin.x &&
                        point.y + testOrigin.y == placedPoint.y + placedPiece.origin.y) {
                        return true;
                    }
                }
            }
            if (point.x + testOrigin.x < 0 || point.x + testOrigin.x >= TILESWIDE) return true;
            if (point.y + testOrigin.y < 0 || point.y + testOrigin.y >= TILESHIGH) return true;
        }

        return false;
    }

    public void moveDown(Tetrad tetrad){
        if (collidesAt(tetrad, new Vector2(0, -1))){
            tetrads.add(activeTetrad);
            activeTetrad = null;

            // TODO clear rows
        } else {
            tetrad.origin.y -= 1;
        }
        timeToFall = fallInterval;
    }
}
