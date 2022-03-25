package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class FactionLogicWorkerTest {

    private static final Map<UnitType, Integer> bogusUnitBaseHealth = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitType, Integer> bogusUnitCost = Arrays.stream(UnitType.values()).collect(Collectors.toMap(k -> k, v -> 100));
    private static final Map<UnitMoveType, Integer> bogusUnitMoveCost = Arrays.stream(UnitMoveType.values()).collect(Collectors.toMap(k -> k, v -> 10));
    private static final FactionLogicImpl logic = new FactionLogicImpl();
    private static final GameContext context = new GameContext(0, "test", 100, 50, bogusUnitBaseHealth, bogusUnitCost, bogusUnitMoveCost, Set.of());
    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);

    private static final Location baseLocation_eigen = new Location(0, 0, true, false, false, 0, null);
    private static final Location baseLocation_vijand = new Location(0, 1, true, false, false, 1, null);
    private static final Unit unit = new Unit(0, 0, UnitType.WORKER, 0, 5, false);
    private  List<Location> neighbours = new ArrayList<>();
    /*
       Worker werking
       - Travel: naar een aansluitende locatie, niet van een andere unit
       - Conquer-neutral-tile: locatie niet van andere unit, locatie overwinnen --> 75 goud
       - Generate-gold: locatie van eigen faction, met resource x2
       - Fortify: locatie van eigen faction --> 150
       - Idle (nietsdoen): geen andere mogelijkheid
       --> In principe komt Idle nooit voor want worker kan niet op een vijandelijk vakje terechtkomen?
   */
    /*
        Er zijn een aantal voorwaarden (en uiteraard combinaties) die kunnen worden getest:
        - De factie heeft niet voldoende goud
        - De factie staat op een eigen vakje, op een neutraal vakje of op een vakje van de tegenstander
        - Het vakje is al versterkt OK
        - De aansluitende locaties zijn van de tegenstander OK
        Opmerking: in de code wordt geen goud betaald voor de aangemaakte worker
     */
    @Test
    public void worker_situatie1() {
        logger.info("TEST: Worker: eigen factie, genoeg goud, geen versterking");
        /*
          SITUATIE
            Vakje is van eigen factie, er is voldoende goud, er is geen versterking
            Alle acties van een worker zijn mogelijk, behalve conquer neutral want eigen vakje is van factie
        */

        Faction eigen = new Faction(0, "eigen", baseLocation_eigen, Long.MAX_VALUE, 2, 0, 20, 0, 0, false);
        Location unit_location = new Location(1, 0, false, false, false, 0, unit);

        Location l1 = new Location(0, 0, true, false, false, 0, null);
        Location l2 = new Location(0, 1, false, false, false, null, null);
        Location l3 = new Location(1, 1, false, false, false, null, null);
        Location l4 = new Location(2, 1, false, false, false, null, null);
        Location l5 = new Location(2, 0, false, false, false, null, null);
        neighbours.add(l1);
        neighbours.add(l2);
        neighbours.add(l3);
        neighbours.add(l4);
        neighbours.add(l5);

        UnitMoveInput input = new UnitMoveInput(context, eigen, unit, unit_location, neighbours);
        UnitMove resultaat = logic.nextUnitMove(input);

        logger.info("Type: {}", resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        logic.Stop();
    }

    @Test
    public void worker_situatie2() {
        logger.info("TEST: Worker: eigen factie, geen goud, geen versterking");
        /*
         SITUATIE
         Vakje is van eigen factie, er is onvoldoende goud, er is geen versterking
         Kan geen neutraal vakje veroveren of eigen vakje versterken want er is niet genoeg goud
         */
        Faction eigen = new Faction(0, "eigen", baseLocation_eigen, 0, 2, 0, 20, 0, 0, false);

        Location unit_location = new Location(1, 0, false, false, false, 0, unit);

        Location l1 = new Location(0, 0, true, false, false, 0, null);
        Location l2 = new Location(0, 1, false, false, false, null, null);
        Location l3 = new Location(1, 1, false, false, false, null, null);
        Location l4 = new Location(2, 1, false, false, false, null, null);
        Location l5 = new Location(2, 0, false, false, false, null, null);
        neighbours.add(l1);
        neighbours.add(l2);
        neighbours.add(l3);
        neighbours.add(l4);
        neighbours.add(l5);

        UnitMoveInput input = new UnitMoveInput(context, eigen, unit, unit_location, neighbours);
        UnitMove resultaat = logic.nextUnitMove(input);

        logger.info("Type: {}", resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.FORTIFY, resultaat.type());
        logic.Stop();
    }

    @Test
    public void worker_situatie3() {
        logger.info("TEST: Worker: aansluitende vakjes van andere factie");

        /*
         SITUATIE
         aansluitende vakjes van andere factie, waardoor worker niet kan travellen
        */

        Faction eigen = new Faction(0, "eigen", baseLocation_eigen, Long.MAX_VALUE, 2, 0, 20, 0, 0, false);
        Faction vijand = new Faction(1, "vijand", baseLocation_vijand, Long.MAX_VALUE, 4, 0, 20, 0, 0, false);


        Location unit_location = new Location(1, 0, false, false, false, 0, unit);

        Location l1 = new Location(0, 0, true, false, false, 0, null);
        Location l2 = new Location(0, 1, true, false, false, 1, null);
        Location l3 = new Location(1, 1, false, false, false, 1, null);
        Location l4 = new Location(2, 1, false, false, false, 1, null);
        Location l5 = new Location(2, 0, false, false, false, 1, null);
        neighbours.add(l1);
        neighbours.add(l2);
        neighbours.add(l3);
        neighbours.add(l4);
        neighbours.add(l5);

        UnitMoveInput input = new UnitMoveInput(context, eigen, unit, unit_location, neighbours);
        UnitMove resultaat = logic.nextUnitMove(input);

        logger.info("Type: {}", resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        //assertNotEquals(UnitMoveType.TRAVEL, resultaat.type());
        assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        logic.Stop();
    }

    @Test
    public void worker_situatie4() {
        logger.info("TEST: Worker: neutraal vakje");

        /*
         SITUATIE
         Neutraal vakje: geen goud genereren, of fortify
         */

        Faction eigen = new Faction(0, "eigen", baseLocation_eigen, Long.MAX_VALUE, 1, 0, 20, 0, 0, false);


        Location unit_location = new Location(1, 0, false, false, false, null, unit);

        Location l1 = new Location(0, 0, true, false, false, 0, null);
        Location l2 = new Location(0, 1, false, false, false, null, null);
        Location l3 = new Location(1, 1, false, false, false, null, null);
        Location l4 = new Location(2, 1, false, false, false, null, null);
        Location l5 = new Location(2, 0, false, false, false, null, null);
        neighbours.add(l1);
        neighbours.add(l2);
        neighbours.add(l3);
        neighbours.add(l4);
        neighbours.add(l5);

        UnitMoveInput input = new UnitMoveInput(context, eigen, unit, unit_location, neighbours);
        UnitMove resultaat = logic.nextUnitMove(input);

        logger.info("Type: {}", resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.GENERATE_GOLD, resultaat.type());
        assertNotEquals(UnitMoveType.FORTIFY, resultaat.type());
        logic.Stop();
    }

    @Test
    public void worker_situatie5() {
        logger.info("TEST: Worker: vakje al versterkt");
        /*
         SITUATIE
         Als vakje al versterkt, kan je niet nog meer versterken
         */
        Faction eigen = new Faction(0, "eigen", baseLocation_eigen, Long.MAX_VALUE, 2, 0, 20, 0, 0, false);

        Location unit_location = new Location(1, 0, false, false, true, 0, unit);

        Location l1 = new Location(0, 0, true, false, false, 0, null);
        Location l2 = new Location(0, 1, false, false, false, null, null);
        Location l3 = new Location(1, 1, false, false, false, null, null);
        Location l4 = new Location(2, 1, false, false, false, null, null);
        Location l5 = new Location(2, 0, false, false, false, null, null);
        neighbours.add(l1);
        neighbours.add(l2);
        neighbours.add(l3);
        neighbours.add(l4);
        neighbours.add(l5);

        UnitMoveInput input = new UnitMoveInput(context, eigen, unit, unit_location, neighbours);
        UnitMove resultaat = logic.nextUnitMove(input);

        logger.info("Type: {}", resultaat.type());
        assertNotEquals(UnitMoveType.NEUTRALIZE_ENEMY_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.ATTACK, resultaat.type());
        assertNotEquals(UnitMoveType.PREPARE_DEFENSE, resultaat.type());
        assertNotEquals(UnitMoveType.HEAL, resultaat.type());
        assertNotEquals(UnitMoveType.CONVERT, resultaat.type());
        assertNotEquals(UnitMoveType.CONQUER_NEUTRAL_TILE, resultaat.type());
        assertNotEquals(UnitMoveType.FORTIFY, resultaat.type());
        logic.Stop();
    }
}
