package com.team.yeogibeoryeo.data.notice.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreNoticeRemoteDataSourceTest {
    @Test
    fun `공개 공지만 조회하고 문서와 nullable timestamp를 DTO로 변환한다`() = runBlocking {
        val firestore = mockk<FirebaseFirestore>()
        val collection = mockk<CollectionReference>()
        val query = mockk<Query>()
        val querySnapshot = mockk<QuerySnapshot>()
        val datedDocument = mockk<DocumentSnapshot>()
        val undatedDocument = mockk<DocumentSnapshot>()
        val publishedAt = Timestamp(1_754_000_000, 0)
        val updatedAt = Timestamp(1_754_000_100, 0)

        every { firestore.collection("notices") } returns collection
        every { collection.whereEqualTo("isPublished", true) } returns query
        every { query.get() } returns Tasks.forResult(querySnapshot)
        every { querySnapshot.documents } returns listOf(datedDocument, undatedDocument)

        every { datedDocument.id } returns "dated"
        every { datedDocument.getString("title") } returns "서비스 업데이트 안내"
        every { datedDocument.getString("body") } returns "새 기능을 안내합니다."
        every { datedDocument.getTimestamp("publishedAt") } returns publishedAt
        every { datedDocument.getTimestamp("updatedAt") } returns updatedAt

        every { undatedDocument.id } returns "undated"
        every { undatedDocument.getString("title") } returns "점검 안내"
        every { undatedDocument.getString("body") } returns "점검 시간을 안내합니다."
        every { undatedDocument.getTimestamp("publishedAt") } returns null
        every { undatedDocument.getTimestamp("updatedAt") } returns null

        val notices = FirestoreNoticeRemoteDataSource(firestore).fetchPublishedNotices()

        verify(exactly = 1) { firestore.collection("notices") }
        verify(exactly = 1) { collection.whereEqualTo("isPublished", true) }
        verify(exactly = 1) { query.get() }
        assertEquals(listOf("dated", "undated"), notices.map { notice -> notice.id })
        assertEquals("서비스 업데이트 안내", notices[0].title)
        assertEquals("새 기능을 안내합니다.", notices[0].body)
        assertEquals(publishedAt.toDate().time, notices[0].publishedAtMillis)
        assertEquals(updatedAt.toDate().time, notices[0].updatedAtMillis)
        assertNull(notices[1].publishedAtMillis)
        assertNull(notices[1].updatedAtMillis)
    }
}
