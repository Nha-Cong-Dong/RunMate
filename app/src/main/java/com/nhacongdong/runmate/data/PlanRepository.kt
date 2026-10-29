package com.nhacongdong.runmate.data

import com.nhacongdong.runmate.data.local.PlanDao
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.model.PlanStatus
import kotlinx.coroutines.flow.Flow

// Cửa ngõ đọc và ghi kế hoạch. Không kiểm tra quy tắc BR-xx, việc đó thuộc domain/.
class PlanRepository(private val planDao: PlanDao) {
    fun observePlans(): Flow<List<RunPlan>> = planDao.observeAll()
    fun observePlan(id: Long): Flow<RunPlan?> = planDao.observeById(id)
    suspend fun getPlan(id: Long): RunPlan? = planDao.getById(id)
    suspend fun addPlan(plan: RunPlan): Long = planDao.insert(plan)
    suspend fun updatePlan(plan: RunPlan) = planDao.update(plan)
    suspend fun updateStatus(id: Long, status: PlanStatus) = planDao.updateStatus(id, status)

    // Buổi chạy của kế hoạch (nếu có) được giữ lại thành chạy tự do nhờ SET_NULL (BR-52)
    suspend fun deletePlan(plan: RunPlan) = planDao.delete(plan)
}
