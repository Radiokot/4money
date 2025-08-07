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

package ua.com.radiokot.money.util

import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import org.slf4j.LoggerFactory

object KoinSlf4jLogger : Logger() {

    private val logger: org.slf4j.Logger by lazy {
        LoggerFactory.getLogger("Koin")
    }

    override fun display(level: Level, msg: MESSAGE) {
        if (level == Level.NONE) {
            return
        }

        @Suppress("KotlinConstantConditions")
        logger
            .atLevel(
                when (level) {
                    Level.DEBUG -> org.slf4j.event.Level.DEBUG
                    Level.INFO -> org.slf4j.event.Level.INFO
                    Level.WARNING -> org.slf4j.event.Level.WARN
                    Level.ERROR,
                    Level.NONE,
                    -> org.slf4j.event.Level.ERROR
                }
            )
            .log(msg)
    }
}
