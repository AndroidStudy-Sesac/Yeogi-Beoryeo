package com.team.yeogibeoryeo.data.item.remote.datasource

class ItemApiException(
    val code: String,
    override val message: String,
) : IllegalStateException("Item API error $code: $message")
