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

package ua.com.radiokot.money.colors.data

class HardcodedItemColorSchemeRepository : ItemColorSchemeRepository {

    private val schemes: List<ItemColorScheme> by lazy {
        listOf(
            // region Black
            ItemColorScheme(
                name = "Black1",
                primary = 0xFFEDEDED,
                onPrimary = 0xFF181818,
            ),
            ItemColorScheme(
                name = "Black2",
                primary = 0xFFD6D5D5,
                onPrimary = 0xFF181818,
            ),
            ItemColorScheme(
                name = "Black3",
                primary = 0xFFB0B0B0,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Black4",
                primary = 0xFF797979,
                onPrimary = 0xFFF3F3F3,
            ),
            ItemColorScheme(
                name = "Black5",
                primary = 0xFF414141,
                onPrimary = 0xFFF3F3F3,
            ),
            ItemColorScheme(
                name = "Black6",
                primary = 0xFF292929,
                onPrimary = 0xFFEDEDED,
            ),
            // endregion
            // region Red
            ItemColorScheme(
                name = "Red1",
                primary = 0xFFF8E4E4,
                onPrimary = 0xFFCA4B4B,
            ),
            ItemColorScheme(
                name = "Red2",
                primary = 0xFFF1B6B7,
                onPrimary = 0xFFAC2B2E,
            ),
            ItemColorScheme(
                name = "Red3",
                primary = 0xFFDE7778,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Red4",
                primary = 0xFFBF3D3F,
                onPrimary = 0xFFFFF6F6,
            ),
            ItemColorScheme(
                name = "Red5",
                primary = 0xFF763536,
                onPrimary = 0xFFFFF6F6,
            ),
            ItemColorScheme(
                name = "Red6",
                primary = 0xFF452122,
                onPrimary = 0xFFED757B,
            ),
            // endregion
            // region Orange
            ItemColorScheme(
                name = "Orange1",
                primary = 0xFFFDECDE,
                onPrimary = 0xFFDB5A37,
            ),
            ItemColorScheme(
                name = "Orange2",
                primary = 0xFFF3B695,
                onPrimary = 0xFFA83800,
            ),
            ItemColorScheme(
                name = "Orange3",
                primary = 0xFFEA8D5B,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Orange4",
                primary = 0xFFDA6526,
                onPrimary = 0xFFFFF9F6,
            ),
            ItemColorScheme(
                name = "Orange5",
                primary = 0xFF964F28,
                onPrimary = 0xFFFFF9F6,
            ),
            ItemColorScheme(
                name = "Orange6",
                primary = 0xFF4E2D0F,
                onPrimary = 0xFFF38F5A,
            ),
            // endregion
            // region Yellow
            ItemColorScheme(
                name = "Yellow1",
                primary = 0xFFFFF5DC,
                onPrimary = 0xFFF6A626,
            ),
            ItemColorScheme(
                name = "Yellow2",
                primary = 0xFFFFE4A2,
                onPrimary = 0xFFCB6900,
            ),
            ItemColorScheme(
                name = "Yellow3",
                primary = 0xFFF2C355,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Yellow4",
                primary = 0xFFEBA120,
                onPrimary = 0xFFFFFBF7,
            ),
            ItemColorScheme(
                name = "Yellow5",
                primary = 0xFFB37439,
                onPrimary = 0xFFFFFBF7,
            ),
            ItemColorScheme(
                name = "Yellow6",
                primary = 0xFF62370E,
                onPrimary = 0xFFFFDA75,
            ),
            // endregion
            // region Green
            ItemColorScheme(
                name = "Green1",
                primary = 0xFFECF7E0,
                onPrimary = 0xFF7FA721,
            ),
            ItemColorScheme(
                name = "Green2",
                primary = 0xFFC3DBA8,
                onPrimary = 0xFF366D06,
            ),
            ItemColorScheme(
                name = "Green3",
                primary = 0xFFA1C577,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Green4",
                primary = 0xFF78A348,
                onPrimary = 0xFFF5FCEE,
            ),
            ItemColorScheme(
                name = "Green5",
                primary = 0xFF5A733F,
                onPrimary = 0xFFF5FCEE,
            ),
            ItemColorScheme(
                name = "Green6",
                primary = 0xFF273616,
                onPrimary = 0xFFC8F09B,
            ),
            // endregion
            // region Turquoise
            ItemColorScheme(
                name = "Turquoise1",
                primary = 0xFFE3F3EB,
                onPrimary = 0xFF3C905E,
            ),
            ItemColorScheme(
                name = "Turquoise2",
                primary = 0xFFB6E0CB,
                onPrimary = 0xFF136835,
            ),
            ItemColorScheme(
                name = "Turquoise3",
                primary = 0xFF79B78F,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Turquoise4",
                primary = 0xFF3FA56F,
                onPrimary = 0xFFEEFDF5,
            ),
            ItemColorScheme(
                name = "Turquoise5",
                primary = 0xFF406B50,
                onPrimary = 0xFFEEFDF5,
            ),
            ItemColorScheme(
                name = "Turquoise6",
                primary = 0xFF153731,
                onPrimary = 0xFFA7F6C0,
            ),
            // endregion
            // region Blue
            ItemColorScheme(
                name = "Blue1",
                primary = 0xFFE6F2F2,
                onPrimary = 0xFF1D9EC6,
            ),
            ItemColorScheme(
                name = "Blue2",
                primary = 0xFFB5E5E5,
                onPrimary = 0xFF087389,
            ),
            ItemColorScheme(
                name = "Blue3",
                primary = 0xFF66ACB9,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Blue4",
                primary = 0xFF35AAB3,
                onPrimary = 0xFFF1FEFF,
            ),
            ItemColorScheme(
                name = "Blue5",
                primary = 0xFF176368,
                onPrimary = 0xFFF1FEFF,
            ),
            ItemColorScheme(
                name = "Blue6",
                primary = 0xFF093033,
                onPrimary = 0xFFB9F9FE,
            ),
            // endregion
            // region Purple
            ItemColorScheme(
                name = "Purple1",
                primary = 0xFFEFF2FF,
                onPrimary = 0xFF6B6DB6,
            ),
            ItemColorScheme(
                name = "Purple2",
                primary = 0xFFC7CEEB,
                onPrimary = 0xFF4E5090,
            ),
            ItemColorScheme(
                name = "Purple3",
                primary = 0xFF7988C4,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Purple4",
                primary = 0xFF4F63B3,
                onPrimary = 0xFFF1F4FF,
            ),
            ItemColorScheme(
                name = "Purple5",
                primary = 0xFF424B73,
                onPrimary = 0xFFF1F4FF,
            ),
            ItemColorScheme(
                name = "Purple6",
                primary = 0xFF232D52,
                onPrimary = 0xFFBDCAFC,
            ),
            // endregion
            // region Pink
            ItemColorScheme(
                name = "Pink1",
                primary = 0xFFF8EEF4,
                onPrimary = 0xFFC45396,
            ),
            ItemColorScheme(
                name = "Pink2",
                primary = 0xFFF0C2D8,
                onPrimary = 0xFFC3398B,
            ),
            ItemColorScheme(
                name = "Pink3",
                primary = 0xFFDE81AC,
                onPrimary = 0xFFFFFFFF,
            ),
            ItemColorScheme(
                name = "Pink4",
                primary = 0xFFCA4E88,
                onPrimary = 0xFFFFEEF6,
            ),
            ItemColorScheme(
                name = "Pink5",
                primary = 0xFF733552,
                onPrimary = 0xFFFFEEF6,
            ),
            ItemColorScheme(
                name = "Pink6",
                primary = 0xFF44192D,
                onPrimary = 0xFFE590B8,
            ),
            // endregion
        ).apply {
            forEach { scheme ->
                if (scheme.primary == scheme.onPrimary) {
                    error("Colors blend in ${scheme.name}")
                }
            }
        }
    }

    private val schemesByName: Map<String, ItemColorScheme> by lazy {
        schemes.associateBy(ItemColorScheme::name)
    }

    override fun getItemColorSchemes(): List<ItemColorScheme> =
        schemes

    override fun getItemColorSchemesByName(): Map<String, ItemColorScheme> =
        schemesByName
}
