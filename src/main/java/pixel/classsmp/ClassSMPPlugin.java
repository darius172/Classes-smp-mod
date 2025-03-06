package pixel.classsmp;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.OnAStickItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.arguments.StringArgumentType;
import pixel.classsmp.ClassSMPState;

import java.util.HashMap;
import java.util.UUID;

public class ClassSMPPlugin implements ModInitializer {
	public static final String MOD_ID = "classsmp";
	public static final Logger LOGGER = LoggerFactory.getLogger("classsmp");
	private static final HashMap<UUID, Long> supportCooldowns = new HashMap<>();

	@Override
	public void onInitialize() {
		LOGGER.info("ClassSMP Plugin initialized.");

		// Command to manually set a player's class
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("setclass")
					.requires(source -> source.hasPermissionLevel(2)) // Admin-only
					.then(CommandManager.argument("class", StringArgumentType.string())
							.executes(context -> {
								ServerPlayerEntity player = context.getSource().getPlayer();
								if (player == null) return 0;

								ServerWorld world = player.getServerWorld();
								ClassSMPState state = ClassSMPState.getServerState(world.getServer());

								String chosenClass = StringArgumentType.getString(context, "class");
								if (isValidClass(chosenClass)) {
									state.setPlayerClass(player.getUuid(), chosenClass);
									player.sendMessage(Text.of("Your class has been set to " + chosenClass + "!"), false);
								} else {
									player.sendMessage(Text.of("Invalid class! Choose miner, fighter, or support."), false);
								}
								return 1;
							})));
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("setlevel")
					.requires(source -> source.hasPermissionLevel(2)) // Admin-only
					.then(CommandManager.argument("level", IntegerArgumentType.integer(0, 2)) // Level must be between -1 and 2
							.executes(context -> {
								ServerPlayerEntity player = context.getSource().getPlayer();
								if (player == null) return 0;

								ServerWorld world = player.getServerWorld();
								ClassSMPState state = ClassSMPState.getServerState(world.getServer());

								int chosenLevel = IntegerArgumentType.getInteger(context, "level");

								// Store the level in the state (assuming a method like setPlayerLevel exists)
								state.setPlayerEffectLeveles(player.getUuid(),chosenLevel);

								player.sendMessage(Text.of("Your level has been set to " + chosenLevel + "!"), false);
								return 1;
							})
					)
			);
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("rerollclass")
					.requires(source -> source.hasPermissionLevel(2)) // Admin-only
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;

						ServerWorld world = player.getServerWorld();
						ClassSMPState state = ClassSMPState.getServerState(world.getServer());

						// Check if the player has the required items
						if (player.getInventory().count(Items.GOLD_BLOCK) >= 1 &&
								player.getInventory().count(Items.NETHERITE_BLOCK) >= 1 &&
								player.getInventory().count(Items.DIAMOND) >= 7) {

							// Remove the items from the player's inventory
							player.getInventory().removeStack(player.getInventory().getSlotWithStack(new ItemStack(Items.GOLD_BLOCK)), 1);
							player.getInventory().removeStack(player.getInventory().getSlotWithStack(new ItemStack(Items.NETHERITE_BLOCK)), 1);
							player.getInventory().removeStack(player.getInventory().getSlotWithStack(new ItemStack(Items.DIAMOND)), 7);

							// Get the player's current class
							String currentClass = state.getPlayerClass(player.getUuid());

							// Get a new random class (different from the current one)
							String newClass;
							do {
								newClass = assignRandomClass();
							} while (newClass.equals(currentClass));

							// Set the new class
							state.setPlayerClass(player.getUuid(), newClass);
							player.sendMessage(Text.of("You have rerolled! Your new class is " + newClass + "!"), false);

						} else {
							player.sendMessage(Text.of("You need 1 Gold Block, 1 Netherite Block, and 7 Diamonds to reroll!"), false);
						}
						return 1;
					}));
		});


		// Assign or load class when player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			ClassSMPState state = ClassSMPState.getServerState(server);
			String playerClass = state.getPlayerClass(player.getUuid());

			// If no class is assigned, give them a random one
			if (playerClass.equals("none")) {
				playerClass = assignRandomClass();
				state.setPlayerClass(player.getUuid(), playerClass);
				state.setPlayerEffectLeveles(player.getUuid(), 1);
			}

			player.sendMessage(Text.of("Welcome to the server ! You are in the " + playerClass + " class !"), false);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {


			if (entity instanceof ServerPlayerEntity killedPlayer) {
				ClassSMPState state = ClassSMPState.getServerState(killedPlayer.getServer());
				int level = state.getPlayerEffectLeveles(killedPlayer.getUuid());
				if(level > 0 ){
					state.setPlayerEffectLeveles(entity.getUuid(),state.getPlayerEffectLeveles(entity.getUuid())-1);
				}

			}
			if (entity instanceof ServerPlayerEntity killedPlayer) {
				ClassSMPState state = ClassSMPState.getServerState(killedPlayer.getServer());

				// Reduce level of killed player if above 0
				int killedPlayerLevel = state.getPlayerEffectLeveles(killedPlayer.getUuid());
				if (killedPlayerLevel > 0) {
					state.setPlayerEffectLeveles(killedPlayer.getUuid(), killedPlayerLevel - 1);
				}

				// Check if the killer is a player
				if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
					int killerLevel = state.getPlayerEffectLeveles(killer.getUuid());
					state.setPlayerEffectLeveles(killer.getUuid(), killerLevel + 1); // Increase killer's level
					killer.sendMessage(Text.of("You gained +1 level! New level: " + (killerLevel + 1)), false);
				}
			}
		});





		// Support class: Heals every 3 seconds
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			for (ServerPlayerEntity player : world.getPlayers()) {
				ServerWorld serverWorld = (ServerWorld) player.getWorld();
				ClassSMPState state = ClassSMPState.getServerState(serverWorld.getServer());

				if ("support".equals(state.getPlayerClass(player.getUuid()))) {
					long currentTime = world.getTime();
					long lastUsed = supportCooldowns.getOrDefault(player.getUuid(), 0L);
					int playerEffectLevel = state.getPlayerEffectLeveles(player.getUuid());
					if (currentTime - lastUsed > 60) { // 3 seconds in ticks
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, playerEffectLevel));
						supportCooldowns.put(player.getUuid(), currentTime);
					}
				}
			}
		});
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			for (ServerPlayerEntity player : world.getPlayers()) {
				ServerWorld serverWorld = (ServerWorld) player.getWorld();
				ClassSMPState state = ClassSMPState.getServerState(serverWorld.getServer());

				if ("fighter".equals(state.getPlayerClass(player.getUuid()))) {


					int playerEffectLevel = state.getPlayerEffectLeveles(player.getUuid());
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 64, playerEffectLevel));

				}
				if ("miner".equals(state.getPlayerClass(player.getUuid()))) {


					int playerEffectLevel = state.getPlayerEffectLeveles(player.getUuid());
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 64, playerEffectLevel));

				}

			}
		});




	}

	private String assignRandomClass() {
		String[] classes = {"miner", "fighter", "support"};
		return classes[(int) (Math.random() * classes.length)];
	}

	private boolean isValidClass(String className) {
		return className.equals("miner") || className.equals("fighter") || className.equals("support");
	}
}