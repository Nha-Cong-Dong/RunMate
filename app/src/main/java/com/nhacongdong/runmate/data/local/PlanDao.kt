package com.nhacongdong.runmate.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

// Truy vấn bảng run_plan. Room 3 bắt buộc hàm DAO là suspend hoặc trả về Flow.
@Dao
interface PlanDao {
    @Insert suspend fun insert(plan: RunPlan): Long
    @Update suspend fun update(plan: RunPlan)
    @Delete suspend fun delete(plan: RunPlan)

    @Query("SELECT * FROM run_plan ORDER BY startTime")
    fun observeAll(): Flow<List<RunPlan>>

    @Query("SELECT * FROM run_plan WHERE id = :id")
    fun observeById(id: Long): Flow<RunPlan?>

    @Query("SELECT * FROM run_plan WHERE id = :id")
    suspend fun getById(id: Long): RunPlan?
}
