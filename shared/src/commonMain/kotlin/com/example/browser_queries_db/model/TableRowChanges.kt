package com.example.browser_queries_db.model

/**
 * Изменённая строка таблицы.
 *
 * @property id идентификатор строки в БД.
 * @property values изменённые колонки: имя -> новое значение.
 */
data class UpdatedRow(
    val id: Long,
    val values: Map<String, String?>
)

/**
 * Набор изменений строк таблицы, подготовленный редактором.
 *
 * @property addedRows новые строки: значения колонок без id.
 * @property updatedRows изменённые строки.
 * @property deletedRowIds идентификаторы удалённых строк.
 */
data class TableRowChanges(
    val addedRows: List<Map<String, String?>> = emptyList(),
    val updatedRows: List<UpdatedRow> = emptyList(),
    val deletedRowIds: List<Long> = emptyList()
) {
    /**
     * Признак отсутствия изменений.
     */
    val isEmpty: Boolean
        get() = addedRows.isEmpty() &&
                updatedRows.isEmpty() &&
                deletedRowIds.isEmpty()
}
