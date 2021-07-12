package scheduler.internal

import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.world.World
import scheduler.internal.util.*
import scheduler.internal.util.InternalC2SPacket
import scheduler.internal.util.InternalS2CPacket
import scheduler.internal.util.nbtToSchedule
import scheduler.internal.util.scheduleToNbt
import java.util.*

internal interface C2SPacket<T : C2SPacket<T>> : InternalC2SPacket<T> {
    override val modId get() = ModId
}

internal interface S2CPacket<T : S2CPacket<T>> : InternalS2CPacket<T> {
    override val modId get() = ModId
}

internal val PACKET_ID_TICK_IN_SERVER: Identifier = Identifier(ModId, TickInServerPacket::class.java.simpleName.lowercase())
internal val PACKET_ID_FINISH_SCHEDULE_IN_CLIENT : Identifier = Identifier(ModId, FinishScheduleInClientPacket::class.java.simpleName.lowercase())
internal val PACKET_ID_CANCEL_TICKING_IN_SERVER: Identifier = Identifier(ModId, CancelTickingInServerPacket::class.java.simpleName.lowercase())

internal data class TickInServerPacket(val schedule: Schedule) : C2SPacket<TickInServerPacket> {
    companion object {
        fun factory(buf: PacketByteBuf) : TickInServerPacket = TickInServerPacket( nbtToSchedule(buf.readNbt() ?: NbtCompound()))
    }

    override fun write(buf: PacketByteBuf) { buf.writeNbt(scheduleToNbt(NbtCompound(), schedule))}
    override fun getPacketId(): Identifier = PACKET_ID_TICK_IN_SERVER
    override fun use(world: World) {
        if (world !is ServerWorld || world.isClient) {
            logWarning("A packet to the server is somehow not in a server world.")
            return
        }
        val scheduleable = getScheduleableFromRegistry(schedule.context.blockId) ?: return
        scheduleServer(world, schedule, scheduleable)

    }

}


internal data class FinishScheduleInClientPacket(var scheduleContext: ScheduleContext) :
    S2CPacket<FinishScheduleInClientPacket> {
    companion object {
        fun factory(buf: PacketByteBuf) : FinishScheduleInClientPacket = FinishScheduleInClientPacket(buf.readNbt()?.let { nbtToScheduleContext(it) }!!)
    }

    override fun write(buf: PacketByteBuf) { buf.writeNbt(scheduleContextToNbt(NbtCompound(), scheduleContext)) }
    override fun getPacketId() : Identifier = PACKET_ID_FINISH_SCHEDULE_IN_CLIENT
    override fun use(world: World) {
        val scheduleable = getScheduleableFromRegistry(scheduleContext.blockId) ?: return
        scheduleable.onScheduleEnd(
            world,
            scheduleContext.blockPos,
            scheduleContext.scheduleId,
            scheduleContext.additionalData
        )
    }
}

internal data class CancelTickingInServerPacket(val cancellationUUID: UUID) : C2SPacket<CancelTickingInServerPacket> {
    companion object {
        fun factory(buf: PacketByteBuf) : CancelTickingInServerPacket = CancelTickingInServerPacket(buf.readUuid())
    }

    override fun write(buf: PacketByteBuf) { buf.writeUuid(cancellationUUID) }
    override fun getPacketId(): Identifier = PACKET_ID_CANCEL_TICKING_IN_SERVER
    override fun use(world: World) {
        if (world !is ServerWorld || world.isClient) {
            logWarning("A packet to the server is somehow not in a server world.")
            return
        }
        cancelScheduleServer(world, cancellationUUID)
    }
}


