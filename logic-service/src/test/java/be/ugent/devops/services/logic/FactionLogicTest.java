package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


// TODO: loggers toevoegen als test gelukt met bevestiging

public class FactionLogicTest {

    private static final Map<UnitType, Integer> bogusUnitBaseHealth = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitType, Integer> bogusUnitCost = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitMoveType, Integer> bogusUnitMoveCost = Arrays.stream(UnitMoveType.values()).collect(Collectors.toMap(k -> k, v -> 10));
    private static final FactionLogicImpl logic = new FactionLogicImpl();
    private static final GameContext context = new GameContext(0, "test", 100, 50, bogusUnitBaseHealth, bogusUnitCost, bogusUnitMoveCost, Set.of());
    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);

    //vars shared between methods

    private static final Unit cleric = new Unit(0,0, UnitType.CLERIC,10,10,false);
    private static final Unit cleric_defense_bonus = new Unit(0,0, UnitType.CLERIC,10,10,true);
    private static final Location own_square = new Location(0, 0, false, false, false, 0, cleric);

    @Test
    public void FactionBuildsUnitTest() {

         //if nextBaseMove returns a BaseMove with Type START_BUILDING_UNIT when the Faction has
         //enough gold and the population limit has not been reached (and the Faction's building slot is empty).

        logger.info("TEST: testFactionBuildsUnit");

        var baseLocation = new Location(0, 0, false, false, false, 0, null);
        // Faction with maximum amount of gold and sufficient population capacity
        var faction = new Faction(0, "TestFaction", baseLocation, Long.MAX_VALUE, 200, 0, 20, 0, 0, false);
        // The build slot of the faction is empty
        var input = new BaseMoveInput(context, faction, Optional.empty());
        var move = logic.nextBaseMove(input);

        // With these conditions, the Faction Logic should return a START_BUILDING_UNIT move
        // Assert if this true
        assertEquals(BaseMoveType.START_BUILDING_UNIT, move.type());
        logic.Stop();
    }



    @Test
    public void testNextBaseMoveTest() {
        // Momenteel geen rekening gehouden met redeem_code
        logger.info("TEST: testNextBaseMove");
        logger.info("TEST: output = BaseMove?");

        var baseLocation = new Location(0, 0, false, false, false, 0, null);
        var faction = new Faction(0, "eigen", baseLocation, Long.MAX_VALUE, 200, 0, 20, 0, 0, false);
        var input = new BaseMoveInput(context, faction, Optional.empty());

        // Als nog geen unit aan het maken bent, kan je niet continue build
        var move = logic.nextBaseMove(input);
        logger.info("Actie: {}", move.type());
        assertNotEquals(BaseMoveType.CONTINUE_BUILDING_UNIT, move.type());

        // Wanneer je een move doet van de base als je bezig bent met een unit, wordt de buildingslot gereset.
        if (move.type().equals(BaseMoveType.MOVE_BASE)) {
            if (input.buildSlotState().isPresent() && input.buildSlotState().get().turnsLeft() != 0) {
                logger.info("RESET - Nog bezig met {} voor {} turns.", input.buildSlotState().get().unitType(), input.buildSlotState().get().turnsLeft());
            }
        }
        logic.Stop();
    }

}


