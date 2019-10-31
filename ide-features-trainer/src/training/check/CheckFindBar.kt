/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package training.check

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class CheckFindBar : Check {
  internal var project: Project? = null
  internal var editor: Editor? = null

  override fun set(project: Project, editor: Editor) {
    this.project = project
    this.editor = editor
  }

  override fun before() {}
  
  override fun check(): Boolean {
    return editor!!.headerComponent == null
  }

  override fun listenAllKeys(): Boolean {
    return false
  }
}