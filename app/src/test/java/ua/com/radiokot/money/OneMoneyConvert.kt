/* Copyright 2025 Oleg Koretsky

   This file is part of the 4Money,
   a budget tracking Android app.

   4Money is free software: you can redistribute it
   and/or modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation, either version 3 of the License,
   or (at your option) any later version.

   4Money is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
   See the GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with 4Money. If not, see <http://www.gnu.org/licenses/>.
*/

package ua.com.radiokot.money

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.io.bytestring.buildByteString
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

private data class OneMoneyAccount(
    val title: String,
    val balanceDecimal: BigDecimal,
    val currencyCode: String,
) {

    constructor(row: List<String>) : this(
        title = row[0].trim(),
        balanceDecimal = BigDecimal(row[1]),
        currencyCode = row[2],
    )
}

private data class MoneyAppCurrency(
    val code: String,
    val precision: Int,
    val id: UUID,
) {

    constructor(row: List<String>) : this(
        code = row[1].trim(),
        precision = row[3].toInt(),
        id = UUID.fromString(row[4].trim()),
    )
}

private data class MoneyAppAccount(
    val userId: UUID,
    val title: String,
    val balance: BigInteger,
    val currencyId: UUID,
) {
    val id: UUID =
        deterministicUuid(
            userId,
            title,
            currencyId,
        )

    fun toCsvRow(): List<String> =
        listOf(
            id.toString(),
            userId.toString(),
            title,
            balance.toString(),
            currencyId.toString(),
        )

    companion object {
        val CSV_HEADER = "id,user_id,title,balance,currency_id".split(',')

        fun fromOneMoney(
            oneMoneyAccount: OneMoneyAccount,
            moneyAppUserId: UUID,
            moneyAppCurrenciesByCode: Map<String, MoneyAppCurrency>,
        ): MoneyAppAccount {
            val moneyAppCurrency = moneyAppCurrenciesByCode.getValue(oneMoneyAccount.currencyCode)

            return MoneyAppAccount(
                userId = moneyAppUserId,
                title = oneMoneyAccount.title,
                balance = oneMoneyAccount.balanceDecimal
                    .movePointRight(moneyAppCurrency.precision)
                    .toBigInteger(),
                currencyId = moneyAppCurrency.id,
            )
        }
    }
}

private data class OneMoneyTransfer(
    val localDate: LocalDate,
    val type: Type,
    val fromAccountTitle: String,
    val toAccountCategoryTitle: String,
    val amountDecimal: BigDecimal,
    val currencyCode: String,
    val amountDecimal2: BigDecimal,
    val currencyCode2: String,
    val notes: String?,
) {

    constructor(row: List<String>) : this(
        localDate = LOCAL_DATE_FORMAT.parse(row[0]),
        type = Type.valueOfCsvType(row[1]),
        fromAccountTitle = row[2].trim(),
        toAccountCategoryTitle = row[3].trim(),
        amountDecimal = BigDecimal(row[4].trim()),
        currencyCode = row[5].trim(),
        amountDecimal2 = BigDecimal(row[6].trim()),
        currencyCode2 = row[7].trim(),
        notes = row[9].trim().takeIf(String::isNotBlank),
    )

    val categoryAndSubcategory: Pair<String, String?> =
        CATEGORY_REGEX
            .matchEntire(toAccountCategoryTitle)!!
            .run { groupValues[1] to groupValues.getOrNull(2)?.takeIf(String::isNotBlank) }

    enum class Type {
        Income,
        Expense,
        Transfer,
        ;

        companion object {
            fun valueOfCsvType(csvType: String): Type = when (csvType) {
                "Расход" -> Expense
                "Доход" -> Income
                "Перевод" -> Transfer
                else -> throw IllegalArgumentException("Unknown transfer type $csvType")
            }
        }
    }

    private companion object {
        val LOCAL_DATE_FORMAT = LocalDate.Format {
            dayOfMonth()
            char('.')
            monthNumber()
            char('.')
            year()
        }
        val CATEGORY_REGEX = "(.+?)(?:\\s\\((.+?)\\))?\$".toRegex()
    }
}

private data class MoneyAppCategory(
    val userId: UUID,
    val title: String,
    val isIncome: Boolean,
    val currencyId: UUID,
    val subcategoriesByTitle: MutableMap<String, Subcategory> = mutableMapOf(),
) {
    val id: UUID = deterministicUuid(
        userId,
        title,
        isIncome,
        currencyId,
    )

    val subcategoryCount: Int
        get() = subcategoriesByTitle.size

    fun toCsvRows(): List<List<String>> = buildList {
        // Add self.
        listOf(
            id.toString(),
            title,
            currencyId.toString(),
            userId.toString(),
            "",
            isIncome.toString(),
        ).also(::add)

        // Add subcategories
        subcategoriesByTitle.values.forEach { subcategory ->
            listOf(
                subcategory.id.toString(),
                subcategory.title,
                currencyId.toString(),
                userId.toString(),
                id.toString(),
                isIncome.toString(),
            ).also(::add)
        }
    }

    data class Subcategory(
        val title: String,
    ) {
        val id: UUID =
            deterministicUuid(
                title,
            )
    }

    companion object {
        val CSV_HEADER =
            "id,title,currency_id,user_id,parent_category_id,is_income".split(',')
    }
}

private data class MoneyAppTransfer(
    val userId: UUID,
    val time: Instant,
    val sourceId: UUID,
    val sourceAmount: BigInteger,
    val destinationId: UUID,
    val destinationAmount: BigInteger,
    val notes: String?,
) {
    val id: UUID =
        deterministicUuid(
            userId,
            time,
            sourceId,
            sourceAmount,
            destinationId,
            destinationAmount,
        )

    fun toCsvRow(): List<String> =
        listOf(
            id.toString(),
            userId.toString(),
            time.toString(),
            sourceId.toString(),
            sourceAmount.toString(),
            destinationId.toString(),
            destinationAmount.toString(),
            notes ?: "",
        )

    companion object {
        val CSV_HEADER =
            "id,user_id,time,source_id,source_amount,destination_id,destination_amount,notes"
                .split(',')

        private val EXTRA_SECONDS_BY_DAY = mutableMapOf<LocalDate, Int>()

        fun fromOneMoney(
            oneMoneyTransfer: OneMoneyTransfer,
            moneyAppUserId: UUID,
            moneyAppCurrenciesByCode: Map<String, MoneyAppCurrency>,
            moneyAppCategoriesByTitleAndCurrency: Map<Pair<String, UUID>, MoneyAppCategory>,
            moneyAppAccountsByTitleAndCurrency: Map<Pair<String, UUID>, MoneyAppAccount>,
        ): MoneyAppTransfer {
            val sourceId: UUID
            val sourceAmount: BigInteger
            val destinationId: UUID
            val destinationAmount: BigInteger

            when (oneMoneyTransfer.type) {
                OneMoneyTransfer.Type.Income -> {
                    val sourceCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode2)
                    val (sourceCategoryTitle, sourceSubcategoryTitle) =
                        oneMoneyTransfer.categoryAndSubcategory
                    val sourceCategory =
                        moneyAppCategoriesByTitleAndCurrency
                            .getValue(sourceCategoryTitle to sourceCurrency.id)
                    val sourceSubcategory =
                        sourceCategory.subcategoriesByTitle[sourceSubcategoryTitle]
                    val destinationCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode)
                    val destinationAccount =
                        moneyAppAccountsByTitleAndCurrency
                            .getValue(oneMoneyTransfer.fromAccountTitle to destinationCurrency.id)

                    sourceId = sourceSubcategory?.id ?: sourceCategory.id
                    sourceAmount = oneMoneyTransfer.amountDecimal2
                        .movePointRight(sourceCurrency.precision)
                        .toBigInteger()
                    destinationId = destinationAccount.id
                    destinationAmount = oneMoneyTransfer.amountDecimal
                        .movePointRight(destinationCurrency.precision)
                        .toBigInteger()
                }

                OneMoneyTransfer.Type.Expense -> {
                    val sourceCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode)
                    val sourceAccount =
                        moneyAppAccountsByTitleAndCurrency
                            .getValue(oneMoneyTransfer.fromAccountTitle to sourceCurrency.id)
                    val destinationCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode2)
                    val (destinationCategoryTitle, destinationSubcategoryTitle) =
                        oneMoneyTransfer.categoryAndSubcategory
                    val destinationCategory =
                        moneyAppCategoriesByTitleAndCurrency
                            .getValue(destinationCategoryTitle to destinationCurrency.id)
                    val destinationSubcategory =
                        destinationCategory.subcategoriesByTitle[destinationSubcategoryTitle]

                    sourceId = sourceAccount.id
                    sourceAmount = oneMoneyTransfer.amountDecimal
                        .movePointRight(sourceCurrency.precision)
                        .toBigInteger()
                    destinationId = destinationSubcategory?.id ?: destinationCategory.id
                    destinationAmount = oneMoneyTransfer.amountDecimal2
                        .movePointRight(destinationCurrency.precision)
                        .toBigInteger()
                }

                OneMoneyTransfer.Type.Transfer -> {
                    val sourceCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode)
                    val sourceAccount =
                        moneyAppAccountsByTitleAndCurrency
                            .getValue(oneMoneyTransfer.fromAccountTitle to sourceCurrency.id)
                    val destinationCurrency =
                        moneyAppCurrenciesByCode.getValue(oneMoneyTransfer.currencyCode2)
                    val destinationAccount =
                        moneyAppAccountsByTitleAndCurrency
                            .getValue(oneMoneyTransfer.toAccountCategoryTitle to destinationCurrency.id)

                    sourceId = sourceAccount.id
                    sourceAmount = oneMoneyTransfer.amountDecimal
                        .movePointRight(sourceCurrency.precision)
                        .toBigInteger()
                    destinationId = destinationAccount.id
                    destinationAmount = oneMoneyTransfer.amountDecimal2
                        .movePointRight(destinationCurrency.precision)
                        .toBigInteger()
                }
            }

            // As 1Money doesn't export seconds,
            // I fake them to avoid simultaneous transfers.
            val extraSeconds = synchronized(Companion) {
                EXTRA_SECONDS_BY_DAY
                    .getOrPut(oneMoneyTransfer.localDate) { 86300 }
                    .minus(10)
                    .also { EXTRA_SECONDS_BY_DAY[oneMoneyTransfer.localDate] = it }
            }

            return MoneyAppTransfer(
                userId = moneyAppUserId,
                time = Instant.fromEpochSeconds(
                    LocalDateTime(
                        date = oneMoneyTransfer.localDate,
                        time = LocalTime.fromSecondOfDay(extraSeconds),
                    )
                        .toInstant(TimeZone.currentSystemDefault())
                        .epochSeconds
                ),
                sourceId = sourceId,
                sourceAmount = sourceAmount,
                destinationId = destinationId,
                destinationAmount = destinationAmount,
                notes = oneMoneyTransfer.notes,
            )
        }
    }
}

fun main() {
    val csvReader = csvReader()
    val csvWriter = csvWriter()

    val oneMoneyCsvString = File("C:\\Users\\spiri\\Desktop\\1money-4money\\1Money.csv")
        .readText()
    val moneyAppCurrenciesCsvString = File("C:\\Users\\spiri\\Desktop\\1money-4money\\currencies_rows.csv")
        .readText()
    val moneyAppUserId = UUID.fromString("0b451d9f-527f-4d06-bd0b-d2ba9acc7719")
    val moneyAppCsvOutputDir = File("C:\\Users\\spiri\\Desktop\\1money-4money")
        .apply(File::mkdirs)

    val oneMoneyCsvStringSplit = oneMoneyCsvString.split(",\n,\n")
    val oneMoneyTransfers = csvReader
        .readAll(oneMoneyCsvStringSplit[0])
        // Drop the header.
        .drop(1)
        .map(::OneMoneyTransfer)
    val oneMoneyAccounts = csvReader
        // Remove ending.
        .readAll(oneMoneyCsvStringSplit[1].trimEnd('\n', ','))
        // Drop the header.
        .drop(1)
        .map(::OneMoneyAccount)

    val moneyAppCurrenciesByCode: Map<String, MoneyAppCurrency> = csvReader
        .readAll(moneyAppCurrenciesCsvString)
        // Drop the header.
        .drop(1)
        .map(::MoneyAppCurrency)
        .associateBy(MoneyAppCurrency::code)

    val moneyAppAccounts = oneMoneyAccounts
        .map { oneMoneyAccount ->
            MoneyAppAccount.fromOneMoney(
                oneMoneyAccount = oneMoneyAccount,
                moneyAppUserId = moneyAppUserId,
                moneyAppCurrenciesByCode = moneyAppCurrenciesByCode,
            )
        }
    val moneyAppAccountsByTitleAndCurrency = moneyAppAccounts
        .associateBy { moneyAppAccount ->
            moneyAppAccount.title to moneyAppAccount.currencyId
        }

    val moneyAppCategoriesByTitleAndCurrency = buildMap<Pair<String, UUID>, MoneyAppCategory> {
        oneMoneyTransfers
            .asSequence()
            .filter { it.type != OneMoneyTransfer.Type.Transfer }
            .forEach { oneMoneyTransfer ->
                val (categoryTitle, subcategoryTitle) =
                    oneMoneyTransfer.categoryAndSubcategory
                val currencyId = moneyAppCurrenciesByCode
                    .getValue(oneMoneyTransfer.currencyCode2)
                    .id

                val category = getOrPut(categoryTitle to currencyId) {
                    MoneyAppCategory(
                        userId = moneyAppUserId,
                        title = categoryTitle,
                        isIncome = oneMoneyTransfer.type == OneMoneyTransfer.Type.Income,
                        currencyId = currencyId,
                    )
                }

                if (subcategoryTitle != null) {
                    category.subcategoriesByTitle.getOrPut(subcategoryTitle) {
                        MoneyAppCategory.Subcategory(
                            title = subcategoryTitle,
                        )
                    }
                }
            }
    }

    val moneyAppTransfers = oneMoneyTransfers
        .map { oneMoneyTransfer ->
            MoneyAppTransfer.fromOneMoney(
                oneMoneyTransfer = oneMoneyTransfer,
                moneyAppUserId = moneyAppUserId,
                moneyAppCurrenciesByCode = moneyAppCurrenciesByCode,
                moneyAppCategoriesByTitleAndCurrency = moneyAppCategoriesByTitleAndCurrency,
                moneyAppAccountsByTitleAndCurrency = moneyAppAccountsByTitleAndCurrency,
            )
        }

    csvWriter
        .open(
            targetFile = File(moneyAppCsvOutputDir, "accounts.csv"),
            append = false,
        ) {
            writeRow(MoneyAppAccount.CSV_HEADER)
            moneyAppAccountsByTitleAndCurrency
                .values
                .forEach { writeRow(it.toCsvRow()) }

            println("Written ${moneyAppAccountsByTitleAndCurrency.size} accounts")
        }

    csvWriter
        .open(
            targetFile = File(moneyAppCsvOutputDir, "categories.csv"),
            append = false,
        ) {
            writeRow(MoneyAppCategory.CSV_HEADER)
            moneyAppCategoriesByTitleAndCurrency
                .values
                .forEach { writeRows(it.toCsvRows()) }

            println(
                "Written ${moneyAppCategoriesByTitleAndCurrency.size} categories " +
                        "and ${moneyAppCategoriesByTitleAndCurrency.values.sumOf(MoneyAppCategory::subcategoryCount)} subcategories"
            )
        }

    csvWriter
        .open(
            targetFile = File(moneyAppCsvOutputDir, "transfers.csv"),
            append = false,
        ) {
            writeRow(MoneyAppTransfer.CSV_HEADER)
            moneyAppTransfers
                .forEach { writeRow(it.toCsvRow()) }

            println("Written ${moneyAppTransfers.size} transfers")
        }
}

private fun deterministicUuid(vararg primaryKey: Any): UUID =
    UUID.nameUUIDFromBytes(
        buildByteString {
            primaryKey.forEach { key ->
                append(key.toString().toByteArray())
            }
        }.toByteArray()
    )
