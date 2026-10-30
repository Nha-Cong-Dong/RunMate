package com.nhacongdong.runmate.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.nhacongdong.runmate.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

// Truy vấn bảng run_session.
@Dao
interface SessionDao {
    @Insert suspend fun insert(session: RunSession): Long
    @Update suspend fun update(session: RunSession)
    @Delete suspend fun delete(session: RunSession)

    // S8: lịch sử, mới nhất trước
    @Query("SELECT * FROM run_session WHERE status = :status ORDER BY startedAt DESC")
    fun observeByStatus(status: SessionStatus): Flow<List<RunSession>>

    @Query("SELECT * FROM run_session WHERE id = :id")
    fun observeById(id: Long): Flow<RunSession?>

    // BR-08 và khôi phục buổi chạy dở (SPEC 10.1)
    @Query("SELECT * FROM run_session WHERE status IN (:statuses) LIMIT 1")
    suspend fun getFirstWithStatus(statuses: List<SessionStatus>): RunSession?

    @Query("SELECT * FROM run_session WHERE planId = :planId")
    suspend fun getByPlanId(planId: Long): RunSession?
}
