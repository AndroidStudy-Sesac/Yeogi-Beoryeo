package com.team.yeogibeoryeo.domain.notice.usecase

import com.team.yeogibeoryeo.domain.notice.repository.NoticeRepository
import javax.inject.Inject

class GetReadNoticeIdsUseCase
@Inject
constructor(
    private val repository: NoticeRepository,
) {
    suspend operator fun invoke(): Set<String> = repository.getReadNoticeIds()
}
