package scheduler.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.PersistentState
import scheduler.Scheduleable
import scheduler.internal.util.nbtToSchedule
import scheduler.internal.util.scheduleToNbt
import java.util.*

internal data class Schedule(
    val context: ScheduleContext = ScheduleContext(),
    val repetition: Repetition = Repetition.Once(),
    // If if not null, a packet will be sent to client and the callback will be executed there.
    // Otherwise, It will just execute on the server.
    val clientRequestingSchedule: UUID? = null,
    val cancellationUUID: UUID = UUID.randomUUID()
) {
    @Transient
    lateinit var scheduleable: Scheduleable
}

@Serializable
internal sealed class Repetition {
    abstract var nextTickTime: Long

    @Serializable
    data class RepeatAmount(
        override var nextTickTime: Long = 0,
        val repeatInterval: Int = 1,
        var amountLeft: Int = 1
    ) :
        Repetition()

    @Serializable
    data class RepeatUntil(
        override var nextTickTime: Long = 0,
        val repeatInterval: Int = 1,
        val stopTime: Long = 0
    ) :
        Repetition()

    @Serializable
    data class Once(override var nextTickTime: Long = 0) : Repetition()
}

internal data class ScheduleContext(
    val blockPos: BlockPos = BlockPos.ORIGIN, val scheduleId: Int = 0,
    val blockId: Identifier = Identifier("minecraft:air"),
    val additionalData: NbtCompound = NbtCompound()
)


internal const val SchedulerId = "working_scheduler"

internal fun getScheduleableFromRegistry(scheduleableBlockId: Identifier): Scheduleable? {
    val block = Registry.BLOCK.get(scheduleableBlockId)
    if (block == Registry.BLOCK.defaultId) {
        logWarning("Block with id '$scheduleableBlockId' no longer exists.")
        return null
    }
    if (block !is Scheduleable) {
        logWarning("Block $block (id = $scheduleableBlockId) no longer implements Scheduleable.")
        return null
    }
    return block
}

internal class TickerState : PersistentState() {
    private val tickers =
        PriorityQueue<Schedule> { a, b -> (a.repetition.nextTickTime - b.repetition.nextTickTime).toInt() }

    fun add(ticker: Schedule) {
        tickers.add(ticker)
    }

    val closestToEnd: Schedule get() = tickers.peek()
    val hasAnyTickers: Boolean get() = tickers.isNotEmpty()
    fun removeClosestToEnd(): Schedule? = tickers.poll()
    fun cancel(cancellationUUID: UUID): Boolean = tickers.removeIf { it.cancellationUUID == cancellationUUID }

    override fun writeNbt(rootTag: NbtCompound?): NbtCompound? = rootTag?.also {
        val list = NbtList()
        for (schedule in tickers) {
            list.add(NbtCompound().also { newTag -> scheduleToNbt(newTag, schedule) })
        }
        rootTag.put("savedTickers", list)
    }
}

/**
 * Factory function used in `PersistentState.getOrCreate`. Will create a TickerState and populate values from its saved NbtCompound.
 */
internal fun loadTickerStateFromNbt(rootTag: NbtCompound): TickerState = TickerState().also { state ->
    val savedTickers: NbtList = rootTag.get("savedTickers") as NbtList
    for (tag in savedTickers) {
        if (tag is NbtCompound) {
            val schedule = nbtToSchedule(tag)
            val registryScheduleable = getScheduleableFromRegistry(schedule.context.blockId) ?: continue
            state.add(schedule.apply { scheduleable = registryScheduleable })
        }
    }
}


/**
 * String from `BlockPos.toShortString()` -> BlockPos
 */
internal fun stringToBlockPos(string: String): BlockPos {
    if (string.isEmpty())
        return BlockPos.ORIGIN
    val split = string.replace(" ", "").split(",")
    return BlockPos(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]))
}

/**
 * Returns a UUID from tag or returns null
 */
internal fun getNullableUUID(tag: NbtCompound, key: String): UUID? {
    if (tag.contains(key))
        return tag.getUuid(key)
    return null
}



