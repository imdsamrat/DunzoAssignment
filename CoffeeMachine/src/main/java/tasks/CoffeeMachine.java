package tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import inventory.InventoryManager;
import model.Beverage;
import model.CoffeeMachineDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


public class CoffeeMachine {
    private static final Logger logger = LoggerFactory.getLogger(CoffeeMachine.class);

    private static CoffeeMachine coffeeMachine;
    public CoffeeMachineDetails coffeeMachineDetails;
    public InventoryManager inventoryManager;
    private static final int MAX_QUEUED_REQUEST = 100;
    private ThreadPoolExecutor executor;

    /**
     * makes class singleton in nature
     * will return CoffeeMachine.INSTANCE is it already exits else creates one
     * @return
     * @throws IOException
     */
    public static CoffeeMachine getInstance(final String jsonInput) throws IOException {
        if (coffeeMachine == null) {
            coffeeMachine = new CoffeeMachine(jsonInput);
        }
        return coffeeMachine;
    }

    private CoffeeMachine(String jsonInput) throws IOException {
        System.out.println("New Machine");
        this.coffeeMachineDetails = new ObjectMapper().readValue(jsonInput, CoffeeMachineDetails.class);
        int outlet = coffeeMachineDetails.getMachine().getOutlets().getCount();
        executor = new ThreadPoolExecutor(outlet, outlet, 5000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUED_REQUEST));
        executor.setRejectedExecutionHandler(new RejectedTaskHandler());
    }

    public void process() {
        this.inventoryManager = InventoryManager.getInstance();

        Map<String, Integer> ingredients = coffeeMachineDetails.getMachine().getIngredientQuantityMap();

        for (String key : ingredients.keySet()) {
            inventoryManager.addInventory(key, ingredients.get(key));
        }

        HashMap<String, HashMap<String, Integer>> beverages = coffeeMachineDetails.getMachine().getBeverages();
        for (String key : beverages.keySet()) {
            Beverage beverage = new Beverage(key, beverages.get(key));
            coffeeMachine.addBeverageRequest(beverage);
        }
    }

    public void addBeverageRequest(Beverage beverage) {
        BeverageMakerTask task = new BeverageMakerTask(beverage);
        executor.execute(task);
    }

    public void stopMachine() {
        executor.shutdown();
    }

    /**Resetting inventory and stopping coffee machine. This is only used for testing. In real world, no need for resetting unless machine is stopped.*/
    public void reset() {
        logger.info("Resetting");
        this.stopMachine();
        this.inventoryManager.resetInventory();
    }
}