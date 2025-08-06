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

package ua.com.radiokot.money.powersync

import com.powersync.db.SqlCursor
import com.powersync.db.getBooleanOptional
import com.powersync.db.getDouble
import com.powersync.db.getLong
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.powersync.db.schema.Column
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.categories.data.Category
import ua.com.radiokot.money.categories.data.Subcategory
import ua.com.radiokot.money.colors.data.ItemColorScheme
import ua.com.radiokot.money.currency.data.Currency
import ua.com.radiokot.money.transfers.data.Transfer
import ua.com.radiokot.money.transfers.data.TransferCounterparty
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("MemberVisibilityCanBePrivate")
object DbSchema {

    fun getPowerSyncSchema() = Schema(
        // All the tables have 'id' column by default.
        listOf(
            getPowerSyncCurrencyTable(),
            getPowerSyncAccountsTable(),
            getPowerSyncPairsTable(),
            getPowerSyncCategoriesTable(),
            getPowerSyncTransfersTable(),
            getPowerSyncSyncErrorsTable(),
        )
    )

    const val ID = "id"
    const val METADATA = "_metadata"

    const val CURRENCIES_TABLE = "currencies"
    const val CURRENCY_SELECTED_ID = CURRENCIES_TABLE + ID
    const val CURRENCY_CODE = "code"
    const val CURRENCY_SELECTED_CODE = CURRENCIES_TABLE + CURRENCY_CODE
    const val CURRENCY_SYMBOL = "symbol"
    const val CURRENCY_SELECTED_SYMBOL = CURRENCIES_TABLE + CURRENCY_SYMBOL
    const val CURRENCY_PRECISION = "precision"
    const val CURRENCY_SELECTED_PRECISION = CURRENCIES_TABLE + CURRENCY_PRECISION

    const val CURRENCY_SELECT_COLUMNS = "" +
            "$CURRENCIES_TABLE.$ID as $CURRENCY_SELECTED_ID, " +
            "$CURRENCIES_TABLE.$CURRENCY_CODE as $CURRENCY_SELECTED_CODE, " +
            "$CURRENCIES_TABLE.$CURRENCY_SYMBOL as $CURRENCY_SELECTED_SYMBOL, " +
            "$CURRENCIES_TABLE.$CURRENCY_PRECISION as $CURRENCY_SELECTED_PRECISION "

    private fun getPowerSyncCurrencyTable() = Table(
        name = CURRENCIES_TABLE,
        columns = listOf(
            Column.text(CURRENCY_CODE),
            Column.text(CURRENCY_SYMBOL),
            Column.integer(CURRENCY_PRECISION),
        ),
        ignoreEmptyUpdates = true,
    )

    /**
     * @see CURRENCY_SELECT_COLUMNS
     */
    fun toCurrency(
        sqlCursor: SqlCursor,
    ): Currency = with(sqlCursor) {
        Currency(
            id = getString(CURRENCY_SELECTED_ID),
            code = getString(CURRENCY_SELECTED_CODE).trim(),
            symbol = getString(CURRENCY_SELECTED_SYMBOL).trim(),
            precision = getLong(CURRENCY_SELECTED_PRECISION).toInt(),
        )
    }

    const val PAIRS_TABLE = "pairs"
    const val PAIR_SELECTED_ID = PAIRS_TABLE + ID
    const val PAIR_PRICE = "price"
    const val PAIR_SELECTED_PRICE = PAIRS_TABLE + PAIR_PRICE

    const val PAIR_SELECT_COLUMNS = "" +
            "$PAIRS_TABLE.$ID as $PAIR_SELECTED_ID, " +
            "$PAIRS_TABLE.$PAIR_PRICE as $PAIR_SELECTED_PRICE "

    private fun getPowerSyncPairsTable() = Table(
        // In this table, base_currency_code is the id
        // and all the prices are in USD.
        name = PAIRS_TABLE,
        columns = listOf(
            Column.text(PAIR_PRICE),
        ),
        ignoreEmptyUpdates = true,
    )

    /**
     * @see PAIR_SELECT_COLUMNS
     */
    fun toPricePair(
        sqlCursor: SqlCursor,
    ): Pair<String, BigDecimal> = with(sqlCursor) {
        Pair(
            getString(PAIR_SELECTED_ID),
            BigDecimal(getString(PAIR_SELECTED_PRICE).trim()),
        )
    }

    const val ACCOUNTS_TABLE = "accounts"
    const val ACCOUNT_SELECTED_ID = ACCOUNTS_TABLE + ID
    const val ACCOUNT_TITLE = "title"
    const val ACCOUNT_SELECTED_TITLE = ACCOUNTS_TABLE + ACCOUNT_TITLE
    const val ACCOUNT_BALANCE = "balance"
    const val ACCOUNT_SELECTED_BALANCE = ACCOUNTS_TABLE + ACCOUNT_BALANCE
    const val ACCOUNT_POSITION = "position"
    const val ACCOUNT_SELECTED_POSITION = ACCOUNTS_TABLE + ACCOUNT_POSITION
    const val ACCOUNT_COLOR_SCHEME = "color_scheme"
    const val ACCOUNT_SELECTED_COLOR_SCHEME = ACCOUNTS_TABLE + ACCOUNT_COLOR_SCHEME
    const val ACCOUNT_TYPE = "type"
    const val ACCOUNT_SELECTED_TYPE = ACCOUNTS_TABLE + ACCOUNT_TYPE
    const val ACCOUNT_IS_ARCHIVED = "is_archived"
    const val ACCOUNT_SELECTED_ARCHIVED = ACCOUNTS_TABLE + ACCOUNT_IS_ARCHIVED
    const val ACCOUNT_CURRENCY_ID = "currency_id"
    const val ACCOUNT_SELECTED_CURRENCY_ID = ACCOUNTS_TABLE + ACCOUNT_CURRENCY_ID

    const val ACCOUNT_SELECT_COLUMNS = "" +
            "$ACCOUNTS_TABLE.$ID as $ACCOUNT_SELECTED_ID, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_TITLE as $ACCOUNT_SELECTED_TITLE, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_BALANCE as $ACCOUNT_SELECTED_BALANCE, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_POSITION as $ACCOUNT_SELECTED_POSITION, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_COLOR_SCHEME as $ACCOUNT_SELECTED_COLOR_SCHEME, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_TYPE as $ACCOUNT_SELECTED_TYPE, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_IS_ARCHIVED as $ACCOUNT_SELECTED_ARCHIVED, " +
            "$ACCOUNTS_TABLE.$ACCOUNT_CURRENCY_ID as $ACCOUNT_SELECTED_CURRENCY_ID "

    private fun getPowerSyncAccountsTable() = Table(
        name = ACCOUNTS_TABLE,
        columns = listOf(
            Column.text(ACCOUNT_TITLE),
            Column.text(ACCOUNT_BALANCE),
            Column.text(ACCOUNT_CURRENCY_ID),
            // Real (float8) can't provide sufficient precision,
            // leading to frequent healing.
            Column.text(ACCOUNT_POSITION),
            Column.text(ACCOUNT_COLOR_SCHEME),
            Column.text(ACCOUNT_TYPE),
            Column.integer(ACCOUNT_IS_ARCHIVED),
        ),
        ignoreEmptyUpdates = true,
    )

    /**
     * @see ACCOUNT_SELECT_COLUMNS
     * @see CURRENCY_SELECT_COLUMNS
     */
    fun toAccount(
        sqlCursor: SqlCursor,
        colorSchemesByName: Map<String, ItemColorScheme>,
    ): Account = with(sqlCursor) {
        Account(
            id = getString(ACCOUNT_SELECTED_ID),
            title = getString(ACCOUNT_SELECTED_TITLE).trim(),
            balance = BigInteger(getString(ACCOUNT_SELECTED_BALANCE).trim()),
            position = getDouble(ACCOUNT_SELECTED_POSITION),
            colorScheme = getString(ACCOUNT_SELECTED_COLOR_SCHEME)
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            type = getString(ACCOUNT_SELECTED_TYPE)
                .trim()
                .let(Account.Type::fromSlug),
            isArchived = getBooleanOptional(ACCOUNT_SELECTED_ARCHIVED) == true,
            currency = toCurrency(sqlCursor),
        )
    }

    const val CATEGORIES_TABLE = "categories"
    const val CATEGORY_SELECTED_ID = CATEGORIES_TABLE + ID
    const val CATEGORY_TITLE = "title"
    const val CATEGORY_SELECTED_TITLE = CATEGORIES_TABLE + CATEGORY_TITLE
    const val CATEGORY_CURRENCY_ID = "currency_id"
    const val CATEGORY_SELECTED_CURRENCY_ID = CATEGORIES_TABLE + CATEGORY_CURRENCY_ID
    const val CATEGORY_PARENT_ID = "parent_category_id"
    const val CATEGORY_SELECTED_PARENT_ID = CATEGORIES_TABLE + CATEGORY_PARENT_ID
    const val CATEGORY_IS_INCOME = "is_income"
    const val CATEGORY_SELECTED_IS_INCOME = CATEGORIES_TABLE + CATEGORY_IS_INCOME
    const val CATEGORY_COLOR_SCHEME = "color_scheme"
    const val CATEGORY_SELECTED_COLOR_SCHEME = CATEGORIES_TABLE + CATEGORY_COLOR_SCHEME
    const val CATEGORY_IS_ARCHIVED = "is_archived"
    const val CATEGORY_SELECTED_IS_ARCHIVED = CATEGORIES_TABLE + CATEGORY_IS_ARCHIVED
    const val CATEGORY_POSITION = "position"
    const val CATEGORY_SELECTED_POSITION = CATEGORIES_TABLE + CATEGORY_POSITION

    const val CATEGORY_SELECT_COLUMNS = "" +
            "$CATEGORIES_TABLE.$ID as $CATEGORY_SELECTED_ID, " +
            "$CATEGORIES_TABLE.$CATEGORY_TITLE as $CATEGORY_SELECTED_TITLE, " +
            "$CATEGORIES_TABLE.$CATEGORY_CURRENCY_ID as $CATEGORY_SELECTED_CURRENCY_ID, " +
            "$CATEGORIES_TABLE.$CATEGORY_PARENT_ID as $CATEGORY_SELECTED_PARENT_ID, " +
            "$CATEGORIES_TABLE.$CATEGORY_IS_INCOME as $CATEGORY_SELECTED_IS_INCOME, " +
            "$CATEGORIES_TABLE.$CATEGORY_COLOR_SCHEME as $CATEGORY_SELECTED_COLOR_SCHEME, " +
            "$CATEGORIES_TABLE.$CATEGORY_IS_ARCHIVED as $CATEGORY_SELECTED_IS_ARCHIVED, " +
            "$CATEGORIES_TABLE.$CATEGORY_POSITION as $CATEGORY_SELECTED_POSITION "

    const val SUBCATEGORY_SELECT_COLUMNS = "" +
            "$CATEGORIES_TABLE.$ID as $CATEGORY_SELECTED_ID, " +
            "$CATEGORIES_TABLE.$CATEGORY_TITLE as $CATEGORY_SELECTED_TITLE, " +
            "$CATEGORIES_TABLE.$CATEGORY_PARENT_ID as $CATEGORY_SELECTED_PARENT_ID, " +
            "$CATEGORIES_TABLE.$CATEGORY_POSITION as $CATEGORY_SELECTED_POSITION "

    private fun getPowerSyncCategoriesTable() = Table(
        name = CATEGORIES_TABLE,
        columns = listOf(
            Column.text(CATEGORY_TITLE),
            Column.text(CATEGORY_CURRENCY_ID),
            Column.text(CATEGORY_PARENT_ID),
            Column.integer(CATEGORY_IS_INCOME),
            Column.text(CATEGORY_COLOR_SCHEME),
            Column.integer(CATEGORY_IS_ARCHIVED),
            // Real (float8) can't provide sufficient precision,
            // leading to frequent healing.
            Column.text(CATEGORY_POSITION),
        ),
        ignoreEmptyUpdates = true,
    )

    /**
     * @see CATEGORY_SELECT_COLUMNS
     * @see CURRENCY_SELECT_COLUMNS
     */
    fun toCategory(
        sqlCursor: SqlCursor,
        colorSchemesByName: Map<String, ItemColorScheme>,
    ): Category = with(sqlCursor) {
        Category(
            id = getString(CATEGORY_SELECTED_ID),
            title = getString(CATEGORY_SELECTED_TITLE).trim(),
            isIncome = getBooleanOptional(CATEGORY_SELECTED_IS_INCOME) == true,
            colorScheme = getString(CATEGORY_SELECTED_COLOR_SCHEME)
                .trim()
                .let { colorSchemeName ->
                    colorSchemesByName[colorSchemeName]
                        ?: error("Can't find '$colorSchemeName' color scheme")
                },
            isArchived = getBooleanOptional(CATEGORY_SELECTED_IS_ARCHIVED) == true,
            position = getDouble(CATEGORY_SELECTED_POSITION),
            currency = toCurrency(sqlCursor),
        )
    }

    /**
     * @see SUBCATEGORY_SELECT_COLUMNS
     */
    fun toSubcategory(
        sqlCursor: SqlCursor,
    ): Subcategory = with(sqlCursor) {
        Subcategory(
            id = getString(CATEGORY_SELECTED_ID),
            title = getString(CATEGORY_SELECTED_TITLE).trim(),
            position = getDouble(CATEGORY_SELECTED_POSITION),
            categoryId = getString(CATEGORY_SELECTED_PARENT_ID),
        )
    }

    const val TRANSFERS_TABLE = "transfers"
    const val TRANSFER_SELECTED_ID = TRANSFERS_TABLE + ID
    const val TRANSFER_TIME = "time"
    const val TRANSFER_SELECTED_DATETIME = TRANSFERS_TABLE + "datetime"
    const val TRANSFER_TIME_AS_DATETIME = "datetime($TRANSFERS_TABLE.$TRANSFER_TIME) as $TRANSFER_SELECTED_DATETIME"
    const val TRANSFER_SOURCE_ID = "source_id"
    const val TRANSFER_SELECTED_SOURCE_ID = TRANSFERS_TABLE + TRANSFER_SOURCE_ID
    const val TRANSFER_SOURCE_AMOUNT = "source_amount"
    const val TRANSFER_SELECTED_SOURCE_AMOUNT = TRANSFERS_TABLE + TRANSFER_SOURCE_AMOUNT
    const val TRANSFER_DESTINATION_ID = "destination_id"
    const val TRANSFER_SELECTED_DESTINATION_ID = TRANSFERS_TABLE + TRANSFER_DESTINATION_ID
    const val TRANSFER_DESTINATION_AMOUNT = "destination_amount"
    const val TRANSFER_SELECTED_DESTINATION_AMOUNT = TRANSFERS_TABLE + TRANSFER_DESTINATION_AMOUNT
    const val TRANSFER_MEMO = "memo"
    const val TRANSFER_SELECTED_MEMO = TRANSFERS_TABLE + TRANSFER_MEMO

    const val TRANSFER_SELECT_COLUMNS = "" +
            "$TRANSFERS_TABLE.$ID as $TRANSFER_SELECTED_ID, " +
            "$TRANSFER_TIME_AS_DATETIME, " +
            "$TRANSFERS_TABLE.$TRANSFER_SOURCE_ID as $TRANSFER_SELECTED_SOURCE_ID, " +
            "$TRANSFERS_TABLE.$TRANSFER_SOURCE_AMOUNT as $TRANSFER_SELECTED_SOURCE_AMOUNT, " +
            "$TRANSFERS_TABLE.$TRANSFER_DESTINATION_ID as $TRANSFER_SELECTED_DESTINATION_ID, " +
            "$TRANSFERS_TABLE.$TRANSFER_DESTINATION_AMOUNT as $TRANSFER_SELECTED_DESTINATION_AMOUNT, " +
            "$TRANSFERS_TABLE.$TRANSFER_MEMO as $TRANSFER_SELECTED_MEMO "

    private fun getPowerSyncTransfersTable() = Table(
        name = TRANSFERS_TABLE,
        columns = listOf(
            Column.text(TRANSFER_TIME),
            Column.text(TRANSFER_SOURCE_ID),
            Column.text(TRANSFER_SOURCE_AMOUNT),
            Column.text(TRANSFER_DESTINATION_ID),
            Column.text(TRANSFER_DESTINATION_AMOUNT),
            Column.text(TRANSFER_MEMO),
        ),
        ignoreEmptyUpdates = true,
        trackMetadata = true,
    )

    /**
     * @see TRANSFER_SELECT_COLUMNS
     */
    fun toTransfer(
        sqlCursor: SqlCursor,
        counterpartiesById: Map<String, TransferCounterparty>,
    ): Transfer = with(sqlCursor) {
        val sourceId = getString(TRANSFER_SELECTED_SOURCE_ID)
        val destinationId = getString(TRANSFER_SELECTED_DESTINATION_ID)

        Transfer(
            id = getString(TRANSFER_SELECTED_ID),
            source = counterpartiesById[sourceId]
                ?: error("Source $sourceId not found"),
            sourceAmount = BigInteger(getString(TRANSFER_SELECTED_SOURCE_AMOUNT).trim()),
            destination = counterpartiesById[destinationId]
                ?: error("Destination $destinationId not found"),
            destinationAmount = BigInteger(getString(TRANSFER_SELECTED_DESTINATION_AMOUNT).trim()),
            dateTime = LocalDateTime.fromDbString(getString(TRANSFER_SELECTED_DATETIME)),
            memo = getStringOptional(TRANSFER_SELECTED_MEMO)?.trim(),
        )
    }

    const val SYNC_ERRORS_TABLE = "sync_errors"

    private fun getPowerSyncSyncErrorsTable() = Table(
        name = SYNC_ERRORS_TABLE,
        columns = listOf(
            // Just an ID, which is a timestamp.
        ),
        ignoreEmptyUpdates = true,
    )

    fun LocalDateTime.toDbString() =
        format(LocalDateTime.Formats.ISO)
            .replace('T', ' ')
            // Trim millis.
            .substringBeforeLast('.')

    fun LocalDateTime.Companion.fromDbString(dateTimeString: String) =
        parse(
            input = dateTimeString
                .replace(' ', 'T'),
            format = LocalDateTime.Formats.ISO
        )
}

