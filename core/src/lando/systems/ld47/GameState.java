package lando.systems.ld47;

import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.primitives.MutableInteger;
import com.badlogic.gdx.utils.Pool;
import lando.systems.ld47.entities.Tetrad;
import lando.systems.ld47.leaderboard.LeaderboardService;
import lando.systems.ld47.screens.GameScreen;

public class GameState {

    private final Pool<Tetrad> tetradPool = new Pool<Tetrad>() {
        @Override
        protected Tetrad newObject() {
            return new Tetrad(gameScreen.game);
        }
    };

    public GameScreen gameScreen;
    public Assets assets;
    public TweenManager tween;

    private Tetrad next;
    private MutableInteger mutScore = new MutableInteger(0);
    private MutableInteger mutLineCleared = new MutableInteger(0);
    private MutableInteger comboScore = new MutableInteger(0);

    // Settings
    public boolean showGhost;


    public GameState(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.assets = gameScreen.assets;
        this.tween = gameScreen.game.tween;
        this.showGhost = true;
        popNext();

    }

    public Tetrad popNext() {
        Tetrad tetrad = next;
        next = tetradPool.obtain();
        return tetrad;
    }

    public void freeTetrad(Tetrad tetrad) {
        tetradPool.free(tetrad);
    }

    public void setNext(Tetrad tetrad){
        this.next = tetrad;
    }

    // for hud
    public Tetrad viewNext() {
        return next;
    }

    public int getScore() {
        return mutScore.intValue();
    }

    public int getLineCleared() {
        return mutLineCleared.intValue();
    }

    public int getCombo() {
        return comboScore.intValue();
    }

    public void addScore(int amount, int lineCleared) {
        mutScore.setValue(mutScore.intValue() + amount);
        mutLineCleared.setValue(mutLineCleared.intValue() + lineCleared);
    }
    public void addCombo() {
        comboScore.setValue(comboScore.intValue() + 1);
    }

    public void breakCombo() {
        comboScore.setValue(0);
    }

}
