package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactionLogicClericTest {

    private static final Map<UnitType, Integer> bogusUnitBaseHealth = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitType, Integer> bogusUnitCost = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitMoveType, Integer> bogusUnitMoveCost = Arrays.stream(UnitMoveType.values()).collect(Collectors.toMap(k -> k, v -> 10));
    private static final FactionLogicImpl logic = new FactionLogicImpl();
    private static final GameContext context = new GameContext(0, "test", 100, 50, bogusUnitBaseHealth, bogusUnitCost, bogusUnitMoveCost, Set.of());
    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);

    //vars shared between methods
    private static final Location own_base_location = new Location(0,0,true, false, false, 0, null);
    private static final Faction own_faction = new Faction(0, "own_faction", own_base_location, 1000, 1000, 1000, 10000, 1000, 1000, false);
    private static final Unit cleric = new Unit(0,0, UnitType.CLERIC,10,10,false);
    private static final Unit cleric_defense_bonus = new Unit(0,0, UnitType.CLERIC,10,10,true);
    private static final Location own_square = new Location(0, 0, false, false, false, 0, cleric);

    @Test
    //if there is an enemy nearby: attack
    public void ClericAttack() {
        var enemy_unit = new Unit(1,1, UnitType.SOLDIER,10,10,false);
        var enemy_square_with_enemy = new Location(0, 0, false, false, false, 1, enemy_unit);
        var ally_unit = new Unit(0,0, UnitType.SOLDIER,10,10,false);
        var ally_square_with_ally = new Location(1, 0, false, false, false, 0, ally_unit);
        logger.info("If there is an enemy and an ally with high health (health > 3) nearby the cleric should attack.");
        var input = new UnitMoveInput(context,own_faction, cleric_defense_bonus, own_square, new ArrayList<>(List.of(enemy_square_with_enemy, ally_square_with_ally)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.ATTACK, nextunitmove.type(), "Failed! Cleric did not attack the enemy unit.");
        logger.info("Success! Cleric attacked the nearby enemy unit.");
        logic.Stop();
    }

    @Test
    //if there is an enemy nearby: attack
    public void ClericHeal() {
        var ally_unit = new Unit(0,0, UnitType.SOLDIER,10,2,false);
        var enemy_unit = new Unit(1,1, UnitType.SOLDIER,10,2,false);
        var enemy_square_with_enemy = new Location(0, 1, false, false, false, 1, enemy_unit);
        var ally_square_with_ally = new Location(1, 0, false, false, false, 0, ally_unit);
        logger.info("A cleric should heal a nearby ally unit if it is low on health (health < 3)");
        var input = new UnitMoveInput(context,own_faction, cleric_defense_bonus, own_square, new ArrayList<>(List.of(enemy_square_with_enemy, ally_square_with_ally)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.HEAL, nextunitmove.type(), "Failed! Cleric did not heal the nearby ally unit.");
        logger.info("Success! Cleric healed the nearby ally unit.");
        logic.Stop();
    }

    @Test
    public void ClericConvert() {
        var enemy_unit = new Unit(1,1, UnitType.CLERIC,10,10,false);
        var enemy_square_with_enemy = new Location(0, 0, false, false, false, 1, enemy_unit);
        logger.info("If there is no ally nearby, he has a defensive bonus and there is an enemy unit nearby, a cleric should convert the enemy unit.");
        var input = new UnitMoveInput(context,own_faction, cleric_defense_bonus, own_square, new ArrayList<>(List.of(enemy_square_with_enemy)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.CONVERT, nextunitmove.type(), "Failed! Cleric did not convert the enemy unit.");
        logger.info("Success! Cleric converted the nearby enemy unit.");
        logic.Stop();
    }

    @Test
    //if there is an enemy nearby: attack
    public void ClericDefenseBonus() {
        logger.info("If a cleric does not have a defensive bonus he should prepare his defenses.");
        var input = new UnitMoveInput(context,own_faction, cleric, own_square, new ArrayList<>());
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.PREPARE_DEFENSE, nextunitmove.type(), "Failed! Cleric did not prepare his defenses.");
        logger.info("Success! Cleric prepared his defenses.");
        logic.Stop();
    }

    @Test
    //if there is no enemy and square is owned: make money or travel
    public void ClericTravel() {
        var available_square = new Location(0, 0, false, false, false, 0, null);
        logger.info("If there is no enemy or ally nearby a cleric should travel.");
        var input = new UnitMoveInput(context,own_faction, cleric_defense_bonus, own_square, new ArrayList<>(List.of(available_square)));
        var nextunitmove = logic.nextUnitMove(input);
        assertEquals(UnitMoveType.TRAVEL, nextunitmove.type(), "Failed! Cleric did not travel.");
        logger.info("Success! Cleric traveled.");
        logic.Stop();
    }

}
