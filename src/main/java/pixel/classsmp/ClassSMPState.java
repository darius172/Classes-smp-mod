package pixel.classsmp;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class ClassSMPState extends PersistentState {

    private final HashMap<UUID, String> playerClasses = new HashMap<>();
    private final HashMap<UUID, Integer> playerEffectValues = new HashMap<>();  // Store effect as integer
    private final HashMap<UUID, Integer> playerEffectLeveles = new HashMap<>();

    private static final Type<ClassSMPState> TYPE = new Type<>(
            ClassSMPState::new,
            ClassSMPState::createFromNbt,
            null
    );

    public static ClassSMPState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(TYPE, ClassSMPPlugin.MOD_ID);
    }

    public void setPlayerClass(UUID playerUUID, String playerClass) {
        playerClasses.put(playerUUID, playerClass);
        markDirty();
    }

    public String getPlayerClass(UUID playerUUID) {
        return playerClasses.getOrDefault(playerUUID, "none");
    }

    public void setEffectValue(UUID playerUUID, int value) {
        playerEffectValues.put(playerUUID, value);
        markDirty();
    }

    public int getEffectValue(UUID playerUUID) {
        return playerEffectValues.getOrDefault(playerUUID, 1);
    }
    public void setPlayerEffectLeveles(UUID playerUUID, Integer level) {
        playerEffectLeveles.put(playerUUID, level);
        markDirty();
    }

    public int getPlayerEffectLeveles(UUID playerUUID) {
        return playerEffectLeveles.getOrDefault(playerUUID, Integer.valueOf("0"));
    }

    public static ClassSMPState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ClassSMPState state = new ClassSMPState();

        NbtCompound classesTag = nbt.getCompound("playerClasses");
        for (String key : classesTag.getKeys()) {
            state.playerClasses.put(UUID.fromString(key), classesTag.getString(key));
        }

        NbtCompound effectValuesTag = nbt.getCompound("playerEffectValues");
        for (String key : effectValuesTag.getKeys()) {
            state.playerEffectValues.put(UUID.fromString(key), effectValuesTag.getInt(key));
        }
        NbtCompound effectLevelsTag = nbt.getCompound("playerEffectLevels");
        for (String key : effectLevelsTag.getKeys()) {
            state.playerEffectLeveles.put(UUID.fromString(key), effectValuesTag.getInt(key));
        }


        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound classesTag = new NbtCompound();
        for (UUID uuid : playerClasses.keySet()) {
            classesTag.putString(uuid.toString(), playerClasses.get(uuid));
        }
        nbt.put("playerClasses", classesTag);

        NbtCompound effectValuesTag = new NbtCompound();
        for (UUID uuid : playerEffectValues.keySet()) {
            effectValuesTag.putInt(uuid.toString(), playerEffectValues.get(uuid));
        }
        nbt.put("playerEffectLevels", effectValuesTag);
        NbtCompound effectLevelesTag = new NbtCompound();
        for (UUID uuid : playerEffectValues.keySet()) {
            effectValuesTag.putInt(uuid.toString(), playerEffectValues.get(uuid));
        }
        nbt.put("playerEffectLeveles", effectLevelesTag);

        return nbt;
    }
}
