/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
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

package com.waicool20.wai2k.views.tabs.preferences

import com.waicool20.wai2k.Wai2k
import javafx.scene.control.CheckBox
import javafx.scene.layout.VBox
import tornadofx.*

class ConsolePrefView : View() {
    override val root: VBox by fxml("/views/tabs/preferences/console.fxml")
    private val clearConsoleOnStartCheckBox: CheckBox by fxid()
    private val showConsoleOnStartCheckBox: CheckBox by fxid()

    override fun onDock() {
        super.onDock()
        Wai2k.config.apply {
            clearConsoleOnStartCheckBox.bind(clearConsoleOnStartProperty)
            showConsoleOnStartCheckBox.bind(showConsoleOnStartProperty)
        }
    }
}
