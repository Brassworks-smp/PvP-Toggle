package org.opnsoc.dererneuerer.pvptoggle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.util.UUID;

public record PvpMenuActionPayload(Action action, UUID target) implements CustomPacketPayload {
    public static final Type<PvpMenuActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Pvptoggle.MODID, "pvp_menu_action")
    );

    public static final StreamCodec<ByteBuf, PvpMenuActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeByte(payload.action.ordinal());
                buffer.writeBoolean(payload.target != null);
                if (payload.target != null) {
                    buffer.writeLong(payload.target.getMostSignificantBits());
                    buffer.writeLong(payload.target.getLeastSignificantBits());
                }
            },
            buffer -> {
                int actionIndex = Byte.toUnsignedInt(buffer.readByte());
                Action[] actions = Action.values();
                Action action = actionIndex < actions.length ? actions[actionIndex] : Action.REFRESH;
                UUID target = null;
                if (buffer.readBoolean()) {
                    target = new UUID(buffer.readLong(), buffer.readLong());
                }
                return new PvpMenuActionPayload(action, target);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        ENABLE,
        DISABLE,
        BLOCK,
        UNBLOCK,
        REFRESH
    }
}
