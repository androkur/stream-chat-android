package io.getstream.chat.android.client.api2.mapping

import io.getstream.chat.android.client.api2.model.dto.ReactionDto
import io.getstream.chat.android.client.models.Reaction

internal fun Reaction.toDto(): ReactionDto =
    ReactionDto(
        created_at = createdAt,
        message_id = messageId,
        score = score,
        type = type,
        updated_at = updatedAt,
        user = user?.toDto(),
        user_id = userId,
        extraData = extraData,
    )

internal fun ReactionDto.toDomain(): Reaction =
    Reaction(
        createdAt = created_at,
        messageId = message_id,
        score = score,
        type = type,
        updatedAt = updated_at,
        user = user?.toDomain(),
        userId = user_id,
        extraData = extraData.toMutableMap(),
    )
