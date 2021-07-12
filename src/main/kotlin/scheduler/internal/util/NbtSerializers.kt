package scheduler.internal.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import scheduler.internal.Schedule
import scheduler.internal.ScheduleContext
import scheduler.internal.getNullableUUID
import scheduler.internal.stringToBlockPos

/**
 * Creates and returns a new `ScheduleContext` instance from tag
 */
internal fun nbtToScheduleContext(tag: NbtCompound): ScheduleContext =
    ScheduleContext(
        blockPos = stringToBlockPos(tag.getString("blockPos")),
        scheduleId = tag.getInt("id"),
        blockId = Identifier(tag.getString("blockId")),
        additionalData = tag.getCompound("additionalData")
    )


/**
 * Writes `ScheduleContext` to tag and returns tag
 */
internal fun scheduleContextToNbt(tag: NbtCompound, context: ScheduleContext): NbtCompound = tag.also {
    it.putInt("id", context.scheduleId)
    it.putString("blockPos", context.blockPos.toShortString())
    it.putString("blockId", context.blockId.toString())
    it.put("additionalData", context.additionalData)
}

/**
 * Creates and returns a new `Schedule` instance from tag
 */
internal fun nbtToSchedule(tag: NbtCompound): Schedule =
    Schedule(
        context = nbtToScheduleContext(tag),
        repetition = Json.decodeFromString(tag.getString("repetition")),
        clientRequestingSchedule = getNullableUUID(tag, "clientRequestingSchedule"),
        cancellationUUID = tag.getUuid("cancellationUUID")
    )

/**
 * Writes `Schedule` to tag and returns tag
 */
internal fun scheduleToNbt(tag: NbtCompound, schedule: Schedule): NbtCompound = tag.also {
    scheduleContextToNbt(tag, schedule.context)
    it.putString("repetition", Json.encodeToJsonElement(schedule.repetition).toString())
    if (schedule.clientRequestingSchedule != null)
        it.putUuid("clientRequestingSchedule", schedule.clientRequestingSchedule)
    it.putUuid("cancellationUUID", schedule.cancellationUUID)
}