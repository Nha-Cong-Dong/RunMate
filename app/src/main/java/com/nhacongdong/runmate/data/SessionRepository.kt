package com.nhacongdong.runmate.data

import androidx.room3.withWriteTransaction
import com.nhacongdong.runmate.data.local.PlanDao
import com.nhacongdong.runmate.data.local.RunMateDatabase
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.local.SessionDao
import com.nhacongdong.runmate.data.local.TrackPoint
import com.nhacongdong.runmate.data.local.TrackPointDao
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow

// Cửa ngõ đọc và ghi buổi chạy, điểm GPS. Giữ kế hoạch và buổi chạy khớp nhau khi xoá.
class SessionRepository(
    private val database: RunMateDatabase,
    private val sessionDao: SessionDao,
    private val trackPointDao: TrackPointDao,
    private val planDao: PlanDao
) {
    // S8: lịch sử, mới nhất trước (BR-50)
    fun observeFinishedSessions(): Flow<List<RunSession>> =
        sessionDao.observeByStatus(SessionStatus.FINISHED)

    fun observeSession(id: Long): Flow<RunSession?> = sessionDao.observeById(id)

    // Buổi đang RECORDING hoặc PAUSED (BR-08, SPEC 10.1)
    suspend fun getActiveSession(): RunSession? =
        sessionDao.getFirstWithStatus(listOf(SessionStatus.RECORDING, SessionStatus.PAUSED))

    suspend fun getSessionForPlan(planId: Long): RunSession? = sessionDao.getByPlanId(planId)
    suspend fun addSession(session: RunSession): Long = sessionDao.insert(session)
    suspend fun updateSession(session: RunSession) = sessionDao.update(session)
    suspend fun addTrackPoint(point: TrackPoint) = trackPointDao.insert(point)
    suspend fun getTrackPoints(sessionId: Long): List<TrackPoint> = trackPointDao.getBySession(sessionId)

    // Xoá buổi chạy (điểm GPS xoá theo CASCADE). Nếu thuộc kế hoạch thì đổi trạng thái
    // kế hoạch trong cùng transaction (SPEC 7.7, BR-53, BR-36). planStatusAfter do domain tính.
    suspend fun deleteSession(session: RunSession, planStatusAfter: PlanStatus? = null) {
        val planId = session.planId
        database.withWriteTransaction {
            sessionDao.delete(session)
            if (planId != null) {
                val status = requireNotNull(planStatusAfter) {
                    "Buổi chạy thuộc kế hoạch thì phải truyền trạng thái mới cho kế hoạch"
                }
                planDao.updateStatus(planId, status)
            }
        }
    }
}
