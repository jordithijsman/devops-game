package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FactionLogicPioneerTest {
    /*
       Pionier werking
        - Travel: naar een aansluitende locatie, niet van een andere unit
        - Conquer_Neutral_tile: locatie niet van andere unit, locatie overwinnen --> 75 goud
        - Neutralize-enemy-tile: unit staat op enemy location, als gefortified enkel fortifications weg --> 25 goud
        - Generate-gold: locatie van eigen faction, met resource x2
        - Attack: locatie naast unit met tegenstander --> 25 goud
        - Idle (nietsdoen): geen andere mogelijkheid
   */
    /*
        Er zijn een aantal voorwaarden (en uiteraard combinaties) die kunnen worden getest:
        - De factie heeft niet voldoende goud
        - De factie staat op een eigen vakje, op een neutraal vakje of op een vakje van de tegenstander
        - De aansluitende locaties van tegenstander
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
    private static final Unit pioneer = new Unit(0,0, UnitType.PIONEER,10,10,false);
    private static final Location own_square = new Location(0, 0, false, false, false, 0, pioneer);

    @Test
    //if a pioneer is on an empty square: conquer it
    public void pioneerConquer() {
        var neutral_square = new Location(0, 0, false, false, false, null, pioneer);
        logger.info("If a pioneer is on an empty square he should conquer it.");
        var input = new UnitMoveInput(context,own_faction, pioneer, neutral_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.GENERATE_GOLD, nextunitmove.type(), "Failed! Pioneer did not conquer the square.");
        logger.info("Success! Pioneer conquered the square.");
        logic.Stop();
    }

    @Test
    //if a pioneer is on an enemy square: neutralize it
    public void pioneerNeutralize() {
        var enemy_square = new Location(0, 0, false, false, false, 1, pioneer);
        logger.info("If a pioneer is on an enemy square he should neutralize it.");
        var input = new UnitMoveInput(context,own_faction, pioneer, enemy_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.GENERATE_GOLD, nextunitmove.type(),"Failed! Pioneer did not neutralize the square.");
        logger.info("Success! Pioneer neutralized the square.");
        logic.Stop();
    }

    @Test
    //if there is an enemy nearby: attack
    public void pioneerAttack() {
        var enemy_unit = new Unit(1,1, UnitType.PIONEER,10,10,false);
        var enemy_square_with_enemy = new Location(0, 0, false, false, false, 1, enemy_unit);
        logger.info("If there is an enemy nearby the pioneer should attack.");
        var input = new UnitMoveInput(context,own_faction, pioneer, own_square, new ArrayList<>(List.of(enemy_square_with_enemy)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.GENERATE_GOLD, nextunitmove.type(), "Failed! Pioneer did not attack the enemy unit.");
        logger.info("Success! Pioneer attacked the nearby enemy unit.");
        logic.Stop();
    }

    @Test
    //if there is no enemy and square is owned: make money or travel
    public void pioneerTravelOrGold() {
        var available_square = new Location(0, 0, false, false, false, 0, null);
        logger.info("If there is no enemy nearby and the pioneer's faction owns the square he should generate gold or travel.");
        var input = new UnitMoveInput(context,own_faction, pioneer, own_square, new ArrayList<>(List.of(available_square)));
        var nextunitmove = logic.nextUnitMove(input);
        var expected_result = new ArrayList<>(List.of(UnitMoveType.GENERATE_GOLD, UnitMoveType.TRAVEL));
        assertTrue(expected_result.contains(nextunitmove.type()), "Failed! The unit did: \"" + nextunitmove.type() + "\" instead.");
        logger.info("Success! Pioneer executed the correct action: \"" + nextunitmove.type() + "\".");
        logic.Stop();
    }

    /*
    @Test
    // Neutraal vakje: enkel eigen methodes
    public void neutralTile(){
        own_faction = new Faction(0, "own_faction", own_base_location, 1000, 0, 0, 20, 0, 0, false);
        var neutral_square = new Location(0, 0, false, false, false, null, pioneer);
        logger.info("Neutral tile, don't neutralize or generate gold");
        var input = new UnitMoveInput(context,own_faction, pioneer, neutral_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        UnitMove resultaat = logic.nextUnitMove(input);
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.IDLE, resultaat.type());
        // Gaat niet want neutraal vakje
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        //assertNotEquals(UnitMoveType.GENERATE_GOLD, resultaat.type());
        logic.Stop();
    }
*/

    /*
    @Test
    public void situatie1() {
        // Houd rekening dat map rondom gaat
        ArrayList<> new ArrayList<>()
        var input = new UnitMoveInput(context,own_faction, pioneer, neutral_square, );
        var nextunitmove = logic.nextUnitMove(input);
        UnitMove resultaat = logic.nextUnitMove(input);
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.IDLE, resultaat.type());
        assertNotEquals(UnitMoveType.TRAVEL, resultaat.type());
        logic.Stop();
    }
*/
    /*
    @Test
    public void notEnoughGold() {
        logger.info("Niet genoeg goud, neutraal vakje");
        own_faction = new Faction(0, "own_faction", own_base_location, 0, 0, 0, 20, 0, 0, false);
        var neutral_square = new Location(0, 0, false, false, false, null, pioneer);

        var input = new UnitMoveInput(context,own_faction, pioneer, neutral_square, new ArrayList<>());
        UnitMove resultaat = logic.nextUnitMove(input);
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.IDLE, resultaat.type());
        assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        // Gaat niet want neutraal vakje
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        //assertNotEquals(UnitMoveType.GENERATE_GOLD, resultaat.type());
        logic.Stop();
    }
     */
}
