package scheduler.internal.util

import io.netty.buffer.Unpooled
import kotlinx.serialization.KSerializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.world.World
import scheduler.internal.C2SPacket
import scheduler.internal.S2CPacket
import java.util.*
import java.util.function.Function


/*******
 * Fabric Api Wrappers
 ********/

private fun  PlayerEntity.sendPacket(packetId: Identifier, packetBuilder: PacketByteBuf.() -> Unit) {
    val packet = PacketByteBuf(Unpooled.buffer()).apply(packetBuilder)
    ServerPlayNetworking.send(this as ServerPlayerEntity?, packetId, packet)
}

private fun sendPacketToServer(packetId: Identifier, packetBuilder: PacketByteBuf.() -> Unit) {
    val packet = PacketByteBuf(Unpooled.buffer()).apply(packetBuilder)
    ClientPlayNetworking.send(packetId, packet)

}

/******************************
 * Automatic Serializer Wrappers
 ******************************/


internal fun <T : C2SPacket<T>> CommonModInitializationContext.registerC2S(id: Identifier, factory: (PacketByteBuf) -> T) {
    ServerPlayNetworking.registerGlobalReceiver(
        id
    ) { server, player, handler, buf, responseSender ->
        factory.invoke(buf).use(player.world)
    }
}

internal fun <T : S2CPacket<T>> ClientModInitializationContext.registerS2C(
    id: Identifier,  factory: (PacketByteBuf) -> T
) {
    ClientPlayNetworking.registerGlobalReceiver(
        id
    ) { client, handler, buf, responseSender ->
        factory.invoke(buf).use(client.world as World)
    }
}

/**
 * Sends a packet from the server to the client for all the players in the stream.
 */
internal fun <T : InternalS2CPacket<T>> PlayerEntity.sendPacket(packet: T) {
    sendPacket(packet.getPacketId()) {
        packet.write(this)
    }
}

/**
 * Sends a packet from the server to the client for all the players in the stream.
 */
internal fun <T : InternalC2SPacket<T>> sendPacketToServer(packet: T) {
    sendPacketToServer(packet.getPacketId()) {
        packet.write(this)
    }
}

internal val PacketContext.world: World get() = player.world


private val <T : Packet<out T>> KSerializer<out T>.packetId get() = javaClass.simpleName.lowercase(Locale.getDefault())//descriptor.serialName.lowercase(Locale.getDefault())


internal interface Packet<T : Packet<T>> {
    val modId: String
    fun use(world: World)
    fun getPacketId() : Identifier
    fun write(buf: PacketByteBuf)
}

internal interface InternalC2SPacket<T : Packet<T>> : Packet<T>
internal interface InternalS2CPacket<T : Packet<T>> : Packet<T>

