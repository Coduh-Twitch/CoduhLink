package lol.duckyyy;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@Config(name = CoduhLink.MOD_ID)
public class ConfigModel implements ConfigData {
    public String api_key = "API KEY HERE";
    public String api_url = "https://...";
    public Map<String, String> summon_rewards = new HashMap<String, String>();
    public Map<String, String> action_rewards = new HashMap<String, String>();

    public static class GiveItemReward {
        public String item;
        public int amount;
    }

    public Map<String, GiveItemReward> give_item_rewards = new HashMap<String, GiveItemReward>();

    public static class PotionEffectReward {
        public String effect;
        public int strength;
        public int seconds;

        public PotionEffectReward setEffect(String effect) {
            this.effect = effect;
            return this;
        }

        public PotionEffectReward setStrength(int strength) {
            this.strength = strength;
            return this;
        }

        public PotionEffectReward setSeconds(int seconds) {
            this.seconds = seconds;
            return this;
        }
    }

    public Map<String, PotionEffectReward> potion_effect_rewards = new HashMap<String, PotionEffectReward>();
}
