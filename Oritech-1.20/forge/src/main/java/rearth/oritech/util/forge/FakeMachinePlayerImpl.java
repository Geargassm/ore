package rearth.oritech.util.forge;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.util.FakePlayerMarker;

import java.util.Map;

/**
 * Forge 1.20.1 implementation of the fake machine player.
 *
 * <p>The only difference from the Fabric implementation is that this extends
 * {@link FakePlayer} from {@code net.minecraftforge.common.util} instead of
 * the Fabric equivalent.
 *
 * <p>Inspired by Fabric's FakePlayer and NeoForge's FakePlayerFactory.
 */
public class FakeMachinePlayerImpl extends FakePlayer implements FakePlayerMarker {

    private static final Map<FakeMachinePlayerKey, FakePlayer> FAKE_MACHINE_PLAYERS =
        new MapMaker().weakValues().makeMap();

    private record FakeMachinePlayerKey(ServerLevel world, GameProfile profile) {}

    private final SimpleInventoryStorage inventory;

    private FakeMachinePlayerImpl(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        super(world, profile);
        this.inventory = inventory;
    }

    public static ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        FakeMachinePlayerKey key = new FakeMachinePlayerKey(world, profile);
        return FAKE_MACHINE_PLAYERS.computeIfAbsent(key, k -> new FakeMachinePlayerImpl(k.world(), k.profile(), inventory));
    }

    @Override
    public boolean addItem(ItemStack itemStack) {
        this.inventory.insert(itemStack, false);
        return true;
    }
}
