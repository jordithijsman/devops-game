package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class FactionLogicSoldierTest {

    /*
       Soldier werking
       - Travel: naar een aansluitende locatie, niet van een andere unit
       - Conquer-Neutral-tile: locatie niet van andere unit, locatie overwinnen --> 75 goud
        - Neutralize-enemy-tile: unit staat op enemy location, als gefortified enkel fortifications weg --> 25 goud
        - Attack: locatie naast unit met tegenstander --> 25 goud
        - Prepare_defense: goud 15
        - Idle (nietsdoen): geen andere mogelijkheid

   */

    private static final Map<UnitType, Integer> bogusUnitBaseHealth = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitType, Integer> bogusUnitCost = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitMoveType, Integer> bogusUnitMoveCost = Arrays.stream(UnitMoveType.values()).collect(Collectors.toMap(k -> k, v -> 10));
    private static final FactionLogicImpl logic = new FactionLogicImpl();
    private static final GameContext context = new GameContext(0, "test", 100, 50, bogusUnitBaseHealth, bogusUnitCost, bogusUnitMoveCost, Set.of());
    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);

    //vars shared between methods
    private static final Location own_base_location = new Location(0,0,true, false, false, 0, null);
    private static Faction own_faction = new Faction(0, "own_faction", own_base_location, 1000, 1000, 1000, 10000, 1000, 1000, false);
    private static final Unit soldier = new Unit(0,0, UnitType.SOLDIER,10,10,false);
    private static final Unit soldier_bonus = new Unit(0,0, UnitType.SOLDIER,10,10,true);
    private static final Location soldier_location = new Location(0, 0, false, false, false, 0, soldier);
    private static final Location own_square = new Location(0, 1, false, false, false, 0, soldier);

    @Test
    public void SoldierAttack(){
        var enemy_unit = new Unit(1,1, UnitType.PIONEER,10,10,false);
        var enemy_square_with_enemy = new Location(0, 0, false, false, false, 1, enemy_unit);
        logger.info("If there is an enemy unit nearby a soldier should attack it.");
        var input = new UnitMoveInput(context,own_faction, soldier, soldier_location, new ArrayList<>(List.of(enemy_square_with_enemy)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.ATTACK, nextunitmove.type(), "Failed! Soldier did not attack the enemy.");
        logger.info("Success! Soldier conquered the square.");
        logic.Stop();
    }

    @Test
    public void SoldierDefenseBonus(){
        logger.info("If there is no enemy to attack, a soldier should prepare his defenses.");
        var input = new UnitMoveInput(context,own_faction, soldier, soldier_location, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.PREPARE_DEFENSE, nextunitmove.type(), "Failed! Soldier did not prepare his defenses.");
        logger.info("Success! Soldier prepared his defense.");
        logic.Stop();
    }

    @Test
    //if a soldier is on an empty square and has prepared defenses: conquer it
    public void SoldierConquer() {
        var neutral_square = new Location(0, 0, false, false, false, null, soldier_bonus);
        logger.info("If there is no enemy to attack, his defense is prepared and he is on a neutral square, a soldier should conquer it.");
        var input = new UnitMoveInput(context,own_faction, soldier_bonus, neutral_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, nextunitmove.type(), "Failed! Soldier did not conquer the enemy square.");
        logger.info("Success! Soldier conquered the square.");
        logic.Stop();
    }

    @Test
    //if a soldier is on an enemy square: neutralize it
    public void SoldierNeutralize() {
        var enemy_square = new Location(0, 0, false, false, false, 1, soldier_bonus);
        logger.info("If there is no enemy to attack, his defense is prepared and he is on an enemy square, a soldier shoud neutralize it.");
        var input = new UnitMoveInput(context,own_faction, soldier_bonus, enemy_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, nextunitmove.type(), "Failed! Soldier did not neutralize the enemy square.");
        logger.info("Success! Soldier neutralized the square.");
        logic.Stop();
    }

    /*
    @Test
    public void eigenMethodes() {
        logger.info("Neutraal vakje");
        var neutral_square = new Location(0, 0, false, false, false, null, soldier);
        var input = new UnitMoveInput(context,own_faction, soldier_bonus, neutral_square, new ArrayList<>());

        UnitMove resultaat = logic.nextUnitMove(input);
        assertNotEquals(UnitMoveType.GENERATE_GOLD, resultaat.type());
        assertNotEquals(UnitMoveType.FORTIFY, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        logic.Stop();
    }

    @Test
    public void SoldiernotEnoughGold() {
        logger.info("Niet genoeg goud, neutraal vakje");
        own_faction = new Faction(0, "own_faction", own_base_location, 0, 0, 0, 20, 0, 0, false);
        var neutral_square = new Location(0, 0, false, false, false, null, soldier);
        var input = new UnitMoveInput(context,own_faction, soldier_bonus, neutral_square, new ArrayList<>());

        var nextunitmove = logic.nextUnitMove(input);
        UnitMove resultaat = logic.nextUnitMove(input);
        assertNotEquals(UnitMoveType.GENERATE_GOLD, resultaat.type());
        assertNotEquals(UnitMoveType.FORTIFY, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        // Niet genoeg goud
        //assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        logic.Stop();
    }
*/
}
