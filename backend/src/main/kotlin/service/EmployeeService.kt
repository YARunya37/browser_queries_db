package com.example.service

import com.example.repository.EmployeeRepository

class EmployeeService(
    private val employeeRepository: EmployeeRepository
) {

    private val allowedFields = mapOf(
        "id" to "id",
        "fio" to "fio",
        "phone" to "phone",
        "salary" to "salary",
        "address" to "address",
        "experience" to "experience"
    )

    // Метод для получения работников и информации о них. Возвращает список с полями в формате имя - значение
    fun getEmployees(
        fields: String?,
        sort: String?,
        direction: String?,
        experienceMin: String?
    ): List<Map<String, Any?>> {

        // Формирование выбранных полей для запроса
        // Если указаны определенные поля, то указываем только их. Иначе - все поля
        val sqlFields = validateFields(fields)

        // Сортировка по определённому полю
        val sqlSort = validateSort(sort)

        // Направление сортировки
        val sqlDirection = validateDirection(direction, sqlSort)

        // Фильтрация по минимальному опыту
        val experience = validateExperience(experienceMin)

        // Собираем SQL-запрос
        val sqlRequest = buildSqlRequest(
            sqlFields = sqlFields,
            sqlSort = sqlSort,
            sqlDirection = sqlDirection,
            experience = experience
        )

        return employeeRepository.findEmployees(
            sqlRequest = sqlRequest,
            experience = experience
        )
    }

    private fun validateFields(fields: String?): List<String> {
        if (fields == null) {
            return listOf("*")
        }

        val requestedFields = fields.split(",")

        val result = mutableListOf<String>()

        for (field in requestedFields) {

            val fieldName = field.trim()

            val sqlField = allowedFields[fieldName]
                ?: throw IllegalArgumentException(
                    "Unknown field: $fieldName"
                )

            result.add(sqlField)
        }

        if (result.isEmpty()) {
            throw IllegalArgumentException(
                "Fields cannot be empty"
            )
        }

        return result
    }

    private fun validateSort(sort: String?): String? {
        return sort?.let {
            allowedFields[it]
                ?: throw IllegalArgumentException(
                    "Unknown sort field: $it"
                )
        }
    }

    private fun validateDirection(
        direction: String?,
        sqlSort: String?
    ): String? {

        // Направление сортировки
        var sqlDirection: String? = null

        if (direction != null) {
            sqlDirection = when (direction.lowercase()) {
                "asc" -> "ASC"
                "desc" -> "DESC"

                else -> throw IllegalArgumentException(
                    "Direction must be ASC or DESC"
                )
            }
        }

        // direction имеет смысл только вместе с sort
        if (sqlDirection != null && sqlSort == null) {
            throw IllegalArgumentException(
                "Direction requires sort"
            )
        }

        // Значение по умолчанию, если направление не указано
        if (sqlSort != null && sqlDirection == null) {
            sqlDirection = "ASC"
        }

        return sqlDirection
    }

    private fun validateExperience(experienceMin: String?): Int? {
        // Фильтрация по минимальному опыту
        var experience: Int? = null

        if (experienceMin != null) {

            experience = experienceMin.toIntOrNull()
                ?: throw IllegalArgumentException(
                    "experienceMin must be an integer"
                )

            if (experience < 0) {
                throw IllegalArgumentException(
                    "experienceMin must be >= 0"
                )
            }
        }

        return experience
    }

    private fun buildSqlRequest(
        sqlFields: List<String>,
        sqlSort: String?,
        sqlDirection: String?,
        experience: Int?
    ): String {
        // Собираем SQL-запрос
        return buildString {
            append("SELECT ")
            append(sqlFields.joinToString(", "))
            append(" FROM table_kliuchikov")

            if (experience != null) {
                // Значение опыта передаём через параметр SQL-запроса
                append(" WHERE experience > ?")
            }

            if (sqlSort != null) {
                append(" ORDER BY ")
                append(sqlSort)

                append(" ")
                append(sqlDirection)
            }
        }
    }
}
