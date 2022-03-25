package be.ugent.devops.services.logic;

import be.ugent.devops.commons.model.*;
import be.ugent.devops.services.logic.utils.ServiceStats;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class FactionLogicImpl implements FactionLogic {
    private GameState gameState;
    private static final Logger logger = LoggerFactory.getLogger(FactionLogicImpl.class);
    private static final Random rg = new Random();
    private static double PIONEER_GENERATE_GOLD_CHANCE = 1.00;
    private static double WORKER_GENERATE_GOLD_CHANCE = 0.65;
    private static final int MONEY_CAP = 5000;
    static final Gauge territory = Gauge.build()
            .name("faction_territory")
            .help("Amount of territory hold by the Faction.")
            .register();
    static final Gauge population = Gauge.build()
            .name("faction_population")
            .help("Population of the Faction.")
            .register();
    static final Gauge money = Gauge.build()
            .name("faction_money")
            .help("Gold of faction")
            .register();
    static final Gauge score = Gauge.build()
            .name("faction_score")
            .help("Score of faction")
            .register();
    static final Gauge wchance = Gauge.build()
            .name("worker_chance")
            .help("Chance for worker to generate gold")
            .register();
    static final Gauge pchance = Gauge.build()
            .name("pioneer_chance")
            .help("Chance for pioneer to generate gold")
            .register();
    static final Gauge kills = Gauge.build()
            .name("kills")
            .help("Amount of kills")
            .register();
    static final Gauge popcap = Gauge.build()
            .name("popcap")
            .help("Population cap")
            .register();

    private final JsonObject config;
    private String currentGameId;
    //private List<Code> codes;
    private HTTPServer prometheusServer;

    public void Stop(){
        prometheusServer.stop();
    }

    public FactionLogicImpl() {
        this(new JsonObject());
        logger.info("constructor");
        if(gameState == null){
            gameState = new GameState();
            //gameState.setPIONEER_GENERATE_GOLD_CHANCE(1.00);
            //gameState.setWORKER_GENERATE_GOLD_CHANCE(0.65);
            //gameState.setHints(new ArrayList<>());
            //gameState.saveState();

        }
        gameState.restoreState();
        /*
        try {
            URL obj = new URL("https://devops-proxy.atlantis.ugent.be/api/codes/3");
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                ObjectMapper mapper = new ObjectMapper();


                // convert JSON string to object

                 codes = mapper.readValue(response.toString(), new TypeReference<List<Code>>() {
                });
            } else {
                System.out.println("GET request failed");
            }
        } catch (IOException ignored) {

        }
         */
    }

    public FactionLogicImpl(JsonObject config) {
        this.config = config;
        if(gameState == null){
            gameState = new GameState();
            //gameState.setPIONEER_GENERATE_GOLD_CHANCE(1.00);
            //gameState.setWORKER_GENERATE_GOLD_CHANCE(0.65);
            //gameState.setHints(new ArrayList<>());
            //gameState.saveState();

        }
        gameState.restoreState();
        //PIONEER_GENERATE_GOLD_CHANCE = gameState.getPIONEER_GENERATE_GOLD_CHANCE();
        //WORKER_GENERATE_GOLD_CHANCE = gameState.getWORKER_GENERATE_GOLD_CHANCE();
        //hints = gameState.getHints();

        try {
            logger.info("Prometheus server started");
            prometheusServer = new HTTPServer(1234);
        } catch (IOException ex) {
            // Wrap the IO Exception as a Runtime Exception so HttpBinding can remain unchanged.
            logger.info("Prometheus server not started");
            throw new RuntimeException("The HTTPServer required for Prometheus could not be created!", ex);
        }
    }

    @Override
    public BaseMove nextBaseMove(BaseMoveInput input) {

        logger.info("Set metrics");
        popcap.set(input.faction().populationCap());
        territory.set(input.faction().territorySize());
        population.set(input.faction().population());
        money.set(input.faction().gold());
        score.set(input.faction().score());
        pchance.set(PIONEER_GENERATE_GOLD_CHANCE);
        wchance.set(WORKER_GENERATE_GOLD_CHANCE);
        kills.set(input.faction().kills());

        if(gameState == null){
            gameState = new GameState();            //for tests
        }
        if (!(input.context().gameId().equals(currentGameId))) {
            currentGameId = input.context().gameId();
            logger.info("Start running game with id {}...", currentGameId);
            PIONEER_GENERATE_GOLD_CHANCE = 1.00;
            WORKER_GENERATE_GOLD_CHANCE = 0.65;
            gameState = new GameState();
            gameState.setPIONEER_GENERATE_GOLD_CHANCE(PIONEER_GENERATE_GOLD_CHANCE);
            gameState.setWORKER_GENERATE_GOLD_CHANCE(WORKER_GENERATE_GOLD_CHANCE);
            gameState.setHints(new ArrayList<>());
        }
            /*
            if(codes != null && !codes.isEmpty()){
                var ret = codes.get(0);
                codes.remove(0);
                return ret.UseCode();
            }
             */
        gameState.saveState();
        return nextUnit(input.faction())
                .filter(type -> input.faction().gold() >= input.context().unitCost().get(type) && input.buildSlotState().isEmpty())
                .map(MoveFactory::baseBuildUnit)
                .orElseGet(() -> input.buildSlotState()
                        .map(it -> MoveFactory.baseContinueBuilding())
                        .orElseGet(MoveFactory::baseReceiveIncome)
                );
    }

    @Override
    public UnitMove nextUnitMove(UnitMoveInput input) {
        return switch (input.unit().type()) {
            case PIONEER -> pioneerLogic(input);
            case SOLDIER -> soldierLogic(input);
            case WORKER -> workerLogic(input);
            case CLERIC -> clericLogic(input);
        };
    }

    private Optional<UnitType> nextUnit(Faction faction) {
        logger.info("nextUnit");
        if (faction.population() < faction.populationCap()) {
            //if gold is high, only create soldiers to expand territory
            if(faction.gold() > MONEY_CAP){
                PIONEER_GENERATE_GOLD_CHANCE = 0.00;
                gameState.setPIONEER_GENERATE_GOLD_CHANCE(0.00);
                if(faction.gold()>MONEY_CAP* 2L){
                    if(faction.gold() > MONEY_CAP* 4L){
                        WORKER_GENERATE_GOLD_CHANCE = 0.1;
                        gameState.setWORKER_GENERATE_GOLD_CHANCE(0.1);
                    }else {
                        WORKER_GENERATE_GOLD_CHANCE = 0.45;
                        gameState.setWORKER_GENERATE_GOLD_CHANCE(0.45);
                    }
                    return Optional.of(randomListItem(List.of(UnitType.SOLDIER)));
                }else {
                    WORKER_GENERATE_GOLD_CHANCE = 0.50;
                    gameState.setWORKER_GENERATE_GOLD_CHANCE(0.50);
                }
                return Optional.of(randomListItem(List.of(UnitType.WORKER,UnitType.WORKER, UnitType.SOLDIER, UnitType.SOLDIER, UnitType.SOLDIER)));
            }else{
                WORKER_GENERATE_GOLD_CHANCE = 0.65;
                gameState.setWORKER_GENERATE_GOLD_CHANCE(0.65);
            }
            //Don't create any more pioneers. 3 workers, 2 soldiers, 1 cleric
            return Optional.of(randomListItem(List.of(UnitType.WORKER)));
        } else {
            return Optional.empty();
        }
    }

    private UnitMove workerLogic(UnitMoveInput input) {
        var worker = input.unit();
        var workerLocation = input.unitLocation();

        // Always try to move away from our own base location and enemy locations
        if (workerLocation.isBase() || isHostileLocation(workerLocation, worker.owner())) {
            return travel(input).orElse(MoveFactory.unitIdle());
        }

        // If not on resource, try moving to a resource (that is not in enemy territory)
        var resourceLocation = input.neighbouringLocations().stream()
                .filter(loc -> loc.isResource() && !isHostileLocation(loc, worker.owner()))
                .findAny();
        if (!workerLocation.isResource() && resourceLocation.isPresent() && resourceLocation.get().getOccupyingUnit().isEmpty()) {
            return MoveFactory.unitTravelTo(resourceLocation.get());
        }

        // If on a neutral or owned resource
        if (!isHostileLocation(workerLocation, worker.owner()) && workerLocation.isResource()) {
            // First capture if neutral
            if (workerLocation.getOwner().isEmpty()) {
                return MoveFactory.unitConquerLocation();
            } else if (!workerLocation.isFortified()){
                // Fortify this strategic location if it contains extra gold
                return MoveFactory.unitFortifyLocation();
            } else {
                // Profit!
                return MoveFactory.unitGenerateGold();
            }
            //not resource but neutral or not owned, don't fortify if not enough money
        }else if(!workerLocation.isResource() && !isHostileLocation(workerLocation, worker.owner())){
            // First capture if neutral
            if (workerLocation.getOwner().isEmpty()) {
                return MoveFactory.unitConquerLocation();
                // Otherwise, generate income a percentage of the time, else travel around to raise the chance of getting to a resource
            }else if(!workerLocation.isFortified() && input.faction().gold() > MONEY_CAP*7){
                return MoveFactory.unitFortifyLocation();
                }else
                if (rg.nextDouble() <= WORKER_GENERATE_GOLD_CHANCE) {
                    return MoveFactory.unitGenerateGold();
                } else {
                    return travel(input).orElse(MoveFactory.unitGenerateGold());
                }
        }

        // Else try to travel
        return travel(input).orElse(MoveFactory.unitIdle());
    }

    private UnitMove pioneerLogic(UnitMoveInput input) {
        logger.info("PioneerLogic");
        var pioneer = input.unit();
        var pioneerLocation = input.unitLocation();
        if(input.faction().gold() > MONEY_CAP){
            PIONEER_GENERATE_GOLD_CHANCE = 0.00;
            gameState.setPIONEER_GENERATE_GOLD_CHANCE(0.00);
            // If possible, conquer territory
            if (!Optional.of(pioneer.owner()).equals(pioneerLocation.getOwner())) {
                if (pioneerLocation.getOwner().isEmpty()) {
                    return MoveFactory.unitConquerLocation();
                } else {
                    return MoveFactory.unitNeutralizeLocation();
                }
            }

            // Attack enemies in range
            var enemyInRange = input.neighbouringLocations().stream()
                    .flatMap(loc -> loc.getOccupyingUnit().stream())
                    .filter(occupyingUnit -> occupyingUnit.owner() != pioneer.owner())
                    .findAny();
            if (enemyInRange.isPresent()) {
                return MoveFactory.unitAttack(enemyInRange.get());
            }
        }

        // Otherwise, generate income a percentage of the time, else travel around
        if (rg.nextDouble() <= PIONEER_GENERATE_GOLD_CHANCE) {
            return MoveFactory.unitGenerateGold();
        } else {
            return travel(input).orElse(MoveFactory.unitGenerateGold());
        }
    }

    private UnitMove soldierLogic(UnitMoveInput input) {
        var soldier = input.unit();
        var soldierLocation = input.unitLocation();

        // Attack if enemy unit is near (should have priority as the soldier is the strongest unit)
        var enemyInRange = input.neighbouringLocations().stream()
                .flatMap(loc -> loc.getOccupyingUnit().stream())
                .filter(occupyingUnit -> occupyingUnit.owner() != soldier.owner())
                .findAny();
        if (enemyInRange.isPresent()) {
            return MoveFactory.unitAttack(enemyInRange.get());
        }

        // Prepare defences for next encounter
        if (!soldier.defenseBonus()) {
            return MoveFactory.unitPrepareDefense();
        }


        // If possible, conquer territory
        if (!Optional.of(soldier.owner()).equals(soldierLocation.getOwner()) && (input.faction().gold() > input.context().unitCost().get(UnitType.SOLDIER))) {
            if (soldierLocation.getOwner().isEmpty()) {
                return MoveFactory.unitConquerLocation();
            } else {
                return MoveFactory.unitNeutralizeLocation();
            }
        }

        // Else try to travel
        return travel(input).orElse(MoveFactory.unitPrepareDefense());
    }

    private UnitMove clericLogic(UnitMoveInput input){
        var cleric = input.unit();
        //check for enemies and allied soldiers and allies in range
        var enemyInRange = input.neighbouringLocations().stream()
                .flatMap(loc -> loc.getOccupyingUnit().stream())
                .filter(occupyingUnit -> occupyingUnit.owner() != cleric.owner())
                .findAny();
        var allySoldierInRange = input.neighbouringLocations().stream()
                .flatMap(loc -> loc.getOccupyingUnit().stream())
                .filter(occupyingUnit -> occupyingUnit.owner() == cleric.owner()  && occupyingUnit.type() == UnitType.SOLDIER)
                .findAny();
        var allyInRange = input.neighbouringLocations().stream()
                .flatMap(loc -> loc.getOccupyingUnit().stream())
                .filter(occupyingUnit -> occupyingUnit.owner() == cleric.owner())
                .findAny();

        //get defensive bonus
        if (!cleric.defenseBonus()) {
            return MoveFactory.unitPrepareDefense();
        }else if(allySoldierInRange.isPresent() && enemyInRange.isPresent()){
            if (allySoldierInRange.get().health()<=3){
                //if enemy present and soldier low on health, heal soldier
                return MoveFactory.unitHeal(allySoldierInRange.get());
            }else{
                //if enemy present and unit not low on health, support attack of soldier
                return MoveFactory.unitAttack(enemyInRange.get());
            }
        }else if(enemyInRange.isPresent() && allyInRange.isEmpty()){
            //if enemy present and no ally present, convert enemy
            return MoveFactory.unitConvert(enemyInRange.get());
        }else if(allyInRange.isPresent() && (allyInRange.get().health()<3)){
            //if no enemy present and an ally needs healing, heal ally
            MoveFactory.unitHeal(allyInRange.get());
        }

        // Else try to travel
        return travel(input).orElse(MoveFactory.unitPrepareDefense());
    }

    private <T> T randomListItem(List<T> input) {
        System.out.println(input.size());
        return input.get(rg.nextInt(input.size()));
    }

    private boolean isHostileLocation(Location location, Integer faction) {
        return location.getOwner().map(owner -> !owner.equals(faction)).orElse(false);
    }

    private Optional<UnitMove> travel(UnitMoveInput input) {
        if(input.unit().type() == UnitType.SOLDIER || input.unit().type() == UnitType.PIONEER){
            var possibleMovesENEMY = input.neighbouringLocations().stream()
                    .filter(loc -> loc.getOwner().isPresent())
                    .filter(loc -> !(loc.getOwner().get().equals(input.unit().owner())) && loc.isFortified()) // Don't go back to own base.
                    .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                    .collect(Collectors.toList());
            if(possibleMovesENEMY.isEmpty()) {
                possibleMovesENEMY = input.neighbouringLocations().stream()
                        .filter(loc -> loc.getOwner().isPresent())
                        .filter(loc -> !(loc.getOwner().get().equals(input.unit().owner())))
                        .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                        .collect(Collectors.toList());
                if (possibleMovesENEMY.isEmpty()) {
                    possibleMovesENEMY = input.neighbouringLocations().stream()
                            .filter(loc -> loc.getOwner().isEmpty())
                            .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                            .collect(Collectors.toList());
                    if (possibleMovesENEMY.isEmpty()) {
                        possibleMovesENEMY = input.neighbouringLocations().stream()
                                .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                                .collect(Collectors.toList());
                        if(possibleMovesENEMY.isEmpty()){
                            if(input.unit().type() == UnitType.PIONEER){
                                return Optional.of(MoveFactory.unitGenerateGold());
                            }
                            return Optional.of(MoveFactory.unitIdle());
                        }
                        return Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMovesENEMY)));
                    }
                    return Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMovesENEMY)));
                }
                return Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMovesENEMY)));
            }
            return Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMovesENEMY)));

        }else if(input.unit().type() == UnitType.WORKER){
            List<Location> possibleMovesWORKER;
            if(input.faction().gold() > MONEY_CAP){
                possibleMovesWORKER = input.neighbouringLocations().stream()
                        .filter(loc -> loc.getOwner().isEmpty())
                        .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                        .collect(Collectors.toList());
            }else{
                possibleMovesWORKER = input.neighbouringLocations().stream()
                        .filter(loc -> loc.getOwner().isPresent())
                        .filter(loc -> loc.getOwner().get() == input.unit().owner())
                        .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                        .collect(Collectors.toList());
            }

            if(possibleMovesWORKER.isEmpty()){
                possibleMovesWORKER = input.neighbouringLocations().stream()
                        .filter(loc -> loc.getOwner().isEmpty())
                        .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                        .collect(Collectors.toList());
                if(possibleMovesWORKER.isEmpty()){
                    possibleMovesWORKER = input.neighbouringLocations().stream()
                            .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                            .collect(Collectors.toList());
                }
            }
            return Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMovesWORKER)));
        }
        var possibleMoves = input.neighbouringLocations().stream()
                .filter(loc -> !loc.isBase() || !loc.getOwner().equals(Optional.of(input.unit().owner()))) // Don't go back to own base.
                .filter(loc -> loc.getOccupyingUnit().isEmpty()) // The target location should not be occupied.
                .collect(Collectors.toList());
        return possibleMoves.isEmpty() ? Optional.empty() : Optional.of(MoveFactory.unitTravelTo(randomListItem(possibleMoves)));
    }

    List<POIsHint> hints = new ArrayList<>();

    public int registerPOIs(POIsHint hint) {
        hints.add(hint);
        gameState.setHints(hints);
        return 1;
    }
}

