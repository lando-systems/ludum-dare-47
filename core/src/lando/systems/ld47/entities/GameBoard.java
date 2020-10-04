package lando.systems.ld47.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedSet;
import lando.systems.ld47.Audio;
import lando.systems.ld47.GameState;
import lando.systems.ld47.input.PlayerInput;

public class GameBoard {
    public static int TILESWIDE = 10;
    public static int TILESHIGH = 20;

    private final GameState gameState;
    private final PlayerInput playerInput;
    private final OrthographicCamera camera;

    private final Array<Tetrad> tetrads;

    FrameBuffer gameFB;
    Texture gameTexture;

    public Tetrad activeTetrad;
    private Tetrad tetradToRemove;
    public Rectangle gameBounds;
    float fallInterval;
    float timeToFall;

    int blocksToFallTilRemove;
    private boolean previousBlockCleared = false;
    PerspectiveCamera boardCam;

    public GameBoard(GameState gameState) {
        this.gameState = gameState;
        this.playerInput = gameState.gameScreen.playerInput;
        this.camera = gameState.gameScreen.worldCamera;
        this.tetrads = new Array<>();
        float width = TILESWIDE * Tetrad.POINT_WIDTH;
        float height = TILESHIGH * Tetrad.POINT_WIDTH;
        gameBounds = new Rectangle((camera.viewportWidth - width) / 2f, (camera.viewportHeight - height) / 2f, width, height);
        blocksToFallTilRemove = 3;
        fallInterval = 1f;
        timeToFall = fallInterval;



        gameFB = new FrameBuffer(Pixmap.Format.RGBA8888, (int)gameBounds.width, (int)gameBounds.height, true);
        gameTexture = gameFB.getColorBufferTexture();

        boardCam = new PerspectiveCamera(60, 12, 20);
//        boardCam.setToOrtho(false);
        boardCam.up.set(0,0,1);
        boardCam.position.set(5,0, 17);
        boardCam.far = 400;
        boardCam.lookAt(5f, 8, 0);
        boardCam.update();

    }

    // this happens in an update loop, so it's cool
    public Tetrad swapActiveTetrad(Tetrad tetrad) {
        Tetrad current = activeTetrad;
        Vector2 origin = null;
        if (current != null) {
            origin = current.removeFromBoard();
        }

        if (tetrad == null) {
            origin = null;
            tetrad = gameState.popNext();
        }

        // figure out positioning
        tetrad.insertIntoBoard(this, origin);
        activeTetrad = tetrad;

        // TODO: move this stuff up to BaseScreen or Game so we can use controllers on other screens
        Controllers.clearListeners();
        Controllers.addListener(playerInput);


        return current;
    }

    public void update(float dt) {
        Tetrad.GLOBAL_ANIM += dt;

        for (int i = tetrads.size - 1; i >= 0; i--) {
            Tetrad tetrad = tetrads.get(i);
            tetrad.update(dt);
            if (tetrad.isEmpty()) {
                tetrads.removeIndex(i);
            }
        }

        for (int y = TILESHIGH - 1; y >= 0; y--) {
            int cellsReady = 0;
            for (int x = 0; x < TILESWIDE; x++) {
                for (Tetrad tetrad : tetrads) {
                    TetradPiece piece = tetrad.getPieceAt(x, y);
                    if (piece != null && piece.remove) {
                        cellsReady++;
                    }
                }
            }
            if (cellsReady == TILESWIDE) {
                deleteRow(y);
            }
        }

        for (int i = tetrads.size -1; i >= 0; i--) {
            Tetrad tetrad = tetrads.get(i);
            if (tetrad.isEmpty()) {
                tetrads.removeIndex(i);
            }
        }

        boolean boardResolving = false;
        for (Tetrad tetrad : tetrads) {
            if (tetrad.resolvingTetrad()) boardResolving = true;
        }

        if (!boardResolving) {

            playerInput.update(dt);
                if (activeTetrad == null) {
                activeTetrad = gameState.popNext();

                activeTetrad.insertIntoBoard(this);
                if (invalidMove(activeTetrad, Vector2.Zero)) {
                    //GAME OVER
                    //TODO something else
                    tetrads.clear();
                }
                timeToFall = fallInterval;
            }

            if (!tetrads.contains(tetradToRemove, true)) {
                tetradToRemove = null;
            }
            checkForPullOut();

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

                if (playerInput.isRotateRight()) {
                    handleRotate(-1);
                }
                if (playerInput.isRotateLeft()) {
                    handleRotate(1);
                }

                if (playerInput.isDownPressed()) {
                    moveDown(activeTetrad);
                }

                if (activeTetrad != null) {
                    if (playerInput.isPlungedPressed()) {
                        while (moveDown(activeTetrad)) {
                        }
                    }
                }
            }

            timeToFall -= dt;
            if (timeToFall < 0) {
                moveDown(activeTetrad);
            }

            if (activeTetrad != null) {
                activeTetrad.update(dt);
            }
        }
    }


    public void render(SpriteBatch batch) {


//        for (Tetrad tetrad : tetrads) {
//            tetrad.render(batch);
//        }
        batch.end();

        gameFB.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        batch.setProjectionMatrix(boardCam.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, .8f);
        batch.draw(gameState.assets.gameBoardTexture, 0, 0, TILESWIDE, TILESHIGH);
        batch.setColor(Color.WHITE);
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        gameState.gameScreen.assets.blockShader.setUniformi("u_texture", 0);
        gameState.gameScreen.assets.blockTextures.bind(0);

        for (Tetrad tetrad : tetrads) {
            tetrad.renderModels(boardCam);
        }
        if (activeTetrad != null) {
            activeTetrad.renderModels(boardCam);
        }
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        gameFB.end();


        batch.begin();
        batch.setProjectionMatrix(gameState.gameScreen.shaker.getCombinedMatrix());
//        if (activeTetrad != null) {
//            activeTetrad.render(batch);
//        }

        batch.draw(gameTexture, gameBounds.x, gameBounds.y + gameBounds.height, gameBounds.width, -gameBounds.height);
    }


    private void handleRotate(int dir) {
        activeTetrad.rotate(dir);
        if (invalidMove(activeTetrad, Vector2.Zero)) {
            if (collidesWithWalls(activeTetrad, Vector2.Zero)) {
                //Test one away first
                if (!invalidMove(activeTetrad, new Vector2(-1, 0))) {
                    activeTetrad.origin.x -= 1;
                } else if (!invalidMove(activeTetrad, new Vector2(1, 0))) {
                    activeTetrad.origin.x += 1;
                } else if (!invalidMove(activeTetrad, new Vector2(-2, 0))) {
                    activeTetrad.origin.x -= 2;
                } else if (!invalidMove(activeTetrad, new Vector2(2, 0))) {
                    activeTetrad.origin.x += 2;
                } else {
                    activeTetrad.rotate(-dir);
                }
            } else {
                activeTetrad.rotate(-dir);
            }
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
            TetradPiece point = tetrad.points.get(i);
            for (int j = 0; j < tetrads.size; j++) {
                Tetrad placedPiece = tetrads.get(j);
                if (placedPiece == tetrad) continue;
                for (TetradPiece placedPoint : placedPiece.points) {
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
        for (TetradPiece point : tetrad.points) {
            if (point.x + testOrigin.x < 0 || point.x + testOrigin.x >= TILESWIDE) return true;
            if (point.y + testOrigin.y < 0 || point.y + testOrigin.y > TILESHIGH) return true;
        }
        return false;
    }

    public Tetrad getFreeBottomPiece() {
        OrderedSet<Tetrad> bottomPieces = new OrderedSet<>();
        for (int x = 0; x < TILESWIDE; x++) {
            for (Tetrad tetrad : tetrads) {
                if (tetrad.containsPoint(x, 0)) {
                    bottomPieces.add(tetrad);
                }
            }
        }
        Array<Tetrad> tetradArray = bottomPieces.orderedItems();
        tetradArray.shuffle();

        if (tetradArray.size > 0) {
            return tetradArray.first();

        }
        return null;
    }

    public boolean moveDown(Tetrad tetrad) {
        boolean valid = false;

        if (invalidMove(tetrad, new Vector2(0, -1))) {
            tetrads.add(activeTetrad);
            playSound(Audio.Sounds.tet_land);
            activeTetrad = null;
            fallInterval = Math.max(.2f, fallInterval - .005f);

            // TODO make this more async
            checkForFullRows();

            blocksToFallTilRemove--;


            if (!tetrads.contains(tetradToRemove, true)) {
                tetradToRemove = null;
            }


        } else {
            tetrad.origin.y -= 1;
            valid = true;
        }
        timeToFall = fallInterval;
        return valid;
    }

    private void checkForPullOut() {
        if (blocksToFallTilRemove <= 0) {
            if (tetradToRemove != null) {
                tetradToRemove.flashing = false;
                gameState.setNext(tetradToRemove);
                tetrads.removeValue(tetradToRemove, true);
                if (tetrads.size > 0) {
                    boolean removedLine = true;
                    float volume = 0.5f;
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
                            playSound(Audio.Sounds.tet_clearLine, volume += 0.1f);
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
                markRowForDeletion(y, .2f);
//                y += 1;
                rowsCleared++;
            }

        }

        gameState.gameScreen.shaker.addDamage(.2f * rowsCleared);


        switch (rowsCleared) {
            case 1:
                gameState.addScore(100, 1);
                break;
            case 2:
                gameState.addScore(300, 2);
                break;
            case 3:
                gameState.addScore(500, 3);
                break;
            case 4:
                gameState.addScore(800, 4);
                break;
        }
        if (rowsCleared > 0) {
            gameState.addCombo();
        }
        else {
            gameState.breakCombo();
        }

        previousBlockCleared = rowsCleared > 0 ? true : false;

    }

    private void deleteRow(int y) {
        for (Tetrad tetrad : tetrads) {
            tetrad.deleteRow(y);
        }
    }

    private void playSound(Audio.Sounds sound) {
        this.playSound(sound, 1f);
    }

    private void playSound(Audio.Sounds sound, float volume) {
        gameState.gameScreen.game.audio.playSound(sound, volume);
    }

    private void markRowForDeletion(int y, float delay) {
        for (Tetrad tetrad : tetrads) {
            for (TetradPiece piece : tetrad.points) {
                if (tetrad.origin.y + piece.y == y) {
                    piece.destroyTimer = delay + (tetrad.origin.x + piece.x) * .05f;
                    gameState.gameScreen.particles.addPieceDeleteParticles(gameBounds.x + (tetrad.origin.x + piece.x + .5f) * Tetrad.POINT_WIDTH,
                            gameBounds.y + (tetrad.origin.y + piece.y + .5f) * Tetrad.POINT_WIDTH,
                            tetrad.color);
                }
            }
        }

    }

    private final Array<Integer> endblocks = new Array<Integer>(TILESHIGH);
    // gets the number of blocks on the ends for punching
    public Array<Integer> getRowEnds(boolean left) {
        int column = left ? 0 : TILESWIDE - 1;

        endblocks.clear();

        // if this is already checked on each update, include this there
        for (Tetrad tetrad : tetrads) {
            for (TetradPiece piece : tetrad.points) {
                if (piece.x + tetrad.origin.x == column) {
                    endblocks.add(new Integer(piece.y + (int)tetrad.origin.y));
                }
            }
        }

        return endblocks;
    }
}
