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

package ua.com.radiokot.money.accounts.logic

import com.powersync.PowerSyncDatabase
import kotlinx.coroutines.flow.first
import ua.com.radiokot.money.accounts.data.Account
import ua.com.radiokot.money.accounts.data.PowerSyncAccountRepository
import ua.com.radiokot.money.lazyLogger
import ua.com.radiokot.money.util.SternBrocotTreeSearch

class PowerSyncMoveAccountUseCase(
    private val database: PowerSyncDatabase,
    private val accountRepository: PowerSyncAccountRepository,
    private val getVisibleAccountsUseCase: GetVisibleAccountsUseCase,
) : MoveAccountUseCase {

    private val log by lazyLogger("PowerSyncMoveAccountUC")

    override suspend operator fun invoke(
        accountToMove: Account,
        accountToPlaceBefore: Account?,
        accountToPlaceAfter: Account?,
    ): Result<Unit> = runCatching {

        val targetType = accountToPlaceBefore?.type
            ?: accountToPlaceAfter?.type
            ?: accountToMove.type

        val isChangingType = accountToMove.type != targetType

        val shownAccountsOfTargetType = getVisibleAccountsUseCase()
            .first()
            .filter { it.type == accountToMove.type }

        if (!isChangingType) {

            val accountToMoveIndexWithinTargetType =
                shownAccountsOfTargetType.indexOf(accountToMove)

            val accountToPlaceBeforeIndexWithinTargetType =
                shownAccountsOfTargetType.indexOf(accountToPlaceBefore)

            val accountToPlaceAfterIndexWithinTargetType =
                shownAccountsOfTargetType.indexOf(accountToPlaceAfter)

            if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 1
                || accountToPlaceAfterIndexWithinTargetType == accountToMoveIndexWithinTargetType - 1
            ) {
                log.debug {
                    "invoke(): skipping as the account is already in place"
                }

                return@runCatching
            }

            // Avoid position recalculation when swapping neighbours within the same type,
            // otherwise the fraction quickly becomes tiny (after tens of swaps).
            if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 2
                || accountToPlaceAfterIndexWithinTargetType == accountToMoveIndexWithinTargetType - 2
            ) {
                val accountToSwapWith =
                    if (accountToPlaceBeforeIndexWithinTargetType == accountToMoveIndexWithinTargetType + 2)
                        shownAccountsOfTargetType[accountToMoveIndexWithinTargetType + 1]
                    else
                        shownAccountsOfTargetType[accountToMoveIndexWithinTargetType - 1]

                log.debug {
                    "invoke(): swapping positions within the same type:" +
                            "\nswap=$accountToMove," +
                            "\nwith=$accountToSwapWith"
                }

                database.writeTransaction { transaction ->

                    accountRepository.updatePosition(
                        accountId = accountToMove.id,
                        newPosition = accountToSwapWith.position,
                        transaction = transaction,
                    )

                    accountRepository.updatePosition(
                        accountId = accountToSwapWith.id,
                        newPosition = accountToMove.position,
                        transaction = transaction,
                    )
                }

                return@runCatching
            }
        }

        // As accounts are ordered by descending position,
        // placing before means assigning greater position value.
        // The end (bottom) is 0.0.
        val lowerBound = accountToPlaceBefore?.position ?: 0.0
        // The start (top) is +∞.
        val upperBound = accountToPlaceAfter?.position ?: Double.POSITIVE_INFINITY

        val newPosition = SternBrocotTreeSearch()
            .goBetween(
                lowerBound = lowerBound,
                upperBound = upperBound,
            )
            .value

        database.writeTransaction { transaction ->

            log.debug {
                "invoke(): updating position:" +
                        "\naccount=$accountToMove," +
                        "\npositionLowerBound=$lowerBound," +
                        "\npositionUpperBound=$upperBound," +
                        "\nnewPosition=$newPosition"
            }

            accountRepository.updatePosition(
                accountId = accountToMove.id,
                newPosition = newPosition,
                transaction = transaction,
            )

            if (isChangingType) {

                log.debug {
                    "invoke(): updating type:" +
                            "\nnewType=$targetType"
                }

                accountRepository.updateType(
                    accountId = accountToMove.id,
                    newType = targetType,
                    transaction = transaction,
                )
            }
        }
    }
}
