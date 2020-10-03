package lando.systems.ld47;

import aurelienribon.tweenengine.*;
import aurelienribon.tweenengine.equations.Linear;
import aurelienribon.tweenengine.equations.Sine;
import aurelienribon.tweenengine.primitives.MutableFloat;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

public class Audio implements Disposable {
    public static final float MUSIC_VOLUME = 0.5f;
    public static final float SOUND_VOLUME = 0.1f;

    public static final boolean soundEnabled = true;
    public static final boolean musicEnabled = true;

    // none should not have a sound
    public enum Sounds {
        none, tet_plunge, tet_rotate, tet_land, tet_clearLine, sas_punch, sas_swat, sas_throw, sas_jump, sas_land, sas_stun
    }

    public enum Musics {
        none, mood_track
    }

    public ObjectMap<Sounds, SoundContainer> sounds = new ObjectMap<>();
    public ObjectMap<Musics, Music> musics = new ObjectMap<>();

    public Music currentMusic;
    public MutableFloat musicVolume;
    public Musics eCurrentMusic;
    public Music oldCurrentMusic;

    private final Assets assets;
    private final TweenManager tween;

    public Audio(Game game) {
        this.assets = game.assets;
        this.tween = game.tween;

        putSound(Sounds.tet_land, assets.sampleSound);
        putSound(Sounds.tet_clearLine, assets.sampleSound);

        musics.put(Musics.mood_track, assets.moodTrack);

        musicVolume = new MutableFloat(0);
        setMusicVolume(MUSIC_VOLUME, 2f);
    }

    public void update(float dt) {
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume.floatValue());
        }

        if (oldCurrentMusic != null) {
            oldCurrentMusic.setVolume(musicVolume.floatValue());
        }
    }

    @Override
    public void dispose() {
        Sounds[] allSounds = Sounds.values();
        for (Sounds sound : allSounds) {
            if (sounds.get(sound) != null) {
                sounds.get(sound).dispose();
            }
        }
        Musics[] allMusics = Musics.values();
        for (Musics music : allMusics) {
            if (musics.get(music) != null) {
                musics.get(music).dispose();
            }
        }
        currentMusic = null;
    }

    public void putSound(Sounds soundType, Sound sound) {
        SoundContainer soundCont = sounds.get(soundType);
        if (soundCont == null) {
            soundCont = new SoundContainer();
        }

        soundCont.addSound(sound);
        sounds.put(soundType, soundCont);
    }

    public long playSound(Sounds soundOption) {
        return playSound(soundOption, SOUND_VOLUME);
    }

    public long playSound(Sounds soundOption, float volume) {
        if (!soundEnabled || soundOption == Sounds.none) return -1;

        SoundContainer soundCont = sounds.get(soundOption);
        if (soundCont == null) {
            Gdx.app.log("NoSound", "No sound found for " + soundOption.toString());
            return 0;
        }

        Sound s = soundCont.getSound();
        return (s != null) ? s.play(volume) : 0;
    }

    public void stopSound(Sounds soundOption) {
        SoundContainer soundCont = sounds.get(soundOption);
        if (soundCont != null) {
            soundCont.stopSound();
        }
    }

    public void stopAllSounds() {
        for (SoundContainer soundCont : sounds.values()) {
            if (soundCont != null) {
                soundCont.stopSound();
            }
        }
    }

    public Music playMusic(Musics musicOptions) {
        return playMusic(musicOptions, false);
    }

    public Music playMusic(Musics musicOptions, boolean playImmediately) {
        return playMusic(musicOptions, playImmediately, true);
    }

    public Music playMusic(Musics musicOptions, boolean playImmediately, boolean looping) {
        if (!musicEnabled) { return null; }

        if (playImmediately) {
            if (currentMusic != null && currentMusic.isPlaying()) {
                currentMusic.stop();
                currentMusic = startMusic(musicOptions, looping);
            }
        } else {
            if (currentMusic == null || !currentMusic.isPlaying()) {
                currentMusic = startMusic(musicOptions, looping);
            } else {
                currentMusic.setLooping(false);
                currentMusic.setOnCompletionListener(music -> {
                    currentMusic = startMusic(musicOptions, looping);
                });
            }
        }
        return currentMusic;
    }

    private Music startMusic(Musics musicOptions, boolean looping) {
        Music music = musics.get(musicOptions);
        if (music != null) {
            music.setLooping(looping);
            music.play();
        }
        return music;
    }

    public void fadeMusic(Musics musicOption) {
        if (eCurrentMusic == musicOption) return;
        Timeline.createSequence()
                .push(Tween.to(musicVolume, 1, 1).target(0).ease(Linear.INOUT))
                .push(Tween.call((type, source) -> {
                    if (currentMusic != null) currentMusic.stop();
                    eCurrentMusic = musicOption;
                    currentMusic = musics.get(musicOption);
                    currentMusic.setLooping(true);
                    currentMusic.play();
                }))
                .push(Tween.to(musicVolume, 1, 1).target(MUSIC_VOLUME).ease(Linear.INOUT))
                .start(tween);
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }

    public void setMusicVolume(float level, float duration) {
        Tween.to(musicVolume, 1, duration).target(level).ease(Sine.IN).start(tween);
    }
}

class SoundContainer {
    public Array<Sound> sounds;
    public Sound currentSound;

    public SoundContainer() {
        sounds = new Array<Sound>();
    }

    public void addSound(Sound s) {
        if (!sounds.contains(s, false)) {
            sounds.add(s);
        }
    }

    public Sound getSound() {
        if (sounds.size > 0) {
            int randIndex = MathUtils.random(0, sounds.size - 1);
            Sound s = sounds.get(randIndex);
            currentSound = s;
            return s;
        } else {
            System.out.println("No sounds found!");
            return null;
        }
    }

    public void stopSound() {
        if (currentSound != null) {
            currentSound.stop();
        }
    }

    public void dispose() {
        if (currentSound != null) {
            currentSound.dispose();
        }
    }
}
