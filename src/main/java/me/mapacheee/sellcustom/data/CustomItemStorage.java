package me.mapacheee.sellcustom.data;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public final class CustomItemStorage {

    private final Logger logger;
    private final Path dataPath;
    private Map<String, CustomItem> items = new HashMap<>();
    private ConfigurationNode rootNode;

    @Inject
    public CustomItemStorage(Logger logger) {
        this.logger = logger;
        this.dataPath = Paths.get("plugins/SellCustomItems", "custom-items.yml");
        load();
    }

    public void load() {
        try {
            if (!Files.exists(dataPath.getParent())) {
                Files.createDirectories(dataPath.getParent());
            }

            if (!Files.exists(dataPath)) {
                Files.createFile(dataPath);
                createDefaultFile();
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(dataPath)
                    .build();
            rootNode = loader.load();

            items.clear();
            ConfigurationNode itemsNode = rootNode.node("items");
            itemsNode.childrenMap();
            for (Object key : itemsNode.childrenMap().keySet()) {
                String itemId = key.toString();
                ConfigurationNode itemNode = itemsNode.node(itemId);
                CustomItem item = itemNode.get(CustomItem.class);
                if (item != null) {
                    if (item.getId() == null || item.getId().isEmpty()) {
                        item.setId(itemId);
                    }
                    items.put(itemId.toLowerCase(), item);
                }
            }

            logger.info("Loaded {} custom items", items.size());
        } catch (IOException e) {
            logger.error("Failed to load custom items", e);
        }
    }

    private void createDefaultFile() throws IOException {
        String defaultContent = """
items:
  diamond:
    name: "&bDiamond"
    material: "DIAMOND"
    buy-price: 500.0
    sell-price: 250.0
    can-buy: true
    can-sell: true
    enabled: true
    slot: 0

  iron_ingot:
    name: "&fIron Ingot"
    material: "IRON_INGOT"
    buy-price: 50.0
    sell-price: 25.0
    can-buy: true
    can-sell: true
    enabled: true
    slot: 1

  gold_ingot:
    name: "&eGold Ingot"
    material: "GOLD_INGOT"
    buy-price: 100.0
    sell-price: 50.0
    can-buy: true
    can-sell: true
    enabled: true
    slot: 2
""";
        Files.writeString(dataPath, defaultContent);
    }

    public void save() {
        try {
            ConfigurationNode itemsNode = rootNode.node("items");
            itemsNode.set(null);
            itemsNode = rootNode.node("items");

            for (Map.Entry<String, CustomItem> entry : items.entrySet()) {
                itemsNode.node(entry.getKey()).set(entry.getValue());
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(dataPath)
                    .build();
            loader.save(rootNode);
            logger.info("Saved {} custom items", items.size());
        } catch (IOException e) {
            logger.error("Failed to save custom items", e);
        }
    }

    public Map<String, CustomItem> getAllItems() {
        return new HashMap<>(items);
    }

    public CustomItem getItem(String id) {
        return items.get(id.toLowerCase());
    }

    public void addItem(CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
        save();
    }

    public void updateItem(CustomItem item) {
        items.put(item.getId().toLowerCase(), item);
        save();
    }

    public void removeItem(String id) {
        items.remove(id.toLowerCase());
        save();
    }

    public List<CustomItem> getEnabledItems() {
        return items.values().stream()
                .filter(CustomItem::isEnabled)
                .toList();
    }
}