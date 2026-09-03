package com.team.yeogibeoryeo.domain.notice.usecase

import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import javax.inject.Inject

class MarkNoticeReadUseCase
@Inject
constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke(noticeId: String) {
        repository.markNoticeRead(noticeId)
    }
}
