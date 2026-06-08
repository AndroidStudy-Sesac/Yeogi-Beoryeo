package com.team.yeogibeoryeo.presentation.search

import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide

sealed interface ItemSearchEvent {
    data class NavigateToGuide(
        val guide: DisposalItemGuide,
    ) : ItemSearchEvent
}
