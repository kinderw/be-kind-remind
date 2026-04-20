package com.bekindremind.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun taskModeToString(value: TaskMode): String = value.name

    @TypeConverter
    fun stringToTaskMode(value: String): TaskMode = TaskMode.valueOf(value)

    @TypeConverter
    fun trafficModelToString(value: TrafficModel): String = value.name

    @TypeConverter
    fun stringToTrafficModel(value: String): TrafficModel = TrafficModel.valueOf(value)

    @TypeConverter
    fun taskStatusToString(value: TaskStatus): String = value.name

    @TypeConverter
    fun stringToTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun alarmTypeToString(value: AlarmType): String = value.name

    @TypeConverter
    fun stringToAlarmType(value: String): AlarmType = AlarmType.valueOf(value)
}
