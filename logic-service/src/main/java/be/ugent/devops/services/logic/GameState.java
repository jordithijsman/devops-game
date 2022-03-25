package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.Location;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GameState {

    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);
    private GameState gameState;

    private double PIONEER_GENERATE_GOLD_CHANCE;
    private double WORKER_GENERATE_GOLD_CHANCE;
    private List<POIsHint> hints = new ArrayList<>();
    private List<Code> codes;

    private int hash = 0;



    @JsonIgnore
    public boolean isChanged() {
        boolean result = hash != hashCode();
        hash = hashCode();
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState1 = (GameState) o;
        return Double.compare(gameState1.PIONEER_GENERATE_GOLD_CHANCE, PIONEER_GENERATE_GOLD_CHANCE) == 0 && Double.compare(gameState1.WORKER_GENERATE_GOLD_CHANCE, WORKER_GENERATE_GOLD_CHANCE) == 0 && hash == gameState1.hash && Objects.equals(gameState, gameState1.gameState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameState, PIONEER_GENERATE_GOLD_CHANCE, WORKER_GENERATE_GOLD_CHANCE, hash);
    }

    private static final Path statePath = Path.of("/app/gamestate"); // Define a path for your state here. Make it configurable!

    public void saveState() {
        // Only try to save the state if it has changed!
        if (gameState == null) {
            gameState = new GameState();
        }
        if (gameState.isChanged()) {
            logger.info("Creating state snapshot!");
            try {
                Files.writeString(statePath, Json.encode(gameState), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.warn("Could not write state!", e);
            }
        }
    }

    public void restoreState() {
        // Only try to restore the state if a state file exists!
        if (statePath.toFile().exists()) {
            try {
                gameState = Json.decodeValue(Files.readString(statePath, StandardCharsets.UTF_8), GameState.class);
            } catch (Exception e) {
                logger.warn("Could not restore state!", e);
            }
        }
        // If this line is reached and gameState is still 'null', initialize a new instance
        if (gameState == null) {
            gameState = new GameState();
        }
    }

    //Getters & Setters
    public double getPIONEER_GENERATE_GOLD_CHANCE() {
        return PIONEER_GENERATE_GOLD_CHANCE;
    }

    public void setPIONEER_GENERATE_GOLD_CHANCE(double PIONEER_GENERATE_GOLD_CHANCE) {
        this.PIONEER_GENERATE_GOLD_CHANCE = PIONEER_GENERATE_GOLD_CHANCE;
    }

    public double getWORKER_GENERATE_GOLD_CHANCE() {
        return WORKER_GENERATE_GOLD_CHANCE;
    }

    public void setWORKER_GENERATE_GOLD_CHANCE(double WORKER_GENERATE_GOLD_CHANCE) {
        this.WORKER_GENERATE_GOLD_CHANCE = WORKER_GENERATE_GOLD_CHANCE;
    }

    public List<Code> getCodes() {
        return codes;
    }

    public void setCodes(List<Code> codes) {
        this.codes = codes;
    }

    public List<POIsHint> getHints() {
        return hints;
    }

    public void setHints(List<POIsHint> hints) {
        this.hints = hints;
    }
}