package addon.brainsynder.weight;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import simplepets.brainsynder.addon.AddonConfig;
import simplepets.brainsynder.addon.PetAddon;
import simplepets.brainsynder.api.Namespace;
import simplepets.brainsynder.api.event.entity.PostPetHatEvent;
import simplepets.brainsynder.api.pet.PetType;
import simplepets.brainsynder.api.pet.PetWeight;
import simplepets.brainsynder.api.pet.annotations.PetCustomization;
import simplepets.brainsynder.api.plugin.SimplePets;
import simplepets.brainsynder.api.user.PetUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Namespace(namespace = "PetWeight")
public class WeightAddon extends PetAddon {
    private NamespacedKey WEIGHT;
    private boolean stacked = false;
    private int maxWeight = 5;

    private Map<PetWeight, Integer> weightMap;

    @Override
    public void loadDefaults(AddonConfig config) {
        if (weightMap == null) weightMap = Maps.newHashMap();

        config.addDefault("Weight_Stacked", false, "If the player has multiple pets as a hat should the weight combine?\nDefault: false");
        config.addDefault("Max_Weight", 5, "How heavy can the total weight be for the player\nDefault: 5");
        config.addComment("Weight_Slowness", "What level of slowness should be given");
        for (PetWeight weight : PetWeight.values()) {
            if (weight == PetWeight.NONE) continue;
            config.addDefault("Weight_Slowness."+weight.name(), weight.ordinal(), "Default: "+weight.ordinal());

            weightMap.put(weight, config.getInt("Weight_Slowness."+weight.name(), weight.ordinal()));
        }



        stacked = config.getBoolean("Weight_Stacked", false);
        maxWeight = config.getInt("Max_Weight", 5);
    }

    @Override
    public void init() {
        if (weightMap == null) weightMap = Maps.newHashMap();
        WEIGHT = new NamespacedKey(SimplePets.getPlugin(), "weight");

        Bukkit.getOnlinePlayers().forEach(player -> {
            SimplePets.getUserManager().getPetUser(player).ifPresent(this::handlePetWeight);
        });
    }

    @Override
    public void cleanup() {
        WEIGHT = new NamespacedKey(SimplePets.getPlugin(), "weight");
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getPersistentDataContainer().has(WEIGHT, PersistentDataType.INTEGER)) {
                player.removePotionEffect(PotionEffectType.SLOW);
                player.getPersistentDataContainer().remove(WEIGHT);
            }
        });
    }

    @Override
    public double getVersion() {
        return 0.2;
    }

    @Override
    public String getAuthor() {
        return "brainsynder";
    }

    @Override
    public List<String> getDescription() {
        return Lists.newArrayList(
                "&7This addon makes it so pets have",
                "&7weight when it is on the players head"
        );
    }

    private void handlePetWeight (PetUser user) {
        int weight = 0;
        Player player = user.getPlayer();

        if (user.getHatPets().isEmpty()) {
            player.removePotionEffect(PotionEffectType.SLOW);
            player.getPersistentDataContainer().remove(WEIGHT);
            return;
        }

        for (PetType type : user.getHatPets()) {
            Optional<PetCustomization> optional = type.getCustomization();
            if (!optional.isPresent()) continue;
            PetCustomization customization = optional.get();
            int def = weightMap.getOrDefault(customization.weight(), customization.weight().ordinal());
            if (stacked) {
                // combine the weight of all the pets that are on the player
                weight = (weight + def);
            } else {
                // Fetch the highest weight
                if (def > weight) weight = def;
            }
        }

        if (weight <= 0) { // No pets on the players head, remove effects
            player.removePotionEffect(PotionEffectType.SLOW);
            player.getPersistentDataContainer().remove(WEIGHT);
            return;
        }

        if (weight > maxWeight) weight = maxWeight;

        player.getPersistentDataContainer().set(WEIGHT, PersistentDataType.INTEGER, weight);
        PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, (weight - 1), false, false);
        player.addPotionEffect(effect, true);
    }

    @EventHandler
    public void onJoin (PlayerJoinEvent event) {
        if (event.getPlayer().getPersistentDataContainer().has(WEIGHT, PersistentDataType.INTEGER)) {
            event.getPlayer().removePotionEffect(PotionEffectType.SLOW);
            event.getPlayer().getPersistentDataContainer().remove(WEIGHT);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEffect(EntityPotionEffectEvent event) {
        if ((event.getAction() != EntityPotionEffectEvent.Action.REMOVED) && event.getAction() != EntityPotionEffectEvent.Action.CLEARED)
            return;
        if (!event.getEntity().getPersistentDataContainer().has(WEIGHT, PersistentDataType.INTEGER)) return;

        if (isEnabled() && (event.getCause() == EntityPotionEffectEvent.Cause.MILK))
            event.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true)
    public void onHat(PostPetHatEvent event) {
        if (!isEnabled()) return;
        handlePetWeight(event.getUser());
    }
}
