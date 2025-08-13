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

package ua.com.radiokot.money.transfers.view

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.json.Json
import ua.com.radiokot.money.lazyLogger

class DatePickerDialogFragment : AppCompatDialogFragment(),
    DatePickerDialog.OnDateSetListener {

    private val log by lazyLogger("DatePickerDialog")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val localDate = requireNotNull(arguments?.let(::getLocalDate)) {
            "No local date argument specified"
        }

        return DatePickerDialog(
            requireContext(),
            this,
            localDate.year,
            localDate.month.number - 1,
            localDate.day,
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val result = LocalDate(
            year = year,
            month = month + 1,
            day = dayOfMonth,
        )

        log.debug {
            "onDateSet(): setting result:" +
                    "\nresult=$result"
        }

        setFragmentResult(
            DATE_REQUEST_KEY,
            getBundle(
                currentDate = result,
            )
        )
    }

    companion object {
        private const val LOCAL_DATE_KEY = "local-date"

        const val TAG = "DatePickerDialogFragment"
        const val DATE_REQUEST_KEY = "date"

        fun getLocalDate(bundle: Bundle): LocalDate =
            Json.decodeFromString(bundle.getString(LOCAL_DATE_KEY)!!)

        fun getBundle(currentDate: LocalDate) = Bundle().apply {
            putString(LOCAL_DATE_KEY, Json.encodeToString(currentDate))
        }

        fun newInstance(bundle: Bundle) = DatePickerDialogFragment().apply {
            arguments = bundle
        }
    }
}
