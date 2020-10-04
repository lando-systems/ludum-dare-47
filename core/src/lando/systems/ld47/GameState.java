package lando.systems.ld47;

import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.primitives.MutableInteger;
import lando.systems.ld47.entities.Tetrad;
import lando.systems.ld47.screens.GameScreen;

public class GameState {

    public GameScreen gameScreen;
    public Assets assets;
    public TweenManager tween;

    private Tetrad next;
    private MutableInteger mutScore = new MutableInteger(0);
    private MutableInteger mutLineCleared = new MutableInteger(0);


    public GameState(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.assets = gameScreen.assets;
        this.tween = gameScreen.game.tween;
        popNext();
    }

    public Tetrad popNext() {
        Tetrad tetrad = next;
        next = new Tetrad(gameScreen.game);
        return tetrad;
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

    public void addScore(int amount, int lineCleared) {
        mutScore.setValue(mutScore.intValue() + amount);
        mutLineCleared.setValue(mutLineCleared.intValue() + lineCleared);
    }
}
