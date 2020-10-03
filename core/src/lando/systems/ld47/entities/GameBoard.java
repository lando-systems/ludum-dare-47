package lando.systems.ld47.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.OrderedSet;
import lando.systems.ld47.GameState;

public class GameBoard {
    public static int TILESWIDE = 10;
    public static int TILESHIGH = 20;

    private final GameState gameState;
    private final OrthographicCamera camera;
    private final PlayerInput playerInput = new PlayerInput();
    private final Array<Tetrad> tetrads;

    private Tetrad activeTetrad;
    private Tetrad tetradToRemove;
    public Rectangle gameBounds;
    float fallInterval;
    float timeToFall;

    int blocksToFallTilRemove;

    public GameBoard(GameState gameState) {
        this.gameState = gameState;
        this.camera = gameState.gameScreen.worldCamera;
        this.tetrads = new Array<>();
        float width = TILESWIDE * Tetrad.POINT_WIDTH;
        float height = TILESHIGH * Tetrad.POINT_WIDTH;
        gameBounds = new Rectangle((camera.viewportWidth - width) / 2f, (camera.viewportHeight - height) / 2f, width, height);
        blocksToFallTilRemove = 3;
        fallInterval = 1f;
        timeToFall = fallInterval;
    }

    public void update(float dt) {
        playerInput.update(dt);
        if (activeTetrad == null) {
            activeTetrad = gameState.popNext();

            activeTetrad.insertIntoBoard(this);
            if (invalidMove(activeTetrad, Vector2.Zero)) {
                //GAME OVER
                //TODO something else
                tetrads.clear();
            }
            timeToFall = fallInterval; // TODO make this faster
        }

        if (activeTetrad != null) {
            PlayerInput.TriggerState state = playerInput.isRightPressed();
            if (state.pressed) {
                if (state.triggered && !invalidMove(activeTetrad, new Vector2(1, 0))) {
                    activeTetrad.origin.x += 1;
                }
            } else if (playerInput.isLeftPressed().triggered) {
                if (!invalidMove(activeTetrad, new Vector2(-1, 0))) {
                    activeTetrad.origin.x -= 1;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                activeTetrad.rotate(-1);
                if (invalidMove(activeTetrad, Vector2.Zero)) {
                    activeTetrad.rotate(1);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                activeTetrad.rotate(1);
                if (invalidMove(activeTetrad, Vector2.Zero)) {
                    activeTetrad.rotate(-1);
                }
            }

            if (playerInput.isDownPressed()) {
                moveDown(activeTetrad);
            }
        }

        timeToFall -= dt;
        if (timeToFall < 0) {
            moveDown(activeTetrad);
        }

        if (activeTetrad != null) {
            activeTetrad.update(dt);
        }

        for (Tetrad tetrad : tetrads) {
            tetrad.update(dt);
        }
    }



    public void render(SpriteBatch batch) {
        batch.setColor(Color.DARK_GRAY);
        batch.draw(gameState.assets.whitePixel, gameBounds.x, gameBounds.y, gameBounds.width, gameBounds.height);
        batch.setColor(Color.WHITE);

        for (Tetrad tetrad : tetrads) {
            tetrad.render(batch);
        }
        if (activeTetrad != null) {
            activeTetrad.render(batch);
        }
    }


    public boolean invalidMove(Tetrad tetrad, Vector2 dir) {
        return collidesWithBlocks(tetrad, dir) || collidesWithWalls(tetrad, dir);
    }

    Vector2 testOrigin = new Vector2();

    public boolean collidesWithBlocks(Tetrad tetrad, Vector2 dir) {
        if (tetrad.origin == null) return false;
        testOrigin.set(tetrad.origin.x + dir.x, tetrad.origin.y + dir.y);
        for (int i = 0; i < tetrad.points.size; i++) {
            Vector2 point = tetrad.points.get(i);
            for (int j = 0; j < tetrads.size; j++) {
                Tetrad placedPiece = tetrads.get(j);
                if (placedPiece == tetrad) continue;
                for (Vector2 placedPoint : placedPiece.points) {
                    if (placedPiece.origin == null) continue;
                    if (point.x + testOrigin.x == placedPoint.x + placedPiece.origin.x &&
                            point.y + testOrigin.y == placedPoint.y + placedPiece.origin.y) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean collidesWithWalls(Tetrad tetrad, Vector2 dir) {
        if (tetrad.origin == null) return false;
        testOrigin.set(tetrad.origin.x + dir.x, tetrad.origin.y + dir.y);
        for (Vector2 point : tetrad.points){
            if (point.x + testOrigin.x < 0 || point.x + testOrigin.x >= TILESWIDE) return true;
            if (point.y + testOrigin.y < 0 || point.y + testOrigin.y >= TILESHIGH) return true;
        }
        return false;
    }

    public Tetrad getFreeBottomPiece(){
        OrderedSet<Tetrad> bottomPieces = new OrderedSet<>();
        for (int x = 0; x < TILESWIDE; x++) {
            for (Tetrad tetrad : tetrads) {
                if (tetrad.containsPoint(x, 0)){
                    bottomPieces.add(tetrad);
                }
            }
        }
        Array<Tetrad> tetradArray = bottomPieces.orderedItems();
        tetradArray.shuffle();
        for (Tetrad t : tetradArray){
            if (!collidesWithBlocks(t, new Vector2(0, -1))){
                return t;
            }
        }
        return null;
    }

    public void moveDown(Tetrad tetrad) {
        if (invalidMove(tetrad, new Vector2(0, -1))) {
            tetrads.add(activeTetrad);
            activeTetrad = null;
            fallInterval = Math.max(.2f, fallInterval - .05f);

            // TODO make this more async
            checkForFullRows();


            if (!tetrads.contains(tetradToRemove, true)) {
                tetradToRemove = null;
            }
            blocksToFallTilRemove --;
            if (blocksToFallTilRemove <= 0){
                if (tetradToRemove != null){
                    tetradToRemove.flashing = false;
                    gameState.setNext(tetradToRemove);
                    tetrads.removeValue(tetradToRemove, true);
                    if (tetrads.size > 0){
                        boolean removedLine = true;
                        while (removedLine) {
                            boolean clearLine = true;
                            for (int x = 0; x < TILESWIDE; x++) {
                                for (Tetrad t : tetrads) {
                                    if (t.containsPoint(x, 0)) {
                                        clearLine = false;
                                    }
                                }
                            }
                            if (clearLine) {
                                deleteRow(0);
                                removedLine = true;
                            } else {
                                removedLine = false;
                            }
                        }
                    }
                }
                tetradToRemove = getFreeBottomPiece();
                if (tetradToRemove != null) {
                    tetradToRemove.flashing = true;
                }
                blocksToFallTilRemove = 3;
            }
        } else {
            tetrad.origin.y -= 1;
        }
        timeToFall = fallInterval;
    }

    public void checkForFullRows() {
        int rowsCleared = 0;
        for (int y = TILESHIGH - 1; y >= 0; y--) {
            boolean emptySpot = false;
            for (int x = 0; x < TILESWIDE; x++) {
                boolean tileFilled = false;
                for (Tetrad tetrad : tetrads) {
                    if (tetrad.containsPoint(x, y)) {
                        tileFilled = true;
                    }
                }
                if (!tileFilled) {
                    emptySpot = true;
                }
            }
            if (!emptySpot) {
                deleteRow(y);
                y += 1;
                rowsCleared++;
            }

        }

        switch (rowsCleared){
            case 1:
                gameState.addScore(100); break;
            case 2:
                gameState.addScore(300); break;
            case 3:
                gameState.addScore(500); break;
            case 4:
                gameState.addScore(800); break;
        }
    }

    private void deleteRow(int y) {
        for (Tetrad tetrad : tetrads) {
            tetrad.deleteRow(y);
        }
    }
}
