package com.team.yeogibeoryeo.data.notice.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.team.yeogibeoryeo.data.notice.remote.dto.NoticeDto
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

interface NoticeRemoteDataSource {
    suspend fun fetchPublishedNotices(): List<NoticeDto>
}

class FirestoreNoticeRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NoticeRemoteDataSource {
    override suspend fun fetchPublishedNotices(): List<NoticeDto> {
        return firestore.collection(NOTICES_COLLECTION)
            .whereEqualTo(IS_PUBLISHED_FIELD, true)
            .get()
            .await()
            .documents
            .map { document -> document.toNoticeDto() }
    }

    private fun DocumentSnapshot.toNoticeDto(): NoticeDto {
        return NoticeDto(
            id = id,
            title = getString(TITLE_FIELD),
            body = getString(BODY_FIELD),
            publishedAtMillis = getTimestamp(PUBLISHED_AT_FIELD)?.toDate()?.time,
            updatedAtMillis = getTimestamp(UPDATED_AT_FIELD)?.toDate()?.time,
        )
    }

    private companion object {
        const val NOTICES_COLLECTION = "notices"
        const val TITLE_FIELD = "title"
        const val BODY_FIELD = "body"
        const val PUBLISHED_AT_FIELD = "publishedAt"
        const val UPDATED_AT_FIELD = "updatedAt"
        const val IS_PUBLISHED_FIELD = "isPublished"
    }
}
