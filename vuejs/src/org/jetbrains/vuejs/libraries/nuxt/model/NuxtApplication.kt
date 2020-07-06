// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.libraries.nuxt.model

import com.intellij.psi.PsiDirectory
import org.jetbrains.vuejs.libraries.vuex.model.store.VuexStore

interface NuxtApplication {

  fun getVuexStore(): VuexStore?

  fun getStaticResourcesDir(): PsiDirectory?

}