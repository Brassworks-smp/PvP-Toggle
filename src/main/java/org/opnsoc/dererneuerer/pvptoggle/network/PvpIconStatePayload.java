package org.opnsoc.dererneuerer.pvptoggle.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.util.UUID;

public record PvpIconStatePayload(UUID target, boolean canAttack)
        implements CustomPacketPayload {

    public static final Type<PvpIconStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Pvptoggle.MODID,
                    "pvp_icon_state"
            ));

    public static final StreamCodec<ByteBuf, PvpIconStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(payload.target().getMostSignificantBits());
                        buf.writeLong(payload.target().getLeastSignificantBits());
                        buf.writeBoolean(payload.canAttack());
                    },
                    buf -> new PvpIconStatePayload(
                            new UUID(buf.readLong(), buf.readLong()),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}