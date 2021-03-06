/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer.prefs

import android.preference.Preference
import universe.constellation.orion.viewer.LastPageInfo

sealed class BookPreferenceKey(
        val prefKey: String,
        val bookKey: String
) {
    object ZOOM : BookPreferenceKey(GlobalOptions.DEFAULT_ZOOM, LastPageInfo::zoom.name)
    object PAGE_LAYOUT : BookPreferenceKey(GlobalOptions.PAGE_LAYOUT, LastPageInfo::pageLayout.name)
    object WALK_ORDER : BookPreferenceKey(GlobalOptions.WALK_ORDER, LastPageInfo::walkOrder.name)
    object SCREEN_ORIENTATION : BookPreferenceKey(GlobalOptions.SCREEN_ORIENTATION, LastPageInfo::screenOrientation.name)
    object COLOR_MODE : BookPreferenceKey(GlobalOptions.COLOR_MODE, LastPageInfo::colorMode.name)
    object CONTRAST : BookPreferenceKey(GlobalOptions.DEFAULT_CONTRAST, LastPageInfo::contrast.name)
    object THRESHOLD : BookPreferenceKey("THRESHOLD", LastPageInfo::threshold.name)
}

class State {
    var isCurrentBookOption: Boolean = false
    lateinit var bookPreferenceKey: BookPreferenceKey
    var onSetInitialValue: Boolean = false
}

interface OrionBookPreference {
    val orionState: State

    var isCurrentBookOption
        get() = orionState.isCurrentBookOption
        set(value) {
            orionState.isCurrentBookOption = value
        }

    var Preference.orionKey
        get() = orionState.bookPreferenceKey
        set(value) {
            key = value.key
            orionState.bookPreferenceKey = value
        }

    val BookPreferenceKey.key
        get() = if (isCurrentBookOption) bookKey else prefKey

    fun Preference.persistValue(value: String) {
        if (!orionState.onSetInitialValue) {
            OrionPreferenceUtil.persistValue(
                    this.context.applicationContext as OrionApplication, orionKey.bookKey, value
            )
        }
    }
}