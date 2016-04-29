package org.jetbrains.exposed.sql

import org.jetbrains.exposed.sql.transactions.TransactionManager
import kotlin.comparisons.compareBy

open class Column<T>(val table: Table, val name: String, override val columnType: ColumnType) : ExpressionWithColumnType<T>(), DdlAware, Comparable<Column<*>> {
    var referee: Column<*>? = null
    internal var indexInPK: Int? = null
    internal var onDelete: ReferenceOption? = null
    internal var defaultValueFun: (() -> T)? = null
    internal var dbDefaultValue: T? = null

    override fun equals(other: Any?): Boolean {
        return (other as? Column<*>)?.let {
            it.table == table && it.name == name && it.columnType == columnType
        } ?: false
    }

    override fun hashCode(): Int {
        return table.hashCode()*31 + name.hashCode()
    }

    override fun toString(): String {
        return "${table.javaClass.name}.$name"
    }

    override fun toSQL(queryBuilder: QueryBuilder): String {
        return TransactionManager.current().fullIdentity(this);
    }

    val ddl: String
        get() = createStatement()

    override fun createStatement(): String = "ALTER TABLE ${TransactionManager.current().identity(table)} ADD COLUMN ${descriptionDdl()}"

    override fun modifyStatement(): String = "ALTER TABLE ${TransactionManager.current().identity(table)} MODIFY COLUMN ${descriptionDdl()}"

    override fun dropStatement(): String = TransactionManager.current().let {"ALTER TABLE ${it.identity(table)} DROP COLUMN ${it.identity(this)}" }

    fun descriptionDdl(): String {
        val ddl = StringBuilder(TransactionManager.current().identity(this)).append(" ")
        val colType = columnType
        ddl.append(colType.sqlType())

        if (colType.nullable) {
            ddl.append(" NULL")
        } else {
            ddl.append(" NOT NULL")
        }

        if (dbDefaultValue != null) {
            ddl.append (" DEFAULT ${colType.valueToString(dbDefaultValue!!)}")
        }

        return ddl.toString()
    }

    override fun compareTo(other: Column<*>): Int = compareBy<Column<*>>({it.table.tableName}, {it.name}).compare(this, other)
}