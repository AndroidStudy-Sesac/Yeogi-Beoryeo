package com.team.yeogibeoryeo.domain.notice.usecase

import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import javax.inject.Inject

class GetPublishedNoticesUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(): List<Notice> {
        return noticeRepository.getPublishedNotices()
    }
}
